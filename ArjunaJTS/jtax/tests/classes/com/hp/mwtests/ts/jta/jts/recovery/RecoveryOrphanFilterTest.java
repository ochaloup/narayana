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

package com.hp.mwtests.ts.jta.jts.recovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TwoPhaseOutcome;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jta.recovery.jts.JCAServerTransactionRecoveryModule;
import com.arjuna.ats.internal.jta.recovery.jts.JTSNodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.jts.SubordinateJTSXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import com.arjuna.ats.internal.jta.utils.jts.XidUtils;
import com.arjuna.ats.internal.jts.ORBManager;
import com.arjuna.ats.internal.jts.orbspecific.recovery.RecoveryEnablement;
import com.arjuna.ats.jta.TransactionManager;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.ats.jts.common.jtsPropertyManager;
import com.arjuna.orbportability.OA;
import com.arjuna.orbportability.ORB;
import com.arjuna.orbportability.RootOA;
import com.arjuna.orbportability.common.opPropertyManager;
import com.hp.mwtests.ts.jta.recovery.TestXAResourceWrapper;

/**
 * Testing JTS recovery and work of the orphan filters.
 */
public class RecoveryOrphanFilterTest {
    private static final Logger log = Logger.getLogger(RecoveryOrphanFilterTest.class);

    private static ORB myORB = null;
    private static RootOA myOA = null;
    private static RecoveryManager recoveryManager = null;

    private int orphanSafetyIntervalOrigin = jtaPropertyManager.getJTAEnvironmentBean().getOrphanSafetyInterval();
    private List<String> xaRecoveryNodesOrigin = jtaPropertyManager.getJTAEnvironmentBean().getXaRecoveryNodes();
    private List<String> xaResourceOrphanFiltersOrigin = jtaPropertyManager.getJTAEnvironmentBean().getXaResourceOrphanFilterClassNames();
    private List<String> recoveryActivatorsOrigin = recoveryPropertyManager.getRecoveryEnvironmentBean().getRecoveryActivatorClassNames();
    private String nodeIdentifierOrigin =  arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();

    @BeforeClass
    public static void classSetup() throws Exception {
        final Map<String, String> orbInitializationProperties = new HashMap<String, String>();
        orbInitializationProperties.put("com.arjuna.orbportability.orb.PreInit1",
            "com.arjuna.ats.internal.jts.recovery.RecoveryInit");
        opPropertyManager.getOrbPortabilityEnvironmentBean()
            .setOrbInitializationProperties(orbInitializationProperties);

        // needed for the surefire-idlj
        final Properties initORBProperties = new Properties();
        initORBProperties.setProperty("com.sun.CORBA.POA.ORBServerId", "1");
        initORBProperties.setProperty("com.sun.CORBA.POA.ORBPersistentServerPort", ""
            + jtsPropertyManager.getJTSEnvironmentBean().getRecoveryManagerPort());

        myORB = ORB.getInstance("test" + (new Random().nextInt(999) + 1));
        myOA = OA.getRootOA(myORB);
        myORB.initORB(new String[] {}, initORBProperties);
        myOA.initOA();
        ORBManager.setORB(myORB);
        ORBManager.setPOA(myOA);
    }

    @AfterClass
    public static void tearDownClass() {
        myOA.destroy();
        myORB.shutdown();
    }

    @Before
    public void setUp () throws Exception {
        jtaPropertyManager.getJTAEnvironmentBean().setOrphanSafetyInterval(0);

        final List<String> recoveryExtensions = new ArrayList<String>();
        recoveryExtensions.add(JCAServerTransactionRecoveryModule.class.getName());
        recoveryExtensions.add(XARecoveryModule.class.getName());
        recoveryPropertyManager.getRecoveryEnvironmentBean().setRecoveryModuleClassNames(recoveryExtensions);

        final List<String> recoveryActivatorClassNames = new ArrayList<String>();
        recoveryActivatorClassNames.add(RecoveryEnablement.class.getName());
        recoveryPropertyManager.getRecoveryEnvironmentBean()
            .setRecoveryActivatorClassNames(recoveryActivatorClassNames);

        recoveryManager = RecoveryManager.manager(RecoveryManager.DIRECT_MANAGEMENT);
        recoveryManager.initialize();
    }

    @After
    public void tearDown () throws Exception {
        jtaPropertyManager.getJTAEnvironmentBean().setOrphanSafetyInterval(orphanSafetyIntervalOrigin);
        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(xaRecoveryNodesOrigin);
        jtaPropertyManager.getJTAEnvironmentBean().setXaResourceOrphanFilterClassNames(xaResourceOrphanFiltersOrigin);
        recoveryPropertyManager.getRecoveryEnvironmentBean().setRecoveryActivatorClassNames(recoveryActivatorsOrigin);

        arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(nodeIdentifierOrigin);
        TxControl.setXANodeName(nodeIdentifierOrigin);

        recoveryManager.terminate();
    }

