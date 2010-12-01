package org.hyperic.hq.inventory.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.roo.addon.test.RooIntegrationTest;
import org.hyperic.hq.inventory.domain.ResourceType;
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
        org.hyperic.hq.inventory.domain.ResourceType obj = dod.getNewTransientResourceType(Integer.MAX_VALUE);
        org.junit.Assert.assertNotNull("Data on demand for 'ResourceType' failed to provide a new transient entity", obj);
        // Neo4J assigns an ID with the Resource constructor is called
        // org.junit.Assert.assertNull("Expected 'Resource' identifier to be null",
        // obj.getId());
        //org.junit.Assert.assertNull("Expected 'ResourceType' identifier to be null", obj.getId());
        obj.persist();
        obj.flush();
        org.junit.Assert.assertNotNull("Expected 'ResourceType' identifier to no longer be null", obj.getId());
    }


	@Test
    public void testRemove() {
        org.hyperic.hq.inventory.domain.ResourceType obj = dod.getRandomResourceType();
        org.junit.Assert.assertNotNull("Data on demand for 'ResourceType' failed to initialize correctly", obj);
        java.lang.Long id = obj.getId();
        org.junit.Assert.assertNotNull("Data on demand for 'ResourceType' failed to provide an identifier", id);
        obj = org.hyperic.hq.inventory.domain.ResourceType.findResourceType(id);
        obj.remove();
        obj.flush();
        //TODO Neo4J keeps the Node in its cache until the tx is committed or rolled back, so the standard roo tests that call
        //findResourceType(id) after the remove get a stale Node ref that then causes NPEs later
        //This seems weird to me - stale cache w/in the same tx? 
        //org.junit.Assert.assertNull("Failed to remove 'ResourceType' with identifier '" + id + "'", org.hyperic.hq.plugin.domain.ResourceType.findResourceType(id));
    }
}
