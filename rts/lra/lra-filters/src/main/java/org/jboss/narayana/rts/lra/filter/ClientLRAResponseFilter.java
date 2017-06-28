package org.jboss.narayana.rts.lra.filter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URL;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

@Provider
public class ClientLRAResponseFilter extends FilterBase implements ClientResponseFilter {
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        Object outgoingLRA = requestContext.getHeaders().getFirst(LRA_HTTP_HEADER);
        Object incomingLRA = responseContext.getHeaders().getFirst(LRA_HTTP_HEADER);
        // if the response does not contain a transaction put back the one that was on the outgoing request
        Object lraId = incomingLRA == null ? outgoingLRA : incomingLRA;

        // if there is an LRA then associate
        if (lraId != null) {
            associateLRA(new URL(lraId.toString()));
        }

       System.out.printf("ClientLRAResponseFilter: response from %s request to %s: %s%n\tContext changed from %s to %s%n",
               requestContext.getMethod(), requestContext.getUri(), responseContext.getStatusInfo(),
               outgoingLRA == null ? "null" : outgoingLRA,
               incomingLRA == null ? "null" : incomingLRA);
    }
}
