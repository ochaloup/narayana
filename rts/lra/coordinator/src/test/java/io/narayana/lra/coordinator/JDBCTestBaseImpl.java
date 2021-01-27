/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package io.narayana.lra.coordinator;

import io.narayana.lra.LRAData;
import io.narayana.lra.logging.LRALogger;

import java.util.ArrayList;
import java.util.List;

public class JDBCTestBaseImpl extends AbstractTestBase {


    private static final String SERVER_CONFIG_FILE = "standalone-jdbc.xml";

    @Override
    void clearRecoveryLog() {

        List<LRAData> LRAList = new ArrayList<>();

        try {
        LRAList = lraClient.getAllLRAs();
        } catch (Exception ex) {
         // transaction logs will only exists after there has been a previous run
            LRALogger.logger.debugf(ex,"Cannot fetch LRAs through the client");
        }

        for (LRAData lra : LRAList) {
            lraClient.closeLRA(lra.getLraId());
        }
    }

    @Override
    String getFirstLRA() {

        List<LRAData> LRAList = new ArrayList<>();

        try {
        LRAList = lraClient.getAllLRAs();
        } catch (Exception ex) {
         // transaction logs will only exists after there has been a previous run
            LRALogger.logger.debugf(ex,"Cannot fetch LRAs through the client");
        }

        return (LRAList.isEmpty() ? null : LRAList.get(0).getLraIdAsString());

    }

    @Override
    String getConfigFilename() {
        return SERVER_CONFIG_FILE;
    }

    @Override
    void startContainer(String bytemanScript) {

        super.startContainer(bytemanScript);

        // Deletes old transactions in the H2 database
        clearRecoveryLog();
    }

}