package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import participant.filter.model.Booking;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
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
@Path(TripController.TRIP_PATH)
@LRA(LRA.LRAType.SUPPORTS)
public class TripController extends Participant {
    static final String TRIP_PATH = "/trip";
    private Client hotelClient;
    private Client flightClient;

    private WebTarget hotelTarget;
    private WebTarget flightTarget;

    @PostConstruct
    private void initController() {
        try {
            URL HOTEL_SERVICE_BASE_URL = new URL("http://localhost:8081");
            URL FLIGHT_SERVICE_BASE_URL = new URL("http://localhost:8081");

            hotelClient = ClientBuilder.newClient();
            flightClient = ClientBuilder.newClient();

            hotelTarget = hotelClient.target(URI.create(new URL(HOTEL_SERVICE_BASE_URL, HotelController.HOTEL_PATH).toExternalForm()));
            flightTarget = flightClient.target(URI.create(new URL(FLIGHT_SERVICE_BASE_URL, FlightController.FLIGHT_PATH).toExternalForm()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void finiController() {
        hotelClient.close();
        flightClient.close();
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
     * @param asyncResponse the response object that will be asynchronously returned back to the caller
     * @param hotelName hotel name
     * @param hotelGuests number of beds required
     * @param flightSeats number of people flying
     */
    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.REQUIRED)
    public void bookTrip(@Suspended final AsyncResponse asyncResponse,
                         @QueryParam(HotelController.HOTEL_NAME_PARAM) @DefaultValue("The Grand") String hotelName,
                         @QueryParam(HotelController.HOTEL_BEDS_PARAM) @DefaultValue("1") Integer hotelGuests,
                         @QueryParam(FlightController.FLIGHT_NUMBER_PARAM) @DefaultValue("123") String flightNumber,
                         @QueryParam(FlightController.FLIGHT_SEATS_PARAM) @DefaultValue("0") Integer flightSeats,
                         @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        CompletableFuture<Booking> hotelBooking = bookHotel(hotelName, hotelGuests);
        CompletableFuture<Booking> flightBooking1 = bookFlight(flightNumber, flightSeats);
        CompletableFuture<Booking> flightBooking2 =
                flightBooking1 == null ? null : bookFlight(flightNumber + "B", flightSeats);
        CompletableFuture<Booking> asyncResult;

        if (hotelBooking != null) {
            if (flightBooking1 != null) {
                asyncResult = hotelBooking
//                        .thenCombineAsync(flightBooking1, Booking::new)
                        .thenCombineAsync(flightBooking2, Booking::new);
            } else {
                asyncResult = hotelBooking;
            }
        } else if (flightBooking1 != null) {
            asyncResult = flightBooking1;
        } else {
            asyncResponse.resume("Invalid booking request: no flight or hotel information");
            return;
        }

        asyncResult
                .thenApply(asyncResponse::resume)
                .exceptionally(e -> asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build()));

        asyncResponse.setTimeout(timeout, TimeUnit.MILLISECONDS);
        asyncResponse.setTimeoutHandler(ar -> ar.resume(Response.status(SERVICE_UNAVAILABLE).entity("Operation timed out").build()));
    }

    private CompletableFuture<Booking> bookHotel(String name, int beds) {
        if (name == null || name.length() == 0 || beds <= 0)
            return null;

        WebTarget webTarget = hotelTarget
                .path("book")
                .queryParam(HotelController.HOTEL_NAME_PARAM, name).queryParam(HotelController.HOTEL_BEDS_PARAM, beds);

        return invokeWebTarget(webTarget);
    }

    private CompletableFuture<Booking> bookFlight(String flightNumber, int seats) {
        if (flightNumber == null || flightNumber.length() == 0 || seats <= 0)
            return null;

        WebTarget webTarget = flightTarget
                .path("book")
                .queryParam(FlightController.FLIGHT_NUMBER_PARAM, flightNumber)
                .queryParam(FlightController.FLIGHT_SEATS_PARAM, seats);

        return invokeWebTarget(webTarget);
    }

    private CompletableFuture<Booking> invokeWebTarget(WebTarget webTarget, Integer ... acceptableStatusCodes) {
        if (acceptableStatusCodes.length == 0)
            acceptableStatusCodes = new Integer[] {Response.Status.OK.getStatusCode()};

        AsyncInvoker asyncInvoker = webTarget.request().async();
        RequestCallback<Booking> callback = new RequestCallback<>(Booking.class, acceptableStatusCodes);

        asyncInvoker.post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE), callback);

        return callback.getCompletableFuture();
    }
}

