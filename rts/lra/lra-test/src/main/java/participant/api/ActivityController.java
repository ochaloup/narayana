package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.Compensate;
import org.jboss.narayana.rts.lra.compensator.api.Complete;
import org.jboss.narayana.rts.lra.compensator.api.Leave;
import org.jboss.narayana.rts.lra.compensator.api.Status;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClientAPI;
import participant.filter.model.Activity;
import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import participant.filter.service.ActivityService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_RECOVERY_HEADER;

@ApplicationScoped
@Path("/activities")
@LRA(LRA.LRAType.SUPPORTS)
public class ActivityController {

    @Inject
    private LRAClientAPI lraClient;

    private static final AtomicInteger completedCount = new AtomicInteger(0);
    private static final AtomicInteger compensatedCount = new AtomicInteger(0);

    @Context
    private UriInfo context;

    @Inject
    private ActivityService activityService;

    /**
     Performing a GET on the compensator URL will return the current status of the compensator {@link CompensatorStatus}, or 404 if the compensator is no longer present.
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response status(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        String txId = LRAClient.getLRAId(lraId);
        Activity activity = activityService.getActivity(txId);

        if (activity.status == null)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        return Response.ok(activity.status.name()).build();
    }

    /**
     * Test that participants can leave an LRA using the {@link LRAClientAPI} programatic API
     * @param lraId
     * @return
     * @throws NotFoundException
     */
    @PUT
    @Path("/leave/{LraId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response leaveWorkViaAPI(@PathParam("LraId")String lraId) throws NotFoundException {

        if (lraId != null) {
            lraClient.leaveLRA(lraId, Testing.getCompensatorUrl(context.getBaseUri(), this.getClass()));
            activityService.getActivity(lraId);

            activityService.remove(lraId);

            return Response.ok(lraId).build();
        }

        return Response.ok("non transactional").build();
    }

    @PUT
    @Path("/leave")
    @Produces(MediaType.APPLICATION_JSON)
    @Leave
    public Response leaveWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        String txId = LRAClient.getLRAId(lraId);

        if (txId != null) {
            activityService.getActivity(txId);

            activityService.remove(txId);

            return Response.ok(txId).build();
        }

        return Response.ok("non transactional").build();
    }

    @POST
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        completedCount.incrementAndGet();

        String txId = LRAClient.getLRAId(lraId);
        Activity activity = activityService.getActivity(txId);

        activity.status = CompensatorStatus.Completed;
        activity.statusUrl = String.format("%s/%s/activity/completed", context.getBaseUri(), txId);

        System.out.printf("ActivityController completing %s%n", txId);
        return Response.ok(activity.statusUrl).build();
    }

    @POST
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        compensatedCount.incrementAndGet();

        String txId = LRAClient.getLRAId(lraId);
        Activity activity = activityService.getActivity(txId);

        activity.status = CompensatorStatus.Compensated;
        activity.statusUrl = String.format("%s/%s/activity/compensated", context.getBaseUri(), txId);

        System.out.printf("ActivityController compensating %s%n", txId);
        return Response.ok(activity.statusUrl).build();
    }

    @PUT
    @Path("/work")
    @LRA(LRA.LRAType.REQUIRED)
    public Response activityWithLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
                                    @HeaderParam(LRA_HTTP_HEADER) String lraId) {
        String txId = LRAClient.getLRAId(lraId);

        System.out.printf("ActivityController: work id %s and rcvId %s %n", txId, rcvId);

        if (txId == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing transaction data").build();

        try {
            activityService.getActivity(txId);
        } catch (NotFoundException e) {
            Activity activity = new Activity(txId);

            activity.rcvUrl = rcvId;
            activity.status = null;

            activityService.add(activity);
        }

        return Response.ok(lraId).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response findAll() {
        List<Activity> results = activityService.findAll();

        return Response.ok(results.size()).build();
    }

    @GET
    @Path("/stats/completed")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response getCompleteCount() {
        return Response.ok(completedCount.get()).build();
    }
    @GET
    @Path("/stats/compensated")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response getCompensatedCount() {
        return Response.ok(compensatedCount.get()).build();
    }

    /**
     * Performing a POST on <compensator URL>/compensate will cause the participant to compensate
     * the work that was done within the scope of the transaction.
     *
     * The compensator will either return a 200 OK code and a <status URL> which indicates the outcome and which can be probed (via GET)
     * and will simply return the same (implicit) information:
     *
     * <URL>/cannot-compensate
     * <URL>/cannot-complete
     */
    @POST
    @Path("/{TxId}/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response compensate(@PathParam("TxId")String txId) throws NotFoundException {
        Activity activity = activityService.getActivity(txId);

        activity.status = CompensatorStatus.Compensated;
        activity.statusUrl = String.format("%s/%s/activity/compensated", context.getBaseUri(), txId);

        return Response.ok(activity.statusUrl).build();
    }

    /**
     * Performing a POST on <compensator URL>/complete will cause the participant to tidy up and it can forget this transaction.
     *
     * The compensator will either return a 200 OK code and a <status URL> which indicates the outcome and which can be probed (via GET)
     * and will simply return the same (implicit) information:
     * <URL>/cannot-compensate
     * <URL>/cannot-complete
     */
    @POST
    @Path("/{TxId}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response complete(@PathParam("TxId")String txId) throws NotFoundException {
        Activity activity = activityService.getActivity(txId);

        activity.status = CompensatorStatus.Completed;
        activity.statusUrl = String.format("%s/%s/activity/completed", context.getBaseUri(), txId);

        return Response.ok(activity.statusUrl).build();
    }

    @POST
    @Path("/{TxId}/forget")
    public void forget(@PathParam("TxId")String txId) throws NotFoundException {
        Activity activity = activityService.getActivity(txId);

        activityService.remove(activity.id);
    }

    @GET
    @Path("/{TxId}/completed")
    @Produces(MediaType.APPLICATION_JSON)
    public String completedStatus(@PathParam("TxId")String txId) {
        return CompensatorStatus.Completed.name();
    }

    @GET
    @Path("/{TxId}/compensated")
    @Produces(MediaType.APPLICATION_JSON)
    public String compensatedStatus(@PathParam("TxId")String txId) {
        return CompensatorStatus.Compensated.name();
    }
}
