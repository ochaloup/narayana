/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package io.narayana.lra.coordinator.api;

import io.narayana.lra.Current;
import io.narayana.lra.LRAConstants;
import io.narayana.lra.LRAData;
import io.narayana.lra.coordinator.domain.model.LongRunningAction;
import io.narayana.lra.coordinator.domain.service.LRAService;
import io.narayana.lra.coordinator.internal.APIVersion;
import io.narayana.lra.logging.LRALogger;

import javax.enterprise.context.ApplicationScoped;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static io.narayana.lra.LRAConstants.CLIENT_ID_PARAM_NAME;
import static io.narayana.lra.LRAConstants.COMPENSATE;
import static io.narayana.lra.LRAConstants.COMPLETE;
import static io.narayana.lra.LRAConstants.COORDINATOR_PATH_NAME;
import static io.narayana.lra.LRAConstants.PARENT_LRA_PARAM_NAME;
import static io.narayana.lra.LRAConstants.PARTICIPANT_TIMEOUT;
import static io.narayana.lra.LRAConstants.RECOVERY_COORDINATOR_PATH_NAME;
import static io.narayana.lra.LRAConstants.STATUS;
import static io.narayana.lra.LRAConstants.STATUS_PARAM_NAME;
import static io.narayana.lra.LRAConstants.TIMELIMIT_PARAM_NAME;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.OK;
import static io.narayana.lra.LRAConstants.LRA_API_VERSION_HEADER_NAME;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_RECOVERY_HEADER;

@ApplicationScoped
@Path(COORDINATOR_PATH_NAME)
@Tag(name = "LRA Coordinator", description = "Operations to work with active LRAs (to start, to get a status, to finish etc.)")
public class Coordinator {
    private static final APIVersion currentAPIVersion = APIVersion.instanceOf("1.0");

    @Context
    private UriInfo context;

