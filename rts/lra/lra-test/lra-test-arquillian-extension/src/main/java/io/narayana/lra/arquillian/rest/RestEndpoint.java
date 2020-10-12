package io.narayana.lra.arquillian.rest;

import org.eclipse.microprofile.lra.annotation.LRAStatus;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Path(RestEndpoint.NARAYANA_LRA_RECOVERY_EXTENSION_PATH_NAME)
public class RestEndpoint {
    public static final String NARAYANA_LRA_RECOVERY_EXTENSION_PATH_NAME = "narayana-lra-recovery-extension";

    public static final Queue<String> ids = new ConcurrentLinkedQueue<>();

    @PUT
    public void request(@HeaderParam(org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId, LRAStatus status) {
        ids.offer(lraId.toASCIIString());
    }
}
