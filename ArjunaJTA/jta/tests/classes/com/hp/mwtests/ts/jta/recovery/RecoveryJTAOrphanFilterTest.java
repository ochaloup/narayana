/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import com.arjuna.ats.arjuna.objectstore.RecoveryStore;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.internal.arjuna.common.UidHelper;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateAtomicActionRecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateJTAXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.subordinate.jca.SubordinateAtomicAction;
import com.arjuna.ats.jta.TransactionManager;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.ats.jta.xa.XATxConverter;

/**
 * Test checking work of orphan filters which is not simulated as just unit tests but
 * there is already initiated transaction and run a recovery scan.
 */
public class RecoveryJTAOrphanFilterTest {
    int orphanSafetyIntervalOrigin;
    List<String> xaRecoveryNodesOrigin = null, xaResourceOrphanFiltersOrigin = null;
    RecoveryManager recoveryManager = null;

    @Before
    public void setUp () throws Exception {
        final List<String> recoveryExtensions = new ArrayList<String>();
        recoveryExtensions.add(SubordinateAtomicActionRecoveryModule.class.getName());
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

    /**
     * Verifies that that participants will be rolled-back when node name orphan filter decides about destiny. 
     */
    @Test
    public void testTopLevelTransactionOrphanFilterNodeName() throws Exception {
        testTopLevelTransactionOrphanFilter(
            (XARecoveryModule xaRecoveryModule) -> {
                // voting to rollback - node name matches
                xaRecoveryModule.addXAResourceOrphanFilter(new JTANodeNameXAResourceOrphanFilter());
            },
            (TestXAResourceWrapper xar1, TestXAResourceWrapper xar2) -> {
                assertEquals("XAResource1 should not rollback as it was committed before scan started", 0, xar1.rollbackCount());
                assertEquals("XAResource2 should rollback", 1, xar2.rollbackCount());
            }
        );
    }

    /**
     * Verifies that if there is the record in the object store then the the {@link JTATransactionLogXAResourceOrphanFilter}
     * ensures the transaction is not rolled-back. Simulation of the fact that the transaction would be rollback is the
     * existence of the {@link JTANodeNameXAResourceOrphanFilter} which votes for rollback as the node name matches. 
     */
    @Test
    public void testTopLevelTransactionOrphanFilterLogRecordExists() throws Exception {
        testTopLevelTransactionOrphanFilter(
            (XARecoveryModule xaRecoveryModule) -> {
                // voting to rollback - node name matches
                xaRecoveryModule.addXAResourceOrphanFilter(new JTANodeNameXAResourceOrphanFilter());
                // voting to leave alone - there is the toplevel record in the object store on the particular uid
                xaRecoveryModule.addXAResourceOrphanFilter(new JTATransactionLogXAResourceOrphanFilter());
            },
            (TestXAResourceWrapper xar1, TestXAResourceWrapper xar2) -> {
                assertEquals("XAResource1 should not rollback", 0, xar1.rollbackCount());
                assertEquals("XAResource2 should not rollback", 0, xar2.rollbackCount());
                assertEquals("XAResource1 should commit", 1, xar1.commitCount());
                assertEquals("XAResource2 should commit", 1, xar2.commitCount());
            }
        );
    }

    public void testTopLevelTransactionOrphanFilter(
        Consumer<XARecoveryModule> orphanFilterPlacer, BiConsumer<TestXAResourceWrapper, TestXAResourceWrapper> xaResourceCheck) throws Exception {
        final CountDownLatch latchWaitOnCommit = new CountDownLatch(1);
        final CountDownLatch latchWaitCommitToStart = new CountDownLatch(1);
        final TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        final TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            @Override
            public void commit(javax.transaction.xa.Xid xid, boolean onePhase) throws XAException {
                try {
                    latchWaitCommitToStart.countDown();
                    latchWaitOnCommit.await();
                    super.commit(xid, onePhase);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Awaiting on latch '" + latchWaitOnCommit + "' failed", e);
                }
            }
        };
        
        Future<Boolean> prepareCommitSynchronizationFuture = Executors.newSingleThreadExecutor().submit(() -> {
            TransactionManager.transactionManager().begin();
            Transaction topLevelTransaction = TransactionManager.transactionManager().getTransaction();

            assertTrue("Fail to enlist first test XAResource", topLevelTransaction.enlistResource(xar1));
            assertTrue("Fail to enlist second XAResource", topLevelTransaction.enlistResource(xar2));

            topLevelTransaction.commit();
            return true;
        });

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        assertTrue("cannot cast recovery module " + recoveryManager.getModules().get(1).getClass() + " as " + XARecoveryModule.class.getName()
                + ", please check the test setup", recoveryManager.getModules().get(1) instanceof XARecoveryModule);
        XARecoveryModule xaRecoveryModule = (XARecoveryModule) recoveryManager.getModules().get(1);
        xaRecoveryModule.addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        orphanFilterPlacer.accept(xaRecoveryModule);

        latchWaitCommitToStart.await(); // waiting to know that we already passed the prepare
        recoveryManager.scan();

        latchWaitOnCommit.countDown(); // leaving the in-flight to be finished
        prepareCommitSynchronizationFuture.get(); // for being sure prepare/commit was all done

        xaResourceCheck.accept(xar1, xar2);
    }

