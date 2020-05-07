package com.arjuna.ats.jta.cdi.common;

import com.arjuna.ats.jta.logging.jtaLogger;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

@ApplicationScoped
public class TransactionHandler {

    public boolean handleExceptionNoThrow(Transactional transactional, Throwable e, Transaction tx)
            throws IllegalStateException, SystemException {

        for (Class<?> dontRollbackOnClass : transactional.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(e.getClass())) {
                return true;
            }
        }

        for (Class<?> rollbackOnClass : transactional.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(e.getClass())) {
                tx.setRollbackOnly();
                return true;
            }
        }

        if (e instanceof RuntimeException) {
            tx.setRollbackOnly();
            return true;
        }

        return false;
    }

    public void endTransaction(TransactionManager tm, Transaction tx, RunnableWithException afterEndTransaction) throws Exception {
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
}
