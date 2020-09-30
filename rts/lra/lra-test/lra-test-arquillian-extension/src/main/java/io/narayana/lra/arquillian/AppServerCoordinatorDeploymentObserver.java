package io.narayana.lra.arquillian;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
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
    public void handleBeforeDeploy(@Observes BeforeDeploy event, Container container) throws Exception {
        log.info(">>>>> handleBeforeDeploy");
        Archive<?> deployment = createDeployment();
        if(deployments.put(deployment.getName(), deployment) == null) {
            log.infof("Deploying %s", deployment.getName());
            container.getDeployableContainer()
                    .deploy(deployment);
        }
    }

    /**
     * This method undeploys the LRA coordinator deployed by method
     * {@link #handleBeforeDeploy(BeforeDeploy, Container)}.
     */
    public void handleAfterUnDeploy(@Observes AfterUnDeploy event, Container container) throws Exception {
        log.info(">>>>> handleAfterUnDeploy");
        for(Archive<?> deployment: deployments.values()) {
            log.infof("Undeploying %s", deployment.getName());
            container.getDeployableContainer().undeploy(deployment);
        }
    }

    public static WebArchive createDeployment() {
        // LRA uses ArjunaCore - for WildFly we need to pull the org.jboss.jts module to get it on the classpath
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