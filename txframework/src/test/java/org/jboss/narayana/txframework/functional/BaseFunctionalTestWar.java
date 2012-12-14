/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.narayana.txframework.functional;

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.narayana.txframework.functional.clients.ATClient;
import org.jboss.narayana.txframework.functional.common.EventLog;
import org.jboss.narayana.txframework.functional.common.ServiceCommand;
import org.jboss.narayana.txframework.functional.common.SomeApplicationException;
import org.jboss.narayana.txframework.functional.interfaces.AT;
import org.jboss.narayana.txframework.functional.services.ATService;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class BaseFunctionalTestWar {

    @Deployment
    public static WebArchive createTestArchive() {
        // todo: Does the application developer have to specify the interceptor?
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "echo_service.war")
                .addClasses(ATClient.class, AT.class, ATService.class, SomeApplicationException.class, EventLog.class,
                        BaseFunctionalTestWar.class, ServiceCommand.class)
                .addAsWebInfResource(new ByteArrayAsset(
                        "<interceptors><class>org.jboss.narayana.txframework.impl.ServiceRequestInterceptor</class></interceptors>"
                                .getBytes()),
                        ArchivePaths.create("beans.xml"))
                .addAsWebInfResource("web.xml", "web.xml");

        archive.delete(ArchivePaths.create("META-INF/MANIFEST.MF"));

        String ManifestMF = "Manifest-Version: 1.0\n" + "Dependencies: org.jboss.narayana.txframework\n";
        archive.setManifest(new StringAsset(ManifestMF));

        return archive;
    }

    public void rollbackIfActive(UserTransaction ut) {

        try {
            ut.rollback();
        } catch (Throwable th2) {
            // do nothing, not active
        }
    }

    public void cancelIfActive(UserBusinessActivity uba) {

        try {
            uba.cancel();
        } catch (Throwable e) {
            // do nothing, not active
        }
    }

}
