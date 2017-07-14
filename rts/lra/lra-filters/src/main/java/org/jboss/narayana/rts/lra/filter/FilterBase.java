/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.narayana.rts.lra.filter;

import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

//@RequestScoped TODO RESTEASY-682 so try
@ApplicationScoped
class FilterBase {
    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    private LRAClient lraClient;

    private static Boolean isTrace = Boolean.getBoolean("trace");

    private boolean hasClient() {
        return lraClient != null && lraClient.isUseable();
    }

    protected void lraTrace(ContainerRequestContext context, URL lraId, String reason) {
        if (isTrace) {
            Method method = resourceInfo.getResourceMethod();
            System.out.printf("%s: container request for method %s: lra: %s%n",
                    reason, method.getDeclaringClass().getName() + "#" + method.getName(),
                    lraId == null ? "context" : lraId);
        }
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
}
