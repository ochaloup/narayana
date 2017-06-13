package participant.filter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

@Provider
public class ClientLRARequestFilter implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext context) throws IOException {
        LRAState txId = LRAState.getCurrentLRA();

        if (txId != null) {
            context.getHeaders().putSingle(LRA_HTTP_HEADER, txId.id);
        }
    }
}
