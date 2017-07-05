package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.Compensate;
import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import org.jboss.narayana.rts.lra.compensator.api.Complete;
import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.Leave;
import org.jboss.narayana.rts.lra.compensator.api.Status;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

public abstract class Participant {
    @Context
    private UriInfo context;

    @Context
    private HttpServletRequest httpRequest;

    private CompensatorStatus status = CompensatorStatus.Active;

    /**
     * Tell the compensator to move to the requested state.
     *
     * @param status the next state to move to
     * @param activityId the current LRA context
     * @return the state that compensator achieved
     */
    protected abstract CompensatorStatus updateCompensator(CompensatorStatus status, String activityId);

    /**
     * Get the LRA context of the currently running method
     * @return the LRA context of the currently running method
     */
    protected String getCurrentActivityId() {
        return httpRequest.getHeader(LRA_HTTP_HEADER);
    }

    @POST
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        return updateState(CompensatorStatus.Completing, lraId);
    }

    @POST
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        return updateState(CompensatorStatus.Compensating, lraId);
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response status(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        System.out.printf("%s: Participant status request%n", getClass().getName());

        return Response.ok(status).build();
    }

    @PUT
    @Path("/leave")
    @Produces(MediaType.APPLICATION_JSON)
    @Leave
    public Response leaveWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        return Response.ok().build();
    }

    private Response updateState(CompensatorStatus status, String activityId) {
        this.status = updateCompensator(CompensatorStatus.Completed, activityId);

        return Response.ok(getStatusUrl(activityId)).build();
    }

    private String getStatusUrl(String lraId) {
        return String.format("%s/%s/activity/compensated", context.getBaseUri(), LRAClient.getLRAId(lraId));
    }
}
