package org.jboss.narayana.rts.lra.coordinator.api;

import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;

public class LRAState {

    public static final int DEFAULT_COORDINATOR_PORT = 8080; // TODO

    private static final ThreadLocal<LRAState> lraContext = new ThreadLocal<>();

    public static LRAState getCurrentLRA() {
        return lraContext.get();
    }

    public static void setCurrentLRA(String lra) {
        lraContext.set(new LRAState(lra));
    }

    public static void setCurrentLRA(LRAState lra) {
        lraContext.set(lra);
    }

    public static LRAState clearCurrentLRA() {
        LRAState state = lraContext.get();

        lraContext.set(null);

        return state;
    }

    public String id;
    public String coordinatorBaseUrl;
    public String rcvUrl;
    public String statusUrl;
    public CompensatorStatus status;
    public boolean registered;
    public String registrationStatus;
    public boolean inClientsLRA;

/*    public Activity(String id, String rcvUrl, String statusUrl, Status status, boolean registered, String registrationStatus) {
        this.id = id;
        this.rcvUrl = rcvUrl;
        this.statusUrl = statusUrl;
        this.status = status;
        this.registered = registered;
        this.registrationStatus = registrationStatus;
    }*/

    public LRAState(String lraId) {
        this(lraId, false, null);
    }


    public LRAState(String lraId, boolean inClientsLRA, String baseUrl) {
        this.id = lraId;
        this.inClientsLRA = inClientsLRA;
        this.coordinatorBaseUrl = baseUrl;
    }
}
