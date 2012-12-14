/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.narayana.txframework.impl.handlers.wsba;

import com.arjuna.mw.wst11.BusinessActivityManager;
import com.arjuna.mw.wst11.BusinessActivityManagerFactory;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.UnknownTransactionException;
import com.arjuna.wst.WrongStateException;
import com.arjuna.wst11.BAParticipantManager;
import org.jboss.narayana.txframework.api.exception.TXFrameworkException;
import org.jboss.narayana.txframework.impl.Participant;
import org.jboss.narayana.txframework.impl.ServiceInvocationMeta;
import org.jboss.narayana.txframework.impl.handlers.ParticipantRegistrationException;

import java.util.HashMap;
import java.util.UUID;

public class WSBAParticipantCompletionHandler extends WSBAHandler {

    public WSBAParticipantCompletionHandler(ServiceInvocationMeta serviceInvocationMeta) throws TXFrameworkException {

        super(serviceInvocationMeta);
    }

    @Override
    protected BAParticipantManager registerParticipants(ServiceInvocationMeta serviceInvocationMeta)
            throws ParticipantRegistrationException {

        try {
            BAParticipantManager baParticipantManager = null;

            synchronized (participantRegistry) {
                BusinessActivityManager businessActivityManager = BusinessActivityManagerFactory
                        .businessActivityManager();
                String txid = businessActivityManager.currentTransaction().toString();

                Participant participantToResume;

                // Only create participant if there is not already a participant
                // for this ServiceImpl and this transaction
                if (!participantRegistry.isRegistered(txid, serviceInvocationMeta.getServiceClass())) {

                    WSBAParticipantCompletionParticipant participantCompletionParticipant = new WSBAParticipantCompletionParticipant(
                            serviceInvocationMeta, new HashMap(), txid);
                    baParticipantManager = businessActivityManager.enlistForBusinessAgreementWithParticipantCompletion(
                            participantCompletionParticipant,
                            serviceInvocationMeta.getServiceClass().getName() + UUID.randomUUID());
                    participantRegistry.register(txid, serviceInvocationMeta.getServiceClass(), baParticipantManager);

                    synchronized (durableServiceParticipants) {
                        participantToResume = participantCompletionParticipant;
                        durableServiceParticipants.put(txid, participantToResume);
                    }
                } else {
                    baParticipantManager = participantRegistry.lookupBAParticipantManager(txid,
                            serviceInvocationMeta.getServiceClass());
                    participantToResume = durableServiceParticipants.get(txid);
                }
                participantToResume.resume();
            }

            return baParticipantManager;
        } catch (WrongStateException e) {
            throw new ParticipantRegistrationException(
                    "Transaction was not in a state in which participants can be registered", e);
        } catch (UnknownTransactionException e) {
            throw new ParticipantRegistrationException("Can't register a participant as the transaction in unknown", e);
        } catch (SystemException e) {
            throw new ParticipantRegistrationException(
                    "A SystemException occurred when attempting to register a participant", e);
        }
    }
}
