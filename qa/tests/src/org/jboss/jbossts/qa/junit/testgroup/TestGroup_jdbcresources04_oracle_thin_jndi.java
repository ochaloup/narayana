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
public class TestGroup_jdbcresources04_oracle_thin_jndi extends TestGroupBase
{
    public String getTestGroupName()
    {
        return "jdbcresources04_oracle_thin_jndi";
    }

    protected Task server0 = null;

    @Before public void setUp()
    {
        super.setUp();
        Task setup = createTask("setup", org.jboss.jbossts.qa.Utils.JNDIManager.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        setup.perform("DB_THIN_JNDI");
        Task setup1 = createTask("setup1", org.jboss.jbossts.qa.JDBCResources04Setups.Setup01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        setup1.perform("2", "DB_THIN_JNDI");
        server0 = createTask("server0", com.arjuna.ats.arjuna.recovery.RecoveryManager.class, Task.TaskType.EXPECT_READY, 480);
        server0.start("-test");
    }

    @After public void tearDown()
    {
        try {
            server0.terminate();
        Task task0 = createTask("task0", org.jboss.jbossts.qa.Utils.RemoveServerIORStore.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        task0.perform("$(1)");
        Task cleanup = createTask("cleanup", org.jboss.jbossts.qa.JDBCResources04Cleanups.Cleanup01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        cleanup.perform("DB_THIN_JNDI");
        } finally {
            super.tearDown();
        }
    }

    @Test public void JDBCResources04_Oracle_thin_jndi_Test01()
    {
        setTestName("Test01");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.JDBCResources04Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("DB_THIN_JNDI", "$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(1)");
        client0.waitFor();
        client1.waitFor();
        server1.terminate();
    }

    @Test public void JDBCResources04_Oracle_thin_jndi_Test02()
    {
        setTestName("Test02");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.JDBCResources04Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("DB_THIN_JNDI", "$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.JDBCResources04Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server2.start("DB_THIN_JNDI", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(2)");
        client0.waitFor();
        client1.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test public void JDBCResources04_Oracle_thin_jndi_Test03()
    {
        setTestName("Test03");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.JDBCResources04Servers.Server02.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("DB_THIN_JNDI", "$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(1)");
        client0.waitFor();
        client1.waitFor();
        server1.terminate();
    }

    @Test public void JDBCResources04_Oracle_thin_jndi_Test04()
    {
        setTestName("Test04");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.JDBCResources04Servers.Server02.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("DB_THIN_JNDI", "$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.JDBCResources04Servers.Server02.class, Task.TaskType.EXPECT_READY, 480);
        server2.start("DB_THIN_JNDI", "$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.JDBCResources04Clients.Client01.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client1.start("$(2)");
        client0.waitFor();
        client1.waitFor();
        server2.terminate();
        server1.terminate();
    }

}