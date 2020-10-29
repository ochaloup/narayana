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

import io.narayana.lra.LRAConstants;
import io.narayana.lra.event.LRAAction;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import static io.narayana.lra.LRAConstants.RECOVERY_COORDINATOR_PATH_NAME;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NarayanaLRARecovery implements LRARecoveryService {
    private static final Logger log = Logger.getLogger(NarayanaLRARecovery.class);
    private static final Map<URI, List<LRAAction>> processedActionRepository = new ConcurrentHashMap<>();

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
        String host = lraId.getHost();
        int port = lraId.getPort();
        Client listenerClient = ClientBuilder.newClient();

        List<LRAAction> intersectionListOfActions = new ArrayList<>();
        try {
            // receiving list of action recorded by the listener
            String listenerUrl = String.format("http://%s:%d/%s/%s",
                    host, port, "listener", URLEncoder.encode(lraId.toASCIIString(), StandardCharsets.UTF_8.name()));
            WebTarget listenerTarget = listenerClient.target(URI.create(listenerUrl));
            Response response = listenerTarget.request().get();
            List<String> receivedListOfActionsString = response.readEntity(List.class);
            List<LRAAction> receivedListOfActions = receivedListOfActionsString.stream()
                    .map(v -> LRAAction.valueOf(v)).sorted().collect(Collectors.toList());
            response.close();
            // taking the previously saved list of the action from the repository
            // (empty when called for the first time for the LRA id, defined from the prior call when called again)
            List<LRAAction> savedListOfActions = processedActionRepository.computeIfAbsent(lraId, k -> new ArrayList<>());

            // calculating difference of the receive list and saved list; expecting the receive list is longer
            Iterator<LRAAction> receivedActionIterator = receivedListOfActions.iterator();
            Iterator<LRAAction> savedActionIterator = savedListOfActions.iterator();
            LRAAction savedActionIndex = (savedActionIterator.hasNext()) ? savedActionIterator.next() : null;
            while(receivedActionIterator.hasNext()) {
                LRAAction nextReceived = receivedActionIterator.next();
                if (nextReceived == savedActionIndex) {
                    savedActionIndex = (savedActionIterator.hasNext()) ? savedActionIterator.next() : null;
                } else {
                    intersectionListOfActions.add(nextReceived);
                }
            }
            // updating the repository of the saved action
            processedActionRepository.put(lraId, receivedListOfActions);
            log.infof(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> lraId: %s, response: %s", lraId, intersectionListOfActions);
        } catch (UnsupportedEncodingException uee) {
            log.errorf("Cannot encode the LRA id %s with encoding %s. The waitForCallbacks returned immediately.",
                    lraId, StandardCharsets.UTF_8.name());
        } finally {
            listenerClient.close();
        }
        if(!intersectionListOfActions.contains(LRAAction.COMPENSATE_IN_PROGRESS) &&
           !intersectionListOfActions.contains(LRAAction.COMPLETE_IN_PROGRESS) &&
           !intersectionListOfActions.contains(LRAAction.COMPENSATE_ATTEMPT_FAILURE) &&
           !intersectionListOfActions.contains(LRAAction.COMPLETE_ATTEMPT_FAILURE) &&
           !intersectionListOfActions.contains(LRAAction.COMPENSATED) &&
           !intersectionListOfActions.contains(LRAAction.COMPLETED) &&
           !intersectionListOfActions.contains(LRAAction.AFTER_CALLBACK_ATTEMPT) &&
           !intersectionListOfActions.contains(LRAAction.AFTER_CALLBACK_FINISHED)
        ) {
            // TODO: do not use the
            waitForCallbacks(lraId);
        }
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
            URI lraCoordinatorUri = LRAConstants.getLRACoordinatorUri(lraId);
            URI recoveryCoordinatorUri = UriBuilder.fromUri(lraCoordinatorUri)
                    .path(RECOVERY_COORDINATOR_PATH_NAME).build();
            WebTarget recoveryTarget = recoveryCoordinatorClient.target(recoveryCoordinatorUri);

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
}
