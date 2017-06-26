package participant.filter.service;

import participant.filter.model.Booking;

import javax.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class TheatreService {
    private AtomicInteger id = new AtomicInteger(0);

    public Booking book(String show, Integer seats) {;
        return new Booking(Integer.valueOf(id.incrementAndGet()).toString(), show, seats);
    }

    public CompletableFuture<Booking> bookAsync(String show, Integer seats) {
        return CompletableFuture.supplyAsync(() -> book(show, seats));
    }
}
