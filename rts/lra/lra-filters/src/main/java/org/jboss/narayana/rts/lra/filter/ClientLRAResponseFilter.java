package org.jboss.narayana.rts.lra.filter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

@Provider
public class ClientLRAResponseFilter extends FilterBase implements ClientResponseFilter {
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        Object lraId = responseContext.getHeaders().getFirst(LRA_HTTP_HEADER);

        // if the response does not contain a transaction put back the one that was on the outgoing request
        if (lraId == null)
            lraId = requestContext.getHeaders().getFirst(LRA_HTTP_HEADER);

        if (lraId != null)
            associateLRA(lraId.toString());
    }
}
