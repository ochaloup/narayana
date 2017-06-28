package participant.filter.service;

import participant.filter.model.Booking;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class FlightService {
    private AtomicInteger id = new AtomicInteger(0);

    private Booking book(Integer seats) {
        return new Booking(Integer.valueOf(id.incrementAndGet()).toString(), "Delag", seats);
    }

    public CompletableFuture<Booking> bookAsync(Integer seats) {
        return CompletableFuture.supplyAsync(() -> book(seats));}
}
