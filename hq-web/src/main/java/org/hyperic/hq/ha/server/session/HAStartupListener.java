package org.hyperic.hq.ha.server.session;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.common.server.session.ProductConfigService;
import org.hyperic.hq.ha.HAService;
import org.springframework.stereotype.Service;

@Service
public class HAStartupListener implements StartupListener {
    
    private final Log log = LogFactory.getLog(HAStartupListener.class);

    @PostConstruct
    public void hqStarted() {
        log.info("Starting services");
        ((ProductConfigService) ProductProperties.getPropertyInstance("hyperic.hq.config.service")).start();
        ((HAService) ProductProperties.getPropertyInstance("hyperic.hq.ha.service")).start();
    }

}
