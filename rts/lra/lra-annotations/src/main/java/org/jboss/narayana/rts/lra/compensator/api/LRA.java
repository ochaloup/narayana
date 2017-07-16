/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.narayana.rts.lra.compensator.api;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import javax.ws.rs.core.Response;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@InterceptorBinding
@Retention(value = RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LRA {

    /**
     * The Type element of the LRA annotation indicates whether a bean method
     * is to be executed within a compensatable transaction context.
     */
    Type value() default Type.REQUIRED;

    /**
     * The Type element of the annotation indicates whether a bean method is to be
     * executed within a compensatable transaction context where the values provide the following
     * corresponding behavior.
     */
    enum Type {
        /**
         *  <p>If called outside a compensatable transaction context, the interceptor must begin a new
         *  JTA transaction, the managed bean method execution must then continue
         *  inside this compensatable transaction context and the transaction must be completed by
         *  the interceptor.</p>
         *  <p>If called inside a compensatable transaction context, the managed bean
         *  method execution must then continue inside this compensatable transaction context.</p>
         */
        REQUIRED,

        /**
         *  <p>If called outside a transaction context, the interceptor must begin a new
         *  JTA transaction, the managed bean method execution must then continue
         *  inside this transaction context, and the transaction must be completed by
         *  the interceptor.</p>
         *  <p>If called inside a transaction context, the current transaction context must
         *  be suspended, a new JTA transaction will begin, the managed bean method
         *  execution must then continue inside this transaction context, the transaction
         *  must be completed, and the previously suspended transaction must be resumed.</p>
         */
        REQUIRES_NEW,

        /**
         *  <p>If called outside a transaction context, a TransactionalException with a
         *  nested TransactionRequiredException must be thrown.</p>
         *  <p>If called inside a transaction context, managed bean method execution will
         *  then continue under that context.</p>
         */
        MANDATORY,

        /**
         *  <p>If called outside a transaction context, managed bean method execution
         *  must then continue outside a transaction context.</p>
         *  <p>If called inside a transaction context, the managed bean method execution
         *  must then continue inside this transaction context.</p>
         */
        SUPPORTS,

        /**
         *  <p>If called outside a transaction context, managed bean method execution
         *  must then continue outside a transaction context.</p>
         *  <p>If called inside a transaction context, the current transaction context must
         *  be suspended, the managed bean method execution must then continue
         *  outside a transaction context, and the previously suspended transaction
         *  must be resumed by the interceptor that suspended it after the method
         *  execution has completed.</p>
         */
        NOT_SUPPORTED,

        /**
         *  <p>If called outside a transaction context, managed bean method execution
         *  must then continue outside a transaction context.</p>
         *  <p>If called inside a transaction context, a TransactionalException with
         *  a nested InvalidTransactionException must be thrown.</p>
         */
        NEVER
    }

    /**
     * Some annotations (such as REQUIRES_NEW) will start an LRA on entry to a method and
     * end it on exit. For some business activities it is desirable for the action to survive
     * method execution and be completed elsewhere.
     *
     * @return whether or not newly created LRAs will survive after the method has executed.
     */
    boolean longRunning() default false;

    /**
     * Normally if an LRA is present when a bean method is invoked it will not be ended when the method returns.
     * To override this behaviour use the terminal element to force its' termination
     *
     * @return true if pre existing LRAs will be terminated when the bean method finishes.
     */
    boolean terminal() default false;

    /**
     * The cancelOnFamily element can be set to indicate which families of HTTP response codes will cause
     * the LRA to cancel. By default client errors (4xx codes) and server errors (5xx codes) will result in
     * cancellation of the LRA.
     *
     * @return the {@link Response.Status.Family} families that will cause cancellation of the LRA
     */
    @Nonbinding
    Response.Status.Family[] cancelOnFamily() default {Response.Status.Family.CLIENT_ERROR, Response.Status.Family.SERVER_ERROR};

    /**
     * The cancelOn element can be set to indicate which  HTTP response codes will cause the LRA to cancel
     *
     * @return the {@link Response.Status} HTTP status codes that will cause cancellation of the LRA
     */
    @Nonbinding
    Response.Status [] cancelOn() default {};
}