    /**
     * <p>
     * Test simulates the state where there is left an in-doubt transaction in remote resource but transaction
     * manager has no notion of it. That's time for orphan filter to finish such in doubt transaction.
     * <p>
     * The simulation is done in way that the commit call of XAResource does not remove record from its
     * internal storage and the follow-up recover call will return unfinished Xid (even the transaction
     * itself finished with success).
     * <p>
     * Here the {@link JTSNodeNameXAResourceOrphanFilter} votes to rollback if the node name saved in the Xid
     * at the side of the remote resource fits to one the jboss eap is responsible for.
     * <p>
     * Here no log record in Narayana transaction log exists and node name maches. Result is rollback.
     */
    @Test
    public void testTopLevelOrphanFilterNodeName() throws Exception {
        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2") {
            @Override
            public void commit(javax.transaction.xa.Xid id, boolean onePhase) throws XAException {
                // by calling this the Xid is not removed during the global transaction commit
                // and a Xid is left unfinished for XAResource#recover returning it
            }
        };

        TransactionManager.transactionManager().begin();
        Transaction topLevelTransaction = TransactionManager.transactionManager().getTransaction();

        assertTrue("Fail to enlist first test XAResource", topLevelTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", topLevelTransaction.enlistResource(xar2));

        TransactionManager.transactionManager().commit();

        assertTrue("cannot cast recovery module " + recoveryManager.getModules().get(1).getClass() + " as " + XARecoveryModule.class.getName()
            + ", please check the test setup", recoveryManager.getModules().get(1) instanceof XARecoveryModule);

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        // orphan filters
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceOrphanFilter(new JTSNodeNameXAResourceOrphanFilter());

        recoveryManager.scan();

        assertEquals("XAResource1 should not be rolled-back as it should be committed by two-phase commit",
            0, xar1.rollbackCount());
        assertEquals("XAResource2 should be rolled-back", 1, xar2.rollbackCount());
        assertEquals("XAResource1 should be committed", 1, xar1.commitCount());
        assertEquals("XAResource2 can't be committed", 0, xar2.commitCount());
    }

    /**
     * <p>
     * Orphan filter should not take into account transaction on resource created by different node id
     * than the current transaction manager owns.
     * <p>
     * This is a variant to {@link #testTopLevelOrphanFilterNodeName()} but the orphan filter should not vote to rollback
     * when the resource xid was saved with the different node name.
     */
    @Test
    public void testTopLevelOrphanFilterWithDifferentNodeName() throws Exception {
        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2") {
            @Override
            public void commit(javax.transaction.xa.Xid id, boolean onePhase) throws XAException {
                // the Xid is not removed from TestXAResourceWrappper store and XAResource#recover returns it
            }
        };

        TransactionManager.transactionManager().begin();
        Transaction topLevelTransaction = TransactionManager.transactionManager().getTransaction();

