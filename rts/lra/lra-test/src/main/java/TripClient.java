import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import participant.api.FlightController;
import participant.api.HotelController;
import participant.api.TripController;
import participant.filter.model.Booking;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

public class TripClient {
    private static String PRIMARY_SERVER;
    private static String TRIP_SERVICE_BASE_URL;
    private static String HOTEL_SERVICE_BASE_URL;
    private static String FLIGHT_SERVICE_BASE_URL;

    ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        initClient();
        TripClient tripClient = new TripClient();

//        tripClient.postRequestAsJson(new URL("http://localhost:8081/trip/book?hotelName=Rex&flightNumber=BA123"), "");
        Booking booking = tripClient.bookTrip("TheGrand", 2, "BA123", "RH456", 2);

        if (booking == null)
            return;

        // cancel the first flight found (and use the second one)
        Optional<Booking> firstFlight = Arrays.stream(booking.getDetails()).filter(b -> "Flight".equals(b.getType())).findFirst();

        firstFlight.ifPresent(Booking::cancel);

        System.out.printf("%nBooking Info:%n\t%s%n", booking);
        System.out.printf("Associated Bookings:%n");

        Arrays.stream(booking.getDetails()).forEach(b -> System.out.printf("\t%s%n", b));

        Booking confirmation = tripClient.confirm(booking);

        System.out.printf("%nBooking confirmation:%n\t%s%n", confirmation);
        Arrays.stream(confirmation.getDetails()).forEach(b -> System.out.printf("\t%s%n", b));
    }

    private static void initClient() {
        PRIMARY_SERVER = "http://localhost:8081";
        TRIP_SERVICE_BASE_URL = String.format("%s%s", PRIMARY_SERVER, TripController.TRIP_PATH);
        HOTEL_SERVICE_BASE_URL = String.format("%s%s", PRIMARY_SERVER, HotelController.HOTEL_PATH);
        FLIGHT_SERVICE_BASE_URL = String.format("%s%s", PRIMARY_SERVER, FlightController.FLIGHT_PATH);
    }

    public Booking bookTrip(String hotelName, Integer hotelGuests, String flightNumber, String altFlightNumber, Integer flightSeats) throws Exception {
        StringBuilder tripRequest =
                new StringBuilder(TRIP_SERVICE_BASE_URL)
                        .append("/book?")
                        .append(HotelController.HOTEL_NAME_PARAM).append('=').append(hotelName).append('&')
                        .append(HotelController.HOTEL_BEDS_PARAM).append('=').append(hotelGuests).append('&')
                        .append(FlightController.FLIGHT_NUMBER_PARAM).append('=').append(flightNumber).append('&')
                        .append(FlightController.ALT_FLIGHT_NUMBER_PARAM).append('=').append(altFlightNumber).append('&')
                        .append(FlightController.FLIGHT_SEATS_PARAM).append('=').append(flightSeats);

        URL url = new URL(tripRequest.toString());
        String json = postRequestAsJson(url, "");

        if (json == null)
            return null;

        return objectMapper.readValue(json, Booking.class);

//        booking.setJson(objectMapper.writeValueAsString(json));
//        booking.setPrettyJson(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
    }


    private Booking confirm(Booking booking) throws Exception {
        URL confirmURL = new URL(TRIP_SERVICE_BASE_URL +"/complete");
        String jsonBody = objectMapper.writeValueAsString(booking);
        String confirmation = updateResource(confirmURL, "PUT", jsonBody);

        return objectMapper.readValue(confirmation, Booking.class);
    }

    private String postRequestAsJson(URL resource, String jsonBody) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) resource.openConnection();

        try (AutoCloseable conc = connection::disconnect) {

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.writeBytes(jsonBody);
            }

            int responseCode = connection.getResponseCode();

            try (InputStream ins = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                // BufferedReader in = new BufferedReader(new InputStreamReader(ins))) {
                // receive response
                Scanner responseScanner = new java.util.Scanner(ins).useDelimiter("\\A");
                String res = responseScanner.hasNext()? responseScanner.next() : null;

                if (res != null && responseCode >= 400) {
                    System.out.println(res);

                    return null;
                }

                return res;
            }
        }
    }

    private String updateResource(URL resource, String method, String jsonBody) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) resource.openConnection();

        try (AutoCloseable conc = connection::disconnect) {

            connection.setDoOutput(true);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.writeBytes(jsonBody);
            }

            int responseCode = connection.getResponseCode();

            try (InputStream ins = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                // BufferedReader in = new BufferedReader(new InputStreamReader(ins))) {
                // receive response
                Scanner responseScanner = new java.util.Scanner(ins).useDelimiter("\\A");
                String res = responseScanner.hasNext()? responseScanner.next() : null;

                if (res != null && responseCode >= 400) {
                    System.out.printf(res);

                    return null;
                }

                return res;
            }
        }
    }

    void bookit2(URL resource) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) resource.openConnection();

        try (AutoCloseable conc = connection::disconnect) {

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
                throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (connection.getInputStream())));

            String output;

            while ((output = br.readLine()) != null)
                System.out.println(output);
        }
    }
}
