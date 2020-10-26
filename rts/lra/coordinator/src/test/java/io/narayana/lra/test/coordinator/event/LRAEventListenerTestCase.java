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
import io.narayana.lra.test.coordinator.TestBase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

@RunWith(Arquillian.class)
@RunAsClient
public class LRAEventListenerTestCase extends TestBase {
    private static final Logger log = Logger.getLogger(LRAEventListenerTestCase.class);

    private Client client;
    private String participantUrl, eventsUrl;

    @TargetsContainer(TestBase.MANAGED_COORDINATOR_CONTAINER)
    @Deployment(name = TestBase.COORDINATOR_DEPLOYMENT, testable = false)
    public static WebArchive createDeployment() {
        // LRA uses ArjunaCore so pull in the jts module to get them on the classpath
        final String ManifestMF = "Manifest-Version: 1.0\n"
                + "Dependencies: org.jboss.jts, org.jboss.logging\n";
        return ShrinkWrap.create(WebArchive.class, COORDINATOR_DEPLOYMENT + ".war")
                .addPackages(false, coordinatorPackages)
                .addPackages(false, participantPackages)
                .addClasses(EventLogListener.class, LRAParticipant.class)
                .addAsManifestResource(new StringAsset(ManifestMF), "MANIFEST.MF")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void before() throws MalformedURLException, URISyntaxException {
        participantUrl = String.format("%s/%s", getDeploymentUrl(), LRAParticipant.PARTICIPANT_PATH);
        eventsUrl = String.format("%s/%s", getDeploymentUrl(), EventLogListener.EVENTS_PATH);

        super.before();
        client = ClientBuilder.newClient();
    }

    @After
    public void after() {
        try {
            client.close();
        } finally {
            super.after();
        }
    }

    @Test
    public void startAndClose() throws URISyntaxException {
        try (Response testInvocation = client.target(participantUrl).request().get()) {
            Assert.assertEquals(Response.Status.OK.getStatusCode(), testInvocation.getStatus());
        }
        try (Response eventListenerInvocation = client.target(eventsUrl).request().get()) {
            Assert.assertEquals(Response.Status.OK.getStatusCode(), eventListenerInvocation.getStatus());
            Assert.assertTrue("Expecting the event listener returns data in response body", eventListenerInvocation.hasEntity());
            Map<String,BigDecimal> counterData = eventListenerInvocation.readEntity(Map.class);
            log.infof("Invocation listener returned counter data: %s, size: %d", counterData, counterData.size());
            Assert.assertEquals("Expecting one LRA was started", BigDecimal.valueOf(1), counterData.get(LRAAction.STARTED.name()));
            Assert.assertEquals("Expecting one participant enlisted to LRA", BigDecimal.valueOf(1), counterData.get(LRAAction.ENLISTED.name()));
            Assert.assertEquals("Expecting the participant was completed", BigDecimal.valueOf(1), counterData.get(LRAAction.COMPLETED.name()));
            Assert.assertEquals("Expecting the LRA was closed once", BigDecimal.valueOf(1), counterData.get(LRAAction.CLOSED.name()));
            Assert.assertEquals("Expecting the LRA participant's after LRA callback was called",
                    BigDecimal.valueOf(1), counterData.get(LRAAction.AFTER_CALLBACK_FINISHED.name()));
            Assert.assertEquals("Not expecting other callback should observed by event listener but the result data contains more events",
                    5, counterData.size());
        }
    }
}
