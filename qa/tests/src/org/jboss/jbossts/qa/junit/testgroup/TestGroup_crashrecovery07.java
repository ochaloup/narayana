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
public class TestGroup_crashrecovery07 extends TestGroupBase
{
    public String getTestGroupName()
    {
        return "crashrecovery07";
    }

    protected Task server0 = null;

    @Before public void setUp()
    {
        super.setUp();
        server0 = createTask("server0", com.arjuna.ats.arjuna.recovery.RecoveryManager.class, Task.TaskType.EXPECT_READY, 960);
        server0.start("-test");
    }

    @After public void tearDown()
    {
        try {
            server0.terminate();
        Task task0 = createTask("task0", org.jboss.jbossts.qa.Utils.RemoveServerIORStore.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        task0.perform("$(1)");
        } finally {
            super.tearDown();
        }
    }

    @Test public void CrashRecovery07_Test01()
    {
        setTestName("Test01");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("1", "$(1)");
        client0.waitFor();
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("1", "1", "$(1)");
        client1.waitFor();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test02()
    {
        setTestName("Test02");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("1", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("1", "$(2)");
        client0.waitFor();
        client1.waitFor();
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("2", "1", "$(1)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("2", "1", "$(2)");
        client2.waitFor();
        client3.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test03()
    {
        setTestName("Test03");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("1", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("1", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("1", "$(3)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("3", "1", "$(1)");
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("3", "1", "$(2)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("3", "1", "$(3)");
        client3.waitFor();
        client4.waitFor();
        client5.waitFor();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test04()
    {
        setTestName("Test04");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task server4 = createTask("server4", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server4.start("$(4)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("1", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("1", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("1", "$(3)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("1", "$(4)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        client3.waitFor();
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("4", "1", "$(1)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("4", "1", "$(2)");
        Task client6 = createTask("client6", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client6.start("4", "1", "$(3)");
        Task client7 = createTask("client7", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client7.start("4", "1", "$(4)");
        client4.waitFor();
        client5.waitFor();
        client6.waitFor();
        client7.waitFor();
        server4.terminate();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test05()
    {
        setTestName("Test05");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("2", "$(1)");
        client0.waitFor();
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("1", "2", "$(1)");
        client1.waitFor();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test06()
    {
        setTestName("Test06");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("2", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("2", "$(2)");
        client0.waitFor();
        client1.waitFor();
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("2", "2", "$(1)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("2", "2", "$(2)");
        client2.waitFor();
        client3.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test07()
    {
        setTestName("Test07");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("2", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("2", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("2", "$(3)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("3", "2", "$(1)");
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("3", "2", "$(2)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("3", "2", "$(3)");
        client3.waitFor();
        client4.waitFor();
        client5.waitFor();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test08()
    {
        setTestName("Test08");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task server4 = createTask("server4", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server4.start("$(4)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("2", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("2", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("2", "$(3)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("2", "$(4)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        client3.waitFor();
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("4", "2", "$(1)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("4", "2", "$(2)");
        Task client6 = createTask("client6", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client6.start("4", "2", "$(3)");
        Task client7 = createTask("client7", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client7.start("4", "2", "$(4)");
        client4.waitFor();
        client5.waitFor();
        client6.waitFor();
        client7.waitFor();
        server4.terminate();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test09()
    {
        setTestName("Test09");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("3", "$(1)");
        client0.waitFor();
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("1", "3", "$(1)");
        client1.waitFor();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test10()
    {
        setTestName("Test10");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("3", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("3", "$(2)");
        client0.waitFor();
        client1.waitFor();
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("2", "3", "$(1)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("2", "3", "$(2)");
        client2.waitFor();
        client3.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test11()
    {
        setTestName("Test11");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("3", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("3", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("3", "$(3)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("3", "3", "$(1)");
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("3", "3", "$(2)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("3", "3", "$(3)");
        client3.waitFor();
        client4.waitFor();
        client5.waitFor();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test12()
    {
        setTestName("Test12");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task server4 = createTask("server4", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server4.start("$(4)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("3", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("3", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("3", "$(3)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("3", "$(4)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        client3.waitFor();
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("4", "3", "$(1)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("4", "3", "$(2)");
        Task client6 = createTask("client6", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client6.start("4", "3", "$(3)");
        Task client7 = createTask("client7", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client7.start("4", "3", "$(4)");
        client4.waitFor();
        client5.waitFor();
        client6.waitFor();
        client7.waitFor();
        server4.terminate();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test13()
    {
        setTestName("Test13");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("4", "$(1)");
        client0.waitFor();
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("1", "4", "$(1)");
        client1.waitFor();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test14()
    {
        setTestName("Test14");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("4", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("4", "$(2)");
        client0.waitFor();
        client1.waitFor();
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("2", "4", "$(1)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("2", "4", "$(2)");
        client2.waitFor();
        client3.waitFor();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test15()
    {
        setTestName("Test15");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("4", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("4", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("4", "$(3)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("3", "4", "$(1)");
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("3", "4", "$(2)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("3", "4", "$(3)");
        client3.waitFor();
        client4.waitFor();
        client5.waitFor();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

    @Test public void CrashRecovery07_Test16()
    {
        setTestName("Test16");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server1.start("$(1)");
        Task server2 = createTask("server2", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server2.start("$(2)");
        Task server3 = createTask("server3", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server3.start("$(3)");
        Task server4 = createTask("server4", org.jboss.jbossts.qa.CrashRecovery07Servers.Server01.class, Task.TaskType.EXPECT_READY, 960);
        server4.start("$(4)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client0.start("4", "$(1)");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client1.start("4", "$(2)");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client2.start("4", "$(3)");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01b.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client3.start("4", "$(4)");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        client3.waitFor();
        Task client4 = createTask("client4", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client4.start("4", "4", "$(1)");
        Task client5 = createTask("client5", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client5.start("4", "4", "$(2)");
        Task client6 = createTask("client6", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client6.start("4", "4", "$(3)");
        Task client7 = createTask("client7", org.jboss.jbossts.qa.CrashRecovery07Clients.Client01a.class, Task.TaskType.EXPECT_PASS_FAIL, 960);
        client7.start("4", "4", "$(4)");
        client4.waitFor();
        client5.waitFor();
        client6.waitFor();
        client7.waitFor();
        server4.terminate();
        server3.terminate();
        server2.terminate();
        server1.terminate();
    }

}
