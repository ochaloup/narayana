/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package io.narayana.lra.test.coordinator.event;

import io.narayana.lra.coordinator.domain.event.LRAAction;
import io.narayana.lra.coordinator.domain.event.LRAEventInfo;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Path(EventLogListener.EVENTS_PATH)
public class EventLogListener {
    private static final Logger log = Logger.getLogger(EventLogListener.class);
    public static final String EVENTS_PATH = "events";

    private Map<LRAAction, Integer> counter = new ConcurrentHashMap<>();

    public void onLraEvent(@Observes LRAEventInfo eventInfo) {
        log.debugf("LRA event observed: %s", eventInfo);
        counter.compute(eventInfo.getLraAction(), (k, v) -> (v == null) ? 1 : v + 1);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    public Map<LRAAction,Integer> listEvents() {
        return counter;
    }
}
