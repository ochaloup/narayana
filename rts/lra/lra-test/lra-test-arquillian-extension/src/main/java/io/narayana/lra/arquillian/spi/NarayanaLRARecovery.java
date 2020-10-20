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
import java.net.URI;

import static io.narayana.lra.LRAConstants.RECOVERY_COORDINATOR_PATH_NAME;
import static io.narayana.lra.LRAConstants.COORDINATOR_PATH_NAME;

public class NarayanaLRARecovery implements LRARecoveryService {
    private static final Logger log = Logger.getLogger(NarayanaLRARecovery.class);

    /**
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
        String host = lraId.getHost();
        int port = lraId.getPort();
        if (!recoverLRAs(host, port, lraId)) {
            // first recovery scan probably collided with periodic recovery which started
            // before the test execution so try once more
            return recoverLRAs(host, port, lraId);
        }

        return true;
    }

    /**
     * Invokes LRA coordinator recovery REST endpoint and returns whether the recovery of intended LRAs happended
     *
     * @param host  the LRA coordinator host address
     * @param port  the LRA coordinator port
     * @param lraId the LRA id of the LRA that is intended to be recovered
     * @return true the intended LRA recovered, false otherwise
     */
    private boolean recoverLRAs(String host, int port, URI lraId) {
        // trigger a recovery scan
        Client recoveryCoordinatorClient = ClientBuilder.newClient();

        try {
            String recoveryCoordinatorUrl = String.format("http://%s:%d/%s/%s/recovery",
                host, port, COORDINATOR_PATH_NAME, RECOVERY_COORDINATOR_PATH_NAME);
            WebTarget recoveryTarget = recoveryCoordinatorClient.target(URI.create(recoveryCoordinatorUrl));

            // send the request to the recovery coordinator
            Response response = recoveryTarget.request().get();
            String json = response.readEntity(String.class);
            response.close();

            if (json.contains(lraId.toASCIIString())) {
                // intended LRA didn't recover
                return false;
            }

            return true;
        } finally {
            recoveryCoordinatorClient.close();
        }
    }

    /**
     * <p>
     * This method extracts the coordinator URI from the provided LRA id.
     * The Narayana LRA id consist of URI of the LRA coordinator followed with the LRA id.
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
    URI extractLRACoordinatorUri(URI lraId) {
        // we know the LRA coordinator API is hardcoded at address of LRAConstants.COORDINATOR_PATH_NAME
        return null;
    }
}
