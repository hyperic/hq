package org.hyperic.hq.ha.server.session;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.ha.HAService;
import org.springframework.stereotype.Service;

@Service
public class HAStartupListener implements StartupListener {
    
    private final Log log = LogFactory.getLog(HAStartupListener.class);

    @PostConstruct
    public void hqStarted() {
        log.info("Starting services");
        ((HAService) ProductProperties.getPropertyInstance("hyperic.hq.ha.service")).start();
    }

}
