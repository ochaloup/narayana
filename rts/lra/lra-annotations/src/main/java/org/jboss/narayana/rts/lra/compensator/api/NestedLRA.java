package org.jboss.narayana.rts.lra.compensator.api;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on the interface or class. Defines that the container will create
 * a new LRA for each method invocation, regardless of whether there is
 * already an LRA associated with the caller. These LRAs will then
 * either be top-level LRAs or nested automatically depending upon the
 * context within which they are created.
 *
 * When a nested LRA is confirmed its' compensators are propagated to the enclosing LRA (in contrast to
 * top level LRAs where compensators are informed that the activity has terminated).
 *
 * Compatability with the @LRA annotation. If @LRA is not present @Nested is ignored, otherwise the behaviour depends upon the value of the LRAType attribute:
 *
 * REQUIRED
 *  if there is an LRA present a new LRA is nested under it
 *
 * REQUIRES_NEW,
 *  the @Nested annotation is ignored
 *
 * MANDATORY,
 *  a new LRA is nested under the incomming LRA
 *
 * SUPPORTS,
 *  if there is an LRA present a new LRA is nested under otherwise a new top level LRA is begun
 *
 * NOT_SUPPORTED,
 *  nested does not make sense and a WebApplicationException exception is thrown (with HTTP status code PRECONDITION_FAILED)
 *
 * NEVER
 *  nested does not make sense and a WebApplicationException exception is thrown (with HTTP status code PRECONDITION_FAILED)
 */
@Inherited
@InterceptorBinding
@Retention(value = RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NestedLRA {
}
