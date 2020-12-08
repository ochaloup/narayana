/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package io.narayana.lra;

import org.eclipse.microprofile.lra.annotation.LRAStatus;

import java.beans.Transient;
import java.net.URI;

/**
 * DTO object which serves to transfer data of particular LRA instance.
 * It's used by {@code io.narayana.lra.coordinator.api.Coordinator}
 * for JSON response creation when LRA info is asked for.
 */
public class LRAData {
    private URI lraId;
    private String clientId;
    private LRAStatus status;
    private boolean isTopLevel;
    private boolean isRecovering;
    private long startTime;
    private long finishTime;
    private int httpStatus;

    public LRAData() {}

    public LRAData(URI lraId, String clientId, LRAStatus status, boolean isTopLevel, boolean isRecovering,
                    long startTime, long finishTime, int httpStatus) {
        this.lraId = lraId;
        this.clientId = clientId;
        this.status = status;
        this.isTopLevel = isTopLevel;
        this.isRecovering = isRecovering;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.httpStatus = httpStatus;
    }

    public URI getLraId() {
        return this.lraId;
    }

    public LRAData setLraId(URI lraId) {
        this.lraId = lraId;
        return this;
    }

    @Transient
    public String getLraIdAsString() {
        return this.lraId.toASCIIString();
    }

    public String getClientId() {
        return this.clientId;
    }

    public LRAData setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public LRAStatus getStatus() {
        return this.status;
    }

    public LRAData setStatus(LRAStatus status) {
        this.status = status;
        return this;
    }

    public boolean isTopLevel() {
        return this.isTopLevel;
    }

    public LRAData setTopLevel(boolean topLevel) {
        isTopLevel = topLevel;
        return this;
    }

    public boolean isRecovering() {
        return this.isRecovering;
    }

    public LRAData setRecovering(boolean recovering) {
        isRecovering = recovering;
        return this;
    }

    public long getStartTime() {
        return startTime;
    }

    public LRAData setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public LRAData setFinishTime(long finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }

    public LRAData setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof LRAData)) {
            return false;
        } else {
            LRAData lraData = (LRAData) o;
            return this.getLraId().equals(lraData.getLraId());
        }
    }

    public int hashCode() {
        return this.getLraId().hashCode();
    }

    @Override
    public String toString() {
        return String.format(
            "%s {lraId='%s', clientId='%s', status='%s', isTopLevel=%b, isRecovering=%b, startTime=%d, finishTime=%d, httpStatus=%d}",
                this.getClass().getSimpleName(), lraId, clientId, status,
                isTopLevel, isRecovering, startTime, finishTime, httpStatus);
    }
}
