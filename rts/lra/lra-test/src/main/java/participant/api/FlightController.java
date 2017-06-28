package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import participant.filter.service.FlightService;

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
@Path("/flight")
@LRA(LRA.LRAType.SUPPORTS)
public class FlightController {

    @Inject
    private LRAClient lraClient;

    @Inject
    private FlightService flightService;

    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    public void bookFlight(@Suspended final AsyncResponse asyncResponse,
                         @QueryParam("seats") @DefaultValue("") Integer seats) {

        flightService.bookAsync(seats)
                .thenApply(asyncResponse::resume)
                .exceptionally(e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

        asyncResponse.setTimeout(500, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Response.status(SERVICE_UNAVAILABLE).entity("Operation timed out").build()));
    }
}
