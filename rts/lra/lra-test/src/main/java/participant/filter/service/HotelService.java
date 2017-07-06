package participant.filter.service;

import participant.filter.model.Booking;

import javax.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class HotelService extends BookingStore {
    public Booking book(String bid, String hotel, Integer beds) {;
        Booking booking = new Booking(bid, hotel, beds, "Hotel");

        add(booking);

        return booking;
    }

    public CompletableFuture<Booking> bookAsync(String bid, String hotel, Integer beds) {
        return CompletableFuture.supplyAsync(() -> book(bid, hotel, beds));
    }
}
