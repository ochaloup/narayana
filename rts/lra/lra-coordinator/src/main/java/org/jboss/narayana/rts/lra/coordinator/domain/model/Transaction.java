/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.narayana.rts.lra.coordinator.domain.model;

import com.arjuna.ats.arjuna.coordinator.AbstractRecord;
import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.arjuna.ats.arjuna.coordinator.BasicAction;
import com.arjuna.ats.arjuna.coordinator.RecordList;
import com.arjuna.ats.arjuna.coordinator.RecordListIterator;
import com.arjuna.ats.internal.arjuna.thread.ThreadActionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jbossts.star.resource.RESTRecord;
import org.jboss.jbossts.star.util.TxStatus;
import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Transaction extends org.jboss.jbossts.star.resource.Transaction {
    private final URL id;
    private final URL parentId; // TODO save_state and restore_state
    private final String clientId;
    private List<LRARecord> pending;
    private CompensatorStatus status; // reuse commpensator states for the LRA
    private List<String> responseData;

    public Transaction(String baseUrl, URL parentId, String clientId) throws MalformedURLException {
        super();

        this.id = new URL(String.format("%s/%s", baseUrl, get_uid().fileStringForm()));
        this.parentId = parentId;
        this.clientId = clientId;

        status = CompensatorStatus.Active;
    }

    public URL getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public CompensatorStatus getLRAStatus() {
        return status;
    }

    public boolean isComplete() {
        return status.equals(CompensatorStatus.Completed);
    }

    public boolean isCompensated() {
        return status.equals(CompensatorStatus.Compensated);
    }

    public boolean isRecovering() {
        return false;
    } // TODO

    // in this version close need to run as blocking code {@link Vertx().executeBlocking}
    public int end(/*Vertx vertx,*/ boolean compensate) {
        int res = status();
        boolean nested = !isTopLevel();

        // nested compensators need to be remembered in case the enclosing LRA decides to compensate
        // also save the list so that we can retrieve any response data after committing compensators
//        if (nested)
        savePendingList();

        if ((res != ActionStatus.RUNNING) && (res != ActionStatus.ABORT_ONLY)) {
            if (nested && compensate) {
                /*
                 * TODO this is wrong - we should be hooking into ActionType.NESTED ... but
                 * Unfortunatly that means that after a nested txn is committed its participants are merged
                 * with the parent and they can then only be aborted if the parent aborts whereas in
                 * the LRA model nested LRAs can be compensated whilst the enclosing LRA is completed
                 */

                // repopulate the pending list TODO it won't neccessarily be present during recovery
                pendingList = new RecordList();

                pending.forEach(r -> pendingList.putRear(r));

                super.phase2Abort(true);

                res = status();

                status = toLRAStatus(status());
            }
        } else {

//        if (!status.equals(CompensatorStatus.Active))
//            return status();


            if (compensate || status() == ActionStatus.ABORT_ONLY) {
                status = CompensatorStatus.Compensating;

                // compensators must be called in reverse order so reverse the pending list
                int sz = pendingList == null ? 0 : pendingList.size();

                if (sz > 0) {
                    for (int i = sz - 1; i > 0; i--) {
                        pendingList.putRear(pendingList.getFront());
                    }
                }

                // tell each compensator that the lra canceled - use commit since we need recovery for compensation actions
                res = super.abort();
            } else {
                status = CompensatorStatus.Completing;

                // tell each compensator that the lra completed ok
                res = super.commit(false);
            }
        }

        // gather up any response data
        if (pending != null && pending.size() != 0) {
            responseData = pending.stream()
                    .map(LRARecord::getResponseData)
                    .collect(Collectors.toList());

            // some compensators may be for nested LRAs so their response data will be an encoded array
            // - let the client handle this case (since a busisness logic compensator can legitimately encode
            // an array as his business data)
            // TODO use a flat map to do it all in one go
            List<String> flattenedData = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();

            responseData.forEach(s -> {
                if (s.startsWith("[")) {
                    try {
                        String[] ja = mapper.readValue(s, String[].class);
                        // TODO should reccurse here since the encoded strings may themselves contain compensator output
                        // TODO fixit
                        flattenedData.addAll(Arrays.asList(ja));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    flattenedData.add(s);
                }
            });

            if (flattenedData.size() != 0) {
                responseData.clear();
                responseData.addAll(flattenedData);
            }

            if (!nested)
                pending.clear(); // TODO we will loose this data if we need recovery
        }

        status = toLRAStatus(res);

        return res;
    }

    private CompensatorStatus toLRAStatus(int atomicActionStatus) {
        switch (atomicActionStatus) {
            case ActionStatus.ABORTING:
                return CompensatorStatus.Compensating;
            case ActionStatus.ABORT_ONLY:
                return CompensatorStatus.Compensating;
            case ActionStatus.ABORTED:
                return CompensatorStatus.Compensated;
            case ActionStatus.COMMITTING:
                return CompensatorStatus.Completing;
            case ActionStatus.COMMITTED:
                return CompensatorStatus.Completed;
            case ActionStatus.H_ROLLBACK:
                return CompensatorStatus.Compensated;
            default:
                return CompensatorStatus.Active;
        }
    }

    @Override
    protected RESTRecord getParticipantRecord(String txId, String coordinatorUrl, String participantUrl, String terminateUrl, String recoveryUrlBase) {
        return new LRARecord(txId, coordinatorUrl, participantUrl);
    }

    public String enlistParticipant(URL coordinatorUrl, String participantUrl, String recoveryUrlBase) {
        RESTRecord participant = findParticipant(participantUrl);

        if (participant != null)
            return participant.get_uid().fileStringForm(); // must have already been enlisted

        // TODO remove dependency on REST-AT since it deosn't add much
        String coordinatorId = super.enlistParticipant(coordinatorUrl.toString(), participantUrl, recoveryUrlBase, null);

        if (coordinatorId != null) { // null means the enlist was rejected - probably because  it is already enlisted or the end protocol has started
            participant = findParticipant(participantUrl);

            // need to remember that there is a new participant
            deactivate(); // if it fails the superclass will have logged a warning
            return coordinatorId;
        }

        return null;
    }

    public String enlistParticipants(URL coordinatorUrl, String compensateURI, String recoveryUrlBase) {
        return enlistParticipant(coordinatorUrl, compensateURI, recoveryUrlBase);

/*        String coordinatorId = super.enlistParticipant(coordinatorUrl, compensateURI, recoveryUrlBase, null);

        if (coordinatorId == null) {
            // null means either the compenstaor is already regitered or the enlist was rejected (probably because the end protocol has started)
            RESTRecord rr = findParticipant(compensateURI);

            if (rr != null)
                return rr.get_uid().fileStringForm();
        }

        RESTRecord participant = findParticipant(compensateURI);

        // need to remember that there is a new participant
        deactivate(); // if it fails the superclass will have logged a warning

        return coordinatorId;*/
    }

    public Boolean isActive() {
        return TxStatus.fromActionStatus(status()).isActive();
    }

    public boolean forgetParticipant(String participantUrl) {
        return pendingList == null || pendingList.size() == 0 || super.forgetParticipant(participantUrl);

    }

    public void forgetAllParticipants() {
        if (pending != null)
            pending.forEach(LRARecord::forget);
    }

    private void savePendingList() {
        if (pendingList == null || pending != null)
            return;

        RecordListIterator i = new RecordListIterator(pendingList);
        AbstractRecord r;

        pending = new ArrayList<>();

        while ((r = i.iterate()) != null) {
            if (r instanceof LRARecord) {
                pending.add((LRARecord) r);
            }
        }
    }

    protected RESTRecord findParticipant(String participantUrl) {
        if (pendingList != null) {

            RecordListIterator i = new RecordListIterator(pendingList);
            AbstractRecord r;

            if (participantUrl.indexOf(',') != -1)
                participantUrl = LRARecord.cannonicalForm(participantUrl);

            while ((r = i.iterate()) != null) {
                if (r instanceof LRARecord) {
                    LRARecord rr = (LRARecord) r;
                    // can't use == because this may be a recovery scenario
                    if (rr.getParticipantPath().equals(participantUrl))
                        return rr;
                }
            }
        }

        return null;
    }

    public boolean isTopLevel() {
        return parentId == null;
    }

    public List<String> getResponseData() {
        return responseData;
    }

    public BasicAction currentLRA() {
        return ThreadActionData.currentAction();
    }

    public int getHttpStatus() {
        switch (status()) {
            case ActionStatus.COMMITTED:
            case ActionStatus.ABORTED:
                return 200;
            default: // TODO return a more comprehensive mapping between states
                return 500;
        }
    }
}
