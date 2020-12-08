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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * The LRA API version. The most probably provided in header {@code io.narayana.lra.LRAConstants#LRA_API_VERSION_HEADER_NAME}.
 * The supported format is {@code #expectedFormat}.
 * </p>
 * <p>
 * The <code>major</code> and <code>minor</code> parts are numbers, the <code>preRelease</code> part is an arbitrary string.
 * Two instances of the {@link APIVersion} may be compared.
 * There is taken into account only the <code>major</code> and <code>minor</code> parts,
 * the <code>preRelease</code> is ignored.
 * But two {@link APIVersion} instances are {@link Object#equals(Object)} only if all three parts are the same.
 * </p>
 */
public class APIVersion implements Comparable<APIVersion> {
    private static final String expectedFormat = "major.minor-preRelease";
    private static final Pattern versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)(?:-(.+))?");

    private final int major, minor;
    private final String preRelease;

    /**
     * Parsing the version string and returns a {@link APIVersion} instance.
     * The expected version format is {@code #expectedFormat}.
     * If <code>null</code> is provided as String to parse then the most up-to-date
     * LRA API version is taken from {@link JaxRsActivator#LRA_API_VERSION}.
     *
     * @param versionString  version string to be parsed; when null or empty the most up-to-date
     *                       {@link JaxRsActivator#LRA_API_VERSION} is returned
     * @return instance of the {@link APIVersion} class based on the parsed String
     * @throws IllegalArgumentException thrown when version string has a wrong format
     */
    public static APIVersion instanceOf(String versionString) {
        Matcher versionMatcher = versionPattern.matcher(versionString);
        if(versionString == null || versionString.isEmpty()) {
            return JaxRsActivator.LRA_API_VERSION;
        }
        if(!versionMatcher.matches()) {
            throw new IllegalArgumentException("Cannot parse provided version string " + versionString
                    + " as it does not match the expected format '" + expectedFormat + "'");
        }
        try {
            int major = Integer.valueOf(versionMatcher.group(1));
            int minor = Integer.valueOf(versionMatcher.group(2));
            String preRelease = versionMatcher.group(3);
            return new APIVersion(major, minor, preRelease);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("The version string " + versionString + " matches the expected format " + expectedFormat
                    + " but the major.minor cannot be converted to numbers", nfe);
        }
    }

    public APIVersion(int major, int minor, String preRelease) {
        this.major = major;
        this.minor = minor;
        this.preRelease = preRelease;
    }

    @Override
    public int compareTo(APIVersion anotherVersion) {
        int result = Integer.compare(major, anotherVersion.major);
        if (result == 0) {
            result = Integer.compare(minor, anotherVersion.minor);
        }
        return result;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append(major).append(".").append(minor);
        if (preRelease != null) {
            sb.append("-").append(preRelease);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        APIVersion that = (APIVersion) o;
        return major == that.major &&
                minor == that.minor &&
                ((preRelease == null && that.preRelease == null)
                  || (preRelease != null && preRelease.equals(that.preRelease)));
    }

    @Override
    public int hashCode() {
        if (preRelease == null) {
            return Objects.hash(major, minor);
        } else {
            return Objects.hash(major, minor, preRelease);
        }
    }
}
