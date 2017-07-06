package org.jboss.narayana.rts.lra.coordinator.api;

import javax.ws.rs.WebApplicationException;

public class InvalidLRAId extends WebApplicationException {
    private String lraId;

    public InvalidLRAId(String lraId, String message, Throwable cause) {
        super(String.format("%s: %s", lraId, message), cause);
    }
}
