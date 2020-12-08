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

package io.narayana.lra.coordinator.internal;

import io.narayana.lra.coordinator.api.JaxRsActivator;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import org.hamcrest.MatcherAssert;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Unit test for version class.
 */
public class APIVersionTest {
    private static final APIVersion testVersion = APIVersion.instanceOf("1.1-Alpha");

    @Test
    public void upToDateVersionIsBiggerOrEqual() {
        APIVersion oneZeroVersion = APIVersion.instanceOf("1.0");
        MatcherAssert.assertThat(JaxRsActivator.LRA_API_VERSION, greaterThanOrEqualTo(oneZeroVersion));
    }

    // Object.equal does not match version with pre-release part
    @Test
    public void preReleaseIsNotEqualToFinal() {
        APIVersion version = APIVersion.instanceOf("1.1");
        MatcherAssert.assertThat(version, CoreMatchers.not(testVersion));
    }

    // Comparable.compareTo matches version with and without pre-release part
    @Test
    public void preReleaseIsCompareEqualToFinal() {
        APIVersion version = APIVersion.instanceOf("1.1");
        MatcherAssert.assertThat(version, comparesEqualTo(testVersion));
    }

    @Test
    public void lowerMajorVersion() {
        APIVersion version = APIVersion.instanceOf("0.1");
        MatcherAssert.assertThat(version, lessThan(testVersion));
    }

    @Test
    public void biggerMinorVersion() {
        APIVersion version = APIVersion.instanceOf("2.0");
        MatcherAssert.assertThat(version, greaterThan(testVersion));
    }

    @Test
    public void lowerMinorVersion() {
        APIVersion version = APIVersion.instanceOf("1.0");
        MatcherAssert.assertThat(version, lessThan(testVersion));
    }

    @Test
    public void biggerMajorVersion() {
        APIVersion version = APIVersion.instanceOf("1.2");
        MatcherAssert.assertThat(version, greaterThan(testVersion));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectVersion() {
        APIVersion.instanceOf("1,3");
    }
}