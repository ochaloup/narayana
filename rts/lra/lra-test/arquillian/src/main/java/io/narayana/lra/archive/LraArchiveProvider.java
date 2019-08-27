package io.narayana.lra.archive;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRACDIExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class LraArchiveProvider {

    public static Archive<?> createLraArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class);

        // adding LRA spec interfaces under the Thorntail deployment
        archive
            .addPackages(true, org.eclipse.microprofile.lra.annotation.Compensate.class.getPackage());

        // adding Narayana LRA implementation under the Thorntail deployment
        archive.addPackages(true, io.narayana.lra.client.NarayanaLRAClient.class.getPackage())
            .addPackages(true, io.narayana.lra.Current.class.getPackage())
            .addPackage(LRACDIExtension.class.getPackage())
            .addAsResource("META-INF/services/javax.enterprise.inject.spi.Extension")
            .addClass(org.jboss.weld.exceptions.DefinitionException.class)
            .addAsManifestResource(new StringAsset("<beans version=\"1.1\" bean-discovery-mode=\"annotated\"></beans>"), "beans.xml");

        // adding Narayana LRA filters under the Thorntail deployment
        String filtersAsset = String.format("%s%n%s",
            io.narayana.lra.filter.ClientLRAResponseFilter.class.getName(),
            io.narayana.lra.filter.ClientLRARequestFilter.class.getName());
        archive.addPackages(true, io.narayana.lra.filter.ClientLRARequestFilter.class.getPackage())
            .addAsResource(new StringAsset(filtersAsset), "META-INF/services/javax.ws.rs.ext.Providers")
            .addAsResource(new StringAsset("org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder"),
                "META-INF/services/javax.ws.rs.client.ClientBuilder");

        return archive;
    }
}
