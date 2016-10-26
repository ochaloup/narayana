/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.narayana.compensations.internal.recovery;

import org.jboss.logging.Logger;
import org.jboss.narayana.compensations.api.Deserializer;

import java.io.ObjectInputStream;
import java.util.Optional;

/**
 * Utility class to work with user registered deserializers.
 * 
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class DeserializerHelper {

    private static final Logger LOGGER = Logger.getLogger(DeserializerHelper.class);

    /**
     * Find a user registered deserializer capable to deserialize objects of the
     * requested type. If such deserializer can be found, then use it to
     * deserialize an object from the provided input stream.
     * 
     * @param objectInputStream
     *            stream to deserialize an object from.
     * @param className
     *            object's class name.
     * @param clazz
     *            object's class
     * @return {@link Optional} with the deserialized object if required
     *         deserializer was found. Or an empty {@link Optional} if the
     *         deserializer wasn't found.
     */
    public <T> Optional<T> deserialize(ObjectInputStream objectInputStream, String className, Class<T> clazz) {
        for (Deserializer deserializer : DeserializersContainerImpl.getInstance().getDeserializers()) {
            if (!deserializer.canDeserialize(className)) {
                continue;
            }

            Optional<T> object = deserializer.deserialize(objectInputStream, clazz);
            if (object.isPresent()) {
                LOGGER.tracef("Restored object: '%s'", object.get());
                return object;
            }
        }

        return Optional.empty();
    }

}
