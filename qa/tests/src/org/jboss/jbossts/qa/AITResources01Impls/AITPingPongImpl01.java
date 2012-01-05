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
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
//
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003
//
// Arjuna Technologies Ltd.,
// Newcastle upon Tyne,
// Tyne and Wear,
// UK.
//
// $Id: AITPingPongImpl01.java,v 1.2 2003/06/26 11:43:08 rbegg Exp $
//

package org.jboss.jbossts.qa.AITResources01Impls;

/*
 * Copyright (C) 1999-2001 by HP Bluestone Software, Inc. All rights Reserved.
 *
 * HP Arjuna Labs,
 * Newcastle upon Tyne,
 * Tyne and Wear,
 * UK.
 *
 * $Id: AITPingPongImpl01.java,v 1.2 2003/06/26 11:43:08 rbegg Exp $
 */

/*
 * Try to get around the differences between Ansi CPP and
 * K&R cpp with concatenation.
 */

/*
 * Copyright (C) 1999-2001 by HP Bluestone Software, Inc. All rights Reserved.
 *
 * HP Arjuna Labs,
 * Newcastle upon Tyne,
 * Tyne and Wear,
 * UK.
 *
 * $Id: AITPingPongImpl01.java,v 1.2 2003/06/26 11:43:08 rbegg Exp $
 */

import com.arjuna.ats.arjuna.ObjectType;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import com.arjuna.ats.jts.extensions.AtomicTransaction;
import com.arjuna.ats.txoj.Lock;
import com.arjuna.ats.txoj.LockManager;
import com.arjuna.ats.txoj.LockMode;
import com.arjuna.ats.txoj.LockResult;
import org.jboss.jbossts.qa.AITResources01.*;
import org.jboss.jbossts.qa.Utils.JVMStats;
import org.omg.CORBA.IntHolder;
import org.omg.CosTransactions.Status;

public class AITPingPongImpl01 extends LockManager implements PingPongOperations {
    public AITPingPongImpl01() throws InvocationException {
        super(ObjectType.ANDPERSISTENT);

        _value = 0;

        try {
            AtomicTransaction atomicTransaction = new AtomicTransaction();

            atomicTransaction.begin();

            if (setlock(new Lock(LockMode.WRITE), 0) == LockResult.GRANTED) {
                atomicTransaction.commit(true);
            } else {
                System.err.println("AITPingPongImpl01.AITPingPongImpl01: failed to get lock");
                atomicTransaction.rollback();
            }
        } catch (Exception exception) {
            System.err.println("AITPingPongImpl01.AITPingPongImpl01: " + exception);
            throw new InvocationException();
        } catch (Error error) {
            System.err.println("AITPingPongImpl01.AITPingPongImpl01: " + error);
            throw new InvocationException();
        }
    }

    public AITPingPongImpl01(Uid uid) throws InvocationException {
        super(uid);
    }

    public void finalize() throws Throwable {
        try {
            super.terminate();
            super.finalize();
        } catch (Exception exception) {
            System.err.println("AITPingPongImpl01.finalize: " + exception);
            throw exception;
        } catch (Error error) {
            System.err.println("AITPingPongImpl01.finalize: " + error);
            throw error;
        }
    }

    public void hit(int count, PingPong ponger, PingPong pinger) throws InvocationException {
        try {
            AtomicTransaction atomicTransaction = new AtomicTransaction();

            try {
                atomicTransaction.begin();

                if (count != 0) {
                    ponger.hit(count - 1, pinger, ponger);
                    atomicTransaction.commit(true);
                } else if (setlock(new Lock(LockMode.WRITE), 0) == LockResult.GRANTED) {
                    _value++;
                    atomicTransaction.commit(true);
                } else {
                    System.err.println("AITPingPongImpl01.hit: failed to get lock");
                    atomicTransaction.rollback();

                    throw new InvocationException();
                }
            } catch (InvocationException invocationException) {
                throw invocationException;
            } catch (Exception exception) {
                System.err.println("AITPingPongImpl01.hit: " + exception);
                if (atomicTransaction.get_status() == Status.StatusActive) {
                    atomicTransaction.rollback();
                }

                throw new InvocationException();
            }
        } catch (InvocationException invocationException) {
            throw invocationException;
        } catch (Exception exception) {
            System.err.println("AITPingPongImpl01.hit: " + exception);
            throw new InvocationException();
        } catch (Error error) {
            System.err.println("AITPingPongImpl01.hit: " + error);
            throw new InvocationException();
        }
    }

