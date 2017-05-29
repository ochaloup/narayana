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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TwoPhaseOutcome;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jta.recovery.jts.JTSNodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.jts.JTSTransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import com.arjuna.ats.internal.jta.utils.jts.XidUtils;
import com.arjuna.ats.internal.jts.ORBManager;
import com.arjuna.ats.internal.jts.orbspecific.recovery.RecoveryEnablement;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.ats.jts.common.jtsPropertyManager;
import com.arjuna.orbportability.OA;
import com.arjuna.orbportability.ORB;
import com.arjuna.orbportability.RootOA;
import com.arjuna.orbportability.common.opPropertyManager;
import com.hp.mwtests.ts.jta.recovery.TestXAResourceWrapper;

/**
 * Testing imported subordinate transaction to work in recovery process.
 */
public class ImportedTransactionRecoveryUnitTest {

    private ORB myORB = null;
    private RootOA myOA = null;
    int orphanSafetyIntervalOrigin;
    List<String> xaRecoveryNodesOrigin = null, xaResourceOrphanFiltersOrigin = null;
    RecoveryManager recoveryManager = null;

    @Before
    public void setUp () throws Exception {
        final Map<String, String> orbInitializationProperties = new HashMap<String, String>();
        orbInitializationProperties.put("com.arjuna.orbportability.orb.PreInit1",
                "com.arjuna.ats.internal.jts.recovery.RecoveryInit");
        opPropertyManager.getOrbPortabilityEnvironmentBean()
                .setOrbInitializationProperties(orbInitializationProperties);

        final Properties initORBProperties = new Properties();
        initORBProperties.setProperty("com.sun.CORBA.POA.ORBServerId", "1");
        initORBProperties.setProperty("com.sun.CORBA.POA.ORBPersistentServerPort", ""
                + jtsPropertyManager.getJTSEnvironmentBean().getRecoveryManagerPort());

        myORB = ORB.getInstance("test");
        myOA = OA.getRootOA(myORB);
        myORB.initORB(new String[] {}, initORBProperties);
        myOA.initOA();
        ORBManager.setORB(myORB);
        ORBManager.setPOA(myOA);

        final List<String> recoveryExtensions = new ArrayList<String>();
        recoveryExtensions.add(XARecoveryModule.class.getName());
        recoveryPropertyManager.getRecoveryEnvironmentBean().setRecoveryModuleClassNames(recoveryExtensions);

        final List<String> recoveryActivatorClassNames = new ArrayList<String>();
        recoveryActivatorClassNames.add(RecoveryEnablement.class.getName());
        recoveryPropertyManager.getRecoveryEnvironmentBean()
                .setRecoveryActivatorClassNames(recoveryActivatorClassNames);

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

        recoveryManager.terminate();
        myOA.destroy();
        myORB.shutdown();
    }

    /**
     * <p>
     * Testing that multiple {@link XAResource}s could be used under imported transaction.<br>
     * Previously there was no check of xid format which resulted to fact of merging usage of multiple XAResources
     * under one Xid even when the subordinate transaction was under management of Narayana (meaning created by Narayana).
     * That could lead to non-recoverable behavior for particular XAResource.
     * <p>
     * By adding check for format of xid Narayana generates new Xid for any XAResource used under transaction
     * created by Narayana. If transaction comes from EIS there is returned the same Xid (as expected).
     */
    @Test
    public void testMultipleXAResourceForImportedJcaTransaction() throws Exception {
        final Xid xid = XidUtils.getXid(new Uid(), true);
        SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().importTransaction(xid);

        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1")
        {
            boolean wasThrown = false;
            @Override
            public void commit(Xid xid, boolean onePhase) throws XAException {
                if(!wasThrown) {
                    wasThrown = true;
                    throw new XAException(XAException.XAER_RMFAIL);
                } else {
                    super.commit(xid, onePhase);
                }
            }
        };
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            boolean wasThrown = false;
            @Override
            public void commit(Xid xid, boolean onePhase) throws XAException {
                if(!wasThrown) {
                    wasThrown = true;
                    throw new XAException(XAException.XAER_RMFAIL);
                } else {
                    super.commit(xid, onePhase);
                }
            }
        };

