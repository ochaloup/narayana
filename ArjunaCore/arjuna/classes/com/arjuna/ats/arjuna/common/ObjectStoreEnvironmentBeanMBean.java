/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
 * (C) 2009,
 * @author JBoss, a division of Red Hat.
 */
package com.arjuna.ats.arjuna.common;

/**
 * A JMX MBean interface containing configuration for the objectstore and
 * various implementations thereof.
 *
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public interface ObjectStoreEnvironmentBeanMBean {
    int getCacheStoreSize();

    boolean isCacheStoreSync();

    int getCacheStoreRemovedItems();

    int getCacheStoreScanPeriod();

    int getCacheStoreWorkItems();

    int getCacheStoreHash();

    String getLocalOSRoot();

    String getObjectStoreDir();

    boolean isObjectStoreSync();

    String getObjectStoreType();

    int getHashedDirectories();

    boolean isTransactionSync();

    int getShare();

    int getHierarchyRetry();

    int getHierarchyTimeout();

    boolean isSynchronousRemoval();

    long getTxLogSize();

    long getPurgeTime();
}
