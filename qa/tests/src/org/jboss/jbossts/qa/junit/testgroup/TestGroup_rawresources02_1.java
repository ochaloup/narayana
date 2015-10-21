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
public class TestGroup_rawresources02_1 extends TestGroupBase
{
    public String getTestGroupName()
    {
        return "rawresources02_1";
    }

    protected Task server0 = null;

    @Before public void setUp()
    {
        super.setUp();
        server0 = createTask("server0", com.arjuna.ats.arjuna.recovery.RecoveryManager.class, Task.TaskType.EXPECT_READY, 480);
        server0.start("-test");
    }

    @After public void tearDown()
    {
        try {
            server0.terminate();
        Task task0 = createTask("task0", org.jboss.jbossts.qa.Utils.RemoveServerIORStore.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        task0.perform("$(1)");
        } finally {
            super.tearDown();
        }
    }

    @Test public void RawResources02_1_Test001()
    {
        setTestName("Test001");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.RawResources02Servers.Server04.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.RawResources02Clients1.Client001.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test public void RawResources02_1_Test002()
    {
        setTestName("Test002");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.RawResources02Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.RawResources02Clients1.Client002.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test public void RawResources02_1_Test003()
    {
        setTestName("Test003");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.RawResources02Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.RawResources02Clients1.Client003.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test public void RawResources02_1_Test004()
    {
        setTestName("Test004");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.RawResources02Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.RawResources02Clients1.Client004.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test public void RawResources02_1_Test005()
    {
        setTestName("Test005");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.RawResources02Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.RawResources02Clients1.Client005.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

    @Test public void RawResources02_1_Test006()
    {
        setTestName("Test006");
        Task server1 = createTask("server1", org.jboss.jbossts.qa.RawResources02Servers.Server01.class, Task.TaskType.EXPECT_READY, 480);
        server1.start("$(1)");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.RawResources02Clients1.Client006.class, Task.TaskType.EXPECT_PASS_FAIL, 480);
        client0.start("$(1)");
        client0.waitFor();
        server1.terminate();
    }

}