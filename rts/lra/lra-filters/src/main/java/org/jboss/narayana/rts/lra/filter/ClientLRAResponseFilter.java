package org.jboss.narayana.rts.lra.filter;

import org.jboss.narayana.rts.lra.coordinator.api.Current;

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
        Object incomingLRA = responseContext.getHeaders().getFirst(LRA_HTTP_HEADER);
//        Object outgoingLRA = requestContext.getHeaders().getFirst(LRA_HTTP_HEADER);

        /*
         * if the incoming response contains a context make it the current one
         * (note we never popped the context in the request filter so we don't need to push outgoingLRA
         */
        if (incomingLRA != null)
            Current.push(new URL(incomingLRA.toString()));
    }
}
