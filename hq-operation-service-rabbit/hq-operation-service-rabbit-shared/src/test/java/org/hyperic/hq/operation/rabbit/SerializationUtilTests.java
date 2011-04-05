package org.hyperic.hq.operation.rabbit;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Helena Edelson
 */
public class SerializationUtilTests {

    private static BigInteger FOO = new BigInteger(
			"-97029424235490125267223648383278313796609415534328015655051436753861088839708112925637575585166033560096810615697574744209306031461371833798723505120163874786203211176873686513374052845353833564048");

	@Test
	public void serializeCycleBestCase() throws Exception {
		assertEquals("foo", SerializationUtil.deserialize(SerializationUtil.serialize("foo")));

        TestObject data = new TestObject("testData");
        assertEquals(data, SerializationUtil.deserialize(SerializationUtil.serialize(data)));


	}

	@Test(expected = IllegalStateException.class)
	public void deserializeUndefined() throws Exception {
		byte[] bytes = FOO.toByteArray();
		Object foo = SerializationUtil.deserialize(bytes);
		assertNotNull(foo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void serializeNonSerializable() throws Exception {
		SerializationUtil.serialize(new Object());
	}

	@Test(expected = IllegalArgumentException.class)
	public void deserializeNonSerializable() throws Exception {
		SerializationUtil.deserialize("foo".getBytes());
	}

	@Test
	public void serializeNull() throws Exception {
		assertNotNull(SerializationUtil.serialize(null));
	}

	@Test
	public void deserializeNull() throws Exception {
		assertNotNull(SerializationUtil.deserialize(null));
	}
}
