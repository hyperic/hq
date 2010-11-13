package org.hyperic.hq.plugin.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.roo.addon.test.RooIntegrationTest;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.junit.Test;

@RooIntegrationTest(entity = ResourceType.class)
public class ResourceTypeIntegrationTest {
    
    @Autowired
    private ResourceTypeDataOnDemand dod;

    @Test
    public void testMarkerMethod() {
    }

	@Test
    public void testPersist() {
        org.junit.Assert.assertNotNull("Data on demand for 'ResourceType' failed to initialize correctly", dod.getRandomResourceType());
        org.hyperic.hq.plugin.domain.ResourceType obj = dod.getNewTransientResourceType(Integer.MAX_VALUE);
        org.junit.Assert.assertNotNull("Data on demand for 'ResourceType' failed to provide a new transient entity", obj);
        // Neo4J assigns an ID with the Resource constructor is called
        // org.junit.Assert.assertNull("Expected 'Resource' identifier to be null",
        // obj.getId());
        //org.junit.Assert.assertNull("Expected 'ResourceType' identifier to be null", obj.getId());
        obj.persist();
        obj.flush();
        org.junit.Assert.assertNotNull("Expected 'ResourceType' identifier to no longer be null", obj.getId());
    }
}
