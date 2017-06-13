package participant.filter;

import org.jboss.narayana.rts.lra.compensator.api.LRA;
import org.jboss.narayana.rts.lra.compensator.api.Compensate;
import org.jboss.narayana.rts.lra.compensator.api.Complete;
import org.jboss.narayana.rts.lra.compensator.api.Leave;
import org.jboss.narayana.rts.lra.compensator.api.Status;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.COMPENSATE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.COMPLETE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LEAVE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_RECOVERY_HEADER;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.STATUS;

@Provider
public class ServerLRAFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private LRAClient lraClient;

    @Context
    private ResourceInfo resourceInfo;

//    private AtomicAction previous = null;

    private void checkForTx(LRA.LRAType type, String txId, boolean shouldNotBeNull) {
        if (txId == null && shouldNotBeNull) {
            Response resonse = Response.status(Response.Status.PRECONDITION_FAILED).entity(type.name() + " but no tx").build();

            throw new WebApplicationException(resonse);
        } else if (txId != null && !shouldNotBeNull) {
            Response resonse = Response.status(Response.Status.PRECONDITION_FAILED).entity(type.name() + " but found tx").build();

            throw new WebApplicationException(resonse);
        }
    }

    private static <T> T getFirstBean(Class<T> clazz) {
        BeanManager bm =  CDI.current().getBeanManager();
        Iterator<Bean<?>> i = bm.getBeans(clazz).iterator();

        if (!i.hasNext())
            return null;

        Bean<T> bean = (Bean<T>) i.next();
        CreationalContext<T> ctx = bm.createCreationalContext(bean);

        return (T) bm.getReference(bean, clazz, ctx);
    }

    private LRAClient getLraClient() {
        // see if the target resource has an injected LRAClient - if so use
        LRAClient client = getFirstBean(LRAClient.class);

        try {
            if (client == null) {
                // this is a client request so may need to start a local coordinator - default to localhost:8080 TODO get the coordinator uri from some config
                return new LRAClient("localhost", 8080);
            } else {
                return client;
            }

        } catch (URISyntaxException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("could not build coordinator client").build());
        }
    }

    private void setCoordinator(String coordinator) {
        String newCoordinator;

        if (coordinator.endsWith("/"))
            newCoordinator = coordinator.substring(0, coordinator.length() - 1);
        else
            newCoordinator = coordinator;

        lraClient.setCoordinator(newCoordinator.substring(0, newCoordinator.lastIndexOf("/")));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();
        String txId = headers.getFirst(LRA_HTTP_HEADER);
        LRA.LRAType type = null;
        LRAState lraState;
        Annotation transactional = method.getDeclaredAnnotation(LRA.class);

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

        if (endAnnotation && txId == null)
            return; // TODO figure out the correct semantics nested versus ...

        lraClient = getLraClient();

        lraState = new LRAState(txId, false, lraClient.getUrl());

        switch (type) {
            case MANDATORY: // a txn must be present
                checkForTx(type, txId, true);
                resumeTransaction(txId); // txId is not null
                break;
            case NEVER: // a txn must not be present
                checkForTx(type, txId, false);
                enlist = false;
                // TODO remove LRA header
                break;
            case NOT_SUPPORTED:
                // suspend any currently active transaction
//                    previous = AtomicAction.suspend();
                enlist = false;
                if (txId != null) {
                    headers.remove(LRA_HTTP_HEADER);
                }
                break;
            case REQUIRED:
                if (txId != null) {
                    resumeTransaction(txId);
                } else {
                    lraState.id = txId = lraClient.startLRA(method.getName(), 500);
                    lraState.inOurLRA = true;
                }

                break;
            case REQUIRES_NEW:
//                    previous = AtomicAction.suspend();
                lraState.id = lraClient.startLRA(method.getName(), 500);
                lraState.inOurLRA = true;
                // TODO put the old one back on the after filter
                break;
            case SUPPORTS:
                if (txId != null)
                    resumeTransaction(txId);
                break;
        }

        if (txId == null)
            return;

        setCoordinator(txId);

        headers.putSingle(LRA_HTTP_HEADER, lraState.id);

        // TODO make sure it is possible to do compensations inside a new LRA
        if (!endAnnotation && enlist) { // don't enlist for methods marked with Compensate, Complete or Leave
            Map<String, String> terminateURIs = getTerminationUris(resourceInfo.getResourceClass());

            Annotation resourcePathAnnotation = resourceInfo.getResourceClass().getAnnotation(Path.class);
            String resourcePath = resourcePathAnnotation == null ? "/" : ((Path) resourcePathAnnotation).value();

            URI baseUri = containerRequestContext.getUriInfo().getBaseUri();
            String uriPrefix = String.format("%s:%s%s",
                    baseUri.getScheme(), baseUri.getSchemeSpecificPart(), resourcePath.substring(1));

            lraClient.joinLRA(lraState.id, 0,
                    String.format("%s%s", uriPrefix, terminateURIs.get(COMPENSATE)),
                    String.format("%s%s", uriPrefix, terminateURIs.get(COMPLETE)),
                    String.format("%s%s", uriPrefix, terminateURIs.get(LEAVE)),
                    String.format("%s%s", uriPrefix, terminateURIs.get(STATUS)));

            headers.putSingle(LRA_HTTP_RECOVERY_HEADER, lraState.rcvUrl);
        }

        if (method.isAnnotationPresent(Leave.class) && lraState.id != null) {
            // leave the LRA
            if (lraState.participantUrl == null)
                getParticipantLinks(lraState, containerRequestContext.getUriInfo().getBaseUri(), lraClient.getUrl());

            lraClient.leaveLRA(lraState.id, lraState.participantUrl);

            // let the compensator know which lra he left by leaving the header intact
//                containerRequestContext.getHeaders().remove(LRA_HTTP_HEADER);
//                LRAState.clearCurrentLRA();
        } else {
            LRAState.setCurrentLRA(lraState);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        LRAState state = LRAState.clearCurrentLRA();

        if (state != null) {
            if (state.inOurLRA) {
                // close the lra before returning
                lraClient.closeLRA(state.id);
                responseContext.getHeaders().remove(LRA_HTTP_HEADER);
            } else {
                responseContext.getHeaders().putSingle(LRA_HTTP_HEADER, state.id);
            }

            if (state.lraToRestore != null)
                responseContext.getHeaders().putSingle(LRA_HTTP_HEADER, state.lraToRestore);
        }

//        if (previous != null)
//            AtomicAction.resume(previous);
    }

    private StringBuilder getParticipantLink(StringBuilder b, String uriPrefix, String key, String value) {

        String terminationUri = String.format("%s%s", uriPrefix, value);
        Link link =  Link.fromUri(terminationUri).title(key + " URI").rel(key).type(MediaType.TEXT_PLAIN).build();

        if (b.length() != 0)
            b.append(',');

        return b.append(link);
    }

    private void getParticipantLinks(LRAState lraState, URI baseUri, String coordinatorUrl) {
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

        lraState.statusUrl = terminateURIs.get(STATUS);
        lraState.participantUrl = linkHeaderValue.toString();
    }

    private void resumeTransaction(String txId) {
        // nothing to do
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
