package participant.filter;

import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;

class LRAState {

    private static final ThreadLocal<LRAState> lraContext = new ThreadLocal<>();

    static LRAState getCurrentLRA() {
        return lraContext.get();
    }

    static void setCurrentLRA(String lra) {
        lraContext.set(new LRAState(lra));
    }

    static void setCurrentLRA(LRAState lra) {
        lraContext.set(lra);
    }

    static LRAState clearCurrentLRA() {
        LRAState state = lraContext.get();

        lraContext.set(null);

        return state;
    }

    String id;
    String lraToRestore;
    String coordinatorBaseUrl;
    String rcvUrl;
    String statusUrl;
    CompensatorStatus status;
    boolean registered;
    String registrationStatus;
    String participantUrl;
    boolean inOurLRA;

/*    public Activity(String id, String rcvUrl, String statusUrl, Status status, boolean registered, String registrationStatus) {
        this.id = id;
        this.rcvUrl = rcvUrl;
        this.statusUrl = statusUrl;
        this.status = status;
        this.registered = registered;
        this.registrationStatus = registrationStatus;
    }*/


    LRAState(String txId) {
        this(txId, false, null);
    }


    LRAState(String txId, boolean inOurLRA, String baseUrl) {
        this.id = this.lraToRestore = txId;
        this.inOurLRA = inOurLRA;
        this.coordinatorBaseUrl = baseUrl;
    }
}
