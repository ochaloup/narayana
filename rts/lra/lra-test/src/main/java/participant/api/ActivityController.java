package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.Compensate;
import org.jboss.narayana.rts.lra.compensator.api.Complete;
import org.jboss.narayana.rts.lra.compensator.api.Leave;
import org.jboss.narayana.rts.lra.compensator.api.NestedLRA;
import org.jboss.narayana.rts.lra.compensator.api.Status;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClientAPI;
import participant.filter.model.Activity;
import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import participant.filter.service.ActivityService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_RECOVERY_HEADER;

@ApplicationScoped
@Path("/activities")
@LRA(LRA.LRAType.SUPPORTS)
public class ActivityController {

    @Inject
    private LRAClient lraClient;

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
     * @param lraUrl
     * @return
     * @throws NotFoundException
     */
    @PUT
    @Path("/leave/{LraUrl}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response leaveWorkViaAPI(@PathParam("LraUrl")String lraUrl) throws NotFoundException, MalformedURLException {

        if (lraUrl != null) {
            String lraId = LRAClient.getLRAId(lraUrl);

            lraClient.leaveLRA(new URL(lraUrl), Testing.getCompensatorUrl(context.getBaseUri(), this.getClass()));
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
    @Path("/supports")
    @LRA(LRA.LRAType.SUPPORTS)
    public Response supportsLRACall(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        addWork(lraId, null);

        return Response.ok(lraId == null ? "" : lraId).build();
    }

    @PUT
    @Path("/startviaapi")
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response subActivity(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        if (lraId != null)
            throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE);

        // manually start an LRA via the injection LRAClient api
        URL lra = lraClient.startLRA("subActivity", 0);

        lraId = lra.toString();

        addWork(lraId, null);

        // invoke a method that SUPPORTS LRAs. The filters should detect the LRA we just started via the injected client
        // and add it as a header before calling the method at path /supports (ie supportsLRACall()).
        // The supportsLRACall method will return LRA id in the body if it is present.
        String id = restPutInvocation("supports", "");

        // check that the invoked method saw the LRA
        if (lraId == null || id == null || !lraId.equals(id))
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Entity.text("Unequal LRA ids")).build();

        return Response.ok(id).build();
    }

    @PUT
    @Path("/work")
    @LRA(LRA.LRAType.REQUIRED)
    public Response activityWithLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
                                    @HeaderParam(LRA_HTTP_HEADER) String lraId) {
        Activity activity = addWork(lraId, rcvId);

        if (activity == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        return Response.ok(lraId).build();
    }

    private String restPutInvocation(String path, String bodyText) {
        String id = null;
        Response response = ClientBuilder.newClient().target(context.getBaseUri())
                .path("activities").path(path).request().put(Entity.text(bodyText));

        if (response.hasEntity())
            id = response.readEntity(String.class);

        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

        return id;
    }

    @PUT
    @Path("/nestedActivity")
    @LRA(LRA.LRAType.MANDATORY)
    @NestedLRA
    public Response nestedActivity(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
                                    @HeaderParam(LRA_HTTP_HEADER) String nestedLRAId) {
        Activity activity = addWork(nestedLRAId, rcvId);

        if (activity == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        return Response.ok(nestedLRAId).build();
    }

    @PUT
    @Path("/multiLevelNestedActivity")
    @LRA(LRA.LRAType.MANDATORY)
    public Response multiLevelNestedActivity(
            @HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
            @HeaderParam(LRA_HTTP_HEADER) String nestedLRAId,
            @QueryParam("nestedCnt") @DefaultValue("1") Integer nestedCnt) {
        Activity activity = addWork(nestedLRAId, rcvId);

        if (activity == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        // invoke resources that enlist nested LRAs
        String[] lras = new String[nestedCnt + 1];
        lras[0] = nestedLRAId;
        IntStream.range(1, lras.length).forEach(i -> lras[i] = restPutInvocation("nestedActivity", ""));

        return Response.ok(String.join(",", lras)).build();
    }

    private Activity addWork(String lraId, String rcvId) {
        String txId = LRAClient.getLRAId(lraId);

        System.out.printf("ActivityController: work id %s and rcvId %s %n", txId, rcvId);

        if (txId == null)
            return null;

        try {
            return activityService.getActivity(txId);
        } catch (NotFoundException e) {
            Activity activity = new Activity(txId);

            activity.rcvUrl = rcvId;
            activity.status = null;

            activityService.add(activity);

            return activity;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response findAll() {
        List<Activity> results = activityService.findAll();

        return Response.ok(results.size()).build();
    }

    @GET
    @Path("/completedactivitycount")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response getCompleteCount() {
        return Response.ok(completedCount.get()).build();
    }
    @GET
    @Path("/compensatedactivitycount")
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

    private void checkStatusAndClose(Response response, int expected) {
        try {
            if (response.getStatus() != expected)
                throw new WebApplicationException(response);
        } finally {
            response.close();
        }
    }
}
