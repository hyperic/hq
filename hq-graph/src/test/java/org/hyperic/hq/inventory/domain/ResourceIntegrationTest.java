package org.hyperic.hq.inventory.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.roo.addon.test.RooIntegrationTest;
import org.hyperic.hq.inventory.domain.Resource;
import org.junit.Test;

@RooIntegrationTest(entity = Resource.class)
public class ResourceIntegrationTest {
    
    @Autowired
    private ResourceDataOnDemand dod;

    @Test
    public void testMarkerMethod() {
    }

    @Test
    public void testPersist() {
        org.junit.Assert.assertNotNull(
            "Data on demand for 'Resource' failed to initialize correctly",
            dod.getRandomResource());
        org.hyperic.hq.inventory.domain.Resource obj = dod
            .getNewTransientResource(Integer.MAX_VALUE);
        org.junit.Assert.assertNotNull(
            "Data on demand for 'Resource' failed to provide a new transient entity", obj);
        // Neo4J assigns an ID with the Resource constructor is called
        // org.junit.Assert.assertNull("Expected 'Resource' identifier to be null",
        // obj.getId());
        // Neo4jEntityManager persist doesn't do anything
        obj.persist();
        obj.flush();
        org.junit.Assert.assertNotNull("Expected 'Resource' identifier to no longer be null",
            obj.getId());
    }


	@Test
    public void testRemove() {
        org.hyperic.hq.inventory.domain.Resource obj = dod.getRandomResource();
        org.junit.Assert.assertNotNull("Data on demand for 'Resource' failed to initialize correctly", obj);
        java.lang.Long id = obj.getId();
        org.junit.Assert.assertNotNull("Data on demand for 'Resource' failed to provide an identifier", id);
        obj = org.hyperic.hq.inventory.domain.Resource.findResource(id);
        obj.remove();
        obj.flush();
        //TODO Neo4J keeps the Node in its cache until the tx is committed or rolled back, so the standard roo tests that call
        //findResourceType(id) after the remove get a stale Node ref that then causes NPEs later
        //This seems weird to me - stale cache w/in the same tx? 
        //org.junit.Assert.assertNull("Failed to remove 'Resource' with identifier '" + id + "'", org.hyperic.hq.inventory.domain.Resource.findResource(id));
    }
}
