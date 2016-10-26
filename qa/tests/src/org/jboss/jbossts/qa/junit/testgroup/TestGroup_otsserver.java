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
public class TestGroup_otsserver extends TestGroupBase {
    protected Task server3 = null;
    protected Task server2 = null;
    protected Task server1 = null;
    protected Task server0 = null;

    @Before
    public void setUp() {
        super.setUp();
        server0 = createTask("server0", com.arjuna.ats.arjuna.recovery.RecoveryManager.class,
                Task.TaskType.EXPECT_READY, 1480);
        server0.start("-test");
        server1 = createTask("server1", com.arjuna.ats.jts.TransactionServer.class, Task.TaskType.EXPECT_READY, 1480);
        server1.start("-test");
        server2 = createTask("server2", org.jboss.jbossts.qa.Utils.RegisterOTSServer2.class, Task.TaskType.EXPECT_READY,
                1480);
        server2.start();
        server3 = createTask("server3", org.jboss.jbossts.qa.Utils.SetupOTSServer2.class, Task.TaskType.EXPECT_READY,
                1480);
        server3.start();
    }

    @After
    public void tearDown() {
        try {
            server0.terminate();
            server1.terminate();
            server2.terminate();
            server3.terminate();
            Task task0 = createTask("task0", org.jboss.jbossts.qa.Utils.RemoveServerIORStore.class,
                    Task.TaskType.EXPECT_PASS_FAIL, 1480);
            task0.perform("$(1)");
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void OTSServer_Test001() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client01.class);
    }

    @Test
    public void OTSServer_Test002() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client02.class);
    }

    @Test
    public void OTSServer_Test003() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client03.class);
    }

    @Test
    public void OTSServer_Test004() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client04.class);
    }

    @Test
    public void OTSServer_Test005() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client05.class);
    }

    @Test
    public void OTSServer_Test006() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client06.class);
    }

    @Test
    public void OTSServer_Test007() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client07.class, "800");
    }

    @Test
    public void OTSServer_Test008() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client08.class, "800");
    }

    @Test
    public void OTSServer_Test009() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client09.class, "800");
    }

    @Test
    public void OTSServer_Test010() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client10.class, "800");
    }

    @Test
    public void OTSServer_Test011() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client11.class, "800");
    }

    @Test
    public void OTSServer_Test012() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client12.class, "800");
    }

    @Test
    public void OTSServer_Test013() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client13.class, "4", "250");
    }

    @Test
    public void OTSServer_Test014() {
        setTestName("Test014");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client0.start("4", "250");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client1.start("4", "250");
        client0.waitFor();
        client1.waitFor();
    }

    @Test
    public void OTSServer_Test015() {
        setTestName("Test015");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client0.start("4", "100");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client1.start("4", "100");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client2.start("4", "100");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
    }

    @Test
    public void OTSServer_Test016() {
        setTestName("Test016");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client0.start("4", "100");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client1.start("4", "100");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client2.start("4", "100");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.OTSServerClients.Client13.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client3.start("4", "100");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        client3.waitFor();
    }

    @Test
    public void OTSServer_Test017() {
        startAndWaitForClient(org.jboss.jbossts.qa.OTSServerClients.Client14.class, "4", "200");
    }

    @Test
    public void OTSServer_Test018() {
        setTestName("Test018");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client0.start("4", "100");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client1.start("4", "100");
        client0.waitFor();
        client1.waitFor();
    }

    @Test
    public void OTSServer_Test019() {
        setTestName("Test019");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client0.start("4", "75");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client1.start("4", "75");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client2.start("4", "75");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
    }

    @Test
    public void OTSServer_Test020() {
        setTestName("Test020");
        Task client0 = createTask("client0", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client0.start("4", "75");
        Task client1 = createTask("client1", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client1.start("4", "75");
        Task client2 = createTask("client2", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client2.start("4", "75");
        Task client3 = createTask("client3", org.jboss.jbossts.qa.OTSServerClients.Client14.class,
                Task.TaskType.EXPECT_PASS_FAIL, 1480);
        client3.start("4", "75");
        client0.waitFor();
        client1.waitFor();
        client2.waitFor();
        client3.waitFor();
    }

}
