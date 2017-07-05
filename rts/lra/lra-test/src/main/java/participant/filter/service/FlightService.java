package participant.filter.service;

import participant.filter.model.Booking;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class FlightService extends BookingStore {
    public Booking book(String bid, String flightNumber, Integer seats) {
        Booking booking = new Booking(bid, flightNumber, seats, "Flight");

        add(booking);

        return booking;
    }

    public CompletableFuture<Booking> bookAsync(String bid, String flightNumber, Integer seats) {
        return CompletableFuture.supplyAsync(() -> book(bid, flightNumber, seats));}
}
