package com.arjuna.webservices11.wscoor.client;

import com.arjuna.webservices11.util.PrivilegedServiceFactory;
import com.arjuna.webservices11.util.PrivilegedServiceHelper;
import com.arjuna.webservices11.wsaddr.AddressingHelper;
import org.jboss.ws.api.addressing.MAP;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.ActivationPortType;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.ActivationService;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.RegistrationPortType;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.RegistrationService;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: adinn Date: Oct 7, 2007 Time: 3:14:28 PM To
 * change this template use File | Settings | File Templates.
 */
public class WSCOORClient {
    // TODO -- do we really need a thread local here or can we just use one
    // service?
    /**
     * thread local which maintains a per thread activation service instance
     */
    private static ThreadLocal<ActivationService> activationService = new ThreadLocal<ActivationService>();

    /**
     * thread local which maintains a per thread activation service instance
     */
    private static ThreadLocal<RegistrationService> registrationService = new ThreadLocal<RegistrationService>();

    /**
     * fetch a coordinator activation service unique to the current thread
     * 
     * @return
     */
    private static synchronized ActivationService getActivationService() {
        if (activationService.get() == null) {
            activationService.set(PrivilegedServiceFactory.getInstance(ActivationService.class).getService());
        }
        return activationService.get();
    }

    /**
     * fetch a coordinator registration service unique to the current thread
     * 
     * @return
     */
    private static synchronized RegistrationService getRegistrationService() {
        if (registrationService.get() == null) {
            registrationService.set(PrivilegedServiceFactory.getInstance(RegistrationService.class).getService());
        }
        return registrationService.get();
    }

    public static ActivationPortType getActivationPort(MAP map, String action) {
        final ActivationService service = getActivationService();
        final ActivationPortType port = PrivilegedServiceHelper.getInstance().getPort(service, ActivationPortType.class,
                new AddressingFeature(true, true));
        BindingProvider bindingProvider = (BindingProvider) port;
        String to = map.getTo();
        /*
         * we no longer have to add the JaxWS WSAddressingClientHandler because
         * we can specify the WSAddressing feature List<Handler>
         * customHandlerChain = new ArrayList<Handler>();
         * customHandlerChain.add(new WSAddressingClientHandler());
         * bindingProvider.getBinding().setHandlerChain(customHandlerChain);
         */

        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        AddressingHelper.configureRequestContext(requestContext, map, to, action);

        return port;
    }

    // don't think we ever need this as we get a registration port from the
    // endpoint ref returned by
    // the activation port request
    public static RegistrationPortType getRegistrationPort(final W3CEndpointReference endpointReference, String action,
            String messageID) {
        final RegistrationService service = getRegistrationService();
        final RegistrationPortType port = PrivilegedServiceHelper.getInstance().getPort(service, endpointReference,
                RegistrationPortType.class, new AddressingFeature(true, true));
        BindingProvider bindingProvider = (BindingProvider) port;
        /*
         * we no longer have to add the JaxWS WSAddressingClientHandler because
         * we can specify the WSAddressing feature List<Handler>
         * customHandlerChain = new ArrayList<Handler>();
         * customHandlerChain.add(new WSAddressingClientHandler());
         * bindingProvider.getBinding().setHandlerChain(customHandlerChain);
         */

        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        MAP map = AddressingHelper.outboundMap(requestContext);
        AddressingHelper.installActionMessageID(map, action, messageID);
        // we should not need to do this but JBossWS does not pick up the value
        // in the addressing properties
        AddressingHelper.configureRequestContext(requestContext, map.getTo(), action);
        return port;
    }
}
