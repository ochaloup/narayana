package participant.filter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

@Provider
public class ClientLRAResponseFilter implements ClientResponseFilter {
    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext responseContext) throws IOException {
        String txId = responseContext.getHeaders().getFirst(LRA_HTTP_HEADER);

        if (txId != null)
            LRAState.setCurrentLRA(txId);
    }
}
