package org.hyperic.hq.operation.rabbit.shared;

import org.hyperic.hq.operation.rabbit.api.Envelope;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnvelopeTests {

    @Test
    public void testEnvelope() { 
        Envelope envelope = new Envelope("registerAgent", "test content", "id");
        assertEquals("registerAgent", envelope.getOperationName());
        assertEquals("test content", envelope.getContent()); 
    }
}
