package org.hyperic.hq.operation.rabbit;

import org.hyperic.hq.operation.Envelope;
import org.hyperic.hq.operation.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Helena Edelson
 */
public class EnvelopeTests {

    @Test
    public void test() {
        Envelope envelope = new Message(0L, "test.operation.name", "test context", "test.response.exchange");
        assertEquals(0L, envelope.getOperationId());
        assertEquals("test.operation.name", envelope.getOperationName());
        assertEquals("test context", envelope.getContext());
        assertEquals("test.response.exchange", envelope.getReplyTo());
    }
}
