/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.stm;

import org.jboss.stm.annotations.NotState;
import org.jboss.stm.annotations.Optimistic;
import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.Transactional;
import org.jboss.stm.annotations.WriteLock;

import com.arjuna.ats.arjuna.AtomicAction;

import junit.framework.TestCase;

/**
 * @author Mark Little
 */

public class TypicalExampleUnitTest extends TestCase {
    @Transactional
    public interface Sample {
        public void increment();
        public void decrement();

        public int value();
    }

    public class MyExample implements Sample {
        public MyExample() {
            this(0);
        }

        public MyExample(int init) {
            _isState = init;
        }

        @ReadLock
        public int value() {
            checkTransaction();

            return _isState;
        }

        @WriteLock
        public void increment() {
            checkTransaction();

            _isState++;
        }

        @WriteLock
        public void decrement() {
            checkTransaction();

            _isState--;
        }

        private void checkTransaction() {
            if (AtomicAction.Current() != null)
                _numberOfTransactions++;
        }

        private int _isState;

        @NotState
        public int _numberOfTransactions;
    }

    public void test() {
        MyExample ex = new MyExample(10);
        Container<Sample> theContainer = new Container<Sample>();
        Sample obj1 = theContainer.create(ex);
        Sample obj2 = theContainer.clone(new MyExample(), obj1);

        assertTrue(obj2 != null);

        AtomicAction act = new AtomicAction();

        act.begin();

        obj1.increment();

        act.commit();

        act = new AtomicAction();

        act.begin();

        assertEquals(obj2.value(), 11);

        act.commit();

        act = new AtomicAction();

        act.begin();

        for (int i = 0; i < 1000; i++) {
            obj1.increment();
        }

        act.abort();

        assertEquals(ex._numberOfTransactions, 1002);
    }
}
