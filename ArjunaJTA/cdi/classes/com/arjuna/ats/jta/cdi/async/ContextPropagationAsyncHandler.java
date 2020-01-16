/*
 * Copyright 2020, Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arjuna.ats.jta.cdi.async;

import com.arjuna.ats.jta.cdi.RunnableWithException;
import com.arjuna.ats.jta.cdi.TransactionHandler;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Handling asynchronous context propagation calls.
 * It extends transactions until the intercepted method's async return type is completed.
 */
public final class ContextPropagationAsyncHandler {

    /**
     * Based on the type of 'objectToHandle' taking a way to await for async to return
     * and delaying transaction completion.
     *
     * @param tm  transaction manager
     * @param tx  the original transaction
     * @param transactional  link to method which is annotated with @{@link Transactional}
     * @param objectToHandle  on interceptor proceed this is the returned type which differentiate the action
     * @param afterEndTransaction  a lamda invocation on transaction finalization
     * @return true if async handling is possible and it was proceeded, false means async processing is not possible
     * @throws Exception failure on async processing error happens
     */
    public static boolean handleReturnType(
            TransactionManager tm, Transaction tx, Transactional transactional, Object objectToHandle, RunnableWithException afterEndTransaction) throws Exception {

        ReactiveTypeConverter<Object> converter = null;
        if (objectToHandle instanceof CompletionStage == false
                && objectToHandle instanceof Publisher == false) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Optional<ReactiveTypeConverter<Object>> lookup = Registry.lookup((Class) objectToHandle.getClass());
            if (lookup.isPresent()) {
                converter = lookup.get();
                if (converter.emitAtMostOneItem()) {
                    objectToHandle = converter.toCompletionStage(objectToHandle);
                } else {
                    objectToHandle = converter.toRSPublisher(objectToHandle);
                }
            }
        }
        if (objectToHandle instanceof CompletionStage) {
            objectToHandle = handleAsync(tm, tx, transactional, objectToHandle, afterEndTransaction);
            // convert back
            if (converter != null)
                objectToHandle = converter.fromCompletionStage((CompletionStage<?>) objectToHandle);
        } else if (objectToHandle instanceof Publisher) {
            objectToHandle = handleAsync(tm, tx, transactional, objectToHandle, afterEndTransaction);
            // convert back
            if (converter != null)
                objectToHandle = converter.fromPublisher((Publisher<?>) objectToHandle);
        } else {
            // not async: handle synchronously
            return false;
        }
        return true;
    }

    private static Object handleAsync(TransactionManager tm, Transaction tx, Transactional transactional, Object ret, RunnableWithException afterEndTransaction) throws Exception {
        // Suspend the transaction to remove it from the main request thread
        tm.suspend();
        afterEndTransaction.run();
        if (ret instanceof CompletionStage) {
            return ((CompletionStage<?>) ret).handle((v, throwable) -> {
                try {
                    doInTransaction(tm, tx, () -> {
                        if (throwable != null) {
                            TransactionHandler.handleExceptionNoThrow(transactional, throwable, tx);
                        }
                        TransactionHandler.endTransaction(tm, tx, () -> {});
                    });
                } catch (RuntimeException e) {
                    if (throwable != null)
                        e.addSuppressed(throwable);
                    throw e;
                } catch (Exception e) {
                    CompletionException x = new CompletionException(e);
                    if (throwable != null)
                        x.addSuppressed(throwable);
                    throw x;
                }
                // pass-through the previous results
                if (throwable instanceof RuntimeException)
                    throw (RuntimeException) throwable;
                if (throwable != null)
                    throw new CompletionException(throwable);
                return v;
            });
        } else if (ret instanceof Publisher) {
            ret = ReactiveStreams.fromPublisher(((Publisher<?>) ret))
                    .onError(throwable -> {
                        try {
                            doInTransaction(tm, tx, () -> TransactionHandler.handleExceptionNoThrow(transactional, throwable, tx));
                        } catch (RuntimeException e) {
                            e.addSuppressed(throwable);
                            throw e;
                        } catch (Exception e) {
                            RuntimeException x = new RuntimeException(e);
                            x.addSuppressed(throwable);
                            throw x;
                        }
                        // pass-through the previous result
                        if (throwable instanceof RuntimeException)
                            throw (RuntimeException) throwable;
                        throw new RuntimeException(throwable);
                    }).onTerminate(() -> {
                        try {
                            doInTransaction(tm, tx, () -> TransactionHandler.endTransaction(tm, tx, () -> {
                            }));
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            RuntimeException x = new RuntimeException(e);
                            throw x;
                        }
                    })
                    .buildRs();
        }
        return ret;
    }

    private static void doInTransaction(TransactionManager tm, Transaction tx, RunnableWithException f) throws Exception {
        // Verify if this thread's transaction is the right one
        Transaction currentTransaction = tm.getTransaction();
        // If not, install the right transaction
        if (currentTransaction != tx) {
            if (currentTransaction != null)
                tm.suspend();
            tm.resume(tx);
        }
        f.run();
        if (currentTransaction != tx) {
            tm.suspend();
            if (currentTransaction != null)
                tm.resume(currentTransaction);
        }
    }
}
