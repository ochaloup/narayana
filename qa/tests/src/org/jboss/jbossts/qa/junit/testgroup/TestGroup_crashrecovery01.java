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
public class TestGroup_crashrecovery01 extends TestGroupBase {
    public String getTestGroupName() {
        return "crashrecovery01";
    }

    protected Task server0 = null;

    @Before
    public void setUp() {
        super.setUp();
        server0 = createTask("server0", com.arjuna.ats.arjuna.recovery.RecoveryManager.class,
                Task.TaskType.EXPECT_READY, 480);
        server0.start("-test");
    }

    @After
    public void tearDown() {
        try {
            server0.terminate();
            Task task0 = createTask("task0", org.jboss.jbossts.qa.Utils.RemoveServerIORStore.class,
                    Task.TaskType.EXPECT_PASS_FAIL, 480);
            task0.perform("$(1)");
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void CrashRecovery01_Test01() {
        setTestName("Test01");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server01.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test02() {
        setTestName("Test02");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client02.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test03() {
        setTestName("Test03");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client03.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test04() {
        setTestName("Test04");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client04.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test05() {
        setTestName("Test05");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client05.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test06() {
        setTestName("Test06");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client06.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test07() {
        setTestName("Test07");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server01.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client07.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test08() {
        setTestName("Test08");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client08.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test09() {
        setTestName("Test09");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client09.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test10() {
        setTestName("Test10");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client10.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test11() {
        setTestName("Test11");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client11.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test12() {
        setTestName("Test12");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client12.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test13() {
        setTestName("Test13");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server01.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery01Servers.Server01.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test14() {
        setTestName("Test14");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test15() {
        setTestName("Test15");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery01Servers.Server02.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client15.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test16() {
        setTestName("Test16");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client16.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test17() {
        setTestName("Test17");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client17.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test18() {
        setTestName("Test18");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery01Servers.Server03.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client18.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test19() {
        setTestName("Test19");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server04.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test20() {
        setTestName("Test20");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server05.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test21() {
        setTestName("Test21");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server05.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client15.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test22() {
        setTestName("Test22");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server06.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client16.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test23() {
        setTestName("Test23");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server06.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client17.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server1.terminate();
    }

    @Test
    public void CrashRecovery01_Test24() {
        setTestName("Test24");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery01Servers.Server06.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery01Clients.Client18.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)", "$(2)");
        client0.waitFor();
        server1.terminate();
    }

}