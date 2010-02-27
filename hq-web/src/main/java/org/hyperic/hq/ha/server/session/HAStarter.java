package org.hyperic.hq.ha.server.session;

import javax.annotation.PostConstruct;

import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.ha.HAService;
import org.springframework.stereotype.Service;

@Service
public class HAStarter {

    @PostConstruct
    public void startHAService() {
        ((HAService) ProductProperties.getPropertyInstance("hyperic.hq.ha.service")).start();
    }

}
