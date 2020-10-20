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

package io.narayana.lra.arquillian.spi;

import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;

import static io.narayana.lra.LRAConstants.*;

public class NarayanaLRARecovery implements LRARecoveryService {
    private static final Logger log = Logger.getLogger(NarayanaLRARecovery.class);

    /*
     * A bit of hacking to change the internals of annotations defined in LRA TCK.
     * There is need to adjust timeout defined on the annotation definition.
     */
    static {
        String[] resourceClassNames = new String[]{
                "org.eclipse.microprofile.lra.tck.participant.api.LraResource",
                "org.eclipse.microprofile.lra.tck.participant.api.RecoveryResource"};
        for(String resourceClassName: resourceClassNames) {
            try {
                Class<?> clazz = Class.forName(resourceClassName);
                LRAAnnotationAdjuster.processWithClass(clazz);
            } catch (ClassNotFoundException e) {
                log.debugf("Cannot load class %s to adjust LRA annotation on the class", resourceClassName);
            }
        }
    }

    @Override
    public void waitForCallbacks(URI lraId) {
        // no action needed
    }

    @Override
    public boolean waitForEndPhaseReplay(URI lraId) {
        if (!recoverLRAs(lraId)) {
            // first recovery scan probably collided with periodic recovery which started
            // before the test execution so try once more
            return recoverLRAs(lraId);
        }
        return true;
    }

    /**
     * Invokes LRA coordinator recovery REST endpoint and returns whether the recovery of intended LRAs happened
     *
     * @param lraId the LRA id of the LRA that is intended to be recovered
     * @return true the intended LRA recovered, false otherwise
     */
    private boolean recoverLRAs(URI lraId) {
        // trigger a recovery scan
        Client recoveryCoordinatorClient = ClientBuilder.newClient();

        try {
            URI lraCoordinatorUri = extractLRACoordinatorUri(lraId);
            URI recoveryCoordinatorUri = UriBuilder.fromUri(lraCoordinatorUri)
                    .path(RECOVERY_COORDINATOR_PATH_NAME).path(RECOVERY_COORDINATOR_SUB_RESOURCE_NAME).build();
            WebTarget recoveryTarget = recoveryCoordinatorClient.target(recoveryCoordinatorUri);

            // send the request to the recovery coordinator
            Response response = recoveryTarget.request().get();
            String json = response.readEntity(String.class);
            response.close();

            // intended LRA didn't recover
            return !json.contains(lraId.toASCIIString());
        } finally {
            recoveryCoordinatorClient.close();
        }
    }

    /**
     * <p>
     * This method extracts the coordinator URI from the provided LRA id.
     * The Narayana LRA id consist of URI of the LRA coordinator followed with the LRA id.
     * We know the LRA coordinator API is hardcoded with path {@value io.narayana.lra.LRAConstants#COORDINATOR_PATH_NAME}
     * and the string will be searched in the original LRA id for.
     * </p>
     * <p>
     * The Narayana LRA works with HTTP requests and URL is used for identification. An example of the standard pattern of the provided URI is:
     * {@code http://localhost:8080/deployment/lra-coordinator/0_ffff0a28054b_9133_5f855916_a7}.
     * This told us that the LRA can be accessed at the provided address by HTTP call.
     * The LRA coordinator base API address is available at {@code http://localhost:8080/deployment/lra-coordinator}
     * and the {@code 0_ffff0a28054b_9133_5f855916_a7} is the LRA transaction identifier used inside of the Coordinator
     * to differentiate the LRA instances.
     * </p>
     *
     * @param lraId  LRA URI for LRA Coordinator base API address being extracted from
     * @return LRA Coordinator base address
     */
    static URI extractLRACoordinatorUri(URI lraId) {
        // we know the LRA coordinator API is hardcoded at address of LRAConstants.COORDINATOR_PATH_NAME
        try {
            String lraCoordinatorSubPath = lraId.getPath(); // as base taking the whole LRA id path
            int coordinatorPathIndex = lraCoordinatorSubPath.lastIndexOf(COORDINATOR_PATH_NAME);
            if (coordinatorPathIndex != lraCoordinatorSubPath.indexOf(COORDINATOR_PATH_NAME)) {
                log.warnf("Extracting LRA coordinator URI from LRA id '%s' but the string contains multiple " +
                        "occurrences of the path '%s'. The coordinator path could not be constructed properly.",
                        lraId, COORDINATOR_PATH_NAME);
                if (coordinatorPathIndex == -1) {
                    coordinatorPathIndex = lraCoordinatorSubPath.length();
                }
            }
            if (coordinatorPathIndex == -1) {
                log.warnf("Extracting LRA coordinator URI from LRA id '%s' but the string contains no " +
                                "occurrences of the path '%s'. Returning just the LRA id path as it is.",
                                lraId, COORDINATOR_PATH_NAME);
            } else {
                lraCoordinatorSubPath = lraCoordinatorSubPath.substring(0, coordinatorPathIndex) + COORDINATOR_PATH_NAME;
            }
            return new URI(lraId.getScheme(), lraId.getUserInfo(), lraId.getHost(), lraId.getPort(), lraCoordinatorSubPath,
                    null, null); // not using the query and fragment from the original LRA id URI
        } catch (URISyntaxException use) {
            throw new IllegalStateException("Cannot build LRA coordinator URI based on the LRA id " + lraId, use);
        }
    }
}
