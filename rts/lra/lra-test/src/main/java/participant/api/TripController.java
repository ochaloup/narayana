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
    private static URL HOTEL_SERVICE_BASE_URL;
    private static URL FLIGHT_SERVICE_BASE_URL;

    private Client hotelClient;
    private Client flightClient;

    private WebTarget hotelTarget;
    private WebTarget flightTarget;

    @PostConstruct
    private void initController() {
        try {
            HOTEL_SERVICE_BASE_URL = new URL("http://localhost:8081");
            FLIGHT_SERVICE_BASE_URL = new URL("http://localhost:8081");

            hotelClient = ClientBuilder.newClient();
            flightClient = ClientBuilder.newClient();

            hotelTarget = hotelClient.target(URI.create(new URL(HOTEL_SERVICE_BASE_URL, "/hotel").toExternalForm()));
            flightTarget = flightClient.target(URI.create(new URL(FLIGHT_SERVICE_BASE_URL, "/flight").toExternalForm()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The quickstart scenario is:
     *
     * start LRA 1
     *   Book hotel
     *   start LRA 2
     *     start LRA 3
     *       Book flight option 1
     *     start LRA 4
     *       Book flight option 2
     *
     * @param asyncResponse
     * @param hotel hotel name
     * @param hotelGuests number of beds required
     * @param flightSeats number of people flying
     */
    @GET
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    public void bookTrip(@Suspended final AsyncResponse asyncResponse,
                         @QueryParam("hotel") @DefaultValue("") String hotel,
                         @QueryParam("hotelGuests") @DefaultValue("1") Integer hotelGuests,
                         @QueryParam("flightSeats") @DefaultValue("0") Integer flightSeats) {

        long timeout = -1;

        CompletableFuture<Booking> hotelBooking = hotelGuests <= 0 ? null : bookHotel();
        CompletableFuture<Booking> flightBooking = flightSeats <= 0 ? null : bookFlight();

        if (hotelBooking != null) {
            timeout = 500;

            if (flightBooking != null) {
                CompletableFuture<Booking> trip = hotelBooking.thenCombineAsync(flightBooking, Booking::new);

                trip
                        .thenApply(
                                asyncResponse::resume)
                        .exceptionally(
                                e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));
            } else {
                hotelBooking
                        .thenApply(asyncResponse::resume)
                        .exceptionally(e ->
                                asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));
            }
        } else if (flightBooking != null) {
            timeout = 500;

            flightBooking
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

    private CompletableFuture<Booking> bookHotel() {
        WebTarget webTarget = hotelTarget
                .path("book")
                .queryParam("hotel", "The Grand").queryParam("hotelGuests", 2);

        return invokeWebTarget(webTarget);
    }
    private CompletableFuture<Booking> bookFlight() {
        WebTarget webTarget = flightTarget
                .path("book")
                .queryParam("flightSeats", 2);

        return invokeWebTarget(webTarget);
    }

    private CompletableFuture<Booking> invokeWebTarget(WebTarget webTarget) {
        AsyncInvoker asyncInvoker = webTarget.request().async();
        BookingCallback<Booking> adapter = new BookingCallback<>(Booking.class);

        asyncInvoker.post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE), adapter);

        return adapter.getCompletableFuture();
    }

}

