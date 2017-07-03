package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.LRA;

import participant.filter.service.HotelService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@ApplicationScoped
@Path(HotelController.HOTEL_PATH)
@LRA(LRA.LRAType.SUPPORTS)
public class HotelController extends Participant {
    static final String HOTEL_PATH = "/hotel";
    static final String HOTEL_NAME_PARAM = "hotelName";
    static final String HOTEL_BEDS_PARAM = "beds";

    @Inject
    private HotelService hotelService;

    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.REQUIRED)
    public void bookRoom(@Suspended final AsyncResponse asyncResponse,
                         @QueryParam(HOTEL_NAME_PARAM) @DefaultValue("Default") String hotelName,
                         @QueryParam(HOTEL_BEDS_PARAM) @DefaultValue("1") Integer beds,
                         @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        hotelService.bookAsync(hotelName, beds)
                .thenApply(asyncResponse::resume)
                .exceptionally(e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

        asyncResponse.setTimeout(timeout, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Response.status(SERVICE_UNAVAILABLE).entity("Operation timed out").build()));
    }
}
