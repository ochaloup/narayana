package org.jboss.narayana.rts.lra.coordinator.domain.model;

import com.arjuna.ats.arjuna.coordinator.TwoPhaseOutcome;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import org.jboss.jbossts.star.resource.RESTRecord;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

public class LRARecord extends RESTRecord {
    private String coordinatorURI;
    private String participantPath;

    private String completeURI;
    private String compensateURI;
    private String statusURI;
    private String leaveURI;

    private boolean isCompelete;
    private boolean isCompensated;
    private boolean isFailed;

    LRARecord(String lraId, String coordinatorURI, String linkURI) {
        super(lraId, coordinatorURI, linkURI, null);

        // if compensateURI is a link parse it into compensate,complete and status urls
        if (linkURI.indexOf(',') != -1) {
            linkURI = cannonicalForm(linkURI);
            Arrays.stream(linkURI.split(",")).forEach(this::parseLink);
        } else {
            this.compensateURI = String.format("%s/compensate", linkURI);
            this.completeURI = String.format("%s/complete", linkURI);
            this.leaveURI = String.format("%s/leave", linkURI);
            this.statusURI = String.format("%s/status", linkURI);
        }

        this.participantPath = linkURI;
        this.coordinatorURI = coordinatorURI;
    }

    public String getParticipantPath() {
        return participantPath;
    }

    public static String cannonicalForm(String linkStr) {
        if (linkStr.indexOf(',') == -1)
            return linkStr;

        SortedMap<String, String> lm = new TreeMap<>();
        Arrays.stream(linkStr.split(",")).forEach(link -> lm.put(Link.valueOf(link).getRel(), link));
        StringBuilder sb = new StringBuilder();

        lm.forEach((k, v) -> appendLink(sb, v));

        return sb.toString();
    }

    private static StringBuilder appendLink(StringBuilder b, String value) {
        if (b.length() != 0)
            b.append(',');

        return b.append(value);
    }

    private void parseLink(String linkStr) {
        Link link = Link.valueOf(linkStr);
        String rel = link.getRel();

        if ("compensate".equals(rel))
            compensateURI = link.getUri().toString();
        else if ("complete".equals(rel))
            completeURI = link.getUri().toString();
        else if ("status".equals(rel))
            statusURI = link.getUri().toString();
        else if ("leave".equals(rel))
            leaveURI = link.getUri().toString();
    }

    @Override
    public int topLevelPrepare() {
        return TwoPhaseOutcome.PREPARE_OK;
    }

    @Override
    // complete actions are mapped onto abort since complete is best effort. If a compensator needs to know
    // if the lra completed then it can ask the org.jboss.narayana.rts.lra.coordinator. A 404 status implies:
    // - all compensators completed ok, or
    // - all compensators compensated ok
    // This compensator can infer which possibility happened since it will have been told to complete or compensate
    public int topLevelAbort() {
        // TODO if this wa due to a timeout then we need to return doEnd(true);

        // put to compensateURI
        return doEnd(false);
    }

    @Override
    public int topLevelOnePhaseCommit() {

        return topLevelCommit();
    }

    @Override
    // compensate actions are mapped onto commit since we need recovery to kick in if any compensation failes
    public int topLevelCommit() {
        return doEnd(true);
    }

    private int doEnd(boolean compensate) {
        // put to completeURI
        String endPath = compensate ? compensateURI : completeURI;

        try {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(URI.create(new URL(endPath).toExternalForm()));

            Response response = target.request()
                    .header(LRA_HTTP_HEADER, coordinatorURI)
                    .post(Entity.entity("", MediaType.APPLICATION_JSON));

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                isFailed = true;
                return TwoPhaseOutcome.FINISH_ERROR;
            }

            if (compensate)
                isCompensated = true;
            else
                isCompelete = true;

            return TwoPhaseOutcome.FINISH_OK;
        } catch (MalformedURLException error) {
            isFailed = true;
            return TwoPhaseOutcome.FINISH_ERROR;
        }
    }

    public boolean isCompelete() {
        return isCompelete;
    }

    public boolean isCompensated() {
        return isCompensated;
    }

    public boolean isFailed() {
        return isFailed;
    }

    @Override
    public boolean save_state(OutputObjectState os, int t) {
        if (super.save_state(os, t)) {
            try {
                os.packString(coordinatorURI);
                os.packString(participantPath);
                os.packString(completeURI);
                os.packString(compensateURI);
                os.packString(statusURI);
                os.packString(leaveURI);

                os.packBoolean(isCompelete);
                os.packBoolean(isCompensated);
                os.packBoolean(isFailed);
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean restore_state(InputObjectState os, int t) {
        if (super.restore_state(os, t)) {
            participantPath = getParticipantURI();

            try {
                coordinatorURI = os.unpackString();
                participantPath = os.unpackString();
                completeURI = os.unpackString();
                compensateURI = os.unpackString();
                statusURI = os.unpackString();
                leaveURI = os.unpackString();

                isCompelete = os.unpackBoolean();
                isCompensated = os.unpackBoolean();
                isFailed = os.unpackBoolean();
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }
}
