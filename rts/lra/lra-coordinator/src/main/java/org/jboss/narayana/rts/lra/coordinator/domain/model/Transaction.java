package org.jboss.narayana.rts.lra.coordinator.domain.model;

import com.arjuna.ats.arjuna.coordinator.AbstractRecord;
import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.arjuna.ats.arjuna.coordinator.BasicAction;
import com.arjuna.ats.arjuna.coordinator.RecordList;
import com.arjuna.ats.arjuna.coordinator.RecordListIterator;
import com.arjuna.ats.internal.arjuna.thread.ThreadActionData;
import org.jboss.jbossts.star.resource.RESTRecord;
import org.jboss.jbossts.star.util.TxStatus;
import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;

import java.util.ArrayList;
import java.util.List;

public class Transaction extends org.jboss.jbossts.star.resource.Transaction {
    private final String id;
    private final String parentId; // TODO save_state and restore_state
    private final String clientId;
    private List<LRARecord> pending;
    private CompensatorStatus status; // reuse commpensator states for the LRA

    public Transaction(String baseUrl, String parentId, String clientId) {
        super();

        this.id = String.format("%s/%s", baseUrl, get_uid().fileStringForm());
        this.parentId = parentId;
        this.clientId = clientId;

        status = CompensatorStatus.Active;
    }

    public String getId() {
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

        if (nested)
            savePendingList();

        if ((res != ActionStatus.RUNNING) && (res != ActionStatus.ABORT_ONLY)) {
            if (nested && compensate) {
                /*
                 * TODO this is wrong - we should be hooking into ActionType.NESTED
                 * Unfortunatly that means that after a nested txn is committed its participants are merged
                 * with the parent and they can then only be aborted if the parent aborts whereas in
                 * the LRA model nested LRAs can be compensated whilst the enclosing LRA is completed
                 */

                // repopulate the pending list TODO it won't neccessarily be present during recovery
                pendingList = new RecordList();

                pending.forEach(r -> pendingList.putRear(r));

                super.phase2Abort(true);

                status = toLRAStatus(status());
            }

            return res;
        }

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

        status = toLRAStatus(res);

        return res;
    }

    private CompensatorStatus toLRAStatus(int atomicActionStatues) {
        switch (atomicActionStatues) {
            case ActionStatus.ABORTING:
                return CompensatorStatus.Compensating;
            case ActionStatus.ABORT_ONLY:
                return CompensatorStatus.Compensating;
            case ActionStatus.ABORTED:
                return CompensatorStatus.Compensating;
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

    public String enlistParticipant(String coordinatorUrl, String participantUrl, String recoveryUrlBase) {
        String coordinatorId = super.enlistParticipant(coordinatorUrl, participantUrl, recoveryUrlBase, null);

        if (coordinatorId != null) { // null means the enlist was rejected - probably because the end protocol has started
            RESTRecord participant = findParticipant(participantUrl);

            // need to remember that there is a new participant
            deactivate(); // if it fails the superclass will have logged a warning
        }

        return coordinatorId;
    }

    public String enlistParticipants(String coordinatorUrl, String compensateURI, String recoveryUrlBase) {
        String coordinatorId = super.enlistParticipant(coordinatorUrl, compensateURI, recoveryUrlBase, null);

        if (coordinatorId == null) {
            // null means either the compenstaor is already regitered or the enlist was rejected (probably because the end protocol has started)
            RESTRecord rr = findParticipant(compensateURI);

            if (rr != null)
                return rr.get_uid().fileStringForm();
        }

        RESTRecord participant = findParticipant(compensateURI);

        // need to remember that there is a new participant
        deactivate(); // if it fails the superclass will have logged a warning

        return coordinatorId;
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
        return parentId == null || parentId.isEmpty();
    }

    public BasicAction currentLRA() {
        return ThreadActionData.currentAction();
    }
}
