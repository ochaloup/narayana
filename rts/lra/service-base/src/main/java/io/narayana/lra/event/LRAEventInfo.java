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

package io.narayana.lra.event;

import java.net.URI;
import java.util.Optional;

/**
 * An object to be used as CDI event informing about LRA processing.
 */
public class LRAEventInfo {
    private final LRAAction lraAction;
    private final URI lraId, parentLraId;
    private final String participantUri, clientId;

    private LRAEventInfo(Builder builder) {
        this.lraAction = builder.lraAction;
        this.lraId = builder.lraId;
        this.participantUri = builder.participantUri;
        this.parentLraId = builder.parentLraId;
        this.clientId = builder.clientId;
    }

    public LRAAction getLraAction() {
        return lraAction;
    }

    public URI getLraId() {
        return lraId;
    }

    public Optional<String> getParticipantUri() {
        return Optional.ofNullable(participantUri);
    }

    public Optional<URI> getParentLraId() {
        return Optional.ofNullable(parentLraId);
    }

    public Optional<String> getClientId() {
        return Optional.ofNullable(clientId);
    }

    @Override
    public String toString() {
        return "LRAInfoEvent{" +
                "lraAction=" + lraAction +
                ", lraId=" + lraId +
                (parentLraId != null ? ", parentLraId=" + parentLraId : "" ) +
                (participantUri != null ? ", participantUri='" + participantUri + '\'' : "" ) +
                (clientId != null ? ", clientId='" + clientId + '\'' : "" ) +
                '}';
    }

    public static class Builder {
        private final LRAAction lraAction;
        private final URI lraId;
        private URI parentLraId;
        private String participantUri;
        private String clientId;

        public Builder(final LRAAction lraAction, final URI lraId) {
            this.lraAction = lraAction;
            this.lraId = lraId;
        }
        public Builder participantUri(final String participantUri) {
            this.participantUri = participantUri;
            return this;
        }
        public Builder parentLraId(final URI parentLraId) {
            this.parentLraId = parentLraId;
            return this;
        }
        public Builder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }
        public LRAEventInfo build() {
            return new LRAEventInfo(this);
        }
    }
}
