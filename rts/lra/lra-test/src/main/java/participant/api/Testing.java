package participant.api;

import org.jboss.narayana.rts.lra.compensator.api.Compensate;
import org.jboss.narayana.rts.lra.compensator.api.Complete;
import org.jboss.narayana.rts.lra.compensator.api.Leave;
import org.jboss.narayana.rts.lra.compensator.api.Status;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.COMPENSATE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.COMPLETE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LEAVE;
import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.STATUS;

/*
  * Just for testing - the following is how the interceptors figure out compensator URLs via annotations
  * Compenators that wish to manage their lifecycle programatically should follow the raw spec
  */
class Testing {

    private static StringBuilder getParticipantLink(StringBuilder b, String key, String value) {

        Link link =  Link.fromUri(value).title(key + " URI").rel(key).type(MediaType.TEXT_PLAIN).build();

        if (b.length() != 0)
            b.append(',');

        return b.append(link);
    }

    static String getCompensatorUrl(URI baseUri, Class<?> resourceClass) {

        Map<String, String> terminateURIs = getTerminationUris(resourceClass, baseUri);

        if (terminateURIs.size() < 3)
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing complete, compensate or status annotations").build());

        // register with the coordinator
        StringBuilder linkHeaderValue = new StringBuilder();

        terminateURIs.forEach((k, v) -> getParticipantLink(linkHeaderValue, k, v));

        return linkHeaderValue.toString();
    }

    static private Map<String, String> getTerminationUris(Class<?> compensatorClass, URI baseUri) {
        Map<String, String> paths = new HashMap<>();

        Annotation resourcePathAnnotation = compensatorClass.getAnnotation(Path.class);
        String resourcePath = resourcePathAnnotation == null ? "/" : ((Path) resourcePathAnnotation).value();

        String uriPrefix = String.format("%s:%s%s",
                baseUri.getScheme(), baseUri.getSchemeSpecificPart(), resourcePath.substring(1));

        Arrays.stream(compensatorClass.getMethods()).forEach(method -> {
            Annotation pathAnnotation = method.getAnnotation(Path.class);

            if (pathAnnotation != null) {
                checkMethod(paths, COMPLETE, (Path) pathAnnotation, method.getAnnotation(Complete.class), uriPrefix);
                checkMethod(paths, COMPENSATE, (Path) pathAnnotation, method.getAnnotation(Compensate.class), uriPrefix);
                checkMethod(paths, STATUS, (Path) pathAnnotation, method.getAnnotation(Status.class), uriPrefix);
                checkMethod(paths, LEAVE, (Path) pathAnnotation, method.getAnnotation(Leave.class), uriPrefix);
            }
        });

        return paths;
    }

    static private void checkMethod(Map<String, String> paths, String rel,
                                    Path pathAnnotation,
                                    Annotation annotationClass,
                                    String uriPrefix) {
        if (annotationClass != null)
            paths.put(rel, uriPrefix + pathAnnotation.value());
    }
}
