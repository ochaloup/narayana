package participant.api;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;

public class BookingCallback<T> implements InvocationCallback<Response> {

    private final Class<T> responseType;
    private final CompletableFuture<T> future = new CompletableFuture<T>();

    public BookingCallback(Class<T> responseType) {
        this.responseType = responseType;
    }

    CompletableFuture<T> getCompletableFuture() {
        return future;
    }

    @Override
    public void completed(Response response) {
        T object;

        try {
            object = (T) response.readEntity(responseType);
        } catch (Throwable t) {
            failed(t);

            return;
        }

        future.complete(object);
    }

    @Override
    public void failed(Throwable t) {
        future.completeExceptionally(t);
    }
}

