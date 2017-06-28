package participant.filter.service;

import participant.filter.model.Booking;

import javax.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class HotelService {
    private AtomicInteger id = new AtomicInteger(0);

    public Booking book(String hotel, Integer seats) {;
        return new Booking(Integer.valueOf(id.incrementAndGet()).toString(), hotel, seats);
    }

    public CompletableFuture<Booking> bookAsync(String hotel, Integer seats) {
        return CompletableFuture.supplyAsync(() -> book(hotel, seats));
    }
}
