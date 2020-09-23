package io.narayana.lra.arquillian;

import org.apache.http.HttpConnection;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import javax.inject.Inject;

public class AppServerCoordinatorDeploymentObserver {
    private static final Logger log = Logger.getLogger(AppServerCoordinatorDeploymentObserver.class);

    private static final String LRA_COORDINATOR_DEPLOYMENT_NAME = "lra-coordinator";

    @Inject
    private Deployer deployer;

    /**
     * The goal of this method is to create a deployment of the LRA coordinator
     * which is deployed before the test deployment is put on the server.
     */
    public synchronized void handleBeforeDeployment(@Observes BeforeDeploy event, Container container) throws Exception {
        log.info(">>>>> handleBeforeDeployment");
    }

    /**
     * This method undeploys the LRA coordinator deployed at {@link #handleBeforeDeployment(BeforeDeploy, Container)}.
     */
    public synchronized void handleAfterDeployment(@Observes BeforeDeploy event, Container container) throws Exception {
        log.info(">>>>> handleAfterDeployment");
    }

    public static WebArchive createDeployment() {
        // LRA uses ArjunaCore so pull in the jts module to get them on the classpath
        // (maybe in the future we can add a WFLY LRA subsystem)
        final String ManifestMF = "Manifest-Version: 1.0\n"
                + "Dependencies: org.jboss.jts, org.jboss.logging\n";

        MavenDependencyResolver resolver = DependencyResolvers
                .use(MavenDependencyResolver.class)
                .loadMetadataFromPom("pom.xml");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(...)
         .addAsLibraries(resolver.artifact("com.google.guava:guava:11.0.2").resolveAsFiles())
                .addAsWebResource(EmptyAsset.INSTANCE, "beans.xml");
        // verify that the JAR files ended up in the WAR
        System.out.println(war.toString(true));
        return war;

        return ShrinkWrap.create(WebArchive.class, LRA_COORDINATOR_DEPLOYMENT_NAME + ".war")
                .addPackages(false, coordinatorPackages)
                .addPackages(false, participantPackages)
                .addPackages(true, HttpConnection.class.getPackage())
                .addAsManifestResource(new StringAsset(ManifestMF), "MANIFEST.MF")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }
}