    @Inject // Will not work in an async scenario: CDI-452
    private LRAService lraService;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns all LRAs", description = "Gets both active and recovering LRAs")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "The LRAData json array which is known to coordinator",
            content = @Content(schema = @Schema(title = "LRAData array", type = SchemaType.ARRAY, implementation = LRAData.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)}),
        @APIResponse(responseCode = "400", description = "",
            content = @Content(schema = @Schema(title = "Error description", implementation = String.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)}),
    })
    public Response getAllLRAs(
            @Parameter(name = STATUS_PARAM_NAME, description = "Filter the returned LRAs to only those in the give state (see CompensatorStatus)")
            @QueryParam(STATUS_PARAM_NAME) @DefaultValue("") String state,
            @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
            @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version) {
        verifyVersion(version);
        LRAStatus requestedLRAStatus = null;
        if(!state.isEmpty()) {
            try {
                requestedLRAStatus = LRAStatus.valueOf(state);
            } catch (NullPointerException | IllegalArgumentException e) {
                String errorMsg = "Status " + state + " is not a valid LRAStatus value";
                throw new WebApplicationException(errorMsg, e,
                        Response.status(BAD_REQUEST).header(LRA_API_VERSION_HEADER_NAME, version).entity(errorMsg).build());
            }
        }

        List<LRAData> lras = lraService.getAll(requestedLRAStatus);

        return Response.ok()
            .entity(lras)
            .header(LRA_API_VERSION_HEADER_NAME, version).build();
    }

    @GET
    @Path("{LraId}/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Obtain the status of an LRA as a string")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "The LRA exists. The status is reported in the content body.",
            content = @Content(schema = @Schema(implementation = String.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
        @APIResponse(responseCode = "404", description = "The coordinator has no knowledge of this LRA",
            content = @Content(schema = @Schema(title = "Unknown LRA error", implementation = String.class))),
    })
    public Response getLRAStatus(
        @Parameter(name = "LraId", description = "The unique identifier of the LRA." +
                "Expecting to be a valid URL where the participant can be contacted at. If not in URL format it will be considered " +
                "to be a id which will be declared to exist at URL where coordinator is deployed at.", required = true)
        @PathParam("LraId")String lraId,
        @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
        @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version)
            throws NotFoundException {
        verifyVersion(version);
        LongRunningAction transaction = lraService.getTransaction(toURI(lraId)); // throws NotFoundException -> response 404
        LRAStatus status = transaction.getLRAStatus();

        if (status == null) {
            status = LRAStatus.Active;
        }

        return Response.ok()
            .entity(status.name())
            .header(LRA_API_VERSION_HEADER_NAME, version).build();
    }

    @GET
    @Path("{LraId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain the information about an LRA as a JSON structure")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "The LRA exists and the information is packed as JSON in the content body.",
            content = @Content(schema = @Schema(title = "LRAData", implementation = LRAData.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
        @APIResponse(responseCode = "404", description = "The coordinator has no knowledge of this LRA",
            content = @Content(schema = @Schema(title = "Error description", implementation = String.class)))
    })
    public Response getLRAInfo(
            @Parameter(name = "LraId", description = "The unique identifier of the LRA", required = true)
            @PathParam("LraId") String lraId,
            @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
            @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version) {
        verifyVersion(version);
        URI lraIdURI = toURI(lraId);
        LRAData lraData = lraService.getLRA(lraIdURI);
        return Response.status(OK).entity(lraData)
                .header(LRA_API_VERSION_HEADER_NAME, version).build();
    }

    /**
     * Performing a POST on {@value LRAConstants#COORDINATOR_PATH_NAME}/start?ClientID=<ClientID>
     * will start a new lra with a default timeout and return a lra URL
     * of the form <coordinator url>/{@value LRAConstants#COORDINATOR_PATH_NAME}/<LraId>.
     * Adding a query parameter, {@value LRAConstants#TIMELIMIT_PARAM_NAME}=<timeout>, will start a new lra with the specified timeout.
     * If the lra is terminated because of a timeout, the lra URL is deleted and all further invocations on the URL will return 404.
     * The invoker can assume this was equivalent to a compensate operation.
     */
    @POST
    @Path("start")
    @Produces(MediaType.TEXT_PLAIN)
    @Bulkhead
    @Operation(summary = "Start a new LRA",
        description = "The LRA model uses a presumed nothing protocol: the coordinator must communicate "
            + "with Compensators in order to inform them of the LRA activity. Every time a "
            + "Compensator is enrolled with a LRA, the coordinator must make information about "
            + "it durable so that the Compensator can be contacted when the LRA terminates, "
            + "even in the event of subsequent failures. Compensators, clients and coordinators "
            + "cannot make any presumption about the state of the global transaction without "
            + "consulting the coordinator and all compensators, respectively.")
    @APIResponses({
        @APIResponse(responseCode = "201",
            description = "The request was successful and the response body contains the id of the new LRA",
            content = @Content(schema = @Schema(title = "An LRA id", description = "An URI of the new LRA", implementation = String.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
        @APIResponse(responseCode = "404", description = "Parent LRA id cannot be joint to the started LRA",
            content = @Content(schema = @Schema(title = "Failure description", description = "Message containing problematic LRA id", implementation = String.class))),
        @APIResponse(responseCode = "500", description = "A new LRA could not be started. Coordinator internal error.",
                content = @Content(schema = @Schema(title = "LRA cannot be started error", implementation = String.class)))
    })
    public Response startLRA(
            @Parameter(name = CLIENT_ID_PARAM_NAME,
                description = "Each client is expected to have a unique identity (which can be a URL).",
                required = true)
            @QueryParam(CLIENT_ID_PARAM_NAME) @DefaultValue("") String clientId,
            @Parameter(name = TIMELIMIT_PARAM_NAME,
                description = "Specifies the maximum time in milli seconds that the LRA will exist for.\n"
                    + "If the LRA is terminated because of a timeout, the LRA URL is deleted.\n"
                    + "All further invocations on the URL will return 404.\n"
                    + "The invoker can assume this was equivalent to a compensate operation.")
            @QueryParam(TIMELIMIT_PARAM_NAME) @DefaultValue("0") Long timelimit,
            @Parameter(name = PARENT_LRA_PARAM_NAME,
                description = "The enclosing LRA if this new LRA is nested")
            @QueryParam(PARENT_LRA_PARAM_NAME) @DefaultValue("") String parentLRA,
            @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
            @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version) throws WebApplicationException {
        verifyVersion(version);
        URI parentLRAUrl = null;

        if (parentLRA != null && !parentLRA.isEmpty()) {
            parentLRAUrl = toDecodedURI(parentLRA);
        }

        String coordinatorUrl = String.format("%s%s", context.getBaseUri(), COORDINATOR_PATH_NAME);
        URI lraId = lraService.startLRA(coordinatorUrl, parentLRAUrl, clientId, timelimit);

        if (parentLRAUrl != null) {
            // register with the parentLRA as a participant (extract the LRAId)
            String compensatorUrl = String.format("%s/nested/%s", coordinatorUrl, LRAConstants.getLRAUid(lraId));

            if (lraService.hasTransaction(parentLRAUrl)) {
                Response response = joinLRAViaBody(parentLRAUrl.toASCIIString(), timelimit, null, version, compensatorUrl);

                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    return Response.status(response.getStatus()).header(LRA_API_VERSION_HEADER_NAME, version).build();
                }
            } else {
                // TODO: investigate on reasons why starting the nested transaction goes to parent participant URL
                Client client = null;
                Response response = null;

                try {
                    client = ClientBuilder.newClient();
                    response = client.target(parentLRAUrl)
                        .request()
                        .async()
                        .put(Entity.text(compensatorUrl))
                        .get(PARTICIPANT_TIMEOUT, TimeUnit.SECONDS);

                    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                        String errMessage = String.format("The coordinator at %s returned an unexpected response: %d"
                                + "when trying the LRA '%s' to join the parent LRA id '%s'", parentLRAUrl, response.getStatus(), lraId, parentLRA);
                        return Response.status(response.getStatus()).entity(errMessage).build();
                    }
                } catch (Exception e) {
                    String errorMsg = String.format("Cannot contact the LRA Coordinator at '%s' for LRA '%s' joining parent LRA '%s'",
                            parentLRAUrl, lraId, parentLRA);
                    LRALogger.logger.debugf(errorMsg);
                    throw new WebApplicationException(errorMsg, e,
                            Response.status(INTERNAL_SERVER_ERROR).header(LRA_API_VERSION_HEADER_NAME, version)
                                    .entity(errorMsg).build());
                } finally {
                    if (client != null) {
                        client.close();
                    }
                }
            }
        }

        Current.push(lraId);

        return Response.created(lraId)
                .entity(lraId)
                .header(LRA_HTTP_CONTEXT_HEADER, Current.getContexts())
                .header(LRA_API_VERSION_HEADER_NAME, version)
                .build();
    }

    @PUT
    @Path("{LraId}/renew")
    @Operation(summary = "Update the TimeLimit for an existing LRA",
        description = "LRAs can be automatically cancelled if they aren't closed or cancelled before the TimeLimit "
            + "specified at creation time is reached. The time limit can be updated.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "If the LRA time limit has been updated",
            content = @Content(schema = @Schema(title = "Renewed LRA id", implementation = String.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
        @APIResponse(responseCode = "404", description = "The coordinator has no knowledge of this LRA or " +
            "the LRA is not longer active (ie the complete or compensate messages have been sent",
            content = @Content(schema = @Schema(title = "Unknown LRA error", implementation = String.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
    })
    public Response renewTimeLimit(
            @Parameter(name = "LraId", description = "The unique identifier of the LRA", required = true)
            @PathParam("LraId") String lraId,
            @Parameter(name = TIMELIMIT_PARAM_NAME, description = "The new time limit for the LRA", required = true)
            @QueryParam(TIMELIMIT_PARAM_NAME) @DefaultValue("0") Long timeLimit,
            @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
            @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version) {
        verifyVersion(version);
        return Response.status(lraService.renewTimeLimit(toURI(lraId), timeLimit))
            .header(LRA_API_VERSION_HEADER_NAME, version)
            .entity(lraId)
            .build();
    }

    @GET
    @Path("nested/{NestedLraId}/status")
    public Response getNestedLRAStatus(@PathParam("NestedLraId")String nestedLraId) {
        if (!lraService.hasTransaction(nestedLraId)) {
            // it must have compensated TODO maybe it's better to keep nested LRAs in separate collection
            return Response.ok(ParticipantStatus.Compensated.name()).build();
        }

        LongRunningAction lra = lraService.getTransaction(toURI(nestedLraId));
        LRAStatus status = lra.getLRAStatus();

        if (status == null || lra.getLRAStatus() == null) {
            LRALogger.i18NLogger.error_cannotGetStatusOfNestedLraURI(nestedLraId, lra.getId());
            String errMsg = String.format("LRA (parent: %s, nested: %s) is in the wrong state for operation " +
                    "'getNestedLRAStatus': The LRA is still active.", nestedLraId, lra.getId());
            throw new WebApplicationException(errMsg,
                    Response.status(Response.Status.PRECONDITION_FAILED).entity(errMsg).build());
        }

        return Response.ok(mapToParticipantStatus(lra.getLRAStatus()).name()).build();
    }

    private ParticipantStatus mapToParticipantStatus(LRAStatus lraStatus) {
        switch (lraStatus) {
            case Active: return ParticipantStatus.Active;
            case Closed: return ParticipantStatus.Completed;
            case Cancelled: return ParticipantStatus.Compensated;
            case Closing: return ParticipantStatus.Completing;
            case Cancelling: return ParticipantStatus.Compensating;
            case FailedToClose: return ParticipantStatus.FailedToComplete;
            case FailedToCancel: return ParticipantStatus.FailedToCompensate;
            default: return null;
        }
    }

    @PUT
    @Path("nested/{NestedLraId}/complete")
    public Response completeNestedLRA(@PathParam("NestedLraId") String nestedLraId) {
        return Response.ok(mapToParticipantStatus(endLRA(toURI(nestedLraId), false, true)).name()).build();
    }

    @PUT
    @Path("nested/{NestedLraId}/compensate")
    public Response compensateNestedLRA(@PathParam("NestedLraId") String nestedLraId) {
        return Response.ok(mapToParticipantStatus(endLRA(toURI(nestedLraId), true, true)).name()).build();
    }

    @PUT
    @Path("nested/{NestedLraId}/forget")
    public Response forgetNestedLRA(@PathParam("NestedLraId") String nestedLraId) {
        lraService.remove(toURI(nestedLraId));

        return Response.ok().build();
    }

    /**
     * Performing a PUT on {@value LRAConstants#COORDINATOR_PATH_NAME}/<LraId>/close will trigger the successful completion
     * of the LRA and all compensators will be dropped by the LRA Coordinator.
     * The complete message will be sent to the compensators.
     * Upon termination, the URL is implicitly deleted. If it no longer exists, then 404 will be returned.
     * The invoker cannot know for sure whether the lra completed or compensated without enlisting a participant.
     */
    // TODO: Question: is this message best effort or at least once?
    // TODO rework spec to allow an LRAStatus header everywhere
    @PUT
    @Path("{LraId}/close")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Attempt to close an LRA",
        description = "Trigger the successful completion of the LRA. All"
            + " compensators will be dropped by the coordinator."
            + " The complete message will be sent to the compensators."
            + " Upon termination, the URL is implicitly deleted."
            + " The invoker cannot know for sure whether the lra completed"
            + " or compensated without enlisting a participant.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "The complete message was sent to all coordinators",
                content = @Content(schema = @Schema(title = "LRAStatus enum value", implementation = String.class)),
                headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
            @APIResponse(responseCode = "404", description = "The coordinator has no knowledge of this LRA",
                    content = @Content(schema = @Schema(title = "Unknown LRA error", implementation = String.class)),
                    headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
    })
    public Response closeLRA(
            @Parameter(name = "LraId", description = "The unique identifier of the LRA", required = true)
            @PathParam("LraId") String txId,
            @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
            @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version) {
        verifyVersion(version);
        return Response.ok(endLRA(toURI(txId), false, false).name())
                .header(LRA_API_VERSION_HEADER_NAME, version)
                .build();
    }

    @PUT
    @Path("{LraId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Attempt to cancel an LRA",
        description = " Trigger the compensation of the LRA. All"
            + " compensators will be triggered by the coordinator (ie the compensate message will be sent to each compensators)."
            + " Upon termination, the URL is implicitly deleted."
            + " The invoker cannot know for sure whether the lra completed or compensated without enlisting a participant.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "The compensate message was sent to all coordinators",
            content = @Content(schema = @Schema(title = "LRAStatus enum value", implementation = String.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
        @APIResponse(responseCode = "404", description = "The coordinator has no knowledge of this LRA",
            content = @Content(schema = @Schema(title = "Unknown LRA error", implementation = String.class)),
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
    })
    public Response cancelLRA(
        @Parameter(name = "LraId", description = "The unique identifier of the LRA", required = true)
        @PathParam("LraId")String lraId,
        @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
        @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version)
            throws NotFoundException {
        verifyVersion(version);
        return Response.ok(endLRA(toURI(lraId), true, false).name())
                .header(LRA_API_VERSION_HEADER_NAME, version)
                .build();
    }


    private LRAStatus endLRA(URI lraId, boolean compensate, boolean fromHierarchy) throws NotFoundException {
        LRAData lraData = lraService.endLRA(lraId, compensate, fromHierarchy);

        return lraData.getStatus();
    }

    @PUT
    @Path("{LraId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "A Compensator can join with the LRA at any time prior to the completion of an activity")
    @APIResponses({
        @APIResponse(responseCode = "200",
            description = "The participant was successfully registered with the LRA",
            content = @Content(schema = @Schema(title = "A new LRA recovery id",
                               description = "An URI representing the recovery id of this join request",implementation = String.class)),
            headers = {
                @Header(name = LRA_HTTP_RECOVERY_HEADER, description = "It contains a unique resource reference for that participant:\n"
                        + " - HTTP GET on the reference returns the original participant URL;\n" // TODO: verify recovery coordinator works this way
                        + " - HTTP PUT on the reference will overwrite the old participant URL with the new one supplied.",
                    schema = @Schema(implementation = String.class)),
                @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
        @APIResponse(responseCode = "400", description = "Link does not contain all required fields for joining the LRA. " +
                "Probably no compensator or after 'rel' is available.",
            content = @Content(schema = @Schema(title = "Error to enlist", implementation = String.class))),
        @APIResponse(responseCode = "404", description = "The coordinator has no knowledge of this LRA",
            content = @Content(schema = @Schema(title = "Unknown LRA error", implementation = String.class))),
        @APIResponse(responseCode = "412",
            description = "The LRA is not longer active, or wrong format of compensator data",
            content = @Content(schema = @Schema(title = "Wrong format LRA error", implementation = String.class)),
            headers = {@Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)}),
        @APIResponse(responseCode = "500", description = "Format of the compensator data (e.g. Link format) could not be processed",
            content = @Content(schema = @Schema(title = "Internal failure", implementation = String.class))),
    })
    public Response joinLRAViaBody(
            @Parameter(name = "LraId", description = "The unique identifier of the LRA", required = true)
            @PathParam("LraId")String lraId,
            @Parameter(name = TIMELIMIT_PARAM_NAME,
                description = "The time limit in milliseconds that the Compensator can guarantee that it can compensate "
                    + "the work performed by the service. After this time period has elapsed, it may no longer be "
                    + "possible to undo the work within the scope of this (or any enclosing) LRA. It may therefore "
                    + "be necessary for the application or service to start other activities to explicitly try to "
                    + "compensate this work. The application or coordinator may use this information to control the "
                    + "lifecycle of a LRA.")
            @QueryParam(TIMELIMIT_PARAM_NAME) @DefaultValue("0") long timeLimit,
            @Parameter(name = "Link",
                description = "The resource paths that the coordinator will use to complete or compensate and to request"
                    + " the status of the participant. The link rel names are"
                    + " complete, compensate and status.")
            @HeaderParam("Link") @DefaultValue("") String compensatorLink,
            @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
            @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version,
            @RequestBody(name = "Compensator data",
                description = "opaque data that will be stored with the coordinator and passed back to "
                    + "the participant when the LRA is closed or cancelled.") String compensatorData) throws NotFoundException {
        verifyVersion(version);
        // test to see if the join request contains any participant specific data
        boolean isLink = isLink(compensatorData);

        if (compensatorLink != null && !compensatorLink.isEmpty()) {
            return joinLRA(toURI(lraId), timeLimit, null, compensatorLink, compensatorData, version);
        }

        if (!isLink) { // interpret the content as a standard participant url
            compensatorData += "/";

            Map<String, String> terminateURIs = new HashMap<>();

            try {
                terminateURIs.put(COMPENSATE, new URL(compensatorData + "compensate").toExternalForm());
                terminateURIs.put(COMPLETE, new URL(compensatorData + "complete").toExternalForm());
                terminateURIs.put(STATUS, new URL(compensatorData + "status").toExternalForm());
            } catch (MalformedURLException e) {
                String errorMsg = String.format("Cannot join to LRA id '%s' with body as compensator url '%s' is invalid",
                        lraId, compensatorData);
                if (LRALogger.logger.isTraceEnabled()) {
                    LRALogger.logger.trace(errorMsg, e);
                }

                return Response.status(PRECONDITION_FAILED)
                        .header(LRA_API_VERSION_HEADER_NAME, version)
                        .entity(errorMsg)
                        .build();
            }

            // register with the coordinator, put the lra id in an http header
            StringBuilder linkHeaderValue = new StringBuilder();

            terminateURIs.forEach((k, v) -> makeLink(linkHeaderValue, "", k, v)); // or use Collectors.joining(",")

            compensatorData = linkHeaderValue.toString();
        }

        return joinLRA(toURI(lraId), timeLimit, null, compensatorData, null, version);
    }


    private static StringBuilder makeLink(StringBuilder b, String uriPrefix, String key, String value) {

        if (value == null) {
            return b;
        }

        String terminationUri = uriPrefix == null ? value : String.format("%s%s", uriPrefix, value);
        Link link =  Link.fromUri(terminationUri).rel(key).type(MediaType.TEXT_PLAIN).build();

        if (b.length() != 0) {
            b.append(',');
        }

        return b.append(link);
    }

    private boolean isLink(String linkString) {
        try {
            Link.valueOf(linkString);

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Response joinLRA(URI lraId, long timeLimit, String compensatorUrl, String linkHeader, String userData, String version)
            throws NotFoundException {
        final String recoveryUrlBase = String.format("%s%s/%s",
                context.getBaseUri().toASCIIString(), COORDINATOR_PATH_NAME, RECOVERY_COORDINATOR_PATH_NAME);

        StringBuilder recoveryUrl = new StringBuilder();

        int status = lraService.joinLRA(recoveryUrl, lraId, timeLimit, compensatorUrl, linkHeader, recoveryUrlBase, userData);

        try {
            return Response.status(status)
                    .entity(recoveryUrl.toString())
                    .location(new URI(recoveryUrl.toString()))
                    .header(LRA_HTTP_RECOVERY_HEADER, recoveryUrl)
                    .header(LRA_API_VERSION_HEADER_NAME, version)
                    .build();
        } catch (URISyntaxException e) {
            LRALogger.i18NLogger.error_invalidRecoveryUrlToJoinLRAURI(recoveryUrl.toString(), lraId);
            String errorMsg = lraId + ": Invalid recovery URL " + recoveryUrl.toString();
            throw new WebApplicationException(errorMsg, e ,
                    Response.status(INTERNAL_SERVER_ERROR).entity(errorMsg)
                            .header(LRA_API_VERSION_HEADER_NAME, version)
                            .build());
        }
    }

    /**
     * A participant can resign from a lra at any time prior to the completion of an activity by performing a PUT
     * PUT on {@value LRAConstants#COORDINATOR_PATH_NAME}/<LraId>/remove with the URL of the participant.
     */
    @PUT
    @Path("{LraId}/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "A Compensator can resign from the LRA at any time prior to the completion of an activity")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "If the participant was successfully removed from the LRA",
            headers = { @Header(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME) }),
        @APIResponse(responseCode = "400", description = "The coordinator has no knowledge of this participant compensator URL",
            content = @Content(schema = @Schema(title = "Unknown participant error", implementation = String.class))),
        @APIResponse(responseCode = "404", description = "The coordinator has no knowledge of this LRA",
                content = @Content(schema = @Schema(title = "Unknown LRA id error", implementation = String.class))),
        @APIResponse(responseCode = "412",
            description = "The LRA is not longer active (ie in the complete or compensate messages have been sent"),
    })
    public Response leaveLRA(
            @Parameter(name = "LraId", description = "The unique identifier of the LRA", required = true)
            @PathParam("LraId") String lraId,
            @Parameter(ref = LRAConstants.LRA_API_VERSION_HEADER_NAME)
            @HeaderParam(LRAConstants.LRA_API_VERSION_HEADER_NAME) @DefaultValue(JaxRsActivator.LRA_API_VERSION_STRING) String version,
            String participantCompensatorUrl) throws NotFoundException, URISyntaxException {
        verifyVersion(version);
        int status = lraService.leave(toURI(lraId), participantCompensatorUrl);

        return Response.status(status)
                .header(LRA_API_VERSION_HEADER_NAME, version)
                .build();
    }

    private void verifyVersion(String versionString) {
        APIVersion apiVersion = null;
        try {
            apiVersion = APIVersion.instanceOf(versionString);
            if (apiVersion.compareTo(currentAPIVersion) > 0) {
                String errorMsg = "Demanded API version " + versionString
                        + " is bigger than the supported one " + currentAPIVersion;
                throw new WebApplicationException(errorMsg,
                        Response.status(PRECONDITION_FAILED).entity(errorMsg).build());
            }
        } catch (Exception iae) {
            String errorMsg = "Wrong format of the provided version " + versionString + ": " + iae.getMessage();
            throw new WebApplicationException(errorMsg, iae,
                    Response.status(PRECONDITION_FAILED).entity(errorMsg).build());
        }
    }

    private URI toDecodedURI(String lraId) {
        try {
            return toURI(URLDecoder.decode(lraId, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            LRALogger.i18NLogger.error_invalidStringFormatOfUrl(lraId, e);
            String erroMsg = lraId + ": Invalid LRA id format " + e.getMessage();
            throw new WebApplicationException(erroMsg, e,
                    Response.status(BAD_REQUEST).entity(erroMsg).build());
        }
    }

    private URI toURI(String lraId) {
        URL url;

        try {
            // see if it already in the correct format
            url = new URL(lraId);
            url.toURI();
        } catch (Exception e) {
            try {
                url = new URL(String.format("%s%s/%s", context.getBaseUri(), COORDINATOR_PATH_NAME, lraId));
            } catch (MalformedURLException e1) {
                LRALogger.i18NLogger.error_invalidStringFormatOfUrl(lraId, e1);
                String errorMsg = lraId + ": Illegal LRA id format " + e1.getMessage();
                throw new WebApplicationException(errorMsg, e1,
                        Response.status(BAD_REQUEST).entity(errorMsg).build());
            }
        }

        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            LRALogger.i18NLogger.error_invalidStringFormatOfUrl(lraId, e);
            String errorMsg = lraId + ": Invalid format of LRA id URL format to convert to URI " + e.getMessage();
            throw new WebApplicationException(errorMsg, e,
                    Response.status(BAD_REQUEST).entity(errorMsg).build());
        }
    }
}
