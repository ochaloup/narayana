package org.jboss.narayana.rts.lra.coordinator.api;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

@ApplicationScoped
public class LRAClient implements LRAClientAPI {
    public static final String LRA_HTTP_HEADER = "X-lra";
    public static final String LRA_HTTP_HEADER2 = "X-lra2";
    public static final String LRA_HTTP_RECOVERY_HEADER = "X-lra-recovery";

    public static final String COORDINATOR_PATH_NAME = "lra-coordinator";
    public static final String RECOVERY_COORDINATOR_PATH_NAME = "lra-recovery-coordinator";

    public static final String COMPLETE = "complete";
    public static final String COMPENSATE = "compensate";
    public static final String STATUS = "status";
    public static final String LEAVE = "leave";

    public static final String TIMEOUT_PARAM_NAME = "TimeLimit";
    public static final String CLIENT_ID_PARAM_NAME = "ClientId";

    private static final String startLRAUrl = "/start";///?ClientId=abc&timeout=300000";
    private static final String getAllLRAsUrl = "/";
    private static final String getRecoveringLRAsUrl = "/recovery";
    private static final String getActiveLRAsUrl = "/active";

    private static final String isActiveUrlFormat = "/%s";
    private static final String isCompletedUrlFormat = "/completed/%s";
    private static final String isCompensatedUrlFormat = "/compensated/%s";

    private static final String confirmFormat = "/%s/close";
    private static final String compensateFormat = "/%s/cancel";
    private static final String leaveFormat = "/%s/remove";

    private WebTarget target;
    private URI base;
    private Client client;
    private Stack<String> lraStack;
    private boolean isUseable;

     @Produces
     @ApplicationScoped
     public LRAClient createLRAClient() throws URISyntaxException {
         return new LRAClient();
     }

    public LRAClient() throws URISyntaxException {
        this("http", "localhost", 8080);
    }

    public LRAClient(String host, int port) throws URISyntaxException {
        this("http", host, port);
    }

    public LRAClient(String scheme, String host, int port) throws URISyntaxException {
        init(scheme, host, port);
    }

    public LRAClient(String coordinatorUrl) throws MalformedURLException, URISyntaxException {
        init(coordinatorUrl);
    }

    private void init(String coordinatorUrl) throws MalformedURLException, URISyntaxException {
        URL url = new URL(coordinatorUrl);

        init(url.getProtocol(), url.getHost(), url.getPort());
    }

    private void init(String scheme, String host, int port) throws URISyntaxException {
        if (client == null)
            client = ClientBuilder.newClient();

        base = new URI(scheme, null, host, port, "/" + COORDINATOR_PATH_NAME, null, null);
        target = client.target(base);

        if (lraStack == null)
            lraStack = new Stack<>();

        isUseable = true;
    }

    /**
     * Update the clients notion of the current coordinator. Warning all further operations will be performed
     * on the LRA manager that created the passed in coordinator.
     *
     * @param coordinatorUrl the full url of an LRA
     */
    public void setCurrentLRA(String coordinatorUrl) {
        try {
            init(coordinatorUrl);

            lraStack.add(coordinatorUrl);
        } catch (MalformedURLException | URISyntaxException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
        }
    }

    public boolean isUseable() {
        return isUseable;
    }

    @PostConstruct
    public void postConstruct() {
        // an opportunity to consult any config

    }

    @PreDestroy
    public void preDestroy() {
        isUseable = false;
    }
    public static String getLRAId(String lraId) {
        return lraId == null ? null : lraId.replaceFirst(".*/([^/?]+).*", "$1");
    }

    public String startLRA(String clientID) throws WebApplicationException {
        return startLRA(clientID, 0);
    }

