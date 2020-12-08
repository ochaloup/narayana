/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package io.narayana.lra.arquillian.api;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.LRAData;
import io.narayana.lra.arquillian.Deployer;
import io.narayana.lra.client.NarayanaLRAClient;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;

/**
 * <p>
 * REST API tests against the LRA coordinator in version for LRA 1.0.
 * </p>
 * <p>
 * The tests serves to verify the compliance of the {@link io.narayana.lra.coordinator.api.Coordinator}
 * REST API methods with the Narayana LRA API version {@code API_VERSION_1_0}.<br/>
 * Each test case corresponds by name with the method in the {@link io.narayana.lra.coordinator.api.Coordinator}.
 * Then it verifies the expected API respond on particular state. It verifies if the status code
 * was correct, if the return data was in correct format, it check the returned headers etc.
 * </p>
 * <p>
 * This class is not expected to be changed. The Narayana LRA API of version 1.0 is not expected to be changed.<br/>
 * When Coordinator changes the API there will be updated the API version for such change.
 * The new or changed behaviour belongs to a separate test class
 * which verifies the behaviour of the particular API version.
 * </p>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CoordinatorApi_1_0_IT {
    private static final Logger log = Logger.getLogger(CoordinatorApi_1_0_IT.class);
    private static final String API_VERSION_1_0 = "1.0";

    private Client client;
    private NarayanaLRAClient lraClient;
    private String coordinatorUrl;
    private List<URI> lrasToAfterFinish;

    static final String NOT_SUPPORTED_FUTURE_LRA_VERSION = Integer.MAX_VALUE + ".1";

    static final String LRA_API_VERSION_HEADER_NAME_V1_0 = "Narayana-LRA-API-version";
    static final String RECOVERY_HEADER_NAME_V1_0 = "Long-Running-Action-Recovery";
    static final String STATUS_PARAM_NAME_V1_0 = "Status";
    static final String CLIENT_ID_PARAM_NAME_V1_0 = "ClientID";
    static final String TIME_LIMIT_PARAM_NAME_V1_0 = "TimeLimit";
    static final String PARENT_LRA_PARAM_NAME_V1_0 = "ParentLRA";

    @Rule
    public TestName testName = new TestName();

    @Deployment
    public static WebArchive deploy() {
        return Deployer.deploy(CoordinatorApi_1_0_IT.class.getSimpleName());
    }

    @Before
    public void before() {
        log.info("Running test " + testName.getMethodName());
        client = ClientBuilder.newClient();
        lraClient = new NarayanaLRAClient();
        coordinatorUrl = lraClient.getCoordinatorUrl();
        lrasToAfterFinish = new ArrayList<>();
    }

    @After
    public void after() {
        for (URI lraToFinish: lrasToAfterFinish) {
            lraClient.cancelLRA(lraToFinish);
        }
        if (client != null) {
            client.close();
        }
    }

    /**
     * GET - /
     * To gets all active LRAs.
     */
    @Test
    public void getAllLRAs() {
        // be aware of risk of non monotonic java time, ie. https://www.javaadvent.com/2019/12/measuring-time-from-java-to-kernel-and-back.html
        long beforeTime = Instant.now().toEpochMilli();

        String clientId1 = testName.getMethodName() + "_OK_1";
        String clientId2 = testName.getMethodName() + "_OK_2";
        URI lraId1 = lraClient.startLRA(clientId1);
        URI lraId2 = lraClient.startLRA(lraId1, clientId2, 0L, null);
        lrasToAfterFinish.add(lraId1); // lraId2 is nested and will be closed in regards to lraId1

        List<LRAData> data;
        try (Response response = client.target(coordinatorUrl)
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0).get()) {
            Assert.assertEquals("Expected that the call succeeds, GET/200.", Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Provided API header, expected that one is returned",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            data = response.readEntity(new GenericType<List<LRAData>>() {});
        }

        Optional<LRAData> lraTopOptional = data.stream().filter(record -> record.getLraId().equals(lraId1)).findFirst();
        Assert.assertTrue("Expected to find the top-level LRA id " + lraId1 + " from REST get all call", lraTopOptional.isPresent());
        LRAData lraTop = lraTopOptional.get();
        Optional<LRAData> lraNestedOptional = data.stream().filter(record -> record.getLraId().equals(lraId2)).findFirst();
        Assert.assertTrue("Expected to find the nested LRA id " + lraId2 + " from REST get all call", lraNestedOptional.isPresent());
        LRAData lraNested = lraNestedOptional.get();

        Assert.assertEquals("Expected top-level LRA '" + lraTop + "'  being active",
                LRAStatus.Active, lraTop.getStatus());
        Assert.assertEquals("Expected top-level LRA '" + lraTop + "'  being active, HTTP status 204.",
                Status.NO_CONTENT.getStatusCode(), lraTop.getHttpStatus());
        Assert.assertFalse("Expected top-level LRA '" + lraTop + "' not being recovering", lraTop.isRecovering());
        Assert.assertTrue("Expected top-level LRA '" + lraTop + "' to be top level", lraTop.isTopLevel());
        MatcherAssert.assertThat("Expected the start time of top-level LRA '" + lraTop + "' is after the test start time",
                beforeTime, lessThan(lraTop.getStartTime()));

        Assert.assertEquals("Expected nested LRA '" + lraNested + "'  being active",
                LRAStatus.Active, lraNested.getStatus());
        Assert.assertEquals("Expected nested LRA '" + lraNested + "'  being active, HTTP status 204.",
                Status.NO_CONTENT.getStatusCode(), lraNested.getHttpStatus());
        Assert.assertFalse("Expected nested LRA '" + lraNested + "' not being recovering", lraNested.isRecovering());
        Assert.assertFalse("Expected nested LRA '" + lraNested + "' to be nested", lraNested.isTopLevel());
        MatcherAssert.assertThat("Expected the start time of nested LRA '" + lraNested + "' is after the test start time",
                beforeTime, lessThan(lraNested.getStartTime()));
    }

    /**
     * GET - /?Status=Active
     * To gets active LRAs with status.
     */
    @Test
    public void getAllLRAsStatusFilter() {
        String clientId1 = testName.getMethodName() + "_1";
        String clientId2 = testName.getMethodName() + "_2";
        URI lraId1 = lraClient.startLRA(clientId1);
        URI lraId2 = lraClient.startLRA(lraId1, clientId2, 0L, null);
        lrasToAfterFinish.add(lraId1);
        lraClient.closeLRA(lraId2);

        try (Response response = client.target(coordinatorUrl).request().get()) {
            Assert.assertEquals("Expected that the call succeeds, GET/200.", Status.OK.getStatusCode(), response.getStatus());
            List<LRAData> data = response.readEntity(new GenericType<List<LRAData>>() {});
            Collection<URI> returnedLraIds = data.stream().map(LRAData::getLraId).collect(Collectors.toList());
            MatcherAssert.assertThat("Expected the coordinator returns the first started and second closed LRA",
                    returnedLraIds, hasItems(lraId1, lraId2));
        }
        try (Response response = client.target(coordinatorUrl)
                .queryParam(STATUS_PARAM_NAME_V1_0, "Active").request().get()) {
            Assert.assertEquals("Expected that the call succeeds, GET/200.", Status.OK.getStatusCode(), response.getStatus());
            List<LRAData> data = response.readEntity(new GenericType<List<LRAData>>() {});
            Collection<URI> returnedLraIds = data.stream().map(LRAData::getLraId).collect(Collectors.toList());
            MatcherAssert.assertThat("Expected the coordinator returns the first started top-level LRA",
                    returnedLraIds, hasItem(lraId1));
            MatcherAssert.assertThat("Expected the coordinator filtered out the non-active nested LRA",
                    returnedLraIds, not(hasItem(lraId2)));
        }
    }

    /**
     * GET - /?Status=NonExistingStatus
     * Asking for LRAs with status while providing a wrong status.
     */
    @Test
    public void getAllLRAsFailedStatus() {
        String nonExistingStatusValue = "NotExistingStatusValue";
        try (Response response = client.target(coordinatorUrl)
                .queryParam(STATUS_PARAM_NAME_V1_0, nonExistingStatusValue).request().get()) {
            Assert.assertEquals("Expected that the call fails on wrong status, GET/500.",
                    Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the failure to contain the wrong status value",
                    response.readEntity(String.class), containsString(nonExistingStatusValue));
        }
    }

    /**
     * GET - /{lraId}/status
     * Finding a status of a started LRA.
     */
    @Test
    public void getLRAStatus() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl).path(encodedLraId).path("status")
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0).get()) {
            Assert.assertEquals("Expected that the get status call succeeds, GET/200.", Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Expected API header, the latest one version  to be returned",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            Assert.assertEquals("Expected the returned LRA status is Active",
                    "Active", response.readEntity(String.class));
        }
    }

    /**
     * GET - /{lraId}/status
     * Finding a status of a non existing LRA or wrong LRA id.
     */
    @Test
    public void getLRAStatusFailed() {
        String nonExistingLRAUrl = "http://localhost:1234/Non-Existing-LRA-id";
        try (Response response = client.target(coordinatorUrl).path(nonExistingLRAUrl).path("status").request().get()) {
            Assert.assertEquals("Expected that the call finds not found of " + nonExistingLRAUrl + ", GET/404.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the failure message to contain the wrong LRA id",
                    response.readEntity(String.class), containsString(nonExistingLRAUrl));
        }

        String nonExistingLRAWrongUrlFormat = "Non-Existing-LRA-id";
        try (Response response = client.target(coordinatorUrl).path(nonExistingLRAWrongUrlFormat).path("status").request().get()) {
            Assert.assertEquals("Expected that the call fails on LRA not found of " + nonExistingLRAWrongUrlFormat + " , GET/404.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the failure message to contain the wrong LRA id",
                    response.readEntity(String.class), containsString(lraClient.getCoordinatorUrl() + "/" + nonExistingLRAWrongUrlFormat));
        }
    }

    /**
     * GET - /{lraId}
     * Obtaining info of a started LRA.
     */
    @Test
    public void getLRAInfo() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl).path(encodedLraId)
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0).get()) {
            Assert.assertEquals("Expected that the get status call succeeds, GET/200.", Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Expected API header, the latest one version  to be returned",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            LRAData data = response.readEntity(new GenericType<LRAData>() {});
            Assert.assertEquals("Expected returned LRA is started one", lraId, data.getLraId());
            Assert.assertEquals("Expected the returned LRA being Active", LRAStatus.Active, data.getStatus());
            Assert.assertTrue("Expected the returned LRA is top-level", data.isTopLevel());
            Assert.assertEquals("Expected the returned LRA get HTTP status as active, HTTP status 204.",
                    Status.NO_CONTENT.getStatusCode(), data.getHttpStatus());
        }
    }

    /**
     * GET - /{lraId}
     * Obtaining info of a non-existing LRA.
     */
    @Test
    public void getLRAInfoNotExisting() {
        String nonExistingLRA = "Non-Existing-LRA-id";
        try (Response response = client.target(coordinatorUrl).path(nonExistingLRA).request().get()) {
            Assert.assertEquals("Expected that the call fails on LRA not found, GET/404.", Status.NOT_FOUND.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the failure message to contain the wrong LRA id",
                    response.readEntity(String.class), containsString(nonExistingLRA));
        }
    }

    /**
     * POST - /start?TimeLimit=...&ClientID=...&ParentLRA=...
     * PUT - /{lraId}/close
     * Starting and closing an LRA.
     */
    @Test
    public void startCloseLRA() throws UnsupportedEncodingException {
        URI lraId1, lraId2;

        try (Response response = client.target(coordinatorUrl)
                .path("start")
                .queryParam(CLIENT_ID_PARAM_NAME_V1_0, testName.getMethodName() + "_1")
                .queryParam(TIME_LIMIT_PARAM_NAME_V1_0, "-42") // negative time limit is permitted by spec
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .post(null)) {
            Assert.assertEquals("Creating top-level LRA should be successful, POST/201 is expected.",
                    Status.CREATED.getStatusCode(), response.getStatus());
            lraId1 = URI.create(response.readEntity(String.class));
            Assert.assertNotNull("Expected non null LRA id from entity of response '" + response + "'", lraId1);
            lrasToAfterFinish.add(lraId1);

            URI lraIdFromLocationHeader = URI.create(response.getHeaderString(HttpHeaders.LOCATION));
            Assert.assertEquals("Expecting the LOCATION header configures the same LRA id as entity content on starting top-level LRA",
                    lraId1, lraIdFromLocationHeader);
            // context header is returned strangely to client, some investigation will be needed
            // URI lraIdFromLRAContextHeader = URI.create(response.getHeaderString(LRA.LRA_HTTP_CONTEXT_HEADER));
            // Assert.assertEquals("Expecting the LRA context header configures the same LRA id as entity content on starting top-level LRA",
            //        lraId1, lraIdFromLRAContextHeader);
            Assert.assertEquals("Expecting to get the same API version as used for the request on top-level LRA start",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
        }

        String encodedLraId1 = URLEncoder.encode(lraId1.toString(), StandardCharsets.UTF_8.name());
        try(Response response = client.target(coordinatorUrl)
                .path("start")
                .queryParam(CLIENT_ID_PARAM_NAME_V1_0, testName.getMethodName() + "_2")
                .queryParam(PARENT_LRA_PARAM_NAME_V1_0, encodedLraId1)
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .post(null)) {
            Assert.assertEquals("Creating nested LRA should be successful, POST/201 is expected.",
                    Status.CREATED.getStatusCode(), response.getStatus());
            lraId2 = URI.create(response.readEntity(String.class));
            Assert.assertNotNull("Expected non null nested LRA id from entity of response '" + response + "'", lraId2);

            // the nested LRA id is in format <nested LRA id>?ParentLRA=<parent LRA id>
            URI lraIdFromLocationHeader = URI.create(response.getHeaderString(HttpHeaders.LOCATION));
            Assert.assertEquals("Expecting the LOCATION header configures the same LRA id as entity content on starting nested LRA",
                    lraId2, lraIdFromLocationHeader);
            // context header is returned strangely to client, some investigation will be needed
            // String lraContextHeader = response.getHeaderString(LRA.LRA_HTTP_CONTEXT_HEADER);
            // the context header is in format <parent LRA id>,<nested LRA id>?ParentLRA=<parent LRA id>
            // MatcherAssert.assertThat("Expected the nested LRA context header gives the parent LRA id at first",
            //        lraContextHeader, startsWith(lraId1.toASCIIString()));
            // MatcherAssert.assertThat("Expected the nested LRA context header provides LRA id of started nested LRA",
            //        lraContextHeader, containsString("," + lraId2.toASCIIString()));
            Assert.assertEquals("Expecting to get the same API version as used for the request on nested LRA start",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
        }

        Collection<URI> returnedLraIds = lraClient.getAllLRAs().stream().map(LRAData::getLraId).collect(Collectors.toList());
        MatcherAssert.assertThat("Expected the coordinator knows about the top-level LRA", returnedLraIds, hasItem(lraId1));
        MatcherAssert.assertThat("Expected the coordinator knows about the nested LRA", returnedLraIds, hasItem(lraId2));

        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId1 + "/close")
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .put(null)) {
            lrasToAfterFinish.clear(); // we've closed the LRA manually here, skipping the @After
            Assert.assertEquals("Closing top-level LRA should be successful, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Closing top-level LRA should return the right status.",
                    LRAStatus.Closed.name(), response.readEntity(String.class));
            Assert.assertEquals("Expecting to get the same API version as used for the request to close top-level LRA",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
        }

        Collection<LRAData> activeLRAsAfterClosing = lraClient.getAllLRAs().stream()
                .filter(data -> data.getLraId().equals(lraId1) || data.getLraId().equals(lraId2)).collect(Collectors.toList());
        MatcherAssert.assertThat("Expecting the started LRAs are not more active after closing the top-level one",
                activeLRAsAfterClosing, emptyCollectionOf(LRAData.class));
    }

    /**
     * POST - /start?ClientID=...
     * PUT - /{lraId}/cancel
     * Starting and canceling an LRA.
     */
    @Test
    public void startCancelLRA() throws UnsupportedEncodingException {
        URI lraId;
        try (Response response = client.target(coordinatorUrl)
                .path("start")
                .queryParam(CLIENT_ID_PARAM_NAME_V1_0, testName.getMethodName())
                .request()
                .post(null)) {
            Assert.assertEquals("Creating top-level LRA should be successful, POST/201 is expected.",
                    Status.CREATED.getStatusCode(), response.getStatus());
            lraId = URI.create(response.readEntity(String.class));
            Assert.assertNotNull("Expected non null LRA id from entity of response '" + response + "'", lraId);
            lrasToAfterFinish.add(lraId);
            Assert.assertEquals("Expecting to get the most up-to-date API version when passed no one on POST query",
                    LRAConstants.NARAYANA_LRA_API_VERSION_STRING, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
        }

        Collection<URI> returnedLraIds = lraClient.getAllLRAs().stream().map(LRAData::getLraId).collect(Collectors.toList());
        MatcherAssert.assertThat("Expected the coordinator knows about the LRA", returnedLraIds, hasItem(lraId));
        try (Response response = client.target(coordinatorUrl)
                .path(URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name()) + "/cancel")
                .request()
                .put(null)) {
            lrasToAfterFinish.clear(); // we've closed the LRA manually just now, skipping the @After
            Assert.assertEquals("Closing LRA should be successful, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Canceling top-level LRA should return the right status.",
                    LRAStatus.Cancelled.name(), response.readEntity(String.class));
            Assert.assertEquals("Expecting to get the most up-to-date API version when passed no one on POST query",
                    LRAConstants.NARAYANA_LRA_API_VERSION_STRING, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
        }

        Collection<LRAData> activeLRAsAfterClosing = lraClient.getAllLRAs().stream()
                .filter(data -> data.getLraId().equals(lraId)).collect(Collectors.toList());
        MatcherAssert.assertThat("Expecting the started LRA is no more active after closing it",
                activeLRAsAfterClosing, emptyCollectionOf(LRAData.class));
    }

    /**
     * POST - /start?ClientId=...&ParentLRA=...
     * Starting a nested LRA with a non-existing parent.
     */
    @Test
    public void startLRANotExistingParentLRA() {
        String notExistingParentLRA = "not-existing-parent-lra-id";
        try (Response response = client.target(coordinatorUrl)
                .path("start")
                .queryParam(CLIENT_ID_PARAM_NAME_V1_0, testName.getMethodName())
                .queryParam(PARENT_LRA_PARAM_NAME_V1_0, notExistingParentLRA)
                .request()
                .post(null)) {
            Assert.assertEquals("Expected failure on non-existing parent LRA, POST/404 is expected.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            String errorMsg = response.readEntity(String.class);
            MatcherAssert.assertThat("Expected error message to contain the not found parent LRA id",
                    errorMsg, containsString(notExistingParentLRA));
        }
    }

    /**
     * PUT - /{lraId}/close
     * Closing a non-existing LRA.
     */
    @Test
    public void closeNotExistingLRA() {
        String notExistingLRAid = "not-existing-lra-id";
        try (Response response = client.target(coordinatorUrl)
                .path(notExistingLRAid)
                .path("close")
                .request()
                .put(null)) {
            Assert.assertEquals("Expected failure on non-existing LRA id, PUT/404 is expected.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            String errorMsg = response.readEntity(String.class);
            MatcherAssert.assertThat("Expected error message to contain the not found LRA id",
                    errorMsg, containsString(notExistingLRAid));
        }
    }

    /**
     * PUT - /{lraId}/cancel
     * Canceling a non-existing LRA.
     */
    @Test
    public void cancelNotExistingLRA() {
        String notExistingLRAid = "not-existing-lra-id";
        try (Response response = client.target(coordinatorUrl)
                .path(notExistingLRAid)
                .path("cancel")
                .request()
                .put(null)) {
            Assert.assertEquals("Expected failure on non-existing LRA id, PUT/404 is expected.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            String errorMsg = response.readEntity(String.class);
            MatcherAssert.assertThat("Expected error message to contain the not found LRA id",
                    errorMsg, containsString(notExistingLRAid));
        }
    }

    /**
     * PUT - /renew?TimeLimit=
     * Renewing the time limit of the started LRA.
     */
    @Test
    public void renewTimeLimit() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        Optional<LRAData> data = lraClient.getAllLRAs().stream().filter(l -> l.getLraId().equals(lraId)).findFirst();
        Assert.assertTrue("Expected the started LRA will retrieved by LRA client get", data.isPresent());
        Assert.assertEquals("Expected not defined finish time", 0L, data.get().getFinishTime());

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .path("renew")
                .queryParam(TIME_LIMIT_PARAM_NAME_V1_0, Integer.MAX_VALUE)
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .put(null)) {
            Assert.assertEquals("Expected time limit request to succeed, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Expecting to get the most up-to-date API version when passed no one on POST query",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            MatcherAssert.assertThat("Expected the found LRA id is returned",
                    response.readEntity(String.class), containsString(lraId.toString()));
        }

        data = lraClient.getAllLRAs().stream().filter(l -> l.getLraId().equals(lraId)).findFirst();
        Assert.assertTrue("Expected the started LRA will retrieved by LRA client get", data.isPresent());
        MatcherAssert.assertThat("Expected finish time to not be 0 as time limit was defined",
                data.get().getFinishTime(), greaterThan(0L));
    }

    /**
     * PUT - /renew?TimeLimit=
     * Renewing the time limit of a non-existing LRA.
     */
    @Test
    public void renewTimeLimitNotExistingLRA() {
        String notExistingLRAid = "not-existing-lra-id";
        try (Response response = client.target(coordinatorUrl)
                .path(notExistingLRAid)
                .path("renew")
                .queryParam(TIME_LIMIT_PARAM_NAME_V1_0, Integer.MAX_VALUE)
                .request()
                .put(null)) {
            Assert.assertEquals("Expected time limit request to succeed, PUT/404 is expected.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            String errorMsg = response.readEntity(String.class);
            MatcherAssert.assertThat("Expected error message to contain the not found LRA id",
                    errorMsg, containsString(notExistingLRAid));
        }
    }

    /**
     * PUT - /{lraId}
     * Joining an LRA participant via entity body.
     */
    @Test
    public void joinLRAWithBody() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .put(Entity.text("http://compensator.url:8080"))) {
            Assert.assertEquals("Expected joining LRA succeeded, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Expecting API version header",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            String recoveryHeaderUrlMessage = response.getHeaderString(RECOVERY_HEADER_NAME_V1_0);
            String recoveryUrlBody = response.readEntity(String.class);
            URI recoveryUrlLocation = response.getLocation();
            Assert.assertEquals("Expecting returned body and recovery header has the same content",
                    recoveryUrlBody, recoveryHeaderUrlMessage);
            Assert.assertEquals("Expecting returned body and location has the same content",
                    recoveryUrlBody, recoveryUrlLocation.toString());
            MatcherAssert.assertThat("Expected returned message contains the subpath of LRA recovery URL",
                    recoveryUrlBody, containsString("lra-coordinator/recovery"));
            MatcherAssert.assertThat("Expected returned message contains the LRA id",
                    recoveryUrlBody, containsString(encodedLraId));
        }
    }

    /**
     * PUT - /{lraId}
     * Joining an LRA participant via link header.
     */
    @Test
    public void joinLRAWithLinkSimple() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .header("Link", "http://compensator.url:8080")
                .put(null)) {
            Assert.assertEquals("Expected joining LRA succeeded, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Expecting API version header",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            String recoveryHeaderUrlMessage = response.getHeaderString(RECOVERY_HEADER_NAME_V1_0);
            String recoveryUrlBody = response.readEntity(String.class);
            URI recoveryUrlLocation = response.getLocation();
            Assert.assertEquals("Expecting returned body and recovery header has the same content",
                    recoveryUrlBody, recoveryHeaderUrlMessage);
            Assert.assertEquals("Expecting returned body and location has the same content",
                    recoveryUrlBody, recoveryUrlLocation.toString());
            MatcherAssert.assertThat("Expected returned message contains the subpath of LRA recovery URL",
                    recoveryUrlBody, containsString("lra-coordinator/recovery"));
            MatcherAssert.assertThat("Expected returned message contains the LRA id",
                    recoveryUrlBody, containsString(encodedLraId));
        }
    }

    /**
     * PUT - /{lraId}
     * Joining an LRA participant via link header with link rel specified.
     */
    @Test
    public void joinLRAWithLinkCompensate() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        Link link = Link.fromUri("http://compensate.url:8080").rel("compensate").build();
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .request()
                .header("Link", link.toString())
                .put(null)) {
            Assert.assertEquals("Expected joining LRA succeeded, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Expecting the most up-to-date API version header",
                    LRAConstants.NARAYANA_LRA_API_VERSION_STRING, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            String recoveryHeaderUrlMessage = response.getHeaderString(RECOVERY_HEADER_NAME_V1_0);
            String recoveryUrlBody = response.readEntity(String.class);
            Assert.assertEquals("Expecting returned body and recovery header has the same content",
                    recoveryUrlBody, recoveryHeaderUrlMessage);
            MatcherAssert.assertThat("Expected returned message contains the subpath of LRA recovery URL",
                    recoveryUrlBody, containsString("lra-coordinator/recovery"));
        }
    }

    /**
     * PUT - /{lraId}
     * Joining an LRA participant via link header with link after specified.
     */
    @Test
    public void joinLRAWithLinkAfter() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        Link afterLink = Link.fromUri("http://after.url:8080").rel("after").build();
        Link unknownLink = Link.fromUri("http://unknow.url:8080").rel("uknown").build();
        String linkList = afterLink.toString() + "," + unknownLink.toString();
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .request()
                .header("Link", linkList)
                .put(null)) {
            Assert.assertEquals("Expected joining LRA succeeded, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            String recoveryHeaderUrlMessage = response.getHeaderString(RECOVERY_HEADER_NAME_V1_0);
            String recoveryUrlBody = response.readEntity(String.class);
            Assert.assertEquals("Expecting returned body and recovery header has the same content",
                    recoveryUrlBody, recoveryHeaderUrlMessage);
            MatcherAssert.assertThat("Expected returned message contains the subpath of LRA recovery URL",
                    URLDecoder.decode(recoveryUrlBody, StandardCharsets.UTF_8.name()), containsString("lra-coordinator/recovery"));
        }
    }

    /**
     * PUT - /{lraId}
     * Joining an LRA participant via link header with wrong link format.
     */
    @Test
    public void joinLRAIncorrectLinkFormat() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);
        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .request()
                .header("Link", "<link>;rel=myrel;<wrong>")
                .put(null)) {
            Assert.assertEquals("Expected the join failing, PUT/500 is expected.",
                    Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        }
    }

    /**
     * PUT - /{lraId}
     * Joining a non-existing LRA.
     */
    @Test
    public void joinLRAUnknownLRA() {
        String notExistingLRAid = "not-existing-lra-id";
        try (Response response = client.target(coordinatorUrl)
                .path(notExistingLRAid)
                .request()
                .put(Entity.text("http://localhost:8080"))) {
            Assert.assertEquals("Expected the join failing on unknown LRA id, PUT/404 is expected.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected error message to contain the LRA id where enlist failed",
                    response.readEntity(String.class), containsString(notExistingLRAid));
        }
    }

    /**
     * PUT - /{lraId}
     * Joining an LRA participant via entity body of a wrong format.
     */
    @Test
    public void joinLRAWrongCompensatorData() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);
        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .request()
                .put(Entity.text("this-is-not-an-url::::"))) {
            Assert.assertEquals("Expected the join failing on wrong compensator data format, PUT/412 is expected.",
                    Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected error message to contain the LRA id where enlist failed",
                    response.readEntity(String.class), containsString(lraId.toString()));
        }
    }

    /**
     * PUT - /{lraId}
     * Joining an LRA participant via link header missing required rel items.
     */
    @Test
    public void joinLRAWithLinkNotEnoughData() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);

        String encodedLraId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        Link link = Link.fromUri("http://complete.url:8080").rel("complete").build();
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLraId)
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .header("Link", link.toString())
                .put(null)) {
            Assert.assertEquals("Expected the joining fails as no compensate in link, PUT/400 is expected.",
                    Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            String errorMsg = response.readEntity(String.class);
            MatcherAssert.assertThat("Expected error message to contain the LRA id where enlist failed",
                    errorMsg, containsString(lraId.toString()));
        }
    }

    /**
     * PUT - /{lraId}/remove
     * Leaving an LRA as participant.
     */
    @Test
    public void leaveLRA() throws UnsupportedEncodingException {
        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);
        URI recoveryUri = lraClient.joinLRA(lraId, 0L, URI.create("http://localhost:8080"), "");

        String encodedLRAId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl)
                .path(encodedLRAId)
                .path("remove")
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .put(Entity.text(recoveryUri.toString()))) {
            Assert.assertEquals("Expected leaving of LRA to succeed, PUT/200 is expected.",
                    Status.OK.getStatusCode(), response.getStatus());
            Assert.assertEquals("Expecting API version header",
                    API_VERSION_1_0, response.getHeaderString(LRA_API_VERSION_HEADER_NAME_V1_0));
            Assert.assertFalse("Expecting 'remove' API call returns no entity body", response.hasEntity());
        }

        try (Response response = client.target(coordinatorUrl)
                .path(encodedLRAId)
                .path("remove")
                .request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, API_VERSION_1_0)
                .put(Entity.text(recoveryUri.toString()))) {
            Assert.assertEquals("Expected leaving of LRA to fail as it was removed just before, PUT/400 is expected.",
                    Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the failure message to contain the non existing participant id",
                    response.readEntity(String.class), containsString(recoveryUri.toASCIIString()));
        }
    }

    /**
     * PUT - /{lraId}/remove
     * Leaving a non-existing LRA as participant.
     */
    @Test
    public void leaveLRANonExistingFailure() throws UnsupportedEncodingException {
        String nonExistingLRAId = "http://localhost:1234/Non-Existing-LRA-id";
        String encodedNonExistingLRAId = URLEncoder.encode(nonExistingLRAId, StandardCharsets.UTF_8.name());
        try (Response response = client.target(coordinatorUrl).path(encodedNonExistingLRAId).path("remove").request().put(Entity.text("nothing"))) {
            Assert.assertEquals("Expected that the call finds not found of " + encodedNonExistingLRAId + ", PUT/404.",
                    Status.NOT_FOUND.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the failure message to contain the wrong LRA id",
                    response.readEntity(String.class), containsString(nonExistingLRAId));
        }

        URI lraId = lraClient.startLRA(testName.getMethodName());
        lrasToAfterFinish.add(lraId);
        String encodedLRAId = URLEncoder.encode(lraId.toString(), StandardCharsets.UTF_8.name());
        String nonExistingParticipantUrl = "http://localhost:1234/Non-Existing-participant-LRA";
        try (Response response = client.target(coordinatorUrl).path(encodedLRAId).path("remove").request()
                .put(Entity.text(nonExistingParticipantUrl))) {
            Assert.assertEquals("Expected that the call fails on LRA participant " + nonExistingParticipantUrl + " not found , PUT/400.",
                    Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the failure message to contain the wrong participant id",
                    response.readEntity(String.class), containsString(nonExistingParticipantUrl));
        }
    }


    // ------------------------ VERSION HEADER VERIFICATION -----------------------------
    // Test methods down here verifies a wrong version being provided to the API

    @Test
    public void getAllLRAsWrongVersion() {
        try (Response response = client.target(coordinatorUrl)
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).get()) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

    @Test
    public void getLRAStatusWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("status")
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).get()) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

    @Test
    public void getLRAInfoWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("lra-id")
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).get()) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

    @Test
    public void startLRAWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("start")
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).post(null)) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

    @Test
    public void renewTimeLimitWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("lra-id").path("renew")
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).put(null)) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

    @Test
    public void closeLRAWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("lra-id").path("close")
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).put(null)) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

    @Test
    public void cancelLRAWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("lra-id").path("cancel")
                .request().header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).put(null)) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

    @Test
    public void joinViaBodyWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("lra-id").request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).put(Entity.text("compensator-url"))) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }
    @Test
    public void leaveLRAWrongVersion() {
        try (Response response = client.target(coordinatorUrl).path("lra-id").path("remove").request()
                .header(LRA_API_VERSION_HEADER_NAME_V1_0, NOT_SUPPORTED_FUTURE_LRA_VERSION).put(Entity.text("participant-url"))) {
            Assert.assertEquals("Expected version on method call is not supported, GET/417.",
                    Status.EXPECTATION_FAILED.getStatusCode(), response.getStatus());
            MatcherAssert.assertThat("Expected the response to contain the wrong version",
                    response.readEntity(String.class), containsString(NOT_SUPPORTED_FUTURE_LRA_VERSION));
        }
    }

}
