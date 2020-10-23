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

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Path(LRAParticipant.PARTICIPANT_PATH)
public class LRAParticipant {
    static final String PARTICIPANT_PATH = "participant";

    @GET
    @Path("/")
    @LRA(value = LRA.Type.REQUIRED)
    public Response action(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId.toASCIIString()).build();
    }

    @Complete
    @PUT
    @Path("/complete")
    public Response complete(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId.toASCIIString()).build();
    }

    @Compensate
    @PUT
    @Path("/compensate")
    public Response compensate(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId.toASCIIString()).build();
    }

    @AfterLRA
    @PUT
    @Path("/afterlra")
    public Response afterLra(LRAStatus endStatus) {
        return Response.ok().build();
    }
}
