package participant.filter.service;

import participant.filter.model.Booking;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class TaxiService {
    private AtomicInteger id = new AtomicInteger(0);

    private Booking book(Integer seats) {
        return new Booking(Integer.valueOf(id.incrementAndGet()).toString(), "ABC Taxis", seats);
    }

    public CompletableFuture<Booking> bookAsync(Integer seats) {
        return CompletableFuture.supplyAsync(() -> book(seats));}
}
