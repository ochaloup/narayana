package org.jboss.narayana.rts.lra.filter;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.Compensate;
import org.jboss.narayana.rts.lra.compensator.api.Complete;
import org.jboss.narayana.rts.lra.compensator.api.Leave;
import org.jboss.narayana.rts.lra.compensator.api.NestedLRA;
import org.jboss.narayana.rts.lra.compensator.api.Status;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.COMPENSATE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.COMPLETE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LEAVE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER2;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_RECOVERY_HEADER;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.STATUS;

@Provider
public class ServerLRAFilter extends FilterBase implements ContainerRequestFilter, ContainerResponseFilter {

    private LRAClient lraClient;

//    private AtomicAction previous = null;

    private void checkForTx(LRA.LRAType type, URL txId, boolean shouldNotBeNull) {
        if (txId == null && shouldNotBeNull) {
            Response resonse = Response.status(Response.Status.PRECONDITION_FAILED).entity(type.name() + " but no tx").build();

            throw new WebApplicationException(resonse);
        } else if (txId != null && !shouldNotBeNull) {
            Response resonse = Response.status(Response.Status.PRECONDITION_FAILED).entity(type.name() + " but found tx").build();

            throw new WebApplicationException(resonse);
        }
    }

//    // TODO figure out how to disable the filters for the coordinator (they remove the
//    private boolean isCoordinator() {
//        return resourceInfo.getResourceClass().getName().equals("org.jboss.narayana.rts.lra.coordinator.api.Coordinator")
//    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();
        LRA.LRAType type = null;
        Annotation transactional = method.getDeclaredAnnotation(LRA.class);
        URL lraId;

        URL suspendedLRA = null;
        URL newLRA = null;
        URL incommingLRA = null;
        String recoveryUrl = null;
        boolean nested;

        if (transactional == null) {
            transactional = method.getDeclaringClass().getDeclaredAnnotation(LRA.class);

            if (transactional != null)
                type = ((LRA) transactional).value();
        } else {
            type = ((LRA) transactional).value();
        }

        if (type == null)
            return; // not transactional

        boolean enlist = true;
        boolean endAnnotation = method.isAnnotationPresent(Complete.class)
                || method.isAnnotationPresent(Compensate.class)
                || method.isAnnotationPresent(Leave.class);

        if (headers.containsKey(LRA_HTTP_HEADER))
            incommingLRA = new URL(headers.getFirst(LRA_HTTP_HEADER)); // TODO filters for asynchronous JAX-RS motheods should not throw exceptions

        if (endAnnotation && incommingLRA == null)
            return;

        nested = resourceInfo.getResourceMethod().isAnnotationPresent(NestedLRA.class);

        switch (type) {
            case MANDATORY: // a txn must be present
                checkForTx(type, incommingLRA, true);

                if (nested) {
                    // a new LRA is nested under the incomming LRA
                    suspendedLRA = incommingLRA;
                    lraTrace(containerRequestContext, suspendedLRA, "ServerLRAFilter before: MANDATORY start new LRA");
                    newLRA = lraId = startLRA(incommingLRA, method, 500);
                } else {
                    lraId = incommingLRA;
                    resumeTransaction(incommingLRA); // txId is not null
                }
                break;
            case NEVER: // a txn must not be present
                checkForTx(type, incommingLRA, false);

                if (nested) {
                    // nested does not make sense
                    Response resonse = Response
                            .status(Response.Status.PRECONDITION_FAILED)
                            .entity(type.name() + " but found Nested annnotation")
                            .build();

                    throw new WebApplicationException(resonse);
                }

                enlist = false;
                lraId = null;

                break;
            case NOT_SUPPORTED:
                if (nested) {
                    // nested does not make sense
                    Response resonse = Response
                            .status(Response.Status.PRECONDITION_FAILED)
                            .entity(type.name() + " found Nested annnotation").build();

                    throw new WebApplicationException(resonse);
                }

                // suspend any currently active transaction
//                    previous = AtomicAction.suspend();
                enlist = false;
                suspendedLRA = incommingLRA;
                lraId = null;

                break;
            case REQUIRED:
                if (incommingLRA != null) {
                    if (nested) {
                        // if there is an LRA present nest a new LRA under it
                        suspendedLRA = incommingLRA;
                        lraTrace(containerRequestContext, suspendedLRA, "ServerLRAFilter before: REQUIRED start new LRA");
                        newLRA = lraId = startLRA(incommingLRA, method, 500);
                    } else {
                        lraId = incommingLRA;
                        resumeTransaction(incommingLRA);
                    }

                } else {
                    lraTrace(containerRequestContext, null, "ServerLRAFilter before: REQUIRED start new LRA");
                    newLRA = lraId = startLRA(incommingLRA, method, 500);
                }

                break;
            case REQUIRES_NEW:
//                    previous = AtomicAction.suspend();
                suspendedLRA = incommingLRA;
                lraTrace(containerRequestContext, suspendedLRA, "ServerLRAFilter before: REQUIRES_NEW start new LRA");
                newLRA = lraId = startLRA(incommingLRA, method, 500);

                break;
            case SUPPORTS:
                lraId = incommingLRA;

                if (nested) {
                    // if there is an LRA present a new LRA is nested under it otherwise a new top level LRA is begun
                    if (incommingLRA != null)
                        suspendedLRA = incommingLRA;

                    lraTrace(containerRequestContext, incommingLRA, "ServerLRAFilter before: SUPPORTS start new LRA");
                    newLRA = lraId = startLRA(incommingLRA, method, 500);
                } else if (incommingLRA != null) {
                    resumeTransaction(incommingLRA);
                }

                break;
            default:
                lraId = incommingLRA;
        }

