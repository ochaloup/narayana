package com.arjuna.webservices11.wsat.sei;

import org.oasis_open.docs.ws_tx.wsat._2006._06.Notification;
import org.jboss.ws.api.addressing.MAP;

import javax.jws.*;
import javax.jws.soap.SOAPBinding;
import javax.annotation.Resource;
import javax.xml.ws.Action;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.handler.MessageContext;

import com.arjuna.webservices11.wsarj.ArjunaContext;
import com.arjuna.webservices11.wsat.processors.CoordinatorProcessor;
import com.arjuna.webservices11.SoapFault11;
import com.arjuna.webservices11.wsaddr.AddressingHelper;
import com.arjuna.services.framework.task.TaskManager;
import com.arjuna.services.framework.task.Task;
import com.arjuna.webservices.SoapFault;
import org.xmlsoap.schemas.soap.envelope.Fault;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.1-b03- Generated
 * source version: 2.0
 *
 */
@WebService(name = "CoordinatorPortType", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsat/2006/06",
        // wsdlLocation = "/WEB-INF/wsdl/wsat-coordinator-binding.wsdl",
        serviceName = "CoordinatorService", portName = "CoordinatorPortType")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@HandlerChain(file = "/ws-t_handlers.xml")
@Addressing(required = true)
public class CoordinatorPortTypeImpl // implements CoordinatorPortType
{

    @Resource
    private WebServiceContext webServiceCtx;

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "PreparedOperation", action = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/Prepared")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/Prepared")
    public void preparedOperation(
            @WebParam(name = "Prepared", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsat/2006/06", partName = "parameters") Notification parameters) {
        MessageContext ctx = webServiceCtx.getMessageContext();
        final Notification prepared = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                CoordinatorProcessor.getProcessor().prepared(prepared, inboundMap, arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "AbortedOperation", action = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/Aborted")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/Aborted")
    public void abortedOperation(
            @WebParam(name = "Aborted", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsat/2006/06", partName = "parameters") Notification parameters) {
        MessageContext ctx = webServiceCtx.getMessageContext();
        final Notification aborted = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                CoordinatorProcessor.getProcessor().aborted(aborted, inboundMap, arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "ReadOnlyOperation", action = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/ReadOnly")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/ReadOnly")
    public void readOnlyOperation(
            @WebParam(name = "ReadOnly", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsat/2006/06", partName = "parameters") Notification parameters) {
        MessageContext ctx = webServiceCtx.getMessageContext();
        final Notification readOnly = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                CoordinatorProcessor.getProcessor().readOnly(readOnly, inboundMap, arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "CommittedOperation", action = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/Committed")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/Committed")
    public void committedOperation(
            @WebParam(name = "Committed", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsat/2006/06", partName = "parameters") Notification parameters) {
        MessageContext ctx = webServiceCtx.getMessageContext();
        final Notification committed = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                CoordinatorProcessor.getProcessor().committed(committed, inboundMap, arjunaContext);
            }
        });
    }

    @WebMethod(operationName = "SoapFault", action = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/fault")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsat/2006/06/fault")
    public void soapFault(
            @WebParam(name = "Fault", targetNamespace = "http://schemas.xmlsoap.org/soap/envelope/", partName = "parameters") Fault fault) {
        MessageContext ctx = webServiceCtx.getMessageContext();
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);
        final SoapFault soapFault = SoapFault11.fromFault(fault);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                CoordinatorProcessor.getProcessor().soapFault(soapFault, inboundMap, arjunaContext);;
            }
        });
    }
}
