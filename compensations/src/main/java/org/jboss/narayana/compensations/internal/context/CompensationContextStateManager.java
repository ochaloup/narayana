/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.narayana.compensations.internal.context;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import org.jboss.logging.Logger;
import org.jboss.narayana.compensations.internal.recovery.DeserializerHelper;
import org.jboss.narayana.compensations.internal.utils.RecoveryHelper;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager handling all compensation context states.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class CompensationContextStateManager {

    private static final Logger LOGGER = Logger.getLogger(CompensationContextStateManager.class);

    private static final CompensationContextStateManager INSTANCE = new CompensationContextStateManager(
            new RecoveryHelper(StoreManager.getRecoveryStore(), CompensationContextState.getRecordType()),
            new DeserializerHelper());

    /**
     * Ids of all the registered context states. It was introduced to make
     * filtering during the recovery more efficient, because only new states are
     * added during the recovery (see {@code restore} method).
     */
    private final Set<Uid> stateIds = ConcurrentHashMap.newKeySet();

    /**
     * Contexts container indexed with transaction id to which context is
     * attached.
     */
    private final Map<String, CompensationContextState> states = new ConcurrentHashMap<>();

    /**
     * Context state attached to the current thread.
     */
    private final ThreadLocal<CompensationContextState> currentState = new ThreadLocal<>();

    /**
     * Utility to make it easier to persist/recover resources to/from recovery
     * store.
     */
    private final RecoveryHelper recoveryHelper;

    /**
     * Utility to make it easier to deserialize resources using user registered
     * deserializers.
     */
    private final DeserializerHelper deserializerHelper;

    /**
     * @param recoveryHelper
     *            recover helper to use for persistence and recovery.
     * @param deserializerHelper
     *            deserializer helper to use when recreating resources during
     *            the recovery.
     */
    CompensationContextStateManager(RecoveryHelper recoveryHelper, DeserializerHelper deserializerHelper) {
        Objects.requireNonNull(recoveryHelper, "Recovery helper cannot be null");
        Objects.requireNonNull(deserializerHelper, "Deserializer helper cannot be null");
        this.recoveryHelper = recoveryHelper;
        this.deserializerHelper = deserializerHelper;
    }

    /**
     * Get a singleton instance of {@link CompensationContextStateManager}.
     * 
     * @return
     */
    public static CompensationContextStateManager getInstance() {
        return INSTANCE;
    }

    /**
     * Attaches compensation context state to the current thread. If the state
     * for the specified transaction id doesn't exit a new one is created.
     * 
     * @param transactionId
     *            {@code String} id of the transaction to which state is
     *            associated.
     */
    public void activate(String transactionId) {
        Objects.requireNonNull(transactionId, "Transaction id cannot be null");
        if (!states.containsKey(transactionId)) {
            newState(transactionId);
        }

        currentState.set(states.get(transactionId));
        LOGGER.tracef("activated context for transaction '%s'", transactionId);
    }

    /**
     * Detaches compensation context state from the current thread.
     */
    public void deactivate() {
        currentState.remove();
    }

    /**
     * @return true if compensation context state is attached to the current
     *         thread and false otherwise.
     */
    public boolean isActive() {
        return currentState.get() != null;
    }

    /**
     * Get compensation context state of the specific transaction.
     *
     * @param transactionId
     *            String id of the transaction to which state is associated.
     * @return {@link Optional} containing {@link CompensationContextState}
     *         transaction context was found, or an empty {@link Optional} if
     *         context wasn't found.
     */
    public Optional<CompensationContextState> get(String transactionId) {
        Objects.requireNonNull(transactionId, "Transaction id cannot be null");
        return Optional.ofNullable(states.get(transactionId));
    }

    /**
     * Get current compensation context state.
     *
     * @return
     * @throws IllegalStateException
     *             if context is not active.
     */
    public CompensationContextState getCurrent() {
        if (!isActive()) {
            throw new IllegalStateException("Context is not active");
        }
        return currentState.get();
    }

    /**
     * Persist compensation context state to the recovery store.
     *
     * If the requested context exists it is serialized to the newly created
     * {@link OutputObjectState} and persisted using {@code recoveryHelper}.
     *
     * If persistence fails, only a warning message is logged.
     * 
     * @param transactionId
     *            String id of the transaction to which state is associated.
     */
    public void persist(String transactionId) {
        Objects.requireNonNull(transactionId, "Transaction id cannot be null");
        get(transactionId).ifPresent(state -> {
            OutputObjectState output = new OutputObjectState(state.getId(), CompensationContextState.getRecordType());
            if (state.persist(output)) {
                recoveryHelper.writeRecord(output,
                        e -> LOGGER.warnf(e, "Failed to persist compensation context state '%s' for transaction '%s'",
                                state.getId(), transactionId));
            } else {
                LOGGER.warnf("Failed to persist compensation context state '%s' for transaction '%s'", state.getId(),
                        transactionId);
            }
        });
    }

    /**
     * Remove compensation context state.
     *
     * If requested context exists, it is removed from the contexts container as
     * well as from the recovery store.
     *
     * @param transactionId
     *            String id of the transaction to which state is associated.
     */
    public void remove(String transactionId) {
        Objects.requireNonNull(transactionId, "Transaction id cannot be null");
        get(transactionId).ifPresent(state -> {
            states.remove(transactionId);
            stateIds.remove(state.getId());
            recoveryHelper.removeRecord(state.getId(),
                    e -> LOGGER.warnf(e, "Failed to remove compensation context state '%s' for transaction '%s'",
                            state.getId(), transactionId));
        });
    }

    /**
     * Restores compensation context states from the recovery store. If state
     * found in the recovery store already exists - it is ignored.
     */
    public void restore() {
        recoveryHelper.getAllRecords(e -> LOGGER.warnf(e, "Failed to restore compensation context state")).stream()
                .filter(record -> !stateIds.contains(record.stateUid())).forEach(this::restoreState);
    }

    /**
     * Initiate new compensation context state and add it to the contexts
     * container.
     *
     * @param transactionId
     *            transaction id to which context is associated.
     */
    private void newState(String transactionId) {
        CompensationContextState state = new CompensationContextState(new Uid(), transactionId, deserializerHelper);
        states.put(transactionId, state);
        stateIds.add(state.getId());
    }

    /**
     * Restore compensation context state from the {@link InputObjectState}.
     *
     * If deserialization is successful, context is added to the contexts
     * container.
     *
     * If deserialization fails, only a warning message is logged.
     *
     * @param record
     *            record to deserialize context state from.
     */
    private void restoreState(InputObjectState record) {
        CompensationContextState state = new CompensationContextState(deserializerHelper);
        if (state.restore(record)) {
            states.put(state.getTransactionId(), state);
            stateIds.add(state.getId());
        } else {
            LOGGER.warnf("Failed to restore compensation context state '%s'", state.getId());
        }
    }

}
