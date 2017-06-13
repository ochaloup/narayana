
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LRAWrapper {
//    private static final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private String startTxUrl = LRAClient.COORDINATOR_PATH_NAME + "/start";///?ClientId=abc&timeout=300000";
    private String getTxnsUrl = LRAClient.COORDINATOR_PATH_NAME;
    private String lraStatusFormat = LRAClient.COORDINATOR_PATH_NAME + "/%s";
    private String confirmFormat = LRAClient.COORDINATOR_PATH_NAME + "/%s/close";
    private String compensateFormat = LRAClient.COORDINATOR_PATH_NAME + "/%s/cancel";

    private WebTarget target;
    private final URL base;

    LRAWrapper(URL base) throws MalformedURLException, URISyntaxException {
        this.base = base;
        getNewTarget(null);
    }

    private void getNewTarget(String uriString) throws MalformedURLException, URISyntaxException {
        Client client = ClientBuilder.newClient();
        URI uri = uriString == null ? URI.create(new URL(base, "/").toExternalForm()) : new URI(uriString);

        target = client.target(uri);
    }

    JsonArray getLRAs() {
        Response response = target.path(getTxnsUrl).request().get();

        assertTrue(response.hasEntity());

        String lras = response.readEntity(String.class);

        JsonReader reader = Json.createReader(new StringReader(lras));

        return reader.readArray();
    }

    String startLRA() throws MalformedURLException, URISyntaxException {
        Response response = target.path(startTxUrl).request().post(Entity.text(""));

        // validate the HTTP status code says an LRAStatus resource was created
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // validate that there is an LRAStatus response header holding the LRAStatus id
        Object lra = response.getHeaders().getFirst(LRA_HTTP_HEADER);

        assertNotNull(lra);

        // the lra id is available either via the response body or via a header
        String lraId = response.readEntity(String.class);
        String lraId2 = LRAClient.getLRAId(lra.toString());

        // ensure they are equal
        assertEquals(lraId, lraId2);

        // check that the lra is active
        getNewTarget(null); // TODO figure out why reusing target fails
        response = target.path(LRAClient.COORDINATOR_PATH_NAME).path(lraId).request().get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // probably a waste fo time since the response code is good enough
        assertTrue(Boolean.valueOf(response.readEntity(String.class)));

        return lra.toString();
    }

    private void endLRA(String lra, boolean confirm) throws MalformedURLException, URISyntaxException {
        int lraCount = getLRAs().size();
        getNewTarget(null);
        String confirmUrl = String.format(confirm ? confirmFormat : compensateFormat, lra);
        Response response = target.path(confirmUrl).request().put(Entity.text(""));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        getNewTarget(null);
        assertEquals(lraCount - 1, getLRAs().size());
    }

    void confirmLRA(String lra) throws MalformedURLException, URISyntaxException {
        endLRA(LRAClient.getLRAId(lra), true);
    }

    void compensateLRA(String lra) throws MalformedURLException, URISyntaxException {
        endLRA(LRAClient.getLRAId(lra), false);
    }


}

