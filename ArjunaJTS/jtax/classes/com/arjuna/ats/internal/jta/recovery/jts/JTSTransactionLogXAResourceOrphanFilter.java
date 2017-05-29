/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package com.arjuna.ats.internal.jta.recovery.jts;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.transaction.xa.Xid;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.RecoveryStore;
import com.arjuna.ats.arjuna.objectstore.StateStatus;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.transaction.arjunacore.AtomicAction;
import com.arjuna.ats.internal.jta.utils.jtaxLogger;
import com.arjuna.ats.internal.jta.utils.jts.XidUtils;
import com.arjuna.ats.jta.recovery.XAResourceOrphanFilter;
import com.arjuna.ats.jta.xa.XidImple;
import com.arjuna.ats.jts.extensions.Arjuna;

/**
 * <p>
 * An XAResourceOrphanFilter which vetos rollback for xids owned by top level JTS transactions.
 * <p>
 * Inspired at {@link JTATransactionLogXAResourceOrphanFilter}.
 */
public class JTSTransactionLogXAResourceOrphanFilter implements XAResourceOrphanFilter {
    @Override
    public Vote checkXid(Xid xid) {
        if (xid.getFormatId() != Arjuna.XID()) {
            // we only care about Xids created by the JTS
            return Vote.ABSTAIN;
        }

        try {
            if (isLogRecordForTheXid(xid)) {
                // it's owned by a logged transaction which
                // will recover it top down in due course
                return Vote.LEAVE_ALONE;
            }
        } catch (ObjectStoreException | IOException e) {
            jtaxLogger.i18NLogger.warn_could_not_access_object_store(xid, e);
            // we don't know what the state of the parent transaction is so
            // leave it alone
            return Vote.LEAVE_ALONE;
        }

        return Vote.ABSTAIN;
    }

    private boolean isLogRecordForTheXid(Xid xid) throws ObjectStoreException, IOException {
        RecoveryStore recoveryStore = StoreManager.getRecoveryStore();
        List<String> transactionTypesToCheck = Arrays.asList(new String[] {
            new AtomicAction().type(),
            com.arjuna.ats.internal.jts.orbspecific.coordinator.ArjunaTransactionImple.typeName(),
            com.arjuna.ats.internal.jta.transaction.jts.subordinate.jca.coordinator.ServerTransaction.getType(),
            com.arjuna.ats.internal.jts.orbspecific.interposition.coordinator.ServerTransaction.typeName()
        });

        XidImple theXid = new XidImple(xid);
        Uid u = theXid.getTransactionUid();

        if (jtaxLogger.logger.isDebugEnabled())
            jtaxLogger.logger.debugf("%s, checking whether xid '%s' exists in object store '%s'",
                JTSTransactionLogXAResourceOrphanFilter.class.getName(), theXid, recoveryStore);

        if (!u.equals(Uid.nullUid())) {
            if (jtaxLogger.logger.isDebugEnabled())
                jtaxLogger.logger.debugf("Looking for uid '%s' of transaction types %s", u, transactionTypesToCheck);

            boolean isFoundRecord = false;
            for(String transactionType: transactionTypesToCheck) {
                if(recoveryStore.currentState(u, transactionType) != StateStatus.OS_UNKNOWN) {
                    isFoundRecord = true;
                    break;
                }
            }
            if (isFoundRecord) {
                if (jtaxLogger.logger.isDebugEnabled())
                    jtaxLogger.logger.debugf("Found record for xid: %s", theXid);

                return true;
            } else {
                if (jtaxLogger.logger.isDebugEnabled())
                    jtaxLogger.logger.debugf("No record found for xid: %s", theXid);
            }
        }

        return false;
    }
}
