package participant.filter.model;

import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;

import java.io.Serializable;

public class Activity implements Serializable {

    public String id;
    public String rcvUrl;
    public String statusUrl;
    public CompensatorStatus status;
    public boolean registered;
    public String registrationStatus;

    public Activity(String txId) {
        this.id = txId;
    }
}