    public void bad_hit(int count, int bad_count, PingPong ponger, PingPong pinger) throws InvocationException {
        try {
            AtomicTransaction atomicTransaction = new AtomicTransaction();

            try {
                atomicTransaction.begin();

                if (count != 0) {
                    ponger.bad_hit(count - 1, bad_count - 1, pinger, ponger);
                    if (bad_count != 0) {
                        atomicTransaction.commit(true);
                    } else {
                        atomicTransaction.rollback();
                    }
                } else if (setlock(new Lock(LockMode.WRITE), 0) == LockResult.GRANTED) {
                    _value++;
                    if (bad_count != 0) {
                        atomicTransaction.commit(true);
                    } else {
                        atomicTransaction.rollback();
                    }
                } else {
                    System.err.println("AITPingPongImpl01.bad_hit: failed to get lock");
                    atomicTransaction.rollback();

                    throw new InvocationException();
                }
            } catch (InvocationException invocationException) {
                throw invocationException;
            } catch (Exception exception) {
                System.err.println("AITPingPongImpl01.bad_hit: " + exception);
                if (atomicTransaction.get_status() == Status.StatusActive) {
                    atomicTransaction.rollback();
                }

                throw new InvocationException();
            }
        } catch (InvocationException invocationException) {
            throw invocationException;
        } catch (Exception exception) {
            System.err.println("AITPingPongImpl01.bad_hit: " + exception);
            throw new InvocationException();
        } catch (Error error) {
            System.err.println("AITPingPongImpl01.bad_hit: " + error);
            throw new InvocationException();
        }
    }

    public void get(IntHolder value) throws InvocationException {
        try {
            AtomicTransaction atomicTransaction = new AtomicTransaction();

            try {
                atomicTransaction.begin();

                if (setlock(new Lock(LockMode.READ), 0) == LockResult.GRANTED) {
                    value.value = _value;
                    atomicTransaction.commit(true);
                } else {
                    System.err.println("AITPingPongImpl01.get: failed to get lock");
                    atomicTransaction.rollback();

                    throw new InvocationException();
                }
            } catch (InvocationException invocationException) {
                throw invocationException;
            } catch (Exception exception) {
                System.err.println("AITPingPongImpl01.get: " + exception);
                if (atomicTransaction.get_status() == Status.StatusActive) {
                    atomicTransaction.rollback();
                }

                throw new InvocationException();
            }
        } catch (InvocationException invocationException) {
            throw invocationException;
        } catch (Exception exception) {
            System.err.println("AITPingPongImpl01.get: " + exception);
            throw new InvocationException();
        } catch (Error error) {
            System.err.println("AITPingPongImpl01.get: " + error);
            throw new InvocationException();
        }
    }

    public int getMemory() {
        return (int) JVMStats.getMemory();
    }

    public boolean save_state(OutputObjectState objectState, int objectType) {
        super.save_state(objectState, objectType);
        try {
            objectState.packInt(_value);
            return true;
        } catch (Exception exception) {
            System.err.println("AITPingPongImpl01.save_state: " + exception);
            return false;
        } catch (Error error) {
            System.err.println("AITPingPongImpl01.save_state: " + error);
            return false;
        }
    }

    public boolean restore_state(InputObjectState objectState, int objectType) {
        super.restore_state(objectState, objectType);
        try {
            _value = objectState.unpackInt();
            return true;
        } catch (Exception exception) {
            System.err.println("AITPingPongImpl01.restore_state: " + exception);
            return false;
        } catch (Error error) {
            System.err.println("AITPingPongImpl01.restore_state: " + error);
            return false;
        }
    }

    public String type() {
        return "/StateManager/LockManager/AITPingPongImpl01";
    }

    private int _value;
}
