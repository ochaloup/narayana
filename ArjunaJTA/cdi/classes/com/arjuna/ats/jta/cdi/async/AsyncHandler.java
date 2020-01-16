package com.arjuna.ats.jta.cdi.async;

import com.arjuna.ats.jta.cdi.common.RunnableWithException;

import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

public interface AsyncHandler {
    /**
     * Handling the asynchronous types.
     *
     * @param objectToHandle  type of the object to be handled asynchronously
     * @return true if the end transaction was handled, false if it was not capable to handle it asynchronously
     */
    boolean handleReturnType(TransactionManager tm, Transaction tx, Transactional transactional, Object objectToHandle, RunnableWithException afterEndTransaction) throws Exception;
}
