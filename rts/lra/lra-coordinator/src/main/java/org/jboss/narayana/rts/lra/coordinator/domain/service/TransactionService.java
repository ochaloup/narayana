package org.jboss.narayana.rts.lra.coordinator.domain.service;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import org.jboss.narayana.rts.lra.coordinator.domain.model.LRAStatus;
import org.jboss.narayana.rts.lra.coordinator.domain.model.Transaction;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class TransactionService {
    private Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private Map<String, Transaction> recoveringTransactions = new ConcurrentHashMap<>();

    private static Map<String, String> participants = new ConcurrentHashMap<>();

    public Transaction getTransaction(String lraId) throws NotFoundException {

        if (!transactions.containsKey(lraId)) {
            throw new NotFoundException(Response.status(404).entity("Invalid transaction id: " + lraId).build());
        }

        return transactions.get(lraId);
    }

    public List<LRAStatus> getAll() {
        List<LRAStatus> all = getAllActive();

        all.addAll(getAllRecovering());

        return all;
    }

    public List<LRAStatus> getAllActive() {
        return transactions.values().stream().map(LRAStatus::new).collect(toList());
    }

    public List<LRAStatus> getAllRecovering() {
        return recoveringTransactions.values().stream().map(LRAStatus::new).collect(toList());
    }

    public void addTransaction(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);
    }

    public void finished(Transaction transaction, boolean needsRecovery) {
        transactions.remove(transaction.getId());

        if (needsRecovery)
           recoveringTransactions.put(transaction.getId(), transaction);
    }

    public void addCompensator(Transaction transaction, String coordinatorId, String compensatorUrl) {
        participants.put(coordinatorId, compensatorUrl);
    }

    public String getParticipant(String rcvCoordId) {
        return participants.get(rcvCoordId);
    }

    public String startLRA(String baseUri, String clientId, Integer timelimit) {
        Transaction tx = new Transaction(baseUri, clientId);

        if (timelimit < 0)
            timelimit = 0;

        int status = tx.begin(timelimit);

        if (status != ActionStatus.RUNNING) {
            tx.abort();

            throw new InternalServerErrorException(ActionStatus.stringForm(status));
        } else {
            try {
                addTransaction(tx);

                return tx.getId();
            } finally {
                AtomicAction.suspend();
            }
        }
    }

    public int endLRA(String lraId, boolean compensate) {
        Transaction transaction = getTransaction(lraId);

        if (!transaction.isRunning())
            return Response.Status.PRECONDITION_FAILED.getStatusCode();

        AtomicAction.resume(transaction);

        int status = transaction.end(compensate);
        int sc = 500;

        if (compensate) {
            if (status == ActionStatus.COMMITTED)
                sc = 200;
        } else if (status == ActionStatus.ABORTED) {
            sc = 200;
        }

        finished(transaction, sc != 200);

        return sc;
    }

    public int leave(String lraId, String compensatorUrl) {
        Transaction transaction = getTransaction(lraId);

        if (!transaction.isRunning())
            return Response.Status.PRECONDITION_FAILED.getStatusCode();

        try {
            if (!transaction.forgetParticipant(compensatorUrl))
                System.out.printf("WARNING could not determine wether or not the request succeeded%s");

            return Response.Status.OK.getStatusCode();
        } catch (Exception e) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }


    }

    public int joinLRA(StringBuilder recoveryUrl, String txId, int timeLimit, String compensatorUrl, String linkHeader, String recoveryUrlBase) {
        Transaction transaction = getTransaction(txId);

        if (timeLimit < 0)
            timeLimit = 0;

        /*
         * TODO update the spec with:
         *   If the transaction is not TransactionActive then the implementation MUST return a 412 status code
         */
        if (!transaction.isRunning())
            return Response.Status.PRECONDITION_FAILED.getStatusCode();

        String coordinatorId;

//        if (coordinatorUrl.endsWith("/"))
//            coordinatorUrl = coordinatorUrl.substring(0, coordinatorUrl.length() - 1);

        if (linkHeader != null)
            coordinatorId = transaction.enlistParticipants(txId, linkHeader, recoveryUrlBase);
        else
            coordinatorId = transaction.enlistParticipant( txId, compensatorUrl, recoveryUrlBase);

        if (coordinatorId == null)
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        addCompensator(transaction, coordinatorId, compensatorUrl);

        recoveryUrl.append(transaction.getRecoveryUrl());

        return Response.Status.OK.getStatusCode();
    }
}
