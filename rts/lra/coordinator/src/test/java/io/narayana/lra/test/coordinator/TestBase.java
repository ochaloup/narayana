/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package io.narayana.lra.test.coordinator;

import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import io.narayana.lra.Current;
import io.narayana.lra.LRAData;
import io.narayana.lra.client.NarayanaLRAClient;
import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry;
import io.narayana.lra.coordinator.api.Coordinator;
import io.narayana.lra.coordinator.domain.model.Transaction;
import io.narayana.lra.coordinator.domain.service.LRAService;
import io.narayana.lra.coordinator.internal.LRARecoveryModule;
import io.narayana.lra.event.LRAAction;
import io.narayana.lra.filter.ServerLRAFilter;
import io.narayana.lra.logging.LRALogger;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.arquillian.container.test.api.Config;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

import static io.narayana.lra.LRAConstants.COORDINATOR_PATH_NAME;
import static io.narayana.lra.LRAConstants.RECOVERY_COORDINATOR_PATH_NAME;

public abstract class TestBase {
    @Rule
    public TestName testName = new TestName();

    protected static final String MANAGED_COORDINATOR_CONTAINER = "managed-lra-coordinator";
    protected static final String MANUAL_COORDINATOR_CONTAINER = "manual-lra-coordinator";
    protected static final String COORDINATOR_DEPLOYMENT = "lra-coordinator";

    private static String coordinatorUrl;
    private static String recoveryUrl;
    private static String jvmArgs;
    private static Path storeDir;
    private static String deploymentURL;

    protected NarayanaLRAClient lraClient;

    @ArquillianResource
    private ContainerController containerController;

    @ArquillianResource
    private Deployer deployer;

    protected static final Package[] coordinatorPackages = {
            RecoveryModule.class.getPackage(),
            Coordinator.class.getPackage(),
            LRAData.class.getPackage(),
            LRAStatus.class.getPackage(),
            LRALogger.class.getPackage(),
            NarayanaLRAClient.class.getPackage(),
            Current.class.getPackage(),
            LRAService.class.getPackage(),
            LRARecoveryModule.class.getPackage(),
            Transaction.class.getPackage(),
            LRAAction.class.getPackage()
    };
    protected static final Package[] participantPackages = {
            LRA.class.getPackage(),
            ServerLRAFilter.class.getPackage(),
            LRAParticipantRegistry.class.getPackage()
    };

    @BeforeClass
    public static void beforeClass() {
        String host = System.getProperty(NarayanaLRAClient.LRA_COORDINATOR_HOST_KEY, "localhost");
        String port = System.getProperty(NarayanaLRAClient.LRA_COORDINATOR_PORT_KEY, "8080");
        String path = System.getProperty(NarayanaLRAClient.LRA_COORDINATOR_PATH_KEY, COORDINATOR_PATH_NAME);

        coordinatorUrl = String.format("http://%s:%s/%s", host, port, path);
        recoveryUrl = String.format("%s/%s/",
                coordinatorUrl, RECOVERY_COORDINATOR_PATH_NAME);
        storeDir = Paths.get(String.format("%s/standalone/data/tx-object-store", System.getProperty("env.JBOSS_HOME", "null")));
        jvmArgs = System.getProperty("server.jvm.args");

        deploymentURL = String.format("http://%s:%s/%s", host, port, COORDINATOR_DEPLOYMENT);
    }

    @Before
    public void before() throws URISyntaxException, MalformedURLException {
        LRALogger.logger.debugf("Starting test %s", testName);

        lraClient = new NarayanaLRAClient(new URI(coordinatorUrl));
    }

    @After
    public void after() {
        LRALogger.logger.debugf("Finished test %s", testName);
        lraClient.close();
    }

    protected void startContainer(String bytemanScript) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
            // none
        }
        Config config = new Config();
        String javaVmArguments = System.getProperty("server.jvm.args");

        clearRecoveryLog();

        if (bytemanScript != null) {
            String testClassesDir = System.getProperty("maven.test.classes.dir");
            javaVmArguments = javaVmArguments.replaceAll("=listen", "=script:" + testClassesDir + "/scripts/@BMScript@.btm,listen");
            javaVmArguments = javaVmArguments.replace("@BMScript@", bytemanScript);
        }

        config.add("javaVmArguments", javaVmArguments);

        containerController.start(MANUAL_COORDINATOR_CONTAINER, config.map());
        deployer.deploy(COORDINATOR_DEPLOYMENT);
    }

    void restartContainer() {
        try {
            // ensure that the controller is not running
            containerController.kill(MANUAL_COORDINATOR_CONTAINER);
            LRALogger.logger.debug("jboss-as kill worked");
        } catch (Exception e) {
            LRALogger.logger.debugf("jboss-as kill: %s", e.getMessage());
        }

        Config config = new Config();
        String javaVmArguments = System.getProperty("server.jvm.args");
        config.add("javaVmArguments", javaVmArguments);
        containerController.start(MANUAL_COORDINATOR_CONTAINER);
    }

    protected void stopContainer() {
        if (containerController.isStarted(MANUAL_COORDINATOR_CONTAINER)) {
            LRALogger.logger.debug("Stopping container");

            deployer.undeploy(COORDINATOR_DEPLOYMENT);

            containerController.stop(MANUAL_COORDINATOR_CONTAINER);
            containerController.kill(MANUAL_COORDINATOR_CONTAINER);
        }
    }

    int recover() throws URISyntaxException {
        Client client = ClientBuilder.newClient();

        try (Response response = client.target(new URI(recoveryUrl))
                .request()
                .get()) {

            Assert.assertEquals("Unexpected status from recovery call to " + recoveryUrl, 200, response.getStatus());

            // the result will be a List<LRAStatusHolder> of recovering LRAs but we just need the count
            String recoveringLRAs = response.readEntity(String.class);

            return recoveringLRAs.length() - recoveringLRAs.replace(".", "").length();
        } finally {
            client.close();
        }
    }

    void doWait(long millis) throws InterruptedException {
        if (millis > 0L) {
            Thread.sleep(millis);
        }
    }

    private void clearRecoveryLog() {
        try {
            Files.walk(storeDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignore) {
            // transaction logs will only exists after there has been a previous run
        }
    }

    String getFirstLRA() {
        Path lraDir = Paths.get(storeDir.toString(), "ShadowNoFileLockStore", "defaultStore", "StateManager", "BasicAction", "TwoPhaseCoordinator", "LRA");

        try {
            Optional<Path> lra = Files.list(new File(lraDir.toString()).toPath()).findFirst();

            return lra.map(path -> path.getFileName().toString()).orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    LRAStatus getStatus(URI lra) {
        try {
            return lraClient.getStatus(lra);
        } catch (NotFoundException ignore) {
            return null;
        }
    }

    String getCoordinatorUrl() {
        return coordinatorUrl;
    }

    protected String getDeploymentUrl() {
        return deploymentURL;
    }
}
