package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import org.jboss.narayana.rts.lra.compensator.api.LRA;

import participant.filter.model.Booking;
import participant.filter.model.BookingStatus;
import participant.filter.service.HotelService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

//@ApplicationScoped
@RequestScoped
@Path(HotelController.HOTEL_PATH)
@LRA(LRA.LRAType.SUPPORTS)
public class HotelController extends Participant {
    public static final String HOTEL_PATH = "/hotel";
    public static final String HOTEL_NAME_PARAM = "hotelName";
    public static final String HOTEL_BEDS_PARAM = "beds";

    @Inject
    private HotelService hotelService;

    @POST
    @Path("/bookasync")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.REQUIRED)
    public void bookRoomAsync(@Suspended final AsyncResponse asyncResponse,
                              @QueryParam(HOTEL_NAME_PARAM) @DefaultValue("Default") String hotelName,
                              @QueryParam(HOTEL_BEDS_PARAM) @DefaultValue("1") Integer beds,
                              @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        hotelService.bookAsync(getCurrentActivityId(), hotelName, beds)
                .thenApply(asyncResponse::resume)
                .exceptionally(e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

        asyncResponse.setTimeout(timeout, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Response.status(SERVICE_UNAVAILABLE).entity("Operation timed out").build()));
    }

    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.REQUIRED)
    public Booking bookRoom(@QueryParam(HOTEL_NAME_PARAM) @DefaultValue("Default") String hotelName,
                            @QueryParam(HOTEL_BEDS_PARAM) @DefaultValue("1") Integer beds,
                            @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        return hotelService.book(getCurrentActivityId(), hotelName, beds);
    }

    @GET
    @Path("/info/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.SUPPORTS)
    public Booking getBooking(@PathParam("bookingId") String bookingId) {
        return hotelService.get(bookingId);
    }

    @Override
    protected CompensatorStatus updateCompensator(CompensatorStatus status, String bookingId) {
        switch (status) {
            case Completed:
                hotelService.updateBookingStatus(bookingId, BookingStatus.CONFIRMED);
                return status;
            case Compensated:
                hotelService.updateBookingStatus(bookingId, BookingStatus.CANCELLED);
                return status;
            default:
                return status;
        }
    }
}
