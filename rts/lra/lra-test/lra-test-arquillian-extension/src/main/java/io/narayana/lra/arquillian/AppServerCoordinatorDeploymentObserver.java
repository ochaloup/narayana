package io.narayana.lra.arquillian;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppServerCoordinatorDeploymentObserver {
    private static final Logger log = Logger.getLogger(AppServerCoordinatorDeploymentObserver.class);

    private static final String LRA_COORDINATOR_DEPLOYMENT_NAME = "lra-coordinator";
    private static final Map<String,Archive<?>> deployments = new ConcurrentHashMap<>();

    @Inject
    private Deployer deployer;

    /**
     * The goal of this method is to create a deployment of the LRA coordinator
     * which is deployed before the test deployment is put on the server.
     */
    public void handleBeforeDeployment(@Observes BeforeDeploy event, Container container) throws Exception {
        log.info(">>>>> handleBeforeDeployment");
        Archive<?> deployment = createDeployment();
        if(deployments.put(deployment.getName(), deployment) == null) {
            log.infof("Deploying %s", deployment.getName());
            container.getDeployableContainer()
                    .deploy(deployment);
        }
    }

    /**
     * This method undeploys the LRA coordinator deployed at {@link #handleBeforeDeployment(BeforeDeploy, Container)}.
     */
    public void handleAfterDeployment(@Observes AfterDeploy event, Container container) throws Exception {
        log.info(">>>>> handleAfterDeployment");
        for(Archive<?> deployment: deployments.values()) {
            log.infof("Undeploying %s", deployment.getName());
            container.getDeployableContainer().undeploy(deployment);
        }
    }

    public static WebArchive createDeployment() {
        // LRA uses ArjunaCore so pull in the jts module to get them on the classpath
        // (maybe in the future we can add a WFLY LRA subsystem)
        final String ManifestMF = "Manifest-Version: 1.0\n"
                + "Dependencies: org.jboss.jts, org.jboss.logging\n";

        String mavenProjectVersion = System.getProperty("project.version");

        File[] files = Maven.resolver()
                .resolve("org.jboss.narayana.rts:lra-coordinator-war:war:" + mavenProjectVersion)
                .withTransitivity().asFile();

        ZipImporter zip = ShrinkWrap.create(ZipImporter.class, LRA_COORDINATOR_DEPLOYMENT_NAME + ".war");
        for(File file: files) {
            zip.importFrom(file);
        }
        WebArchive war = zip.as(WebArchive.class);

        System.out.println(war.toString(true)); // TODO: delete me!
        return war;
    }
}