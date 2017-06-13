package org.jboss.narayana.rts.lra.compensator.api;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In order to supprt recovery compensators must be able to report their status once the completion part of the protocol
 * starts.
 *
 * Classes annotated with this annotation must be JAX-RS resources (ie are annotated with @Path).
 *
 * Methods that are annotated with the Status annotation must report their status using one of the enum
 * names listed in {@link CompensatorStatus} whenever an HTTP GET request is made on the method
 *
 * If the compensator has not yet been asked to complete or compensate it should throw a {@link javax.ws.rs.WebApplicationException}
 * with HTTP status code 400: Bad request
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Status {
}