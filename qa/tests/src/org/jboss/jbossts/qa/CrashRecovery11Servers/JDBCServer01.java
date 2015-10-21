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
// UK
//

package org.jboss.jbossts.qa.CrashRecovery11Servers;

/*
 * Copyright (C) 1999-2001 by HP Bluestone Software, Inc. All rights Reserved.
 *
 * HP Arjuna Labs,
 * Newcastle upon Tyne,
 * Tyne and Wear,
 * UK.
 *
 * $Id: JDBCServer01.java,v 1.2 2003/06/26 11:43:50 rbegg Exp $
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
 * $Id: JDBCServer01.java,v 1.2 2003/06/26 11:43:50 rbegg Exp $
 */


import org.jboss.jbossts.qa.CrashRecovery11.*;
import org.jboss.jbossts.qa.CrashRecovery11Impls.JDBCServiceImpl01;
import org.jboss.jbossts.qa.Utils.JDBCProfileStore;
import org.jboss.jbossts.qa.Utils.OAInterface;
import org.jboss.jbossts.qa.Utils.ORBInterface;
import org.jboss.jbossts.qa.Utils.ServerIORStore;

public class JDBCServer01
{
    public static void main(String args[])
    {
        try
        {
            ORBInterface.initORB(args, null);
            OAInterface.initOA();

            String profileName = args[args.length - 3];

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

            JDBCServiceImpl01 jdbcServiceImpl = new JDBCServiceImpl01(args[args.length - 2], databaseURL, databaseUser, databasePassword, databaseDynamicClass);
            BeforeCrashServicePOATie servant = new BeforeCrashServicePOATie(jdbcServiceImpl);

            OAInterface.objectIsReady(servant);
            BeforeCrashService service = BeforeCrashServiceHelper.narrow(OAInterface.corbaReference(servant));

            ServerIORStore.storeIOR(args[args.length - 1], ORBInterface.orb().object_to_string(service));

            System.out.println("Ready");

            ORBInterface.run();
        }
        catch (Exception exception)
        {
            System.err.println("JDBCServer01.main: " + exception);
            exception.printStackTrace(System.err);
        }
    }
}

