/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.amqp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Helena Edelson
 */
public class SerializationUtils {

    /**
	 * Serialize the object provided.
	 *
	 * @param object the object to serialize
	 * @return an array of bytes representing the object in a portable fashion
	 */
	public static byte[] serialize(Object object) {
		if (object == null) {
			return null;
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(stream).writeObject(object);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not serialize object of type: " + object.getClass(), e);
		}
		return stream.toByteArray();
	}

	/**
	 * @param bytes a serialized object created
	 * @return the result of deserializing the bytes
	 */
	public static Object deserialize(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try {
			return deserialize(new ObjectInputStream(new ByteArrayInputStream(bytes)));
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not deserialize object", e);
		}
	}

    public static Object deserialize(ObjectInputStream stream) {
		if (stream == null) {
			return null;
		}
		try {
			return stream.readObject();
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not deserialize object", e);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Could not deserialize object type", e);
		}
	}
}
