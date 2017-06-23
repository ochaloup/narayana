package org.jboss.narayana.rts.lra.filter;

import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.util.Iterator;

@ApplicationScoped
class FilterBase {

    @Inject
    private LRAClient lraClient;

    private boolean hasClient() {
        return false;//lraClient != null && lraClient.isUseable();
    }

    LRAClient getLRAClient(boolean create) {
        if (hasClient())
            return lraClient;

        // see if the target resource has an injected LRAClient - if so use
        lraClient = getFirstBean(LRAClient.class);

        try {
            if (lraClient == null && create) {
                // this is a client request so may need to start a local coordinator - default to localhost:8080 TODO get the coordinator uri from some config
                lraClient = new LRAClient("localhost", 8080);
            }

            return lraClient;
        } catch (URISyntaxException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("could not build coordinator client").build());
        }
    }

    <T> T getFirstBean(Class<T> clazz) {
        BeanManager bm =  CDI.current().getBeanManager();
        Iterator<Bean<?>> i = bm.getBeans(clazz).iterator();

        if (!i.hasNext())
            return null;

        Bean<T> bean = (Bean<T>) i.next();
        CreationalContext<T> ctx = bm.createCreationalContext(bean);

        return (T) bm.getReference(bean, clazz, ctx);
    }


    void associateLRA(String lraId) {
        FilterState.setCurrentLRA(lraId);

        if (getLRAClient(false) != null)
            lraClient.setCurrentLRA(lraId);
    }

    String currentLRA() {
        FilterState lraId = FilterState.getCurrentLRA();

        if (lraId != null)
            return lraId.id;

        // see if the resource started an LRA
        if (getLRAClient(false) == null)
            return null;

        return lraClient.getCurrent();
    }
}
