
package org.oasis_open.docs.ws_tx.wscoor._2006._06;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.2-hudson-182-RC1
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "RegistrationService", targetNamespace = "http://docs.oasis-open.org/ws-tx/wscoor/2006/06", wsdlLocation = "wsdl/wscoor-registration-binding.wsdl")
public class RegistrationService extends Service {

    private final static URL REGISTRATIONSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger
            .getLogger(org.oasis_open.docs.ws_tx.wscoor._2006._06.RegistrationService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = org.oasis_open.docs.ws_tx.wscoor._2006._06.RegistrationService.class.getResource("");
            url = new URL(baseUrl, "wsdl/wscoor-registration-binding.wsdl");
        } catch (MalformedURLException e) {
            logger.warning(
                    "Failed to create URL for the wsdl Location: 'wsdl/wscoor-registration-binding.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        REGISTRATIONSERVICE_WSDL_LOCATION = url;
    }

    public RegistrationService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public RegistrationService() {
        super(REGISTRATIONSERVICE_WSDL_LOCATION,
                new QName("http://docs.oasis-open.org/ws-tx/wscoor/2006/06", "RegistrationService"));
    }

    /**
     * 
     * @return returns RegistrationPortType
     */
    @WebEndpoint(name = "RegistrationPortType")
    public RegistrationPortType getRegistrationPortType() {
        return super.getPort(new QName("http://docs.oasis-open.org/ws-tx/wscoor/2006/06", "RegistrationPortType"),
                RegistrationPortType.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure
     *            on the proxy. Supported features not in the
     *            <code>features</code> parameter will have their default
     *            values.
     * @return returns RegistrationPortType
     */
    @WebEndpoint(name = "RegistrationPortType")
    public RegistrationPortType getRegistrationPortType(WebServiceFeature... features) {
        return super.getPort(new QName("http://docs.oasis-open.org/ws-tx/wscoor/2006/06", "RegistrationPortType"),
                RegistrationPortType.class, features);
    }

}
