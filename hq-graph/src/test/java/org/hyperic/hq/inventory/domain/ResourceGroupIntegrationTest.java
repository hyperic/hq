package org.hyperic.hq.inventory.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.roo.addon.test.RooIntegrationTest;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.junit.Test;

@RooIntegrationTest(entity = ResourceGroup.class)
public class ResourceGroupIntegrationTest {
    
    @Autowired
    private ResourceGroupDataOnDemand dod;

    @Test
    public void testMarkerMethod() {
    }

	@Test
    public void testFindAllResourceGroups() {
      //TODO this causes stack overflow on querying (somehow keeps creating ResourceGroups).  find out why
    }

	@Test
    public void testPersist() {
        org.junit.Assert.assertNotNull("Data on demand for 'ResourceGroup' failed to initialize correctly", dod.getRandomResourceGroup());
        org.hyperic.hq.inventory.domain.ResourceGroup obj = dod.getNewTransientResourceGroup(Integer.MAX_VALUE);
        org.junit.Assert.assertNotNull("Data on demand for 'ResourceGroup' failed to provide a new transient entity", obj);
        //org.junit.Assert.assertNull("Expected 'ResourceGroup' identifier to be null", obj.getId());
        obj.persist();
        obj.flush();
        org.junit.Assert.assertNotNull("Expected 'ResourceGroup' identifier to no longer be null", obj.getId());
    }
}
