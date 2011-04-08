package org.hyperic.hq.operation.rabbit.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;

/**
 * Serialization utility class
 * @author Helena Edelson
 */
public class SerializationUtil {

	/**
	 * Serialize the object provided.
	 *
	 * @param object the object to serialize
	 * @return an array of bytes representing the object in a portable fashion
	 */
	public static byte[] serialize(Object object) {

		if (object==null) {
			return null;
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(stream).writeObject(object);
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not serialize object of type: "+object.getClass(), e);
		}

		return stream.toByteArray();

	}

	/**
	 * @param bytes a serialized object created
	 * @return the result of deserializing the bytes
	 */
	public static Object deserialize(byte[] bytes) {

		if (bytes==null) {
			return null;
		}

		try {
			return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
		}
		catch (OptionalDataException e) {
			throw new IllegalArgumentException("Could not deserialize object: eof="+e.eof+ " at length="+e.length, e);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not deserialize object", e);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Could not deserialize object type", e);
		}

	}
}
