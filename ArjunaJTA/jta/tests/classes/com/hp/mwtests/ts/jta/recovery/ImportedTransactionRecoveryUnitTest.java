/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package com.hp.mwtests.ts.jta.recovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TwoPhaseOutcome;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import com.arjuna.ats.jta.TransactionManager;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.ats.jta.xa.XATxConverter;

public class ImportedTransactionRecoveryUnitTest {
    int orphanSafetyIntervalOrigin;
    List<String> xaRecoveryNodesOrigin = null, xaResourceOrphanFiltersOrigin = null;
    RecoveryManager recoveryManager = null;

    @Before
    public void setUp () throws Exception {
        final List<String> recoveryExtensions = new ArrayList<String>();
        recoveryExtensions.add(XARecoveryModule.class.getName());
        recoveryPropertyManager.getRecoveryEnvironmentBean().setRecoveryModuleClassNames(recoveryExtensions);

        orphanSafetyIntervalOrigin = jtaPropertyManager.getJTAEnvironmentBean().getOrphanSafetyInterval();
        jtaPropertyManager.getJTAEnvironmentBean().setOrphanSafetyInterval(0);
        xaResourceOrphanFiltersOrigin = jtaPropertyManager.getJTAEnvironmentBean().getXaResourceOrphanFilterClassNames();
        xaRecoveryNodesOrigin = jtaPropertyManager.getJTAEnvironmentBean().getXaRecoveryNodes();

        recoveryManager = RecoveryManager.manager(RecoveryManager.DIRECT_MANAGEMENT);
        recoveryManager.initialize();
    }

    @After
    public void tearDown () throws Exception {
        jtaPropertyManager.getJTAEnvironmentBean().setOrphanSafetyInterval(orphanSafetyIntervalOrigin);
        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(xaRecoveryNodesOrigin);
        jtaPropertyManager.getJTAEnvironmentBean().setXaResourceOrphanFilterClassNames(xaResourceOrphanFiltersOrigin);
    }
    
    @Test
    public void testTopLevelTransactionOrphanFilter() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        final TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            @Override
            public void commit(javax.transaction.xa.Xid xid, boolean onePhase) throws XAException {
                try {
                    latch.await();
                    super.commit(xid, onePhase);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Awaiting on latch '" + latch + "' failed", e);
                }
            }
        };
        
        Future<Boolean> prepareCommitFuture = Executors.newSingleThreadExecutor().submit(() -> {
            TransactionManager.transactionManager().begin();
            Transaction topLevelTransaction = TransactionManager.transactionManager().getTransaction();

            assertTrue("Fail to enlist first test XAResource", topLevelTransaction.enlistResource(xar1));
            assertTrue("Fail to enlist second XAResource", topLevelTransaction.enlistResource(xar2));

            topLevelTransaction.commit();
            return true;
        });
        
        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceOrphanFilter(new JTANodeNameXAResourceOrphanFilter());
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceOrphanFilter(new JTATransactionLogXAResourceOrphanFilter());
        
        recoveryManager.scan();
        
        latch.countDown();
        prepareCommitFuture.get();
        
        assertEquals("XAResource1 should not rollback", 0, xar1.rollbackCount());
        assertEquals("XAResource2 should not rollback", 0, xar2.rollbackCount());
        assertEquals("XAResource1 should commit", 1, xar1.commitCount());
        assertEquals("XAResource2 should commit", 1, xar2.commitCount());
    }

    @Test
    public void testSubordinateTransactionOrphanFilter() throws Exception {
        Uid uid = new Uid();
        Xid xid = XATxConverter.getXid(uid, true, XATxConverter.FORMAT_ID);
        SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().importTransaction(xid);

        final CountDownLatch latch = new CountDownLatch(1);
        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            @Override
            public void commit(javax.transaction.xa.Xid id, boolean onePhase) throws XAException {
                try {
                    latch.await();
                    super.commit(xid, onePhase);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Awaiting on latch '" + latch + "' failed", e);
                }
            }
        };

        assertTrue("Fail to enlist first test XAResource", subordinateTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", subordinateTransaction.enlistResource(xar2));

        Future<Boolean> prepareCommitFuture = Executors.newSingleThreadExecutor().submit(() -> {
            assertEquals("transaction should be prepared", TwoPhaseOutcome.PREPARE_OK, subordinateTransaction.doPrepare());
            assertTrue("commit should be processed even longer waiting", subordinateTransaction.doCommit());
            return true;
        });

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceOrphanFilter(new JTANodeNameXAResourceOrphanFilter());
        ((XARecoveryModule) recoveryManager.getModules().get(0))
           .addXAResourceOrphanFilter(new JTATransactionLogXAResourceOrphanFilter());

        recoveryManager.scan();

        latch.countDown();
        prepareCommitFuture.get();

        assertEquals("XAResource1 should ? rollback", 0, xar1.rollbackCount());
        assertEquals("XAResource2 should ? rollback", 0, xar2.rollbackCount());
        assertEquals("XAResource1 should ? commit", 1, xar1.commitCount());
    }
}
