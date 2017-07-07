package org.jboss.narayana.rts.lra.coordinator.domain.service;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import org.jboss.narayana.rts.lra.coordinator.api.InvalidLRAId;
import org.jboss.narayana.rts.lra.coordinator.domain.model.LRAStatus;
import org.jboss.narayana.rts.lra.coordinator.domain.model.Transaction;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class LRAService {
    private Map<URL, Transaction> transactions = new ConcurrentHashMap<>();
    private Map<URL, Transaction> recoveringTransactions = new ConcurrentHashMap<>();

    private static Map<String, String> participants = new ConcurrentHashMap<>();

    public Transaction getTransaction(URL lraId) throws NotFoundException {
        if (!transactions.containsKey(lraId))
            throw new NotFoundException(Response.status(404).entity("Invalid transaction id: " + lraId).build());

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

    public void addTransaction(Transaction lra) {
        transactions.put(lra.getId(), lra);
    }

    public void finished(Transaction transaction, boolean fromHierarchy, boolean needsRecovery) {
        if (fromHierarchy || transaction.isTopLevel()) {
            remove(ActionStatus.stringForm(transaction.status()), transaction.getId());
        }

        if (needsRecovery)
            recoveringTransactions.put(transaction.getId(), transaction);
    }

    public void remove(String state, URL lraId) {
        lraTrace(lraId, "remove LRA");

        if (transactions.containsKey(lraId)) {
            Transaction lra = transactions.get(lraId);

//            if (lra.isTopLevel()) {
                transactions.remove(lraId);
                recoveringTransactions.remove(lraId);
//            }

            // TODO make sure we clean up nested LRAs when the top level LRA closes
        }
    }

    public void addCompensator(Transaction transaction, String coordinatorId, String compensatorUrl) {
        participants.put(coordinatorId, compensatorUrl);
    }

    public String getParticipant(String rcvCoordId) {
        return participants.get(rcvCoordId);
    }

    public synchronized URL startLRA(String baseUri, URL parentLRA, String clientId, Integer timelimit) {
        Transaction lra = null;
        try {
            lra = new Transaction(baseUri, parentLRA, clientId);
        } catch (MalformedURLException e) {
            throw new InvalidLRAId(baseUri, "Invalid base uri", e);
        }

        if (timelimit < 0)
            timelimit = 0;

        if (lra.currentLRA() != null)
            System.out.printf("WARNING LRA %s is already associated");

        int status = lra.begin(timelimit);

        if (status != ActionStatus.RUNNING) {
            lraTrace(lra.getId(), "failed to start LRA");

            lra.abort();

            throw new InternalServerErrorException("Could not start LRA: " + ActionStatus.stringForm(status));
        } else {
            try {
                addTransaction(lra);

                lraTrace(lra.getId(), "started LRA");

                return lra.getId();
            } finally {
                AtomicAction.suspend();
            }
        }
    }

    public int endLRA(URL lraId, boolean compensate, boolean fromHierarchy) {
        lraTrace(lraId, "end LRA");

        Transaction transaction = getTransaction(lraId);

        if (!transaction.isRunning() && transaction.isTopLevel())
            return Response.Status.PRECONDITION_FAILED.getStatusCode();

//        AtomicAction.resume(transaction);

        int status = transaction.end(compensate);
        int sc = 500;

        if (!compensate) {
            if (status == ActionStatus.COMMITTED)
                sc = 200;
        } else if (status == ActionStatus.ABORTED) {
            sc = 200;
        }


        if (transaction.currentLRA() != null)
            System.out.printf("WARNING LRA %s ended but is still associated");

        finished(transaction, fromHierarchy, sc != 200);

        if (transaction.isTopLevel()) {
            // forget any nested LRAs
            transaction.forgetAllParticipants(); // instruct compensators to clean up
        }

        return sc;
    }

    public int leave(URL lraId, String compensatorUrl) {
        lraTrace(lraId, "leave LRA");

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

    public synchronized int joinLRA(StringBuilder recoveryUrl, URL lra, int timeLimit, String compensatorUrl, String linkHeader, String recoveryUrlBase) {
        if (lra ==  null)
            lraTrace(null, "Error missing LRA header in join request");

        lraTrace(lra, "join LRA");

        Transaction transaction = getTransaction(lra);

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
            coordinatorId = transaction.enlistParticipants(lra, linkHeader, recoveryUrlBase);
        else
            coordinatorId = transaction.enlistParticipant( lra, compensatorUrl, recoveryUrlBase);

        if (coordinatorId == null)
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        addCompensator(transaction, coordinatorId, compensatorUrl);

        recoveryUrl.append(transaction.getRecoveryUrl());

        return Response.Status.OK.getStatusCode();
    }

    public boolean hasTransaction(String id) {
        return transactions.containsKey(id);
    }

    private void lraTrace(URL lraId, String reason) {
        if (transactions.containsKey(lraId)) {
            Transaction lra = transactions.get(lraId);
            System.out.printf("%s (%s) in state %s: %s%n",
                    reason, lra.getClientId(), ActionStatus.stringForm(lra.status()), lra.getId());
        } else {
            System.out.printf("%s not found: %s%n", reason, lraId);
        }
    }
}
