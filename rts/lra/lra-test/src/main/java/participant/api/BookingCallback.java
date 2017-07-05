package participant.api;

import participant.filter.model.Booking;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class BookingCallback implements InvocationCallback<Booking> {

    private final CompletableFuture<Booking> future = new CompletableFuture<>();

    CompletableFuture<Booking> getCompletableFuture() {
        return future;
    }

    @Override
    public void completed(Booking booking) {
        future.complete(booking);
    }

    @Override
    public void failed(Throwable t) {
        future.completeExceptionally(t);
    }
}

