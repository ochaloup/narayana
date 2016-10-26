/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
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
 * (C) 2007,
 * @author JBoss, a division of Red Hat.
 */
package com.hp.mwtests.ts.arjuna.abstractrecords;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.junit.Test;

import com.arjuna.ats.arjuna.coordinator.RecordType;
import com.arjuna.ats.arjuna.coordinator.TwoPhaseOutcome;
import com.arjuna.ats.internal.arjuna.abstractrecords.LastResourceRecord;
import com.arjuna.ats.internal.arjuna.abstractrecords.PersistenceRecord;

public class LastResourceUnitTest {
    @Test
    public void test() {
        LastResourceRecord cr = new LastResourceRecord(null); // force errors!

        assertFalse(cr.propagateOnAbort());
        assertFalse(cr.propagateOnCommit());
        assertEquals(cr.typeIs(), RecordType.LASTRESOURCE);

        assertTrue(cr.type() != null);
        assertFalse(cr.doSave());

        assertEquals(cr.nestedPrepare(), TwoPhaseOutcome.PREPARE_NOTOK);
        assertEquals(cr.nestedAbort(), TwoPhaseOutcome.FINISH_OK);

        cr = new LastResourceRecord(null);

        assertEquals(cr.nestedPrepare(), TwoPhaseOutcome.PREPARE_NOTOK);
        assertEquals(cr.nestedCommit(), TwoPhaseOutcome.FINISH_ERROR);

        cr = new LastResourceRecord(null);

        assertEquals(cr.topLevelPrepare(), TwoPhaseOutcome.PREPARE_NOTOK);
        assertEquals(cr.topLevelAbort(), TwoPhaseOutcome.FINISH_OK);

        cr = new LastResourceRecord(null);

        assertEquals(cr.topLevelPrepare(), TwoPhaseOutcome.PREPARE_NOTOK);
        assertEquals(cr.topLevelCommit(), TwoPhaseOutcome.FINISH_OK);

        cr = new LastResourceRecord(null);

        assertEquals(cr.topLevelPrepare(), TwoPhaseOutcome.PREPARE_NOTOK);
        assertEquals(cr.topLevelAbort(), TwoPhaseOutcome.FINISH_OK);

        cr.print(new PrintWriter(new ByteArrayOutputStream()));

        assertEquals(cr.value(), null);
        cr.setValue(null);

        assertFalse(cr.shouldAdd(new PersistenceRecord()));
        assertFalse(cr.shouldAlter(new PersistenceRecord()));
        assertFalse(cr.shouldMerge(new PersistenceRecord()));
        assertFalse(cr.shouldReplace(new PersistenceRecord()));

        cr = new LastResourceRecord();

        cr.merge(new PersistenceRecord());
        cr.alter(new PersistenceRecord());
    }
}
