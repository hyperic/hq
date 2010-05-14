package org.hyperic.hq.ha.server.session;

import javax.annotation.PostConstruct;

import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.shared.ProductManager;

import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.ha.HAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HAStarter {
    
    @Autowired
    private ProductManager productManager;
    @Autowired
    private MeasurementManager measurementManager;
    
    @PostConstruct
    public void startHAService() {
        //We need to preload 2nd level cache before the HAServiceImpl initializes triggers 
        //(but only once on initial server start, not in failover scenario)
        productManager.preload();
        measurementManager.findAllEnabledMeasurementsAndTemplates();
        ((HAService) ProductProperties.getPropertyInstance("hyperic.hq.ha.service")).start();
    }

}
