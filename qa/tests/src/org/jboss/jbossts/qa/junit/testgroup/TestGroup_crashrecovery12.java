/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2009,
 * @author JBoss Inc.
 */
package org.jboss.jbossts.qa.junit.testgroup;

import org.jboss.jbossts.qa.junit.*;
import org.junit.*;

// Automatically generated by XML2JUnit
public class TestGroup_crashrecovery12 extends TestGroupBase {
    public String getTestGroupName() {
        return "crashrecovery12";
    }

    @Before
    public void setUp() {
        /*
         * Always run the hornetq store in process since that store is loaded
         * only once on startup (ie subsequent additions to the store by
         * external processes will never be loaded).
         */
        isRecoveryManagerNeeded = !isUsingExecutionWrapper();

        Task setup0 = createTask("setup0", org.jboss.jbossts.qa.CrashRecovery12Setups.Setup01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        setup0.perform("CR12_01.log");
        super.setUp();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void CrashRecovery12_Test01() {
        setTestName("Test01");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery12Clients.Client01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        client0.start("nocrash", "CR12_01.log");
        client0.waitFor();
    }

    @Test
    public void CrashRecovery12_Test02() {
        setTestName("Test02");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery12Clients.Client01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        client0.start("prepare", "CR12_02.log");
        Task outcome0 = createTask("outcome0", org.jboss.jbossts.qa.CrashRecovery12Outcomes.Outcome01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        outcome0.start("CR12_02.log", "no");
        outcome0.waitFor();
        client0.waitFor();
    }

    @Test
    public void CrashRecovery12_Test03() {
        setTestName("Test03");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery12Clients.Client01.class,
                Task.TaskType.EXPECT_READY_PASS_FAIL, 240, "store");
        client0.start("commit", "CR12_03.log");
        suspend(500); // Workaround for JBTM-2648
        Task outcome0 = createTask("outcome0", org.jboss.jbossts.qa.CrashRecovery12Outcomes.Outcome01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240, "store");
        outcome0.start("CR12_03.log", "yes");
        outcome0.waitFor();
        client0.waitFor();
    }

    @Test
    public void CrashRecovery12_Test04() {
        setTestName("Test04");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery12Clients.Client01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        client0.start("rollback", "CR12_04.log");
        Task outcome0 = createTask("outcome0", org.jboss.jbossts.qa.CrashRecovery12Outcomes.Outcome01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        outcome0.start("CR12_04.log", "no");
        outcome0.waitFor();
        client0.waitFor();
    }

    @Test
    public void CrashRecovery12_Test05() {
        setTestName("Test05");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery12Clients.Client01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        client0.start("prepare", "CR12_05.log");
        client0.waitFor();
        Task outcome0 = createTask("outcome0", org.jboss.jbossts.qa.CrashRecovery12Outcomes.Outcome01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        outcome0.start("CR12_05.log", "no");
        outcome0.waitFor();
    }

    @Test
    public void CrashRecovery12_Test06() {
        setTestName("Test06");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery12Clients.Client01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240, "store");
        client0.start("commit", "CR12_06.log");
        client0.waitFor();
        Task outcome0 = createTask("outcome0", org.jboss.jbossts.qa.CrashRecovery12Outcomes.Outcome01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240, "store");
        outcome0.start("CR12_06.log", "yes");
        outcome0.waitFor();
    }

    @Test
    public void CrashRecovery12_Test07() {
        setTestName("Test07");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery12Clients.Client01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        client0.start("rollback", "CR12_07.log");
        client0.waitFor();
        Task outcome0 = createTask("outcome0", org.jboss.jbossts.qa.CrashRecovery12Outcomes.Outcome01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 240);
        outcome0.start("CR12_07.log", "no");
        outcome0.waitFor();
    }

    private void suspend(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
