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

import java.util.Vector;

import javax.transaction.xa.Xid;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.RecoveryModuleMarkerCompletedWithoutError;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter;
import com.arjuna.ats.jta.recovery.XAResourceOrphanFilter;
import com.arjuna.ats.jts.extensions.Arjuna;

/**
 * An XAResourceOrphanFilter which vetos rollback for xids which have an in-flight subordinate transaction.
 * <p>
 * The SubordinateAtomicActionRecoveryModule must be loaded and in a position prior to the XARecoveryModule within the list
 * of recovery modules for this to work so we verify that during orphan detection.
 */
public class JTSSubordinationManagerXAResourceOrphanFilter extends SubordinationManagerXAResourceOrphanFilter implements XAResourceOrphanFilter {
    private RecoveryModuleMarkerCompletedWithoutError jcaServerTransactionRecoveryModule;

    @Override
    public Vote checkXid(Xid xid)
    {
        if(xid == null || xid.getFormatId() != Arjuna.XID()) {
            return Vote.ABSTAIN;
        }
        return super.checkXid(xid);
    }

    /**
     * {@inheritDoc}
     */
    protected RecoveryModuleMarkerCompletedWithoutError getSubordinateAtomicActionRecoveryModule() {
        if (this.jcaServerTransactionRecoveryModule == null) {
            Vector<RecoveryModule> modules = RecoveryManager.manager().getModules();
            for (RecoveryModule module : modules) {
                if (module.getClass().equals(JCAServerTransactionRecoveryModule.class)) {
                    this.jcaServerTransactionRecoveryModule = (JCAServerTransactionRecoveryModule) module;
                }
            }
        }
        return this.jcaServerTransactionRecoveryModule;
    }
}
