package org.hyperic.hq.appdef.server.session;

import java.util.Set;

import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NodeTesterImpl implements NodeTester {

    @Autowired
    private ResourceDao resourceDao;
    
    @Transactional
    public void createAndRelate() {
        Resource resourceB = new Resource() ;
        resourceB.setName("B");
        resourceB.persist();
        Resource resourceA = resourceDao.findByName("A");
        resourceA.relateTo(resourceB,"Jen");
    }
    
    @Transactional(readOnly=true)
    public void traverse() {
        //If I obtain the node after createAndRelate tx runs, everything is fine.  If I grab it before, I get the prob where node is available but not underlying entity
        Resource resourceA = resourceDao.findByName("A");
        Set<Resource> children = resourceA.getResourcesFrom("Jen");
        System.out.println("Stop here");
    }
}