    /**
     * <p>
     * We verify that if there is an in-flight transaction for the record the participant won't be rolled-back.
     * <p>
     * Note: participants are forced to be rolled-back by the node name filter matching the node name.
     */
    @Test
    public void testSubordinateTransactionOrphanFilterInFlightTransactions() throws Exception {
        testSubordinateTransactionOrphanFilter(
            (XARecoveryModule xaRecoveryModule) -> {
                // voting to rollback - node name matches
                xaRecoveryModule.addXAResourceOrphanFilter(new JTANodeNameXAResourceOrphanFilter());
                // voting to leave alone - there is the toplevel record in the object store on the particular uid
                xaRecoveryModule.addXAResourceOrphanFilter(new SubordinationManagerXAResourceOrphanFilter());
            },
            (TestXAResourceWrapper xar1, TestXAResourceWrapper xar2) -> {
                assertEquals("XAResource1 should not rollback", 0, xar1.rollbackCount());
                assertEquals("XAResource2 should not rollback", 0, xar2.rollbackCount());
                assertEquals("XAResource1 should commit", 1, xar1.commitCount());
                assertEquals("XAResource2 should commit", 1, xar2.commitCount());
            }
        );
    }

    /**
     * <p>
     * Checking that subordinate orphan filter SubordinateJTAXAResourceOrphanFilter ensures rollbacking
     * transaction when there is no record in the object store about the transaction.
     * <p>
     * The simulation could sound a bit cryptic here but in general - we start transaction while the
     * first resource is committed but the second is locked in the commit call (see the CountDownLatch).
     * As the next step we remove all records from subordinate jca object store space and then we run
     * the recovery with the orphan filter. The filter can see the prepared state of the resource
     * but there is no log in the object store thus it runs rollback. Later on the count down latch
     * is released and the commit is finished on the second resource too.
     */
    @Test
    public void testSubordinateTransactionOrphanFilterNodeName() throws Exception {
        testSubordinateTransactionOrphanFilter(
            (XARecoveryModule xaRecoveryModule) -> {
                // removing notion about all the subordinate transactions from the log store
                // as the SubordinateJTAXAResourceOrphanFilter then will vote to rolback
                try {
                    RecoveryStore recoveryStore = StoreManager.getRecoveryStore();
                    InputObjectState states = new InputObjectState();
                    recoveryStore.allObjUids(SubordinateAtomicAction.getType(), states);
                    do {
                        Uid uid = UidHelper.unpackFrom(states);
                        if (uid.notEquals(Uid.nullUid())) {
                            recoveryStore.remove_committed(uid, SubordinateAtomicAction.getType());
                        } else {
                            break;
                        }
                    } while(true);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot remove exiting uid from the object store");
                }

                // expected to vote for rollback as subordinate nodeid matches and there are no txn in log store
                xaRecoveryModule.addXAResourceOrphanFilter(new SubordinateJTAXAResourceOrphanFilter());
            },
            (TestXAResourceWrapper xar1, TestXAResourceWrapper xar2) -> {
                assertEquals("XAResource1 should not rollback as it was already committed", 0, xar1.rollbackCount());
                assertEquals("XAResource2 should rollback as commit was delayed and orphan filter made changes meanwhile", 1, xar2.rollbackCount());
                assertEquals("XAResource1 should commit as no delay in processing was added and commit run before recovery", 1, xar1.commitCount());
                assertEquals("XAResource2 commit was called too as the call was delayed by latch", 1, xar2.commitCount());
            }
        );
    }

    private void testSubordinateTransactionOrphanFilter(
            Consumer<XARecoveryModule> orphanFilterPlacer, BiConsumer<TestXAResourceWrapper, TestXAResourceWrapper> xaResourceCheck) throws Exception {
        Uid uid = new Uid();
        Xid xid = XATxConverter.getXid(uid, true, XATxConverter.FORMAT_ID);
        SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().importTransaction(xid);

        final CountDownLatch latchWaitOnCommit = new CountDownLatch(1);
        final CountDownLatch latchWaitCommitToStart = new CountDownLatch(1);
        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            @Override
            public void commit(javax.transaction.xa.Xid id, boolean onePhase) throws XAException {
                try {
                    latchWaitCommitToStart.countDown();
                    latchWaitOnCommit.await();
                    super.commit(xid, onePhase);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Awaiting on latch '" + latchWaitOnCommit + "' failed", e);
                }
            }
        };

        assertTrue("Fail to enlist first test XAResource", subordinateTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", subordinateTransaction.enlistResource(xar2));

        Future<Boolean> synchronizatioPointFuture = Executors.newSingleThreadExecutor().submit(() -> {
            assertEquals("transaction should be prepared", TwoPhaseOutcome.PREPARE_OK, subordinateTransaction.doPrepare());
            assertTrue("commit should be processed even longer waiting", subordinateTransaction.doCommit());
            return true;
        });
        latchWaitCommitToStart.await(); // being sure that commit on xar2 was invoked

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        assertTrue("cannot cast recovery module " + recoveryManager.getModules().get(1).getClass() + " as " + XARecoveryModule.class.getName()
                + ", please check the test setup", recoveryManager.getModules().get(1) instanceof XARecoveryModule);
        XARecoveryModule xaRecoveryModule = (XARecoveryModule) recoveryManager.getModules().get(1);
        xaRecoveryModule.addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        orphanFilterPlacer.accept(xaRecoveryModule);

        recoveryManager.scan();

        latchWaitOnCommit.countDown(); // resume xar2 commit to finish the stopped in-flight transaction will be finished here
        synchronizatioPointFuture.get(); // to sync on finishing the prepare/commit as xar2 was stuck by latch

        xaResourceCheck.accept(xar1, xar2);
    }

}
