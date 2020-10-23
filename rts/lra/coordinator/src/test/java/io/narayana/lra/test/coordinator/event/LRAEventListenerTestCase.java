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

import io.narayana.lra.test.coordinator.TestBase;
import org.apache.http.HttpConnection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
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
import java.net.MalformedURLException;
import java.net.URISyntaxException;

@RunWith(Arquillian.class)
@RunAsClient
public class LRAEventListenerTestCase extends TestBase {
    private static final Logger log = Logger.getLogger(LRAEventListenerTestCase.class);

    private Client client;

    @Deployment(name = TestBase.COORDINATOR_DEPLOYMENT, testable = false, managed = false)
    public static WebArchive createDeployment() {
        // LRA uses ArjunaCore so pull in the jts module to get them on the classpath
        final String ManifestMF = "Manifest-Version: 1.0\n"
                + "Dependencies: org.jboss.jts, org.jboss.logging\n";
        return ShrinkWrap.create(WebArchive.class, COORDINATOR_DEPLOYMENT + ".war")
                .addPackages(false, coordinatorPackages)
                .addPackages(false, participantPackages)
                .addPackages(true, HttpConnection.class.getPackage())
                .addClasses(EventLogListener.class)
                .addAsManifestResource(new StringAsset(ManifestMF), "MANIFEST.MF")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void before() throws MalformedURLException, URISyntaxException {
        super.before();
        client = ClientBuilder.newClient();
    }

    @After
    public void after() {
        try {
            client.close();
        } finally {
            stopContainer();
        }
        super.after();
    }

    @Test
    public void startAndClose() throws URISyntaxException {
        startContainer(null);

        String participantUrl = String.format("%s/%s", getDeploymentUrl(), LRAParticipant.PARTICIPANT_PATH);
        String eventsUrl = String.format("%s/%s", getDeploymentUrl(), EventLogListener.EVENTS_PATH);

        try (Response ignore = client.target(participantUrl).request().get()) {
            Assert.assertEquals(Response.Status.OK.getStatusCode(), ignore.getStatus());
        }
        log.infof(">>>>>> %s", client.target(eventsUrl).request().get());
    }
}