        assertTrue("Fail to enlist first test XAResource", topLevelTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", topLevelTransaction.enlistResource(xar2));

        TransactionManager.transactionManager().commit();

        assertTrue("cannot cast recovery module " + recoveryManager.getModules().get(1).getClass() + " as " + XARecoveryModule.class.getName()
            + ", please check the test setup", recoveryManager.getModules().get(1) instanceof XARecoveryModule);

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier() + "-different"));
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        // orphan filter definition
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceOrphanFilter(new JTSNodeNameXAResourceOrphanFilter());

        recoveryManager.scan();

        assertEquals("XAResource1 should be committed", 0, xar1.rollbackCount());
        assertEquals("XAResource1 should be committed", 1, xar1.commitCount());
        assertEquals("XAResource2 should not be rolled-back as working with different node name", 0, xar2.rollbackCount());
        assertEquals("XAResource2 can't be committed as test deliberately ignores the original commit call", 0, xar2.commitCount());
    }

    /**
     * <p>
     * This test checks that orphan filter is <b>not</b> launched when there are existing records in Narayana object store.
     * <p>
     * The JTS implementation differs from the JTA one in way of saving information about successful finishing
     * of participant prepare call. In difference the JTA saves only information that whole prepare phase
     * finished successfully but it does not have more granular information about what participant prepared
     * successfully during recovery. This brings different way how orphan filter is activated.
     */
    @Test
    public void testNotActivatingOrphanFilterWhenObjectStoreRecordExists() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        final TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            @Override
            public void commit(javax.transaction.xa.Xid xid, boolean onePhase) throws XAException {
                try {
                    log.debugf("TestXAResource waits to commit xid '%s'", xid);
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

        assertTrue("cannot cast recovery module " + recoveryManager.getModules().get(1).getClass() + " as " + XARecoveryModule.class.getName()
            + ", please check the test setup", recoveryManager.getModules().get(1) instanceof XARecoveryModule);
        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceOrphanFilter(new JTSNodeNameXAResourceOrphanFilter());

        recoveryManager.scan();

        latch.countDown();
        prepareCommitFuture.get();

        assertEquals("XAResource1 should not rollback", 0, xar1.rollbackCount());
        assertEquals("XAResource2 should not rollback", 0, xar2.rollbackCount());
        assertEquals("XAResource1 should commit", 1, xar1.commitCount());
        assertEquals("XAResource2 should commit", 1, xar1.commitCount());
    }

    /**
     * Not activating orphan filter handling for <b>subordinate transaction</b> when the log record exists
     * in the Narayana log store. See {@link #testNotActivatingOrphanFilterWhenObjectStoreRecordExists()}.
     */
    @Test
    public void testSubordinateNotActivatingOrphanFilterWhenObjectStoreRecordExists() throws Exception {
        final Xid xid = XidUtils.getXid(new Uid(), true);
        SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().importTransaction(xid);

        final CountDownLatch latch = new CountDownLatch(1);
        final TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        final TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            @Override
            public void commit(javax.transaction.xa.Xid xid, boolean onePhase) throws XAException {
                try {
                    log.debugf("TestXAResource waits to commit xid '%s'", xid);
                    latch.await();
                    super.commit(xid, onePhase);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Awaiting on latch '" + latch + "' failed", e);
                }
            }
        };

        Future<Boolean> prepareCommitFuture = Executors.newSingleThreadExecutor().submit(() -> {
            assertTrue("Fail to enlist first test XAResource", subordinateTransaction.enlistResource(xar1));
            assertTrue("Fail to enlist second XAResource", subordinateTransaction.enlistResource(xar2));

            assertEquals("transaction should be prepared", TwoPhaseOutcome.PREPARE_OK, subordinateTransaction.doPrepare());
            assertTrue("simulating transaction was fully committed", subordinateTransaction.doCommit());
            return true;
        });

        assertTrue("cannot cast recovery module " + recoveryManager.getModules().get(1).getClass() + " as " + XARecoveryModule.class.getName()
            + ", please check the test setup", recoveryManager.getModules().get(1) instanceof XARecoveryModule);

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceOrphanFilter(new JTSNodeNameXAResourceOrphanFilter());
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceOrphanFilter(new SubordinateJTSXAResourceOrphanFilter());

        recoveryManager.scan();

        latch.countDown();
        prepareCommitFuture.get();

        assertEquals("XAResource1 should not rollback", 0, xar1.rollbackCount());
        assertEquals("XAResource2 should not rollback", 0, xar2.rollbackCount());
        assertEquals("XAResource1 should commit", 1, xar1.commitCount());
        assertEquals("XAResource2 should commit", 1, xar1.commitCount());
    }

    /**
     * As the test {@link #testTopLevelOrphanFilterNodeName()} verifies the node name orphan filter
     * ask for rollback. Here the node name does not match and the {@link SubordinateJTSXAResourceOrphanFilter}
     * will cause the rollback to execute as there is no log entry and subordinate node name matches.
     */
    @Test
    public void testSubordinateOrphanFilterNodeName() throws Exception {
        // node name is 1
        final Xid xid = XidUtils.getXid(new Uid(), true);

        // subordinate node name will be 2, simulating we are at node 2
        arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier("2");
        TxControl.setXANodeName("2"); // cached node identifier for importing transactions
        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList("2"));
        SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().importTransaction(xid);

        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2") {
            @Override
            public void commit(javax.transaction.xa.Xid id, boolean onePhase) throws XAException {
                // the Xid is not removed from TestXAResourceWrappper store and XAResource#recover returns it
            }
        };

        assertTrue("Fail to enlist first test XAResource", subordinateTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", subordinateTransaction.enlistResource(xar2));

        assertEquals("transaction should be prepared", TwoPhaseOutcome.PREPARE_OK, subordinateTransaction.doPrepare());
        assertTrue("simulating transaction was fully committed", subordinateTransaction.doCommit());

        assertTrue("cannot cast recovery module " + recoveryManager.getModules().get(1).getClass() + " as " + XARecoveryModule.class.getName()
            + ", please check the test setup", recoveryManager.getModules().get(1) instanceof XARecoveryModule);

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        // orphan filters
        ((XARecoveryModule) recoveryManager.getModules().get(1))
            .addXAResourceOrphanFilter(new JTSNodeNameXAResourceOrphanFilter());
        ((XARecoveryModule) recoveryManager.getModules().get(1))
           .addXAResourceOrphanFilter(new SubordinateJTSXAResourceOrphanFilter());

        recoveryManager.scan();

        assertEquals("XAResource1 be committed by two-phase", 1, xar1.commitCount());
        assertEquals("XAResource1 should not be rolled-back as it should be committed by two-phase process",0, xar1.rollbackCount());
        assertEquals("XAResource2 should not be be committed as the orphan filter is not expected to do so", 0, xar2.commitCount());
        assertEquals("XAResource2 should be rolled-back as no record is in object store and the node id matches the subordinate xid name",
            1, xar2.rollbackCount());
    }

}
