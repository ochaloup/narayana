/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates,
 * and individual contributors as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2010,
 * @author JBoss, by Red Hat.
 */
package com.arjuna.ats.internal.jta.recovery.arjunacore;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import javax.transaction.xa.Xid;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.objectstore.RecoveryStore;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.internal.arjuna.common.UidHelper;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.ats.jta.logging.jtaLogger;
import com.arjuna.ats.jta.recovery.XAResourceOrphanFilter.Vote;
import com.arjuna.ats.jta.utils.XAHelper;
import com.arjuna.ats.jta.xa.XATxConverter;
import com.arjuna.ats.jta.xa.XidImple;

/**
 * An XAResourceOrphanFilter which uses detects orphaned subordinate XA Resources.
 * Needs to be used in variant of checking JTA or JTS transactions (based on the type supplier)
 */
public class SubordinateXAResourceOrphanChecker {
	public static final int RECOVER_ALL_NODES = 0;

	@FunctionalInterface
	public interface XidProvider {
	    XidImple get(Uid uid) throws Exception;
	}

	public Vote checkXid(Xid xid, Supplier<String> transactionTypeSupplier, XidProvider xidImpleProvider) {

		List<String> _xaRecoveryNodes = jtaPropertyManager.getJTAEnvironmentBean().getXaRecoveryNodes();

		if(_xaRecoveryNodes == null || _xaRecoveryNodes.size() == 0) {
			jtaLogger.i18NLogger.info_recovery_noxanodes();
			return Vote.ABSTAIN;
		}

		String subordinateNodeName = XATxConverter.getSubordinateNodeName(new XidImple(xid).getXID());

		if (jtaLogger.logger.isDebugEnabled()) {
			jtaLogger.logger.debug("subordinate node name of " + xid + " is " + subordinateNodeName);
		}

		if (!_xaRecoveryNodes.contains(subordinateNodeName)) {
			// It either doesn't have a subordinate node name or isn't for this server
			return Vote.ABSTAIN;
		}

		// It does have an XID
		if (subordinateNodeName != null) {
			if (transactionLog(xid, subordinateNodeName, transactionTypeSupplier.get(), xidImpleProvider)) {
				// it's owned by a logged transaction which will recover it top down in due course
				return Vote.ABSTAIN;
			} else {
				return Vote.ROLLBACK;
			}
		} else {
			return Vote.ABSTAIN;
		}
	}

	/**
	 * Is there a log file for this transaction?
	 * 
	 * @param recoveredResourceXid
	 *		the transaction to check.
	 * 
	 * @return <code>boolean</code>true if there is a log file,
	 *		<code>false</code> if there isn't.
	 */
	private boolean transactionLog(Xid recoveredResourceXid, String recoveredResourceNodeName,
	        String transactionType, XidProvider xidImpleProvider) {

		XidImple theXid = new XidImple(recoveredResourceXid);
		Uid u = theXid.getTransactionUid();
		RecoveryStore recoveryStore = StoreManager.getRecoveryStore();

		if (jtaLogger.logger.isDebugEnabled()) {
			jtaLogger.logger.debugf("Checking whether Xid %s exists in ObjectStore %s", theXid, recoveryStore);
		}

		if (!u.equals(Uid.nullUid())) {

			if (jtaLogger.logger.isDebugEnabled()) {
				jtaLogger.logger.debugf("Looking for %s of type %s", u, transactionType);
			}

			InputObjectState states = new InputObjectState();
			try {
				if (recoveryStore.allObjUids(transactionType, states) && (states.notempty())) {
					boolean finished = false;

					do {
						Uid uid = null;

						try {
							uid = UidHelper.unpackFrom(states);
						} catch (IOException ex) {
							jtaLogger.i18NLogger.warn_unpacking_xid_state(theXid, recoveryStore, transactionType, ex);

							finished = true;
						}

						if (uid.notEquals(Uid.nullUid())) {
							XidImple transactionXid = xidImpleProvider.get(uid);
							if (transactionXid != null && transactionXid.isSameTransaction(recoveredResourceXid)) {
								if (jtaLogger.logger.isDebugEnabled()) {
									jtaLogger.logger.debugf("Found record for %s of uid %s", theXid, uid);
								}
								return true;
							}
						} else
							finished = true;

					} while (!finished);
					if (jtaLogger.logger.isDebugEnabled()) {
						jtaLogger.logger.debugf("No record found for %s", theXid);
					}
				} else {
					jtaLogger.i18NLogger.info_recovery_notaxid(XAHelper.xidToString(recoveredResourceXid));
				}
			} catch (Exception e) {
				jtaLogger.i18NLogger.warn_reading_from_object_store(recoveryStore, theXid, e);
			}
		}
		return false;
	}
}
