/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package io.narayana.lra.arquillian;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * The appender provides way to bundle LRA interfaces implementation classes to the deployment
 * as the Thorntail container does not bundle the LRA classes to the fat jar until
 * there is no fraction for it.
 */
public class ConfigAuxiliaryArchiveProcessor implements AuxiliaryArchiveProcessor {

    @Override
    public void process(Archive<?> auxiliaryArchive) {
        if(auxiliaryArchive instanceof WebArchive) {
            ClassContainer<?> archive = WebArchive.class.cast(auxiliaryArchive);
                // adding LRA spec interfaces under the Thorntail deployment
                archive.addPackages(true, org.eclipse.microprofile.lra.annotation.Compensate.class.getPackage());
                // adding Narayana LRA implementation under the Thorntail deployment
                archive.addPackages(true, io.narayana.lra.Current.class.getPackage());
                archive.addPackages(true, org.apache.http.HttpEntity.class.getPackage());
                archive.addAsResource("META-INF/services/javax.enterprise.inject.spi.Extension");
                archive.addClass(org.jboss.weld.exceptions.DefinitionException.class);

            // adding Narayana LRA filters under the Thorntail deployment
            String filtersAsset = String.format("%s%n%s",
                    io.narayana.lra.filter.ClientLRAResponseFilter.class.getName(),
                    io.narayana.lra.filter.ClientLRARequestFilter.class.getName());
            archive.addAsResource(new StringAsset(filtersAsset), "META-INF/services/javax.ws.rs.ext.Providers");
            archive.addAsResource(new StringAsset("org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder"),
                            "META-INF/services/javax.ws.rs.client.ClientBuilder");

            // adding TCK required SPI implementations
            archive.addAsResource(new StringAsset("io.narayana.lra.arquillian.spi.NarayanaLRARecovery"),
                    "META-INF/services/org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService");
        }

        if(auxiliaryArchive instanceof ManifestContainer<?>) {
            ((ManifestContainer) auxiliaryArchive)
                    .addAsManifestResource(new StringAsset("<beans version=\"1.1\" bean-discovery-mode=\"annotated\"></beans>"), "beans.xml");
        }
    }
}
