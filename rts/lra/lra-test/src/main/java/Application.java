import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.jaxrs.JAXRSArchive;
import participant.api.ActivityController;

public class Application {
 
    public static void main(String[] args) throws Exception {
        Swarm swarm = new Swarm();
 
        JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class);
        deployment.addClass(ActivityController.class);
 
        swarm.start();
        swarm.deploy(deployment);
    }
}