    @Override
    public String startLRA(String clientID, Integer timeout) throws WebApplicationException {
        Response response = null;
        String lra;

        if (clientID == null)
            clientID = "";

        if (timeout == null)
            timeout = 0;
        else if (timeout < 0)
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid timeout value: " + timeout).build());

        try {
            response = target.path(startLRAUrl)
                    .queryParam(TIMEOUT_PARAM_NAME, timeout)
                    .queryParam(CLIENT_ID_PARAM_NAME, clientID)
                    .request().post(Entity.text(""));

            // validate the HTTP status code says an LRAStatus resource was created
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                    "LRA start returned an unexpected status code: %d versus %d");

            // validate that there is an LRAStatus response header holding the LRAStatus id
            Object lraObject = response.getHeaders().getFirst(LRA_HTTP_HEADER);

            assertNotNull(lraObject, "LRA is null");

            lra = lraObject.toString();

            lraStack.add(lra);
        } finally {
            if (response != null)
                response.close();
        }

        // check that the lra is active
//        isActiveLRA(lra);

        return lra;
    }

    @Override
    public void cancelLRA(String lraId) throws WebApplicationException {
        endLRA(lraId, false);
    }

    @Override
    public void closeLRA(String lraId) throws WebApplicationException {
        endLRA(lraId, true);
    }

    @Override
    public void joinLRA(String lraId, Integer timelimit, String compensatorUrl) throws WebApplicationException {
        enlistCompensator(lraId, timelimit, "",
                String.format("%s/compensate", compensatorUrl),
                String.format("%s/complete", compensatorUrl),
                String.format("%s/leave", compensatorUrl),
                String.format("%s/status", compensatorUrl));
    }

    @Override
    public String joinLRA(String lraId, Integer timelimit,
                          String compensateUrl, String completeUrl, String leaveUrl, String statusUrl) throws WebApplicationException {
        return enlistCompensator(lraId, timelimit, "", compensateUrl, completeUrl, leaveUrl, statusUrl);
    }

    @Override
    public void leaveLRA(String lraId, String compensatorUrl) throws WebApplicationException {
        Response response = null;

        try {
            response = target.path(String.format(leaveFormat, getLRAId(lraId)))
                    .request()
                    .header(LRA_HTTP_HEADER, lraId)
                    .put(Entity.entity(compensatorUrl, MediaType.TEXT_PLAIN));

            if (Response.Status.OK.getStatusCode() != response.getStatus())
                throw new WebApplicationException(response);
        } finally {
            if (response != null)
                response.close();
        }
    }

    @Override
    public List<LRAStatus> getAllLRAs() throws WebApplicationException {
        return getLRAs(getAllLRAsUrl);
    }

    @Override
    public List<LRAStatus> getActiveLRAs() throws WebApplicationException {
        return getLRAs(getActiveLRAsUrl);
    }

    @Override
    public List<LRAStatus> getRecoveringLRAs() throws WebApplicationException {
        return getLRAs(getRecoveringLRAsUrl);
    }

    private List<LRAStatus> getLRAs(String getUrl) {
        Response response = null;

        try {
            response = target.path(getUrl).request().get();

            if (!response.hasEntity())
                throw new WebApplicationException(response);

            List<LRAStatus> actions = new ArrayList<>();

            String lras = response.readEntity(String.class);

            JsonReader reader = Json.createReader(new StringReader(lras));
            JsonArray ja = reader.readArray();

            ja.forEach(jsonValue ->
                    actions.add(toLRAStatus(((JsonObject) jsonValue))));
//            ja.forEach(jsonValue -> actions.add(new LRAStatus(jsonValue.asJsonObject().getString("lraId"))));

            return actions;
        } finally {
            if (response != null)
                response.close();
        }
    }

    private LRAStatus toLRAStatus(JsonObject jo) {
        try {
            return new LRAStatus(
                    jo.getString("lraId"),
                    jo.getString("clientId"),
                    jo.getBoolean("complete"),
                    jo.getBoolean("compensated"),
                    jo.getBoolean("recovering"),
                    jo.getBoolean("active"));
        } catch (Exception e) {
            System.out.printf("Error parsing json LRAStatus");

            return new LRAStatus(jo.getString("lraId"));
        }
    }

    @Override
    public Boolean isActiveLRA(String lraId) throws WebApplicationException {
        return getStatus(lraId, isActiveUrlFormat);
    }

    @Override
    public Boolean isCompensatedLRA(String lraId) throws WebApplicationException {
        return getStatus(lraId, isCompensatedUrlFormat);
    }

    @Override
    public Boolean isCompletedLRA(String lraId) throws WebApplicationException {
        return getStatus(lraId, isCompletedUrlFormat);
    }

    private Boolean getStatus(String lraId, String statusFormat) {
        Response response = null;

        try {
            response = target.path(String.format(statusFormat, getLRAId(lraId))).request().get();

            return Boolean.valueOf(response.readEntity(String.class));
        } finally {
            if (response != null)
                response.close();
        }
    }

    private StringBuilder makeLink(StringBuilder b, String uriPrefix, String key, String value) {

        String terminationUri = String.format("%s%s", uriPrefix, value);
        Link link =  Link.fromUri(terminationUri).title(key + " URI").rel(key).type(MediaType.TEXT_PLAIN).build();

        if (b.length() != 0)
            b.append(',');

        return b.append(link);
    }

    private String enlistCompensator(String lraUrl, int timelimit, String uriPrefix,
                                   String compensateUrl, String completeUrl, String leaveUrl, String statusUrl ) {
        validateURL(completeUrl, false, "Invalid complete URL: %s");
        validateURL(compensateUrl, false, "Invalid compensate URL: %s");
        validateURL(leaveUrl, true, "Invalid status URL: %s");
        validateURL(statusUrl, false, "Invalid status URL: %s");

        Map<String, String> terminateURIs = new HashMap<>();

        terminateURIs.put(LRAClient.COMPENSATE, compensateUrl);
        terminateURIs.put(LRAClient.COMPLETE, completeUrl);
        terminateURIs.put(LRAClient.LEAVE, leaveUrl);
        terminateURIs.put(LRAClient.STATUS, statusUrl);

        // register with the coordinator
        // put the lra id in an http header
        StringBuilder linkHeaderValue = new StringBuilder();

        terminateURIs.forEach((k, v) -> makeLink(linkHeaderValue, uriPrefix, k, v));

        Response response = null;

        try {
            response = target
                    .queryParam(TIMEOUT_PARAM_NAME, timelimit)
                    .request()
                    .header("Link", linkHeaderValue)
                    .header(LRA_HTTP_HEADER, lraUrl)
                    .put(Entity.entity("", MediaType.TEXT_PLAIN));

            if (response.getStatus() != Response.Status.OK.getStatusCode())
                throw new WebApplicationException(Response.status(response.getStatus()).entity("unable to register compensator: ").build());

            return response.readEntity(String.class);
        } finally {
            if (response != null)
                response.close();
        }
    }

    private void endLRA(String lra, boolean confirm) throws WebApplicationException {
        String confirmUrl = String.format(confirm ? confirmFormat : compensateFormat, getLRAId(lra));
        Response response = null;

        try {
            response = target.path(confirmUrl).request().put(Entity.text(""));

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "LRA finished with an unexpected status code");

            // TODO store the hierarchy of LRAs somewhere
            lraStack.remove(lra);

            String nextLRA = getCurrent();

            if (nextLRA != null)
                try {
                    init(nextLRA);
                } catch (MalformedURLException | URISyntaxException ignore) {
                    // the validity of the url was checked when we added it to lraStack
                }
        } finally {
            if (response != null)
                response.close();
        }
    }

    private void validateURL(String url, boolean nullAllowed, String message) {
        if (url == null) {
            if (!nullAllowed)
                throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).entity(String.format(message, "null value")).build());
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).entity(String.format(message, e.getMessage())).build());
            }
        }
    }

    private void assertNotNull(Object lra, String message) {
        if (lra == null)
            throw new WebApplicationException(message);
    }

    private void assertEquals(Object expected, Object actual, String messageFormat) {
        if (!actual.equals(expected))
            throw new WebApplicationException(String.format(messageFormat, expected, actual));
    }

    public String getUrl() {
        return base.toString();
    }

    public String getCurrent() {
        return lraStack.empty() ? null : lraStack.peek();
    }
}
