/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. 
 * See the copyright.txt in the distribution for a full listing 
 * of individual contributors.
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
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
package com.arjuna.webservices11.wsarjtx.server;

import com.arjuna.webservices11.wsarjtx.ArjunaTX11Constants;
import com.arjuna.webservices11.ServiceRegistry;
import org.jboss.jbossts.xts.environment.WSCEnvironmentBean;
import org.jboss.jbossts.xts.environment.WSTEnvironmentBean;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;

/**
 * Activate the Terminator Coordinator service
 * 
 * @author kevin
 */
public class TerminationParticipantInitialisation {
    public static void startup() {
        final ServiceRegistry serviceRegistry = ServiceRegistry.getRegistry();
        WSCEnvironmentBean wscEnvironmentBean = XTSPropertyManager.getWSCEnvironmentBean();
        String bindAddress = wscEnvironmentBean.getBindAddress11();
        int bindPort = wscEnvironmentBean.getBindPort11();
        int secureBindPort = wscEnvironmentBean.getBindPortSecure11();
        WSTEnvironmentBean wstEnvironmentBean = XTSPropertyManager.getWSTEnvironmentBean();
        String clientServiceURLPath = wstEnvironmentBean.getClientServiceURLPath();
        if (clientServiceURLPath == null) {
            clientServiceURLPath = "/ws-t11-client";
        }

        if (bindAddress == null) {
            bindAddress = "localhost";
        }

        if (bindPort == 0) {
            bindPort = 8080;
        }

        if (secureBindPort == 0) {
            secureBindPort = 8443;
        }

        final String baseUri = "http://" + bindAddress + ":" + bindPort + clientServiceURLPath;
        final String uri = baseUri + "/" + ArjunaTX11Constants.TERMINATION_PARTICIPANT_SERVICE_NAME;
        final String secureBaseUri = "https://" + bindAddress + ":" + secureBindPort + clientServiceURLPath;
        final String secureUri = secureBaseUri + "/" + ArjunaTX11Constants.TERMINATION_PARTICIPANT_SERVICE_NAME;

        serviceRegistry.registerServiceProvider(ArjunaTX11Constants.TERMINATION_PARTICIPANT_SERVICE_NAME, uri);
        serviceRegistry.registerSecureServiceProvider(ArjunaTX11Constants.TERMINATION_PARTICIPANT_SERVICE_NAME,
                secureUri);
    }

    public static void shutdown() {
    }
}
