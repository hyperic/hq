package org.hyperic.hq.ha.server.session;

import javax.annotation.PostConstruct;

import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.ha.HAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HAStarter {
    
    private ProductBoss productBoss;
    
    
    @Autowired
    public HAStarter(ProductBoss productBoss) {
        this.productBoss = productBoss;
    }



    @PostConstruct
    public void startHAService() {
        //We need to preload 2nd level cache before the HAServiceImpl initializes triggers 
        //(but only once on initial server start, not in failover scenario)
        productBoss.preload();
        ((HAService) ProductProperties.getPropertyInstance("hyperic.hq.ha.service")).start();
    }

}
