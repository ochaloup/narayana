
/*
 * 
 */

package com.arjuna.schemas.ws._2005._10.wsarjtx;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.2.9-patch-01 Thu Aug 26 17:20:43 BST
 * 2010 Generated source version: 2.2.9-patch-01
 * 
 */

@WebServiceClient(name = "TerminationCoordinatorRPCService", wsdlLocation = "wsdl/wsarjtx-termination-coordinator-rpc-binding.wsdl", targetNamespace = "http://schemas.arjuna.com/ws/2005/10/wsarjtx")
public class TerminationCoordinatorRPCService extends Service {

    public final static URL WSDL_LOCATION;
    public final static QName SERVICE = new QName("http://schemas.arjuna.com/ws/2005/10/wsarjtx",
            "TerminationCoordinatorRPCService");
    public final static QName TerminationCoordinatorRPCPortType = new QName(
            "http://schemas.arjuna.com/ws/2005/10/wsarjtx", "TerminationCoordinatorRPCPortType");
    static {
        URL url = null;
        try {
            url = new URL("wsdl/wsarjtx-termination-coordinator-rpc-binding.wsdl");
        } catch (MalformedURLException e) {
            System.err.println(
                    "Can not initialize the default wsdl from wsdl/wsarjtx-termination-coordinator-rpc-binding.wsdl");
            // e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public TerminationCoordinatorRPCService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public TerminationCoordinatorRPCService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public TerminationCoordinatorRPCService() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     * 
     * @return returns TerminationCoordinatorRPCPortType
     */
    @WebEndpoint(name = "TerminationCoordinatorRPCPortType")
    public TerminationCoordinatorRPCPortType getTerminationCoordinatorRPCPortType() {
        return super.getPort(TerminationCoordinatorRPCPortType, TerminationCoordinatorRPCPortType.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure
     *            on the proxy. Supported features not in the
     *            <code>features</code> parameter will have their default
     *            values.
     * @return returns TerminationCoordinatorRPCPortType
     */
    @WebEndpoint(name = "TerminationCoordinatorRPCPortType")
    public TerminationCoordinatorRPCPortType getTerminationCoordinatorRPCPortType(WebServiceFeature... features) {
        return super.getPort(TerminationCoordinatorRPCPortType, TerminationCoordinatorRPCPortType.class, features);
    }

}
