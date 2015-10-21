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
package org.jboss.jbossts.qa.ArjunaCore.LockManager.client;

import com.arjuna.ats.arjuna.AtomicAction;
import org.jboss.jbossts.qa.ArjunaCore.LockManager.impl.TXBasicLockRecord;
import org.jboss.jbossts.qa.ArjunaCore.Utils.qautil;

public class Worker004 extends Thread
{
    public Worker004(int iterations, int resources, TXBasicLockRecord[] records, int id)
    {
        mMaxIteration = iterations;
        mNumberOfResources = resources;
        mLockRecordList = records;
        mId = id;
    }

    /**
     * The main method of the class that will perform the work.
     */
    public void run()
    {
        expectedValue = new int[mNumberOfResources];
        for (int j = 0; j < mNumberOfResources; j++)
        {
            expectedValue[j] = 0;
        }

        try
        {
            for (int j = 0; j < mNumberOfResources; j++)
            {
                for (int i = 0; i < mMaxIteration; i++)
                {
                    //start transaction
                    AtomicAction a = new AtomicAction();
                    a.begin();
                    int incValue = mLockRecordList[j].increase();
                    if (i % 2 == 0)
                    {
                        a.commit();
                        expectedValue[j] += incValue;
                    }
                    else
                    {
                        a.abort();
                    }
                }
            }

            for (int j = 0; j < mNumberOfResources; j++)
            {
                for (int i = 0; i < mMaxIteration; i++)
                {
                    AtomicAction b = new AtomicAction();
                    b.begin();
                    int incValue = mLockRecordList[j].increase();
                    if (i % 2 == 0)
                    {
                        b.commit();
                        expectedValue[j] += incValue;
                    }
                    else
                    {
                        b.abort();
                    }
                }
            }
        }
        catch (Exception e)
        {
            mCorrect = false;
            qautil.debug("exception in worker001: ", e);
        }
    }

    public boolean isCorrect()
    {
        return mCorrect;
    }

    public int[] getExpectedValues()
    {
        return expectedValue;
    }

    private TXBasicLockRecord[] mLockRecordList;
    private int mMaxIteration;
    private int[] expectedValue;
    private int mNumberOfResources;
    private boolean mCorrect = true;
    private int mId = 0;
}
