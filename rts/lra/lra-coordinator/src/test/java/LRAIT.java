import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;

import org.jboss.narayana.rts.lra.coordinator.api.Coordinator;
import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.StreamSupport;

import javax.json.JsonArray;
import javax.json.JsonObject;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LRAIT {

    @Deployment(testable = false)
    public static WebArchive createDeploymentFromFiles() {
        return ShrinkWrap.create(WebArchive.class).merge(
                ShrinkWrap.create(GenericArchive.class)
                        .as(ExplodedImporter.class)
                        .importDirectory("target/lra-coordinator")
                        .as(GenericArchive.class), "/", Filters.includeAll());
    }

//    @Deployment(testable = false)
    // this one does not register Coordinator.class as a JAXRS resource
    public static WebArchive createDeploymentFromUberJar() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/lra-coordinator-swarm.jar")).addClass(Coordinator.class);
    }

    @ArquillianResource
    private URL base;

    private LRAClient lraClient;
    private LRAWrapper lraWrapper;

    @Before
    public void setupClass() throws MalformedURLException, URISyntaxException {
        lraWrapper = new LRAWrapper(base);
        lraClient = new LRAClient(base.getProtocol(), base.getHost(), base.getPort());
    }


    @Test
    public void testStartLRA() throws MalformedURLException, URISyntaxException {
        String lra = lraClient.startLRA("testStartLRA", 0);
        JsonArray lras = lraWrapper.getLRAs();

        assertNotNull(lras); // there should be at least one
        assertNotEquals(0, lras.size());

        // the new lra should be present in the current list of lras
        String lraId = LRAClient.getLRAId(lra);

        assertTrue(StreamSupport.stream(lras.spliterator(), false).
                anyMatch(jv -> ((JsonObject) jv).getString("lraId").equals(lraId)));
    }

    @Test
    public void testConfirmLRA() throws MalformedURLException, URISyntaxException {
        String lra = lraWrapper.startLRA();

        lraWrapper.confirmLRA(lra);
    }

    @Test
    public void testCompensateLRA() throws MalformedURLException, URISyntaxException {
        String lra = lraWrapper.startLRA();

        lraWrapper.compensateLRA(lra);
    }
}
