package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import participant.filter.model.Booking;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@ApplicationScoped
@Path("/trip")
@LRA(LRA.LRAType.SUPPORTS)
public class TripController {
    private static URL THEATRE_SERVICE_BASE_URL;
    private static URL TAXI_SERVICE_BASE_URL;

    private Client theatreClient;
    private Client taxiClient;

    private WebTarget theatreTarget;
    private WebTarget taxiTarget;

    @PostConstruct
    private void initController() {
        try {
            THEATRE_SERVICE_BASE_URL = new URL("http://localhost:8081");
            TAXI_SERVICE_BASE_URL = new URL("http://localhost:8081");

            theatreClient = ClientBuilder.newClient();
            taxiClient = ClientBuilder.newClient();

            theatreTarget = theatreClient.target(URI.create(new URL(THEATRE_SERVICE_BASE_URL, "/theatre").toExternalForm()));
            taxiTarget = taxiClient.target(URI.create(new URL(TAXI_SERVICE_BASE_URL, "/taxi").toExternalForm()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    public void bookTrip(@Suspended final AsyncResponse asyncResponse,
                         @QueryParam("show") @DefaultValue("") String show,
                         @QueryParam("theatreSeats") @DefaultValue("1") Integer theatreSeats,
                         @QueryParam("taxiSeats") @DefaultValue("0") Integer taxiSeats) {

        long timeout = -1;

        CompletableFuture<Booking> theatreBooking = show.isEmpty() ? null : bookTheatre();
        CompletableFuture<Booking> taxiBooking = taxiSeats <= 0 ? null : bookTaxi();

        if (theatreBooking != null) {
            timeout = 500;

            if (taxiBooking != null) {
                CompletableFuture<Booking> trip = theatreBooking.thenCombineAsync(taxiBooking, Booking::new);

                trip
                        .thenApply(
                                asyncResponse::resume)
                        .exceptionally(
                                e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));
            } else {
                theatreBooking
                        .thenApply(asyncResponse::resume)
                        .exceptionally(e ->
                                asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));
            }
        } else if (taxiBooking != null) {
            timeout = 500;

            taxiBooking
                    .thenApply(asyncResponse::resume)
                    .exceptionally(e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));
        }

        if (timeout >= 0) {
            asyncResponse.setTimeout(timeout, TimeUnit.MILLISECONDS);
            asyncResponse.setTimeoutHandler(ar -> ar.resume(Response.status(SERVICE_UNAVAILABLE).entity("Operation timed out").build()));
        } else {
            asyncResponse.resume("Invalid booking request");
        }
    }

    private CompletableFuture<Booking> bookTheatre() {
        WebTarget webTarget = theatreTarget
                .path("book")
                .queryParam("show", "Cats").queryParam("seats", 2);

        return invokeWebTarget(webTarget);
    }
    private CompletableFuture<Booking> bookTaxi() {
        WebTarget webTarget = taxiTarget
                .path("book")
                .queryParam("seats", 2);

        return invokeWebTarget(webTarget);
    }

    private CompletableFuture<Booking> invokeWebTarget(WebTarget webTarget) {
        AsyncInvoker asyncInvoker = webTarget.request().async();
        BookingCallback<Booking> adapter = new BookingCallback<>(Booking.class);

        asyncInvoker.post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE), adapter);

        return adapter.getCompletableFuture();
    }

}