        if (lraId == null) {
            lraTrace(containerRequestContext, lraId, "ServerLRAFilter before: removing header");
            // the method call needs to run without a transaction
            headers.remove(LRA_HTTP_HEADER);
            FilterState.clearCurrentLRA();
            return; // non transactional
        } else {
            lraTrace(containerRequestContext, lraId, "ServerLRAFilter before: adding header");
            headers.putSingle(LRA_HTTP_HEADER, lraId.toString());
        }

        lraClient = getLRAClient(true);

//        lraState = new FilterState(incommingLRA, false, lraClient.getUrl());

//        if (suspendedLRA != null)
//            lraClient.setCurrentLRA(suspendedLRA);

        lraTrace(containerRequestContext, lraId, "ServerLRAFilter before: making LRA available to injected LRAClient");
        lraClient.setCurrentLRA(lraId); // make the current LRA available to the called method

        // TODO make sure it is possible to do compensations inside a new LRA
        if (!endAnnotation && enlist) { // don't enlist for methods marked with Compensate, Complete or Leave
            URI baseUri = containerRequestContext.getUriInfo().getBaseUri();
            boolean refactored = true;

            if (refactored) {
                Map<String, String> terminateURIs = lraClient.getTerminationUris(resourceInfo.getResourceClass(), baseUri, true);
                recoveryUrl = lraClient.joinLRAWithLinkHeader(lraId, 0, terminateURIs.get("Link"));
            } else {


                Map<String, String> terminateURIs = getTerminationUris(resourceInfo.getResourceClass());

                Annotation resourcePathAnnotation = resourceInfo.getResourceClass().getAnnotation(Path.class);
                String resourcePath = resourcePathAnnotation == null ? "/" : ((Path) resourcePathAnnotation).value();

                String uriPrefix = String.format("%s:%s%s",
                        baseUri.getScheme(), baseUri.getSchemeSpecificPart(), resourcePath.substring(1));

                lraTrace(containerRequestContext, lraId,
                        "ServerLRAFilter before: joining LRA with compensator " + terminateURIs.get(COMPENSATE));
                recoveryUrl = lraClient.joinLRA(lraId, 0,
                        String.format("%s%s", uriPrefix, terminateURIs.get(COMPENSATE)),
                        String.format("%s%s", uriPrefix, terminateURIs.get(COMPLETE)),
                        String.format("%s%s", uriPrefix, terminateURIs.get(LEAVE)),
                        String.format("%s%s", uriPrefix, terminateURIs.get(STATUS)));
            }

            headers.putSingle(LRA_HTTP_RECOVERY_HEADER, recoveryUrl);
        }

        if (method.isAnnotationPresent(Leave.class)) {
            // leave the LRA
            String compensatorId = getCompensatorId(containerRequestContext.getUriInfo().getBaseUri());

            lraTrace(containerRequestContext, lraId, "leaving LRA");
            lraClient.leaveLRA(lraId, compensatorId);

            // let the compensator know which lra he left by leaving the header intact
        }

        lraTrace(containerRequestContext, lraId, "ServerLRAFilter before: making LRA available as a thread local");
        FilterState.setCurrentLRA(new FilterState(lraId, newLRA, suspendedLRA, recoveryUrl));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        FilterState state = FilterState.getCurrentLRA();

