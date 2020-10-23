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

package io.narayana.lra.coordinator.domain.event;

/**
 * A state representing on what type of LRA event was created.
 */
public enum LRAAction {
    /**
     * Start of an LRA instance.
     */
    STARTED,
    /**
     * Attempt to start an LRA instance failed.
     */
    FAILED_TO_START,
    /**
     * An LRA participant was enlisted to the started LRA instance.
     */
    ENLISTED,
    /**
     * LRA was cancelled, aka. finished with the abort.
     */
    CANCEL,
    /**
     * LRA was closed, aka. finished with the commit.
     */
    CLOSED,
    /**
     * LRA participant was compensated, aka. LRA finished with failure
     * and the participant's compensate callback was called.
     */
    COMPENSATED,
    /**
     * LRA participant was completed, aka. LRA finished with success
     * and the participant's complete callback was called.
     */
    COMPLETED,
    /**
     * LRA participant was tried to be compensated but the callback has not been finished yet.
     */
    COMPENSATE_ATTEMPT,
    /**
     * LRA participant was tried to be completed but the callback has not been finished yet.
     */
    COMPLETE_ATTEMPT,
    /**
     * LRA participant was tried to be compensated but the callback call failed.
     */
    COMPENSATE_ATTEMPT_FAILURE,
    /**
     * LRA participant was tried to be completed but the callback call failed.
     */
    COMPLETE_ATTEMPT_FAILURE,
    /**
     * LRA participant was tried to be called at the AFTER_LRA callback endpoint but the call has not finished
     * and will be retried.
     */
    AFTER_CALLBACK_ATTEMPT,
    /**
     * LRA participant AFTER_LRA callback finished.
     */
    AFTER_CALLBACK_FINISHED,
    /**
     * LRA participant was tried to be called at the FORGET callback endpoint but the call has not finished with success
     * and will be retried.
     */
    FORGET_ATTEMPT,
    /**
     * LRA participant FORGET callback finished.
     */
    FORGOTTEN;
}
