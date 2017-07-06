package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.Status;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import org.jboss.narayana.rts.lra.coordinator.api.InvalidLRAId;
import participant.filter.model.Booking;
import participant.filter.model.BookingStatus;
import participant.filter.service.TripService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.awt.print.Book;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

//@ApplicationScoped
@RequestScoped
@Path(TripController.TRIP_PATH)
@LRA(LRA.LRAType.SUPPORTS)
public class TripController extends Participant {
    public static final String TRIP_PATH = "/trip";

    private Client hotelClient;
    private Client flightClient;

    private WebTarget hotelTarget;
    private WebTarget flightTarget;

    @Inject
    private LRAClient lraClient;

    @Inject
    private TripService tripService;

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
    @Path("/bookasync")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.REQUIRED)
    public void bookTripAsync(@Suspended final AsyncResponse asyncResponse,
                              @HeaderParam(LRA_HTTP_HEADER) String lraId,
                              @QueryParam(HotelController.HOTEL_NAME_PARAM) @DefaultValue("") String hotelName,
                              @QueryParam(HotelController.HOTEL_BEDS_PARAM) @DefaultValue("1") Integer hotelGuests,
                              @QueryParam(FlightController.FLIGHT_NUMBER_PARAM) @DefaultValue("") String flightNumber,
                              @QueryParam(FlightController.FLIGHT_SEATS_PARAM) @DefaultValue("1") Integer flightSeats,
                              @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        CompletableFuture<Booking> hotelBooking = bookHotelAsync(hotelName, hotelGuests);
        CompletableFuture<Booking> flightBooking1 = bookFlightAsync(flightNumber, flightSeats);
        CompletableFuture<Booking> flightBooking2 =
                flightBooking1 == null ? null : bookFlightAsync(flightNumber + "B", flightSeats);
        CompletableFuture<Booking> asyncResult;

        if (hotelBooking != null) {
            if (flightBooking1 != null) {
                asyncResult = hotelBooking
                        .thenCombineAsync(flightBooking2, (bookings, bookings2) -> new Booking(null, null, bookings, bookings2))
                        .thenCombineAsync(flightBooking1, (bookings1, bookings12) -> new Booking(null, null, bookings1, bookings12));
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

    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    // longRunning because we want the LRA to be associated with a booking until the user confirms the booking
    @LRA(value = LRA.LRAType.REQUIRED, longRunning = true)
    public Response bookTrip( @HeaderParam(LRA_HTTP_HEADER) String lraId,
                              @QueryParam(HotelController.HOTEL_NAME_PARAM) @DefaultValue("") String hotelName,
                              @QueryParam(HotelController.HOTEL_BEDS_PARAM) @DefaultValue("1") Integer hotelGuests,
                              @QueryParam(FlightController.FLIGHT_NUMBER_PARAM) @DefaultValue("") String flightNumber,
                              @QueryParam(FlightController.ALT_FLIGHT_NUMBER_PARAM) @DefaultValue("") String altFlightNumber,
                              @QueryParam(FlightController.FLIGHT_SEATS_PARAM) @DefaultValue("1") Integer flightSeats,
                              @QueryParam("mstimeout") @DefaultValue("500") Long timeout) {

        Booking hotelBooking = bookHotel(hotelName, hotelGuests);
        Booking flightBooking1 = bookFlight(flightNumber, flightSeats);
        Booking flightBooking2 = bookFlight(altFlightNumber, flightSeats);

        Booking tripBooking = new Booking(lraId, "Trip", hotelBooking, flightBooking1, flightBooking2);

        return Response.status(Response.Status.CREATED).entity(tripBooking).build();
    }

    @PUT
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.SUPPORTS) // the confirmation could be part of an enclosing LRA
    public Booking confirmTrip(Booking booking) throws BookingException {
        tripService.confirmBooking(booking);

        booking = validateBooking(booking, BookingStatus.CONFIRMED);

        return booking;
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.SUPPORTS) // the confirmation could be part of an enclosing LRA
    public Booking cancelTrip(Booking booking) throws BookingException {
        tripService.cancelBooking(booking);

        return validateBooking(booking, BookingStatus.CANCELLED);
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.LRAType.NOT_SUPPORTED)
    public Response status(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        Booking booking = tripService.get(lraId);

        return Response.ok(booking.getStatus().name()).build(); // TODO convert to a CompensatorStatus if we we're enlisted in an LRA
    }

    private Booking validateBooking(Booking booking, BookingStatus status) throws BookingException {
        checkBooking(booking); // there may have been independent updates to the dependent bookings
        booking.setStatus(status);

        return booking;
//        return Response.ok(booking).build();
    }

    private void checkBooking(Booking booking) throws BookingException {
        final BookingException[] bookingException = {null};

        // NB parallel() results in IllegalStateException: WFLYWELD0039 because
        // ... trying to access a weld deployment with a Thread Context ClassLoader that is not associated with the deployment
        Arrays.stream(booking.getDetails()).forEach(b -> {
            try {
                checkDependentBooking(b);
            } catch (BookingException e) {
                bookingException[0] = e;
            }
        });

        if (bookingException[0] != null)
            throw bookingException[0];
    }

    private void checkDependentBooking(Booking booking) throws BookingException {
        if ("Hotel".equals(booking.getType()))
            checkDependentBooking(hotelTarget, booking);
        else if ("Flight".equals(booking.getType()))
            checkDependentBooking(flightTarget, booking);
    }

    private void checkDependentBooking(WebTarget target, Booking booking) throws BookingException {
        Response response = target.path("info").path(booking.getEncodedId()).request().get();

        checkResponse(response, Response.Status.OK, "Could not lookup hotel booking status");

        booking.merge(response.readEntity(Booking.class));
    }

    private void checkResponse(Response response, Response.Status expect, String message) throws BookingException {
        if (response.getStatus() != expect.getStatusCode())
            throw new BookingException(response.getStatus(), message);
    }

    private Booking bookHotel(String name, int beds) {
        if (name == null || name.length() == 0 || beds <= 0)
            return null;

        WebTarget webTarget = hotelTarget
                .path("book")
                .queryParam(HotelController.HOTEL_NAME_PARAM, name).queryParam(HotelController.HOTEL_BEDS_PARAM, beds);

        return webTarget.request().post(Entity.text("")).readEntity(Booking.class);
    }

    private Booking bookFlight(String flightNumber, int seats) {
        if (flightNumber == null || flightNumber.length() == 0 || seats <= 0)
            return null;

        WebTarget webTarget = flightTarget
                .path("book")
                .queryParam(FlightController.FLIGHT_NUMBER_PARAM, flightNumber)
                .queryParam(FlightController.FLIGHT_SEATS_PARAM, seats);

        return webTarget.request().post(Entity.text("")).readEntity(Booking.class);
    }

    private CompletableFuture<Booking> bookHotelAsync(String name, int beds) {
        if (name == null || name.length() == 0 || beds <= 0)
            return null;

        WebTarget webTarget = hotelTarget
                .path("book")
                .queryParam(HotelController.HOTEL_NAME_PARAM, name).queryParam(HotelController.HOTEL_BEDS_PARAM, beds);

        return invokeWebTarget(webTarget);
    }

    private CompletableFuture<Booking> bookFlightAsync(String flightNumber, int seats) {
        if (flightNumber == null || flightNumber.length() == 0 || seats <= 0)
            return null;

        WebTarget webTarget = flightTarget
                .path("book")
                .queryParam(FlightController.FLIGHT_NUMBER_PARAM, flightNumber)
                .queryParam(FlightController.FLIGHT_SEATS_PARAM, seats);

        return invokeWebTarget(webTarget);
    }

    private CompletableFuture<Booking> invokeWebTarget(WebTarget webTarget) {
        AsyncInvoker asyncInvoker = webTarget.request().async();
        BookingCallback callback = new BookingCallback();

        asyncInvoker.post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE), callback);

        return callback.getCompletableFuture();
    }

    private CompletableFuture<Booking> invokeWebTarget(WebTarget webTarget, Integer ... acceptableStatusCodes) {
        if (acceptableStatusCodes.length == 0)
            acceptableStatusCodes = new Integer[] {Response.Status.OK.getStatusCode()};

        AsyncInvoker asyncInvoker = webTarget.request().async();
//        RequestCallback<Booking> callback = new RequestCallback<>(Booking.class, acceptableStatusCodes);
        BookingCallback callback = new BookingCallback();

        asyncInvoker.post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE), callback);

        return callback.getCompletableFuture();
    }

    @GET
    @Path("/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.LRAType.SUPPORTS)
    public Booking getBooking(@PathParam("bookingId") String bookingId) {
        return tripService.get(bookingId);
    }

    @Override
    protected CompensatorStatus updateCompensator(CompensatorStatus status, String bookingId) {
        switch (status) {
            case Completed:
                tripService.updateBookingStatus(bookingId, BookingStatus.CONFIRMED);
                return status;
            case Compensated:
                tripService.updateBookingStatus(bookingId, BookingStatus.CANCELLED);
                return status;
            default:
                return status;
        }
    }
}

