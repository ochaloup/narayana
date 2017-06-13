package org.jboss.narayana.rts.lra.coordinator.domain.model;

import com.arjuna.ats.arjuna.coordinator.AbstractRecord;
import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.arjuna.ats.arjuna.coordinator.RecordListIterator;
import org.jboss.jbossts.star.resource.RESTRecord;
import org.jboss.jbossts.star.util.TxStatus;

public class Transaction extends org.jboss.jbossts.star.resource.Transaction {
    private final String id;
    private final String clientId;

    public Transaction() {
        this(null, null);
    }

    public Transaction(String baseUrl, String clientId) {
        super();

        this.id = String.format("%s/%s", baseUrl, get_uid().fileStringForm());
        this.clientId = clientId;
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isComplete() {
        return status() == ActionStatus.COMMITTED;
    }

    public boolean isCompensated() {
        return status() == ActionStatus.ABORTED;
    }

    public boolean isRecovering() {
        return false;
    } // TODO

    // in this version close need to run as blocking code {@link Vertx().executeBlocking}
    public int end(/*Vertx vertx,*/ boolean compensate) {

        if (compensate || status() == ActionStatus.ABORT_ONLY) {
            // compensators must be called in reverse order so reverse the pending list
            int sz = pendingList == null ? 0 : pendingList.size();

            if (sz > 0) {
                for (int i = sz - 1; i > 0; i--) {
                    pendingList.putRear(pendingList.getFront());
                }
            }

            // tell each compensator that the lra canceled - use commit since we need recovery for compensation actions
            return super.commit(true);
        } else {
            // tell each compensator that the lra completed ok
            return super.abort();
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
        if (pendingList == null || pendingList.size() == 0)
            return true;

        return super.forgetParticipant(participantUrl);
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
}
