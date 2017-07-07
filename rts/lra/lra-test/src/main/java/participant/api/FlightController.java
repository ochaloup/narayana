package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.NestedLRA;
import participant.filter.model.Booking;
import participant.filter.model.BookingStatus;
import participant.filter.service.FlightService;

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
@Path(FlightController.FLIGHT_PATH)
@LRA(LRA.Type.SUPPORTS)
public class FlightController extends Participant {
    public static final String FLIGHT_PATH = "/flight";
    public static final String FLIGHT_NUMBER_PARAM = "flightNumber";
    public static final String ALT_FLIGHT_NUMBER_PARAM = "altFlightNumber";
    public static final String FLIGHT_SEATS_PARAM = "flightSeats";

    @Inject
    private FlightService flightService;

    @POST
    @Path("/bookasync")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.REQUIRED)
    @NestedLRA
    public void bookFlightAsync(@Suspended final AsyncResponse asyncResponse,
                                @HeaderParam(LRA_HTTP_HEADER) String lraId,
                                @QueryParam(FLIGHT_NUMBER_PARAM) @DefaultValue("") String flightNumber,
                                @QueryParam(FLIGHT_SEATS_PARAM) @DefaultValue("1") Integer seats,
                                @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        flightService.bookAsync(lraId, flightNumber, seats)
                .thenApply(asyncResponse::resume)
                .exceptionally(e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

        asyncResponse.setTimeout(timeout, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Response.status(SERVICE_UNAVAILABLE).entity("Operation timed out").build()));
    }

    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.REQUIRED)
    @NestedLRA
    public Booking bookFlight(@HeaderParam(LRA_HTTP_HEADER) String lraId,
                              @QueryParam(FLIGHT_NUMBER_PARAM) @DefaultValue("") String flightNumber,
                              @QueryParam(FLIGHT_SEATS_PARAM) @DefaultValue("1") Integer seats,
                              @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        return flightService.book(lraId, flightNumber, seats);
    }

    @GET
    @Path("/info/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.SUPPORTS)
    public Booking getBooking(@PathParam("bookingId") String bookingId) {
        return flightService.get(bookingId);
    }

    @Override
    protected CompensatorStatus updateCompensator(CompensatorStatus status, String bookingId) {
        switch (status) {
            case Completed:
                flightService.updateBookingStatus(bookingId, BookingStatus.CONFIRMED);
                return status;
            case Compensated:
                flightService.updateBookingStatus(bookingId, BookingStatus.CANCELLED);
                return status;
            default:
                return status;
        }
    }
}
