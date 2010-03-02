package org.hyperic.hq.bizapp.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UpdateFetcher implements Runnable {

    private final Log _log = LogFactory.getLog(UpdateFetcher.class);
    private UpdateBoss updateBoss;
    private final long checkInterval;

    @Autowired
    public UpdateFetcher(UpdateBoss updateBoss, @Value("#{tweakProperties['hq.updateNotify.interval'] }")Long checkInterval) {
        this.updateBoss = updateBoss;
        this.checkInterval = checkInterval;
    } 

    public void run() {
        while (true) {
            try {
                updateBoss.fetchReport();
            } catch (Exception e) {
                _log.warn("Error getting update notification", e);
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                return;
            }
        }
    }


}
