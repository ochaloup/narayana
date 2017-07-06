package org.jboss.narayana.rts.lra.coordinator.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidLRAExceptionMapper implements ExceptionMapper<InvalidLRAId> {
    @Override
    public Response toResponse(InvalidLRAId exception) {
        return Response.status(Response.Status.NOT_ACCEPTABLE)
                .entity(String.format("Invalid LRA id: %s", exception.getMessage())).build();
    }
}
