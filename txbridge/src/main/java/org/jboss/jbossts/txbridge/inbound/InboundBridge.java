/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
 *
 * (C) 2007, 2009 @author JBoss Inc
 */
package org.jboss.jbossts.txbridge.inbound;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.jboss.jbossts.txbridge.utils.txbridgeLogger;
import org.wildfly.transaction.client.LocalTransactionContext;

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import com.arjuna.ats.jta.TransactionManager;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * Manages Thread association of the interposed coordinator.
 * Typically called from handlers in the WS stack.
 *
 * @author jonathan.halliday@redhat.com, 2007-04-30
 */
public class InboundBridge
{
    /**
     * Identifier for the subordinate transaction.
     */
    private final Xid xid;

    /**
     * Create a new InboundBridge to manage the given subordinate JTA transaction.
     *
     * @param xid the subordinate transaction id
     * @throws XAException
     * @throws SystemException
     */
    InboundBridge(Xid xid) throws XAException, SystemException
    {
        txbridgeLogger.logger.trace("InboundBridge.<ctor>(Xid="+xid+")");

        this.xid = xid;

        getTransaction(); // ensures transaction is initialized
    }

    /**
     * Associate the JTA transaction to the current Thread.
     * Typically used by a server side inbound handler.
     *
     * @throws XAException
     * @throws SystemException
     * @throws InvalidTransactionException
     * @throws NamingException
     * @throws IllegalStateException
     */
    public void start() throws XAException, SystemException, InvalidTransactionException, IllegalStateException, NamingException
    {
        txbridgeLogger.logger.trace("InboundBridge.start(Xid="+xid+")");
        txbridgeLogger.logger.warn("context: " + toMap(new InitialContext()));

        Transaction tx = getTransaction();

        TransactionManager.transactionManager(new InitialContext()).resume(tx);
    }

    public static Map toMap(Context ctx) throws NamingException {
        String namespace = ctx instanceof InitialContext ? ctx.getNameInNamespace() : "";
        HashMap<String, Object> map = new HashMap<String, Object>();
        txbridgeLogger.logger.info("> Listing namespace: " + namespace);
        NamingEnumeration<NameClassPair> list = ctx.list(namespace);
        while (list.hasMoreElements()) {
            NameClassPair next = list.next();
            String name = next.getName();
            String jndiPath = namespace + name;
            Object lookup;
            try {
                txbridgeLogger.logger.info("> Looking up name: " + jndiPath);
                Object tmp = ctx.lookup(jndiPath);
                if (tmp instanceof Context) {
                    lookup = toMap((Context) tmp);
                } else {
                    lookup = tmp.toString();
                }
            } catch (Throwable t) {
                lookup = t.getMessage();
            }
            map.put(name, lookup);

        }
        return map;
    }

    /**
     * Disassociate the JTA transaction from the current Thread.
     * Typically used by a server side outbound handler.
     *
     * @throws XAException
     * @throws SystemException
     * @throws InvalidTransactionException
     * @throws NamingException
     */
    public void stop() throws XAException, SystemException, InvalidTransactionException, NamingException
    {
        txbridgeLogger.logger.trace("InboundBridge.stop("+xid+")");

        TransactionManager.transactionManager(new InitialContext()).suspend();
    }

    public void setRollbackOnly() throws XAException, SystemException
    {
        txbridgeLogger.logger.trace("InboundBridge.setRollbackOnly("+xid+")");

        getTransaction().setRollbackOnly();
    }

    /**
     * Get the JTA Transaction which corresponds to the Xid of the instance.
     *
     * @return
     * @throws XAException
     * @throws SystemException
     */
    private Transaction getTransaction()
            throws XAException, SystemException
    {
        Transaction tx = jtaPropertyManager.getJTAEnvironmentBean().importSubordinateTransaction(xid);

        switch (tx.getStatus())
        {
            // TODO: other cases?

            case Status.STATUS_ACTIVE:
            case Status.STATUS_MARKED_ROLLBACK:
                break;
            default:
                throw new IllegalStateException("Transaction not in state ACTIVE");
        }
        return tx;
    }
}
