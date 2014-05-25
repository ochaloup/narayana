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
// Copyright (C) 2004
//
// Arjuna Technologies Ltd.,
// Newcastle upon Tyne,
// Tyne and Wear,
// UK.
//
// $Id: Client01.java,v 1.2 2004/03/29 10:34:39 rbegg Exp $
//

package org.jboss.jbossts.qa.JDBCLocals01;

import org.jboss.jbossts.qa.JDBCLocals01Impls.InfoTable;
import org.jboss.jbossts.qa.JDBCLocals01Impls.JDBCInfoTableImpl01;
import org.jboss.jbossts.qa.JDBCLocals01Impls.JDBCInfoTableImpl02;
import org.jboss.jbossts.qa.Utils.JDBCProfileStore;

public class Client01
{
    public static void main(String[] args)
    {
        try
        {
            String profileName = args[args.length - 1];

            int numberOfDrivers = JDBCProfileStore.numberOfDrivers(profileName);
            for (int index = 0; index < numberOfDrivers; index++)
            {
                String driver = JDBCProfileStore.driver(profileName, index);

                Class.forName(driver);
            }

            String databaseURL = JDBCProfileStore.databaseURL(profileName);
            String databaseUser = JDBCProfileStore.databaseUser(profileName);
            String databasePassword = JDBCProfileStore.databasePassword(profileName);
            String databaseDynamicClass = JDBCProfileStore.databaseDynamicClass(profileName);
            int databaseTimeout = JDBCProfileStore.timeout(profileName);

            InfoTable infoTable = null;
            boolean tableTwo = false;

            for (int i = 0; i < args.length; i++)
            {
                if (args[i].equals("-table2"))
                {
                    tableTwo = true;
                }
            }

            if (!tableTwo)
            {
                infoTable = new JDBCInfoTableImpl01(databaseURL, databaseUser, databasePassword, databaseDynamicClass, databaseTimeout);
            }
            else
            {
                infoTable = new JDBCInfoTableImpl02(databaseURL, databaseUser, databasePassword, databaseDynamicClass, databaseTimeout);
            }

            boolean correct = true;

            for (int index = 0; index < 10; index++)
            {
                String name = "Name_" + index;
                String value = "Value_" + index;

                infoTable.insert(name, value);
            }

            for (int index = 0; correct && (index < 10); index++)
            {
                String name = "Name_" + index;
                String value = "Value_" + index;
                String newValue = infoTable.select(name);

                correct = correct && value.equals(newValue);
            }

            if (correct)
            {
                System.out.println("Passed");
            }
            else
            {
                System.out.println("Failed");
            }
        }
        catch (Exception exception)
        {
            System.out.println("Failed");
            System.err.println("Client01.main: " + exception);
            exception.printStackTrace(System.err);
        }
    }
}