        assertTrue("Fail to enlist first test XAResource", subordinateTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", subordinateTransaction.enlistResource(xar2));

        assertEquals("transaction should be prepared", TwoPhaseOutcome.PREPARE_OK, subordinateTransaction.doPrepare());
        assertFalse("first resource should fail on transaction commit, thus whole txn can't be committed",
            subordinateTransaction.doCommit());

        assertNotEquals("XAResources should be enlisted with different xids", xar1.getXid(), xar2.getXid());

        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));

        recoveryManager.scan();

        assertEquals("XAResource1 can't be rolled-back", 0, xar1.rollbackCount());
        assertEquals("XAResource2 can't be rolled-back", 0, xar2.rollbackCount());
        assertEquals("XAResource1 has to be committed", 1, xar1.commitCount());
        assertEquals("XAResource2 has to be committed", 1, xar2.commitCount());
    }

    /**
     * <p>
     * Test simulates the state where transaction manager does not know what happened after XAResource.prepare.
     * The situation can occurs when jvm is crashed during prepare phase and some resources were prepared and some weren't.
     * After jvm restart you can find the state that a resource was prepared at side of resource but this information
     * hadn't hit the transaction manager before jvm crashed.
     * <p>
     * After jvm is restarted the orphan filter should see that object log store does not contain information about
     * the transaction but there is some orphan on the resource side. Expecting XAResource rollback to be called.
     */
    @Test
    public void testJTSOrphanFilterNodeName() throws Exception {
        final Xid xid = XidUtils.getXid(new Uid(), true);
        SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().importTransaction(xid);
        
        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            Xid[] xids = new Xid[] {};
            @Override
            public int prepare(Xid xid) throws XAException {
                this.xids = new Xid[] {xid};
                return super.prepare(xid);
            }
            @Override
            public Xid[] recover(int flag) throws XAException {
                return xids;
            }
            @Override
            public void rollback(Xid xid) throws XAException {
                xids = new Xid[] {};
                super.rollback(xid);
            }
        };
        
        assertTrue("Fail to enlist first test XAResource", subordinateTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", subordinateTransaction.enlistResource(xar2));
        
        assertEquals("transaction should be prepared", TwoPhaseOutcome.PREPARE_OK, subordinateTransaction.doPrepare());
        assertTrue("simulating transaction was fully committed", subordinateTransaction.doCommit());

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceOrphanFilter(new JTSNodeNameXAResourceOrphanFilter());
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceOrphanFilter(new JTSTransactionLogXAResourceOrphanFilter());
        
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
     * This is a variant to {@link #testJTSOrphanFilterNodeName()} but the orphan filter should not be activated
     * for the different node name.
     */
    @Test
    public void testJTSOrphanFilterWithDifferntNodeName() throws Exception {
        final Xid xid = XidUtils.getXid(new Uid(), true);
        SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().importTransaction(xid);

        TestXAResourceWrapper xar1 = new TestXAResourceWrapper("narayana", "narayana", "java:/test1");
        TestXAResourceWrapper xar2 = new TestXAResourceWrapper("narayana", "narayana", "java:/test2")
        {
            Xid[] xids = new Xid[] {};
            @Override
            public int prepare(Xid xid) throws XAException {
                this.xids = new Xid[] {xid};
                return super.prepare(xid);
            }
            @Override
            public Xid[] recover(int flag) throws XAException {
                return xids;
            }
            @Override
            public void rollback(Xid xid) throws XAException {
                xids = new Xid[] {};
                super.rollback(xid);
            }
        };

        assertTrue("Fail to enlist first test XAResource", subordinateTransaction.enlistResource(xar1));
        assertTrue("Fail to enlist second XAResource", subordinateTransaction.enlistResource(xar2));

        assertEquals("transaction should be prepared", TwoPhaseOutcome.PREPARE_OK, subordinateTransaction.doPrepare());
        assertTrue("simulating transaction was fully committed", subordinateTransaction.doCommit());

        jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(
            arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier() + "franta"));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceRecoveryHelper(new TestXARecoveryHelper(xar1, xar2));
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceOrphanFilter(new JTSNodeNameXAResourceOrphanFilter());
        ((XARecoveryModule) recoveryManager.getModules().get(0))
            .addXAResourceOrphanFilter(new JTSTransactionLogXAResourceOrphanFilter());

        recoveryManager.scan();

        assertEquals("XAResource1 should not be rolled-back", 0, xar1.rollbackCount());
        assertEquals("XAResource2 should not be rolled-back as working with different node name", 1, xar2.rollbackCount());
        assertEquals("XAResource1 should be committed", 1, xar1.commitCount());
        assertEquals("XAResource2 should not be committed as there is no record in transaction log after 2pc finished",
            0, xar2.commitCount());
    }
}