        if (state == null)
            return;

        if (state.newLRA != null) {
            lraTrace(requestContext, state.newLRA, "ServerLRAFilter after: closing LRA");
            lraClient.closeLRA(state.newLRA);
        }

        // TODO the filters should not trigger for the coordinator but they are pulled in via the dependency on LRAClient which
        // the coordinator needs for the constants
        if (responseContext.getHeaders().containsKey(LRA_HTTP_HEADER2)) {
            URL url = new URL(responseContext.getHeaders().getFirst(LRA_HTTP_HEADER2).toString());
            FilterState.setCurrentLRA(url);
        }

        URL current = currentLRA();

        if (current != null) {
            System.out.printf("ServerLRAFilter after: setting header: %s%n", current);
            lraTrace(requestContext, current, "ServerLRAFilter after: adding header");

            responseContext.getHeaders().putSingle(LRA_HTTP_HEADER, current);
        } else {
            lraTrace(requestContext, null, "ServerLRAFilter after: removing header and thread local");
            responseContext.getHeaders().remove(LRA_HTTP_HEADER);
            FilterState.clearCurrentLRA();
        }

        if (state.suspendedLRA != null) {
            lraTrace(requestContext, state.suspendedLRA, "ServerLRAFilter after: resetting header to suspended LRA");
            responseContext.getHeaders().putSingle(LRA_HTTP_HEADER, state.suspendedLRA);
        }

//        if (previous != null)
//            AtomicAction.resume(previous);
    }

    private URL startLRA(URL parentLRA, Method method, int timeout) {
//        getLRAClient(true);
        String clientId = method.getDeclaringClass().getName() +"#" + method.getName();

        return getLRAClient(true).startLRA(parentLRA, clientId, timeout);
    }

    private void resumeTransaction(URL lraId) {
        // nothing to do
    }

    private StringBuilder getParticipantLink(StringBuilder b, String uriPrefix, String key, String value) {

        String terminationUri = String.format("%s%s", uriPrefix, value);
        Link link =  Link.fromUri(terminationUri).title(key + " URI").rel(key).type(MediaType.TEXT_PLAIN).build();

        if (b.length() != 0)
            b.append(',');

        return b.append(link);
    }

    private String getCompensatorId(URI baseUri) {
        Map<String, String> terminateURIs = getTerminationUris(resourceInfo.getResourceClass());

        if (terminateURIs.size() < 3)
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing complete, compensate or status annotations").build());

        // register with the coordinator

        StringBuilder linkHeaderValue = new StringBuilder();
        Annotation resourcePathAnnotation = resourceInfo.getResourceClass().getAnnotation(Path.class);
        String resourcePath = resourcePathAnnotation == null ? "/" : ((Path) resourcePathAnnotation).value();

        String uriPrefix = String.format("%s:%s%s",
                baseUri.getScheme(), baseUri.getSchemeSpecificPart(), resourcePath.substring(1));

        terminateURIs.forEach((k, v) -> getParticipantLink(linkHeaderValue, uriPrefix, k, v));

        return linkHeaderValue.toString();
    }

    /**
     * Checks for Complete, Compensate and Status annotations and returns the JAX-RS paths of the methods
     * they are associated with
     */
    private Map<String, String> getTerminationUris(Class<?> compensatorClass) {
        Map<String, String> paths = new HashMap<>();

        Arrays.stream(compensatorClass.getMethods()).forEach(method -> {
            Annotation pathAnnotation = method.getAnnotation(Path.class);

            if (pathAnnotation != null) {
                checkMethod(paths, COMPLETE, (Path) pathAnnotation, method.getAnnotation(Complete.class));
                checkMethod(paths, COMPENSATE, (Path) pathAnnotation, method.getAnnotation(Compensate.class));
                checkMethod(paths, STATUS, (Path) pathAnnotation, method.getAnnotation(Status.class));
                checkMethod(paths, LEAVE, (Path) pathAnnotation, method.getAnnotation(Leave.class));
            }

            // TODO do we need to tell the coordinaor which HTTP verb the annotations are using
        });

        return paths;
    }

    private void checkMethod(Map<String, String> paths, String rel, Path pathAnnotation, Annotation annotationClass) {
        if (annotationClass != null)
            paths.put(rel, pathAnnotation.value());
    }
}
