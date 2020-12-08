/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package io.narayana.lra.coordinator.api;

import io.narayana.lra.APIVersion;
import io.narayana.lra.Current;
import io.narayana.lra.LRAConstants;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static io.narayana.lra.LRAConstants.LRA_API_VERSION_HEADER_NAME;
import static io.narayana.lra.LRAConstants.NARAYANA_LRA_API_VERSION_STRING;
import static javax.ws.rs.core.Response.Status.EXPECTATION_FAILED;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Provider
public class CoordinatorContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        URI lraId = null;

        verifyHighestSupportedVersion(requestContext);

        if (headers.containsKey(LRA_HTTP_CONTEXT_HEADER)) {
            try {
                lraId = new URI(Current.getLast(headers.get(LRA_HTTP_CONTEXT_HEADER)));
            } catch (URISyntaxException e) {
                String errMsg = String.format("header %s contains an invalid URL %s: %s",
                        LRA_HTTP_CONTEXT_HEADER, Current.getLast(headers.get(LRA_HTTP_CONTEXT_HEADER)), e.getMessage());
                throw new WebApplicationException(errMsg, e,
                        Response.status(PRECONDITION_FAILED.getStatusCode()).entity(errMsg).build());
            }
        }

        if (!headers.containsKey(LRA_HTTP_CONTEXT_HEADER)) {
            Object lraContext = requestContext.getProperty(LRA_HTTP_CONTEXT_HEADER);

            if (lraContext != null) {
                lraId = (URI) lraContext;
            }
        }

        if (lraId != null) {
            Current.updateLRAContext(lraId, headers);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if(!responseContext.getHeaders().containsKey(LRAConstants.LRA_API_VERSION_HEADER_NAME)) { // app code did not provide version to header
            // as version using the api version which came at request or the current version of the api in the library
            String responseVersion = requestContext.getHeaders().containsKey(LRAConstants.LRA_API_VERSION_HEADER_NAME) ?
                    requestContext.getHeaderString(LRAConstants.LRA_API_VERSION_HEADER_NAME) :  NARAYANA_LRA_API_VERSION_STRING;
            responseContext.getHeaders().putSingle(LRAConstants.LRA_API_VERSION_HEADER_NAME, responseVersion);
        }

        Current.updateLRAContext(responseContext);
    }

    /**
     * Verification if the version in the header is in right format
     * and if demanded version is not higher than the supported one
     * (ie. if version is lower than {@link LRAConstants#NARAYANA_LRA_API_VERSION}).
     */
    private void verifyHighestSupportedVersion(ContainerRequestContext requestContext) {
        if (!requestContext.getHeaders().containsKey(LRAConstants.LRA_API_VERSION_HEADER_NAME)) {
            // no header specified, going with 'null' further into processing
            return;
        }
        if (requestContext.getHeaders().get(LRAConstants.LRA_API_VERSION_HEADER_NAME).size() > 1) {
            String errorMsg = "Multiple headers " + LRAConstants.LRA_API_VERSION_HEADER_NAME + " with API version provided."
                    +" Please, pass only one version header in the request.";
            throw new WebApplicationException(errorMsg,
                    Response.status(EXPECTATION_FAILED).entity(errorMsg)
                            .header(LRA_API_VERSION_HEADER_NAME, NARAYANA_LRA_API_VERSION_STRING).build());
        }

        String apiVersionString = requestContext.getHeaderString(LRAConstants.LRA_API_VERSION_HEADER_NAME);
        APIVersion apiVersion;
        try {
            apiVersion = APIVersion.instanceOf(apiVersionString);
        } catch (Exception iae) {
            String errorMsg = "Wrong format of the provided version " + apiVersionString + ": " + iae.getMessage();
            throw new WebApplicationException(errorMsg, iae,
                    Response.status(EXPECTATION_FAILED).entity(errorMsg)
                            .header(LRA_API_VERSION_HEADER_NAME, NARAYANA_LRA_API_VERSION_STRING).build());
        }
        if (apiVersion.compareTo(LRAConstants.NARAYANA_LRA_API_VERSION) > 0) {
            String errorMsg = "Demanded API version " + apiVersionString
                    + " is bigger than the supported one " + NARAYANA_LRA_API_VERSION_STRING;
            throw new WebApplicationException(errorMsg,
                    Response.status(EXPECTATION_FAILED).entity(errorMsg)
                            .header(LRA_API_VERSION_HEADER_NAME, NARAYANA_LRA_API_VERSION_STRING).build());
        }
    }
}
