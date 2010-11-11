package org.hyperic.hq.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.roo.addon.test.RooIntegrationTest;
import org.hyperic.hq.domain.Resource;
import org.junit.Test;

@RooIntegrationTest(entity = Resource.class)
public class ResourceIntegrationTest {
	
	@Autowired
	private ResourceDataOnDemand dod2;

    @Test
    public void testMarkerMethod() {
    }

	@Test
    public void testPersist() {
        org.junit.Assert.assertNotNull("Data on demand for 'Resource' failed to initialize correctly", dod2.getRandomResource());
        org.hyperic.hq.domain.Resource obj = dod2.getNewTransientResource(Integer.MAX_VALUE);
        org.junit.Assert.assertNotNull("Data on demand for 'Resource' failed to provide a new transient entity", obj);
        //Neo4J assigns an ID with the Resource constructor is called
        // org.junit.Assert.assertNull("Expected 'Resource' identifier to be null", obj.getId());
        //Neo4jEntityManager persist doesn't do anything
        obj.persist();
        obj.flush();
        org.junit.Assert.assertNotNull("Expected 'Resource' identifier to no longer be null", obj.getId());
    }
}
