package org.jboss.narayana.rts.lra.filter;

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
        Object lraHeader = context.getHeaders().getFirst(LRA_HTTP_HEADER); // TODO why did this used to say LRA_HTTP_HEADER2
        Object lraHeader2 = context.getHeaders().getFirst(LRA_HTTP_HEADER2);
        URL lraId;

        if (lraHeader == null)
            lraHeader = lraHeader2;

        // don't overwrite the outgoing LRA header
        if (lraHeader == null) {
            lraId = currentLRA();

            if (lraId != null)
                context.getHeaders().putSingle(LRA_HTTP_HEADER, lraId.toString());

            System.out.printf("ClientLRARequestFilter: %s from %s to %s (added header: %s)%n", context.getMethod(), "", context.getUri(), lraId);

//        } else {
//            context.getHeaders().remove(LRA_HTTP_HEADER);
//
//            System.out.printf("ClientLRARequestFilter: %s from %s to %s (removed header: %s)%n", context.getMethod(), "", context.getUri(), lraHeader);
        }

    }
}
