/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

import javax.transaction.xa.Xid;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateXAResourceOrphanChecker;
import com.arjuna.ats.internal.jta.transaction.jts.subordinate.jca.coordinator.ServerTransaction;
import com.arjuna.ats.jta.recovery.XAResourceOrphanFilter;
import com.arjuna.ats.jta.xa.XidImple;
import com.arjuna.ats.jts.extensions.Arjuna;

/**
 * An XAResourceOrphanFilter which uses detects orphaned subordinate XA Resources for JTS.
 */
public class SubordinateJTSXAResourceOrphanFilter implements XAResourceOrphanFilter {
    private SubordinateXAResourceOrphanChecker subordinateOrphanFilterImple = new SubordinateXAResourceOrphanChecker();

	@Override
	public Vote checkXid(Xid xid) {

		if(xid == null || xid.getFormatId() != Arjuna.XID()) {
			// we only care about Xids created by the JTS
			return Vote.ABSTAIN;
		}

	      return subordinateOrphanFilterImple.checkXid(xid, () -> ServerTransaction.getType(),
	              (Uid uid) -> (XidImple) (new ServerTransaction(uid).getXid()));
	}
}
