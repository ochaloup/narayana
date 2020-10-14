package io.narayana.lra.arquillian.rest;

import org.eclipse.microprofile.lra.annotation.LRAStatus;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Path(RestEndpoint.NARAYANA_LRA_RECOVERY_EXTENSION_PATH_NAME)
public class RestEndpoint {
    public static final String NARAYANA_LRA_RECOVERY_EXTENSION_PATH_NAME = "narayana-lra-recovery-extension";

    public static final Queue<String> ids = new ConcurrentLinkedQueue<>();

    @GET
    @Path("/")
    public void compensate(@HeaderParam(org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId) {
        ids.offer(lraId.toASCIIString());
    }

    @PUT
    @Path("/")
    public void afterInvocation(@HeaderParam(org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId, LRAStatus status) {
        ids.add(lraId.toASCIIString());
    }

    @GET
    @Path("{LraId}")
    @Produces(value = MediaType.TEXT_PLAIN)
    public boolean obtain(@PathParam("lraId") String lraId) {
        return ids.remove(lraId);
    }
}
