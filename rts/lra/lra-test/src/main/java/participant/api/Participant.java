package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.Compensate;
import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import org.jboss.narayana.rts.lra.compensator.api.Complete;
import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.Leave;
import org.jboss.narayana.rts.lra.compensator.api.Status;
import org.jboss.narayana.rts.lra.coordinator.api.InvalidLRAId;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

@RequestScoped
public abstract class Participant {
    @Context
    private UriInfo context;

    @Context
    private HttpServletRequest httpRequest;

    private Map<String, CompensatorStatus> compensatorStatusMap;

    /**
     * Tell the compensator to move to the requested state.
     *
     * @param status the next state to move to
     * @param activityId the current LRA context
     * @return the state that compensator achieved
     */
    protected abstract CompensatorStatus updateCompensator(CompensatorStatus status, String activityId);

    /**
     * Get the LRA context of the currently running method.
     * Note that @HeaderParam(LRA_HTTP_HEADER) does not match the header (done't know why) so we the httpRequest
     *
     * @return the LRA context of the currently running method
     */
    protected String getCurrentActivityId() {
        return httpRequest.getHeader(LRA_HTTP_HEADER);
    }

    @POST
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    public Response completeWork() throws NotFoundException {
        return updateState(CompensatorStatus.Completed, getCurrentActivityId());
    }

    @POST
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork() throws NotFoundException {
        return updateState(CompensatorStatus.Compensated, getCurrentActivityId());
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response status() throws NotFoundException {
        String lraId = getCurrentActivityId();

        if (lraId == null)
            throw new InvalidLRAId("null", "not present on Compensator#status request", null);

        if (!compensatorStatusMap.containsKey(lraId))
            throw new InvalidLRAId(lraId, "Compensator#status request: unknown lra id", null);

        return Response.ok(compensatorStatusMap.get(getCurrentActivityId())).build();
    }

    @PUT
    @Path("/leave")
    @Produces(MediaType.APPLICATION_JSON)
    @Leave
    public Response leaveWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        return Response.ok().build();
    }

    @PostConstruct
    public void postConstruct() {
        compensatorStatusMap = new HashMap<>();
    }

    private Response updateState(CompensatorStatus status, String activityId) {
        CompensatorStatus newStatus = updateCompensator(status, activityId);

        compensatorStatusMap.put(activityId, newStatus); // NB in the demo we never remove completed activities

        return Response.ok(getStatusUrl(activityId)).build();
    }

    private String getStatusUrl(String lraId) {
        return String.format("%s/%s/activity/status", context.getBaseUri(), LRAClient.getLRAId(lraId));
    }
}
