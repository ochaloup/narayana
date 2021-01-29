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
package io.narayana.lra.coordinator;

import io.narayana.lra.logging.LRALogger;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static io.narayana.lra.coordinator.LRAListener.LRA_LISTENER_ACTION;
import static io.narayana.lra.coordinator.LRAListener.LRA_LISTENER_STATUS;
import static io.narayana.lra.coordinator.LRAListener.LRA_SHORT_TIMELIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test that check that LRA deadlines are respected during crash recovery
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LRACoordinatorRecovery1TestCase extends JDBCTestBaseImpl {

    private Client client;

    @Before
    public void before() throws MalformedURLException, URISyntaxException {
        super.before();

        client = ClientBuilder.newClient();
        startContainer("participant-byteman-rules");
    }

    @After
    public void after() {
        client.close();
        stopContainer();
        super.after();
    }

    /**
     * Test that an LRA which times out while there is no running coordinator is cancelled
     * when a coordinator is restarted
     * @throws URISyntaxException if the LRA or recovery URIs are invalid (should never happen)
     */
    @Test
    public void testRecovery(@ArquillianResource @OperateOnDeployment(COORDINATOR_DEPLOYMENT) URL deploymentUrl) throws URISyntaxException, InterruptedException {
        String lraId;
        URI lraListenerURI = UriBuilder.fromUri(deploymentUrl.toURI()).path(LRAListener.LRA_LISTENER_PATH).build();

        // start an LRA with a short time limit by invoking a resource annotated with @LRA
        try (Response response = client.target(lraListenerURI).path(LRA_LISTENER_ACTION)
                .request()
                .put(null)) {

            Assert.assertEquals("LRA participant action", 200, response.getStatus());

            lraId = response.readEntity(String.class);
            fail(testName + ": byteman should have killed the container");
        } catch (RuntimeException e) {
            LRALogger.logger.infof("%s: byteman killed the container", testName);
        }

        // the byteman script should have killed the JVM
        // wait for a period longer than the timeout - ie. waiting for the LRA timeouts - before restarting the coordinator
        doWait(LRA_SHORT_TIMELIMIT * 1000);

        restartContainer();

        // expecting the LRA was timeouted, forcing recovery to invoke the participant's callbacks
        int sc = recover();
        if (sc != 0) {
            // first  recover() was not able to finish all penging LRAs, forcing for second time to be sure the callbacks were called
            recover();
        }

        // verify that the resource was notified that the LRA finished
        String listenerStatus = getStatusFromListener(lraListenerURI);

        assertEquals("LRA listener should have been told that the final state of the LRA was cancelled",
                LRAStatus.Cancelled.name(), listenerStatus);
    }

    /**
     * Ask {@link LRAListener} if it has been notified of the final outcome of the LRA
     * @return the listeners view of the LRA status
     */
    private String getStatusFromListener(URI lraListenerURI) {
        try (Response response = client.target(lraListenerURI).path(LRA_LISTENER_STATUS)
                .request()
                .get()) {

            Assert.assertEquals("LRA participant HTTP status", 200, response.getStatus());

            return response.readEntity(String.class);
        }
    }
}
