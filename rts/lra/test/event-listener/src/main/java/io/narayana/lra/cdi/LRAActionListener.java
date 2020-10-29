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

package io.narayana.lra.cdi;

import io.narayana.lra.event.LRAAction;
import io.narayana.lra.event.LRAEventInfo;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Path(LRAActionListener.PATH)
public class LRAActionListener {
    private static final Logger log = Logger.getLogger(LRAActionListener.class);
    public static final String PATH = "listener";

    // map of LRA id to list of received actions
    private Map<String, List<LRAAction>> counter = new ConcurrentHashMap<>();

    public void onLraEvent(@Observes LRAEventInfo eventInfo) {
        log.warnf("------------ LRA event observed: %s", eventInfo);
        counter.compute(eventInfo.getLraId().toASCIIString(), (k, v) -> {
            if (v == null) {
                List<LRAAction> actionList = new ArrayList<>();
                actionList.add(eventInfo.getLraAction());
                return actionList;
            } else {
                v.add(eventInfo.getLraAction());
                return v;
            }
        });
    }

    @GET
    @Path("{lraId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<LRAAction> listEvents(@PathParam("lraId") String lraId) {
        List<LRAAction> events = counter.get(lraId);
        log.infof("****** For lra id %s returning events: %s", lraId, events);
        if(events == null) return new ArrayList<>();
        return events;
    }
}
