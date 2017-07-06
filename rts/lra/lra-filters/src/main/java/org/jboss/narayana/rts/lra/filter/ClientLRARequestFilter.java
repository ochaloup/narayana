package org.jboss.narayana.rts.lra.filter;

import org.jboss.narayana.rts.lra.coordinator.api.Current;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URL;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER2;

@Provider
public class ClientLRARequestFilter extends FilterBase implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext context) throws IOException {
        // NB the following overrides what the caller did with the LRA context header
        Current.updateLRAContext(context.getHeaders());
    }
}
