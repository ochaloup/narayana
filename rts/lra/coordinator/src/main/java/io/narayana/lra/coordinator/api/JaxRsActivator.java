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

package io.narayana.lra.coordinator.api;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.coordinator.internal.APIVersion;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

// mark the war as a JAX-RS archive
@ApplicationPath("/")
@OpenAPIDefinition(
    info = @Info(title = "LRA Coordinator", version = JaxRsActivator.LRA_API_VERSION_STRING,
        contact = @Contact(name = "Narayana", url = "https://narayana.io")),
    components = @Components(
        parameters = {
            @Parameter(name = LRAConstants.LRA_API_VERSION_HEADER_NAME, in = ParameterIn.HEADER,
                description = "API version string in format [major].[minor]-[prerelease]. Major and minor are required and to be numbers, prerelease part is optional.")
        },
        headers = {
            @Header(name = LRAConstants.LRA_API_VERSION_HEADER_NAME, description = "Narayana LRA API version that processed the request")
        }
    )
)
public class JaxRsActivator extends Application {
   /**
     * The LRA API version supported for the release.
     * Any bigger version is considered as unimplemented and unknown.
     * Any lower version is considered as older, implemented but deprecated and in case not supported.
     */
    public static final String LRA_API_VERSION_STRING = "1.0-RC1";
    public static final APIVersion LRA_API_VERSION = APIVersion.instanceOf(LRA_API_VERSION_STRING);
}
