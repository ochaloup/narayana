package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.NestedLRA;
import participant.filter.model.Booking;
import participant.filter.service.FlightService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
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

//@ApplicationScoped
@RequestScoped
@Path(FlightController.FLIGHT_PATH)
@LRA(LRA.LRAType.SUPPORTS)
public class FlightController extends Participant {
    static final String FLIGHT_PATH = "/flight";
    static final String FLIGHT_NUMBER_PARAM = "flightNumber";
    static final String FLIGHT_SEATS_PARAM = "flightSeats";

    @Inject
    private FlightService flightService;

    @POST
    @Path("/bookasync")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.REQUIRED)
    @NestedLRA
    public void bookFlightAsync(@Suspended final AsyncResponse asyncResponse,
                           @QueryParam(FLIGHT_NUMBER_PARAM) @DefaultValue("") String flightNumber,
                           @QueryParam(FLIGHT_SEATS_PARAM) @DefaultValue("1") Integer seats,
                           @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        flightService.bookAsync(flightNumber, seats)
                .thenApply(asyncResponse::resume)
                .exceptionally(e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

        asyncResponse.setTimeout(timeout, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Response.status(SERVICE_UNAVAILABLE).entity("Operation timed out").build()));
    }

    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.REQUIRED)
    @NestedLRA
    public Booking bookFlight(@QueryParam(FLIGHT_NUMBER_PARAM) @DefaultValue("") String flightNumber,
                              @QueryParam(FLIGHT_SEATS_PARAM) @DefaultValue("1") Integer seats,
                              @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        return flightService.book(flightNumber, seats);
    }
}
