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
package com.arjuna.wsc.tests.arq;

import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.soap.SOAPFaultException;

import org.jboss.ws.api.addressing.MAP;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.RegisterResponseType;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.RegisterType;

import com.arjuna.webservices.SoapFaultType;
import com.arjuna.webservices11.wsarj.ArjunaContext;
import com.arjuna.webservices11.wsarj.InstanceIdentifier;
import com.arjuna.webservices11.wscoor.CoordinationConstants;
import com.arjuna.webservices11.wscoor.processors.RegistrationCoordinatorProcessor;
import com.arjuna.wsc.tests.TestUtil;
import com.arjuna.wsc.tests.TestUtil11;

public class TestRegistrationCoordinatorProcessor extends
        RegistrationCoordinatorProcessor
{
    private Map<String, RegisterDetails> messageIdMap = new HashMap<String, RegisterDetails>() ;

    public RegisterResponseType register(final RegisterType register, final MAP map, final ArjunaContext arjunaContext, boolean isSecure)
    {
        final String messageId = map.getMessageID() ;
        synchronized(messageIdMap)
        {
            messageIdMap.put(messageId, new RegisterDetails(register, map, arjunaContext)) ;
            messageIdMap.notifyAll() ;
        }
        String protocolIdentifier = register.getProtocolIdentifier();
        if (TestUtil.ALREADY_REGISTERED_PROTOCOL_IDENTIFIER.equals(protocolIdentifier)) {
            try {
                SOAPFactory factory = SOAPFactory.newInstance();
                SOAPFault soapFault = factory.createFault(SoapFaultType.FAULT_SENDER.getValue(), CoordinationConstants.WSCOOR_ERROR_CODE_CANNOT_REGISTER_QNAME);
                soapFault.addDetail().addDetailEntry(CoordinationConstants.WSCOOR_ERROR_CODE_CANNOT_REGISTER_QNAME).addTextNode("already registered");
                throw new SOAPFaultException(soapFault);
            } catch (Throwable th) {
                throw new ProtocolException(th);
            }
        }
        if (TestUtil.INVALID_PROTOCOL_PROTOCOL_IDENTIFIER.equals(protocolIdentifier)) {
            try {
                SOAPFactory factory = SOAPFactory.newInstance();
                SOAPFault soapFault = factory.createFault(SoapFaultType.FAULT_SENDER.getValue(), CoordinationConstants.WSCOOR_ERROR_CODE_INVALID_PROTOCOL_QNAME);
                soapFault.addDetail().addDetailEntry(CoordinationConstants.WSCOOR_ERROR_CODE_INVALID_PROTOCOL_QNAME).addTextNode("invalid protocol");
                throw new SOAPFaultException(soapFault);
            } catch (Throwable th) {
                throw new ProtocolException(th);
            }
        }
        if (TestUtil.INVALID_STATE_PROTOCOL_IDENTIFIER.equals(protocolIdentifier)) {
            try {
                SOAPFactory factory = SOAPFactory.newInstance();
                SOAPFault soapFault = factory.createFault(SoapFaultType.FAULT_SENDER.getValue(), CoordinationConstants.WSCOOR_ERROR_CODE_INVALID_STATE_QNAME);
                soapFault.addDetail().addDetailEntry(CoordinationConstants.WSCOOR_ERROR_CODE_INVALID_STATE_QNAME).addTextNode("invalid state");
                throw new SOAPFaultException(soapFault);
            } catch (Throwable th) {
                throw new ProtocolException(th);
            }
        }
        if (TestUtil.NO_ACTIVITY_PROTOCOL_IDENTIFIER.equals(protocolIdentifier)) {
            try {
                SOAPFactory factory = SOAPFactory.newInstance();
                SOAPFault soapFault = factory.createFault(SoapFaultType.FAULT_SENDER.getValue(), CoordinationConstants.WSCOOR_ERROR_CODE_CANNOT_REGISTER_QNAME);
                soapFault.addDetail().addDetailEntry(CoordinationConstants.WSCOOR_ERROR_CODE_CANNOT_REGISTER_QNAME).addTextNode("no activity");
                throw new SOAPFaultException(soapFault);
            } catch (Throwable th) {
                throw new ProtocolException(th);
            }
        }
        // we need to cook up a response here
        RegisterResponseType registerResponseType = new RegisterResponseType();
        if (arjunaContext != null) {
            InstanceIdentifier instanceIdentifier = arjunaContext.getInstanceIdentifier();
            registerResponseType.setCoordinatorProtocolService(TestUtil11.getProtocolCoordinatorEndpoint(instanceIdentifier.getInstanceIdentifier()));
        } else {
            registerResponseType.setCoordinatorProtocolService(TestUtil11.getProtocolCoordinatorEndpoint(null));
        }
        return registerResponseType;
    }

    public RegisterDetails getRegisterDetails(final String messageId, final long timeout)
    {
        final long endTime = System.currentTimeMillis() + timeout ;
        synchronized(messageIdMap)
        {
            long now = System.currentTimeMillis() ;
            while(now < endTime)
            {
                final RegisterDetails details = (RegisterDetails)messageIdMap.remove(messageId) ;
                if (details != null)
                {
                    return details ;
                }
                try
                {
                    messageIdMap.wait(endTime - now) ;
                }
                catch (final InterruptedException ie) {} // ignore
                now = System.currentTimeMillis() ;
            }
            final RegisterDetails details = (RegisterDetails)messageIdMap.remove(messageId) ;
            if (details != null)
            {
                return details ;
            }
        }
        throw new NullPointerException("Timeout occurred waiting for id: " + messageId) ;
    }

    public static class RegisterDetails
    {
        private final RegisterType register ;
        private final MAP map ;
        private final ArjunaContext arjunaContext ;

        RegisterDetails(final RegisterType register,
            final MAP map,
            final ArjunaContext arjunaContext)
        {
            this.register = register ;
            this.map = map ;
            this.arjunaContext = arjunaContext ;
        }

        public RegisterType getRegister()
        {
            return register ;
        }

        public MAP getMAP()
        {
            return map ;
        }

        public ArjunaContext getArjunaContext()
        {
            return arjunaContext ;
        }
    }
}
