/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013-2018 Red Hat, Inc., and individual contributors
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

package com.arjuna.ats.jta.cdi.transactional;


import com.arjuna.ats.jta.cdi.RunnableWithException;
import com.arjuna.ats.jta.cdi.TransactionExtension;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.ats.jta.logging.jtaLogger;
import org.jboss.tm.usertx.UserTransactionOperationsProvider;

import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.rmi.registry.Registry;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static java.security.AccessController.doPrivileged;

/**
 * @author paul.robinson@redhat.com 02/05/2013
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public abstract class TransactionalInterceptorBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    transient javax.enterprise.inject.spi.BeanManager beanManager;

    @Inject
    private TransactionExtension extension;

    @Inject
    @Intercepted
    private Bean<?> interceptedBean;

    @Inject
    private TransactionManager transactionManager;

    private final boolean userTransactionAvailable;

    protected TransactionalInterceptorBase(boolean userTransactionAvailable) {
        this.userTransactionAvailable = userTransactionAvailable;
    }

    public Object intercept(InvocationContext ic) throws Exception {

        final Transaction tx = transactionManager.getTransaction();

        boolean previousUserTransactionAvailability = setUserTransactionAvailable(userTransactionAvailable);
        try {
            return doIntercept(transactionManager, tx, ic);
        } finally {
            resetUserTransactionAvailability(previousUserTransactionAvailability);
        }
    }

    protected abstract Object doIntercept(TransactionManager tm, Transaction tx, InvocationContext ic) throws Exception;

    /**
     * <p>
     * Looking for the {@link Transactional} annotation first on the method, second on the class.
     * <p>
     * Method handles CDI types to cover cases where extensions are used.
     * In case of EE container uses reflection.
     *
     * @param ic  invocation context of the interceptor
     * @return instance of {@link Transactional} annotation or null
     */
    private Transactional getTransactional(InvocationContext ic) {
        if(interceptedBean != null) { // not-null for CDI
            // getting annotated type and method corresponding of the intercepted bean and method
            AnnotatedType<?> currentAnnotatedType = extension.getBeanToAnnotatedTypeMapping().get(interceptedBean);
            AnnotatedMethod<?> currentAnnotatedMethod = null;
            for(AnnotatedMethod<?> methodInSearch: currentAnnotatedType.getMethods()) {
                if(methodInSearch.getJavaMember().equals(ic.getMethod())) {
                    currentAnnotatedMethod = methodInSearch;
                    break;
                }
            }
    
            // check existence of the stereotype on method
            Transactional transactionalMethod = getTransactionalAnnotationRecursive(currentAnnotatedMethod.getAnnotations());
            if(transactionalMethod != null) return transactionalMethod;
            // stereotype recursive search, covering ones added by an extension too
            Transactional transactionalExtension = getTransactionalAnnotationRecursive(currentAnnotatedType.getAnnotations());
            if(transactionalExtension != null) return transactionalExtension;
            // stereotypes already merged to one chunk by BeanAttributes.getStereotypes()
            for(Class<? extends Annotation> stereotype: interceptedBean.getStereotypes()) {
                Transactional transactionalAnn = stereotype.getAnnotation(Transactional.class);
                if(transactionalAnn != null) return transactionalAnn;
            }
        } else { // null for EE components
            Transactional transactional = ic.getMethod().getAnnotation(Transactional.class);
            if (transactional != null) {
                return transactional;
            }
    
            Class<?> targetClass = ic.getTarget().getClass();
            transactional = targetClass.getAnnotation(Transactional.class);
            if (transactional != null) {
                return transactional;
            }
        }

        throw new RuntimeException(jtaLogger.i18NLogger.get_expected_transactional_annotation());
    }

    private Transactional getTransactionalAnnotationRecursive(Annotation... annotationsOnMember) {
        if(annotationsOnMember == null) return null;
        Set<Class<? extends Annotation>> stereotypeAnnotations = new HashSet<>();

        for(Annotation annotation: annotationsOnMember) {
            if(annotation.annotationType().equals(Transactional.class)) {
                return (Transactional) annotation;
            }
            if (beanManager.isStereotype(annotation.annotationType())) {
                stereotypeAnnotations.add(annotation.annotationType());
            }
        }
        for(Class<? extends Annotation> stereotypeAnnotation: stereotypeAnnotations) {
            return getTransactionalAnnotationRecursive(beanManager.getStereotypeDefinition(stereotypeAnnotation));
        }
        return null;
    }

    private Transactional getTransactionalAnnotationRecursive(Set<Annotation> annotationsOnMember) {
        return getTransactionalAnnotationRecursive(
            annotationsOnMember.toArray(new Annotation[annotationsOnMember.size()]));
    }

    protected Object invokeInOurTx(InvocationContext ic, TransactionManager tm) throws Exception {
        return invokeInOurTx(ic, tm, () -> {});
    }

    protected Object invokeInOurTx(InvocationContext ic, TransactionManager tm, RunnableWithException afterEndTransaction) throws Exception {

        tm.begin();
        Transaction tx = tm.getTransaction();

        boolean throwing = false;
        Object ret = null;

        try {
            ret = ic.proceed();
        } catch (Exception e) {
            throwing = true;
            handleException(ic, e, tx);
        } finally {
            // handle asynchronously if not throwing
            if (!throwing && ret != null) {
                ReactiveTypeConverter<Object> converter = null;
                if (ret instanceof CompletionStage == false
                        && ret instanceof Publisher == false) {
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    Optional<ReactiveTypeConverter<Object>> lookup = Registry.lookup((Class) ret.getClass());
                    if (lookup.isPresent()) {
                        converter = lookup.get();
                        if (converter.emitAtMostOneItem()) {
                            ret = converter.toCompletionStage(ret);
                        } else {
                            ret = converter.toRSPublisher(ret);
                        }
                    }
                }
                if (ret instanceof CompletionStage) {
                    ret = handleAsync(tm, tx, ic, ret, afterEndTransaction);
                    // convert back
                    if (converter != null)
                        ret = converter.fromCompletionStage((CompletionStage<?>) ret);
                } else if (ret instanceof Publisher) {
                    ret = handleAsync(tm, tx, ic, ret, afterEndTransaction);
                    // convert back
                    if (converter != null)
                        ret = converter.fromPublisher((Publisher<?>) ret);
                } else {
                    // not async: handle synchronously
                    endTransaction(tm, tx, afterEndTransaction);
                }
            } else {
                // throwing or null: handle synchronously
                endTransaction(tm, tx, afterEndTransaction);
            }
        }
        return ret;
    }

    protected Object handleAsync(TransactionManager tm, Transaction tx, InvocationContext ic, Object ret,
                                 RunnableWithException afterEndTransaction) throws Exception {
        // Suspend the transaction to remove it from the main request thread
        tm.suspend();
        afterEndTransaction.run();
        if (ret instanceof CompletionStage) {
            return ((CompletionStage<?>) ret).handle((v, t) -> {
                try {
                    doInTransaction(tm, tx, () -> {
                        if (t != null)
                            handleExceptionNoThrow(ic, t, tx);
                        endTransaction(tm, tx, () -> {});
                    });
                } catch (RuntimeException e) {
                    if (t != null)
                        e.addSuppressed(t);
                    throw e;
                } catch (Exception e) {
                    CompletionException x = new CompletionException(e);
                    if (t != null)
                        x.addSuppressed(t);
                    throw x;
                }
                // pass-through the previous results
                if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                if (t != null)
                    throw new CompletionException(t);
                return v;
            });
        } else if (ret instanceof Publisher) {
            ret = ReactiveStreams.fromPublisher(((Publisher<?>) ret))
                    .onError(t -> {
                        try {
                            doInTransaction(tm, tx, () -> handleExceptionNoThrow(ic, t, tx));
                        } catch (RuntimeException e) {
                            e.addSuppressed(t);
                            throw e;
                        } catch (Exception e) {
                            RuntimeException x = new RuntimeException(e);
                            x.addSuppressed(t);
                            throw x;
                        }
                        // pass-through the previous result
                        if (t instanceof RuntimeException)
                            throw (RuntimeException) t;
                        throw new RuntimeException(t);
                    }).onTerminate(() -> {
                        try {
                            doInTransaction(tm, tx, () -> endTransaction(tm, tx, () -> {
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

    private void doInTransaction(TransactionManager tm, Transaction tx, RunnableWithException f) throws Exception {
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

    protected Object invokeInCallerTx(InvocationContext ic, Transaction tx) throws Exception {

        try {
            return ic.proceed();
        } catch (Exception e) {
            handleException(ic, e, tx);
        }
        throw new RuntimeException("UNREACHABLE");
    }

    protected Object invokeInNoTx(InvocationContext ic) throws Exception {

        return ic.proceed();
    }

    protected void handleExceptionNoThrow(InvocationContext ic, Throwable e, Transaction tx)
            throws IllegalStateException, SystemException {

        Transactional transactional = getTransactional(ic);

        for (Class<?> dontRollbackOnClass : transactional.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(e.getClass())) {
                return;
            }
        }

        for (Class<?> rollbackOnClass : transactional.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(e.getClass())) {
                tx.setRollbackOnly();
                return;
            }
        }

        if (e instanceof RuntimeException) {
            tx.setRollbackOnly();
            return;
        }
    }

    protected void handleException(InvocationContext ic, Exception e, Transaction tx) throws Exception {

        handleExceptionNoThrow(ic, e, tx);
        throw e;
    }

    protected void endTransaction(TransactionManager tm, Transaction tx, RunnableWithException afterEndTransaction) throws Exception {
        try {
            if (tx != tm.getTransaction()) {
                throw new RuntimeException(jtaLogger.i18NLogger.get_wrong_tx_on_thread());
            }

            if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                tm.rollback();
            } else {
                tm.commit();
            }
        } finally {
            afterEndTransaction.run();
        }
    }

    protected boolean setUserTransactionAvailable(boolean available) {

        UserTransactionOperationsProvider userTransactionProvider =
            jtaPropertyManager.getJTAEnvironmentBean().getUserTransactionOperationsProvider();
        boolean previousUserTransactionAvailability = userTransactionProvider.getAvailability();

        setAvailability(userTransactionProvider, available);

        return previousUserTransactionAvailability;
    }

    protected void resetUserTransactionAvailability(boolean previousUserTransactionAvailability) {
        UserTransactionOperationsProvider userTransactionProvider =
            jtaPropertyManager.getJTAEnvironmentBean().getUserTransactionOperationsProvider();
        setAvailability(userTransactionProvider, previousUserTransactionAvailability);
    }

    private void setAvailability(UserTransactionOperationsProvider userTransactionProvider, boolean available) {
        if (System.getSecurityManager() == null) {
            userTransactionProvider.setAvailability(available);
        } else {
            doPrivileged((PrivilegedAction<Object>) () -> {
                userTransactionProvider.setAvailability(available);
                return null;
            });
        }
    }
}
