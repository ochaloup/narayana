package org.jboss.narayana.rts.lra.coordinator.domain.service;

import org.jboss.narayana.rts.lra.coordinator.domain.model.LRAStatus;
import org.jboss.narayana.rts.lra.coordinator.domain.model.Transaction;

import javax.enterprise.context.ApplicationScoped;
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
}
