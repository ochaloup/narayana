package org.jboss.narayana.rts.lra.filter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

@Provider
public class ClientLRARequestFilter extends FilterBase implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext context) throws IOException {
        String lraId = currentLRA();

        if (lraId != null)
            context.getHeaders().putSingle(LRA_HTTP_HEADER, lraId);
        else
            context.getHeaders().remove(LRA_HTTP_HEADER);
    }
}
