package org.jboss.narayana.txframework.functional.rest.at.simpleEJB;

import com.arjuna.wst.Aborted;
import com.arjuna.wst.Prepared;
import com.arjuna.wst.Vote;
import org.jboss.narayana.txframework.api.annotation.lifecycle.wsat.Commit;
import org.jboss.narayana.txframework.api.annotation.lifecycle.wsat.Prepare;
import org.jboss.narayana.txframework.api.annotation.lifecycle.wsat.Rollback;
import org.jboss.narayana.txframework.api.annotation.management.TxManagement;
import org.jboss.narayana.txframework.api.annotation.service.ServiceRequest;
import org.jboss.narayana.txframework.api.annotation.transaction.RESTAT;
import org.jboss.narayana.txframework.api.management.ATTxControl;
import org.jboss.narayana.txframework.api.management.DataControl;
import org.jboss.narayana.txframework.functional.common.EventLog;
import org.jboss.narayana.txframework.functional.common.ServiceCommand;
import org.jboss.narayana.txframework.functional.common.SomeApplicationException;
import org.jboss.narayana.txframework.impl.TXControlException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;

/**
 * @Author paul.robinson@redhat.com 06/04/2012
 */
@Stateless
@RESTAT
public class Service2Impl implements Service2 {

    @Inject
    DataControl dataControl;
    @TxManagement
    public ATTxControl txControl;
    private boolean rollback = false;
    private EventLog eventLog = new EventLog();

    @WebMethod
    @ServiceRequest
    public Response someServiceRequest(String serviceCommand) throws SomeApplicationException {
        dataControl.put("data", "data");

        try {
            if (Service1.THROW_APPLICATION_EXCEPTION.equals(serviceCommand)) {
                throw new SomeApplicationException("Intentionally thrown Exception");
            }

            if (Service1.READ_ONLY.equals(serviceCommand)) {
                // todo: is this right?
                txControl.readOnly();
            }

            if (VOTE_ROLLBACK.equals(serviceCommand)) {
                rollback = true;
            }
        } catch (TXControlException e) {
            throw new RuntimeException("Error invoking lifecycle methods on the TXControl", e);
        }
        return Response.ok().build();
    }

    public Response getEventLog() {

        return Response.ok(EventLog.asString(eventLog.getEventLog())).build();
    }

    public Response clearLogs() {

        eventLog.clear();
        return Response.ok().build();
    }

    @Commit
    private void commit() {
        logEvent(Commit.class);
    }

    @Rollback
    private void rollback() {
        logEvent(Rollback.class);
    }

    @Prepare
    private Vote prepare() {
        logEvent(Prepare.class);
        if (rollback) {
            return new Aborted();
        } else {
            return new Prepared();
        }
    }

    private boolean isPresent(ServiceCommand expectedServiceCommand, ServiceCommand... serviceCommands) {
        for (ServiceCommand foundServiceCommand : serviceCommands) {
            if (foundServiceCommand == expectedServiceCommand) {
                return true;
            }
        }
        return false;
    }

    private void logEvent(Class<? extends Annotation> event) {
        // Check data is available
        if (dataControl.get("data") == null) {
            eventLog.addDataUnavailable(event);
        }

        eventLog.addEvent(event);
    }

}
