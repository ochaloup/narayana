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
public class TestGroup_crashrecovery11_pgsql_jndi extends TestGroupBase {
    public String getTestGroupName() {
        return "crashrecovery11_pgsql_jndi";
    }

    protected Task server1 = null;
    protected Task server0 = null;

    @Before
    public void setUp() {
        super.setUp();
        Task setup = createTask("setup", org.jboss.jbossts.qa.Utils.JNDIManager.class, Task.TaskType.EXPECT_PASS_FAIL,
                480);
        setup.perform("DB_PGSQL_JNDI");
        Task setup2 = createTask("setup2", org.jboss.jbossts.qa.CrashRecovery11Setups.Setup01.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        setup2.perform("DB_PGSQL_JNDI");
        server0 = createTask("server0", com.arjuna.ats.arjuna.recovery.RecoveryManager.class,
                Task.TaskType.EXPECT_READY, 480);
        server0.start("-test");
        server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery11Servers.JDBCServer01.class,
                Task.TaskType.EXPECT_READY, 480);
        server1.start("DB_PGSQL_JNDI", "row1", "$(1)");
    }

    @After
    public void tearDown() {
        try {
            server0.terminate();
            server1.terminate();
            Task task0 = createTask("task0", org.jboss.jbossts.qa.Utils.RemoveObjectUidStore.class,
                    Task.TaskType.EXPECT_PASS_FAIL, 480);
            task0.perform();
            Task task1 = createTask("task1", org.jboss.jbossts.qa.Utils.RemoveServerIORStore.class,
                    Task.TaskType.EXPECT_PASS_FAIL, 480);
            task1.perform("$(1)", "$(2)");
            Task task2 = createTask("task2", org.jboss.jbossts.qa.CrashRecovery11Cleanups.Cleanup01.class,
                    Task.TaskType.EXPECT_PASS_FAIL, 480);
            task2.perform("DB_PGSQL_JNDI");
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void CrashRecovery11_pgsql_jndi_Test01() {
        setTestName("Test01");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery11Clients.Client01b.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery11Servers.JDBCServer02.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("DB_PGSQL_JNDI", "row1", "$(2)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery11Clients.Client01a.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(2)");
        client1.waitFor();
        server2.terminate();
    }

    @Test
    public void CrashRecovery11_pgsql_jndi_Test02() {
        setTestName("Test02");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery11Clients.Client02b.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery11Servers.JDBCServer02.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("DB_PGSQL_JNDI", "row1", "$(2)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery11Clients.Client02a.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(2)");
        client1.waitFor();
        server2.terminate();
    }

    @Test
    public void CrashRecovery11_pgsql_jndi_Test03() {
        setTestName("Test03");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery11Clients.Client03b.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery11Servers.JDBCServer02.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("DB_PGSQL_JNDI", "row1", "$(2)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery11Clients.Client03a.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(2)");
        client1.waitFor();
        server2.terminate();
    }

    @Test
    public void CrashRecovery11_pgsql_jndi_Test04() {
        setTestName("Test04");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery11Clients.Client04b.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery11Servers.JDBCServer02.class,
                Task.TaskType.EXPECT_READY, 480);
        server2.start("DB_PGSQL_JNDI", "row1", "$(2)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery11Clients.Client04a.class,
                Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(2)");
        client1.waitFor();
        server2.terminate();
    }

}
