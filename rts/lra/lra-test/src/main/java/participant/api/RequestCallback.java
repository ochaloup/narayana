package participant.api;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class RequestCallback<T> implements InvocationCallback<Response> {

    private final Class<T> responseType;
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private final Collection<Integer> acceptableStatusCodes;

    RequestCallback(Class<T> responseType) {
        this(responseType, Response.Status.OK.getStatusCode());
    }

    RequestCallback(Class<T> responseType, Integer ... acceptableStatusCodes) {
        this.responseType = responseType;
        this.acceptableStatusCodes = Arrays.asList(acceptableStatusCodes);
    }

    CompletableFuture<T> getCompletableFuture() {
        return future;
    }

    @Override
    public void completed(Response response) {
        try {
            if (acceptableStatusCodes.contains(response.getStatus())) {
                T t = response.readEntity(responseType);
                future.complete(t);
            } else {
                failed(new WebApplicationException(response));
            }
        } catch (Throwable t) {
            failed(t);
        }
    }

    @Override
    public void failed(Throwable t) {
        future.completeExceptionally(t);
    }
}

