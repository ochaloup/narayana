import org.jboss.narayana.rts.lra.compensator.api.CompensatorStatus;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import org.jboss.narayana.rts.lra.coordinator.api.LRAStatus;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Demo {
    private URL MICRSERVICE_BASE_URL;

    private LRAClient lraClient;

    @Before
    public void setupClass() throws MalformedURLException, URISyntaxException {
        MICRSERVICE_BASE_URL = new URL("http://localhost:8081/");

        lraClient = new LRAClient("localhost", 8080);

        // to use client connection pooling use resteasy directly
//        ResteasyClientBuilder builder = new ResteasyClientBuilder();
//        builder.connectionPoolSize(200);
    }

    @Test
    public void testContainerManagedLRA() throws Exception {
        // make a service request that should start an LRA
        // curl -X PUT http://localhost:8081/activities/work
        Client client = ClientBuilder.newClient();
        WebTarget msTarget = client.target(URI.create(new URL(MICRSERVICE_BASE_URL, "/").toExternalForm()));

        Response response = msTarget.path("activities").path("work").request().put(Entity.text(""));

        String lraId = response.readEntity(String.class);

        assertNotNull(lraId);
        // the put to the resource /activities/work should have been performed in the context of an LRA
        assertFalse(lraId.isEmpty());

        // validate that the LRA coordinator no longer knows about lraId
        List<LRAStatus> lras = lraClient.getActiveLRAs();

        // the resource /activities/work is annotated with LRAType.REQUIRED so the container should have ended it
        assertFalse(lras.contains(new LRAStatus(lraId)));
    }

    @Test
    public void testClientManagedLRA() throws MalformedURLException {
        URL lraUrl = lraClient.startLRA("ClientDemo1", 500); // start an LRA
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(URI.create(new URL(MICRSERVICE_BASE_URL, "/").toExternalForm()));

        Response response = target.path("activities").path("work").request().header(LRAClient.LRA_HTTP_HEADER, lraUrl).put(Entity.text(""));

        // we've coded the resource to tell us the LRA context it performed the work in
        String lraId = response.readEntity(String.class);

        assertNotNull(lraId);

        // the put to the resource /activities/work should have been performed in the context of an LRA
        assertFalse(lraId.isEmpty());

        // validate that the LRA coordinator still knows about lraId
        List<LRAStatus> lras = lraClient.getActiveLRAs();

        // the resource /activities/work is annotated with LRAType.REQUIRED but this client started the LRA
        // so the container should *not* have ended it
        assertTrue(lras.contains(new LRAStatus(lraId)));

        // validate that the participant is still active
        validateParticipantStatus(client, lraUrl, null, Response.Status.BAD_REQUEST.getStatusCode());

        // end the LRA started by this client
        lraClient.cancelLRA(new URL(lraId));

        // validate that the participant completed
        validateParticipantStatus(ClientBuilder.newClient(), lraUrl,
                CompensatorStatus.Compensated, Response.Status.OK.getStatusCode());
    }

    private void validateParticipantStatus(Client client, URL lraUrl, CompensatorStatus expectedStatus, int expectedHttpCode) throws MalformedURLException {
        WebTarget target = client.target(URI.create(new URL(MICRSERVICE_BASE_URL, "/").toExternalForm()));

        // NB if lra has terminated lraUrl will no longer exists so make sure to mark the target method as NOT_SUPPORTED
        Response response = target.path("activities").path("status").request().header(LRAClient.LRA_HTTP_HEADER, lraUrl.toString()).get();

        assertEquals(expectedHttpCode, response.getStatus());

        if (expectedStatus != null) {
            String status = response.readEntity(String.class);

            assertTrue(expectedStatus.name().equals(status));
        }
    }
}
