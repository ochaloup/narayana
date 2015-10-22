
package com.arjuna.webservices11.wsba.sei;

import com.arjuna.services.framework.task.Task;
import com.arjuna.services.framework.task.TaskManager;
import com.arjuna.webservices.logging.WSTLogger;
import com.arjuna.webservices11.wsarj.ArjunaContext;
import com.arjuna.webservices11.wsba.processors.ParticipantCompletionCoordinatorProcessor;
import com.arjuna.webservices11.SoapFault11;
import org.jboss.ws.api.addressing.MAP;
import com.arjuna.webservices11.wsaddr.AddressingHelper;
import com.arjuna.webservices.SoapFault;
import org.oasis_open.docs.ws_tx.wsba._2006._06.ExceptionType;
import org.oasis_open.docs.ws_tx.wsba._2006._06.NotificationType;
import org.oasis_open.docs.ws_tx.wsba._2006._06.StatusType;
import org.xmlsoap.schemas.soap.envelope.Fault;

import javax.annotation.Resource;
import javax.jws.*;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Action;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.Addressing;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.1-b03- Generated
 * source version: 2.0
 *
 */
@WebService(name = "BusinessAgreementWithParticipantCompletionCoordinatorPortType", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06",
        // wsdlLocation =
        // "/WEB-INF/wsdl/wsba-participant-completion-coordinator-binding.wsdl",
        serviceName = "BusinessAgreementWithParticipantCompletionCoordinatorService", portName = "BusinessAgreementWithParticipantCompletionCoordinatorPortType")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@HandlerChain(file = "/ws-t_handlers.xml")
@Addressing(required = true)
public class BusinessAgreementWithParticipantCompletionCoordinatorPortTypeImpl // implements
                                                                                // BusinessAgreementWithParticipantCompletionCoordinatorPortType
{
    @Resource
    private WebServiceContext webServiceCtx;

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "CompletedOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Completed")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Completed")
    public void completedOperation(
            @WebParam(name = "Completed", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") NotificationType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".completeOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final NotificationType completed = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().completed(completed, inboundMap,
                        arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "FailOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Fail")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Fail")
    public void failOperation(
            @WebParam(name = "Fail", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") ExceptionType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".failOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final ExceptionType fail = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().fail(fail, inboundMap, arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "CompensatedOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Compensated")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Compensated")
    public void compensatedOperation(
            @WebParam(name = "Compensated", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") NotificationType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".compensatedOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final NotificationType compensated = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().compensated(compensated, inboundMap,
                        arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "ClosedOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Closed")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Closed")
    public void closedOperation(
            @WebParam(name = "Closed", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") NotificationType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".closedOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final NotificationType closed = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().closed(closed, inboundMap, arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "CanceledOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Canceled")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Canceled")
    public void canceledOperation(
            @WebParam(name = "Canceled", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") NotificationType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".canceledOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final NotificationType cancelled = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().cancelled(cancelled, inboundMap,
                        arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "ExitOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Exit")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Exit")
    public void exitOperation(
            @WebParam(name = "Exit", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") NotificationType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".exitOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final NotificationType exit = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().exit(exit, inboundMap, arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "CannotComplete", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/CannotComplete")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/CannotComplete")
    public void cannotComplete(
            @WebParam(name = "CannotComplete", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") NotificationType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".cannotComplete");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final NotificationType cannotComplete = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().cannotComplete(cannotComplete, inboundMap,
                        arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "GetStatusOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/GetStatus")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/GetStatus")
    public void getStatusOperation(
            @WebParam(name = "GetStatus", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") NotificationType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".getStatusOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final NotificationType getStatus = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().getStatus(getStatus, inboundMap,
                        arjunaContext);
            }
        });
    }

    /**
     *
     * @param parameters
     */
    @WebMethod(operationName = "StatusOperation", action = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Status")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/Status")
    public void statusOperation(
            @WebParam(name = "Status", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsba/2006/06", partName = "parameters") StatusType parameters) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".statusOperation");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final StatusType status = parameters;
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().status(status, inboundMap, arjunaContext);
            }
        });
    }

    @WebMethod(operationName = "fault", action = "http://docs.oasis-open.org/ws-tx/wscoor/2006/06/fault")
    @Oneway
    @Action(input = "http://docs.oasis-open.org/ws-tx/wscoor/2006/06/fault")
    public void soapFault(
            @WebParam(name = "Fault", targetNamespace = "http://schemas.xmlsoap.org/soap/envelope/", partName = "parameters") Fault fault) {
        if (WSTLogger.logger.isTraceEnabled()) {
            WSTLogger.logger.trace(getClass().getSimpleName() + ".soapFault");
        }

        MessageContext ctx = webServiceCtx.getMessageContext();
        final MAP inboundMap = AddressingHelper.inboundMap(ctx);
        final ArjunaContext arjunaContext = ArjunaContext.getCurrentContext(ctx);
        final SoapFault soapFault = SoapFault11.fromFault(fault);

        TaskManager.getManager().queueTask(new Task() {
            public void executeTask() {
                ParticipantCompletionCoordinatorProcessor.getProcessor().soapFault(soapFault, inboundMap,
                        arjunaContext);;
            }
        });
    }
}