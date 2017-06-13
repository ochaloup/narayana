import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import org.jboss.narayana.rts.lra.coordinator.api.LRAStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
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
import static org.junit.Assert.assertTrue;

public class SpecTest {
    private static URL MICRSERVICE_BASE_URL;

    private static LRAClient lraClient;
    private static Client msClient;

    private WebTarget msTarget;

    private String lra = null;

    @BeforeClass
    public static void setupClass() throws MalformedURLException, URISyntaxException {
        MICRSERVICE_BASE_URL = new URL("http://localhost:8081/");

        lraClient = new LRAClient("localhost", 8080);
        msClient = ClientBuilder.newClient();
    }

    @Before
    public void setupTest() throws MalformedURLException, URISyntaxException {
        if (lra != null) {
            try {
                lraClient.cancelLRA(lra);
            } catch (Throwable ignore) {
            }

            lra = null;
        }

        msTarget = msClient.target(URI.create(new URL(MICRSERVICE_BASE_URL, "/").toExternalForm()));
    }

    @Test
    public void startLRA() throws WebApplicationException {
        lra = lraClient.startLRA("startLRA", 500);
    }

    @Test
    public void cancelLRA() throws WebApplicationException {
        lra = lraClient.startLRA("cancelLRA", 500);

        lraClient.cancelLRA(lra);

        List<LRAStatus> lras = lraClient.getAllLRAs();

        assertFalse(lras.contains(new LRAStatus(lra)));

        lra = null;
    }

    @Test
    public void closeLRA() throws WebApplicationException {
        lra = lraClient.startLRA("closelLRA", 500);

        lraClient.closeLRA(lra);

        List<LRAStatus> lras = lraClient.getAllLRAs();

        assertFalse(lras.contains(new LRAStatus(lra)));

        lra = null;
    }

    @Test
    public void getActiveLRAs() throws WebApplicationException {
        lra = lraClient.startLRA("getActiveLRAs", 500);
        List<LRAStatus> lras = lraClient.getActiveLRAs();

        assertTrue(lras.contains(new LRAStatus(lra)));
    }

    @Test
    public void getAllLRAs() throws WebApplicationException {
        lra = lraClient.startLRA("getAllLRAs", 500);
        List<LRAStatus> lras = lraClient.getAllLRAs();

        assertTrue(lras.contains(new LRAStatus(lra)));
    }

    @Test
    public void getRecoveringLRAs() throws WebApplicationException {
        // TODO
    }

    @Test
    public void isActiveLRA() throws WebApplicationException {
        lra = lraClient.startLRA("isActiveLRA", 500);

        assertTrue(lraClient.isActiveLRA(lra));

        lraClient.closeLRA(lra);
    }

//    @Test
    // the coordinator cleans up when canceled
    public void isCompensatedLRA() throws WebApplicationException {
        lra = lraClient.startLRA("isCompensatedLRA", 500);
        lraClient.cancelLRA(lra);
        assertTrue(lraClient.isCompensatedLRA(lra));
    }

//    @Test
// the coordinator cleans up when completed
    public void isCompletedLRA() throws WebApplicationException {
        lra = lraClient.startLRA("isCompletedLRA", 500);
        lraClient.closeLRA(lra);
        assertTrue(lraClient.isCompletedLRA(lra));
    }

    @Test
    public void joinLRAViaBody() throws WebApplicationException {
        Response response = msTarget.path("activities").path("work").request().put(Entity.text(""));

        // validate that the LRA coordinator no longer knows about lraId
        List<LRAStatus> lras = lraClient.getActiveLRAs();

        // the resource /activities/work is annotated with LRAType.REQUIRED so the container should have ended it
        assertFalse(lras.contains(new LRAStatus(response.readEntity(String.class))));
    }

    @Test
    public void joinLRAViaHeader () throws WebApplicationException {
        int cnt1 = completedCount();

        lra = lraClient.startLRA("joinLRAViaBody", 500);

        Response response = msTarget.path("activities").path("work")
                .request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

        // validate that the LRA coordinator still knows about lraId
        List<LRAStatus> lras = lraClient.getActiveLRAs();
        assertTrue(lras.contains(new LRAStatus(lra)));

        // close the LRA
        lraClient.closeLRA(lra);

        // check that LRA coordinator no longer knows about lraId
        lras = lraClient.getActiveLRAs();
        assertFalse(lras.contains(new LRAStatus(lra)));

        // check that participant was told to complete
        int cnt2 = completedCount();
        assertEquals(cnt1 + 1, cnt2);
    }

    @Test
    public void leaveLRA() throws WebApplicationException {
        int cnt1 = completedCount();

        lra = lraClient.startLRA("leaveLRA", 500);

        Response response = msTarget.path("activities").path("work").request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

        // perform a second request to the same method in the same LRA context to validate that multiple participants are not registered
        response = msTarget.path("activities").path("work").request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

        // call a method annotated with @Leave (should remove the compensator from the LRA)
        response = msTarget.path("activities").path("leave").request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

//        lraClient.leaveLRA(lra, "some compensator"); // ask the MS for the compensator url so we can test LRAClient

        lraClient.closeLRA(lra);

        // check that participant was not told to complete
        int cnt2 = completedCount();

        assertEquals(cnt1, cnt2);
    }

    @Test
    public void leaveLRAViaAPI() throws WebApplicationException {
        int cnt1 = completedCount();

        lra = lraClient.startLRA("leaveLRA", 500);

        Response response = msTarget.path("activities").path("work").request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

        // perform a second request to the same method in the same LRA context to validate that multiple participants are not registered
        response = msTarget.path("activities").path("work").request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

        // call a method annotated with @Leave (should remove the compensator from the LRA)
        response = msTarget.path("activities").path("leave").path(LRAClient.getLRAId(lra)).request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

//        lraClient.leaveLRA(lra, "some compensator"); // ask the MS for the compensator url so we can test LRAClient

        lraClient.closeLRA(lra);

        // check that participant was not told to complete
        int cnt2 = completedCount();

        assertEquals(cnt1, cnt2);
    }

    private void checkStatusAndClose(Response response, int expected) {
        try {
            if (response.getStatus() != expected)
                throw new WebApplicationException(response);
        } finally {
            response.close();
        }
    }

    private int completedCount() {
        Response response = null;

        try {
            response = msTarget.path("activities").path("stats").path("completed").request().get();

            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

            return Integer.parseInt(response.readEntity(String.class));
        } finally {
            if (response != null)
                response.close();
        }

    }
}
