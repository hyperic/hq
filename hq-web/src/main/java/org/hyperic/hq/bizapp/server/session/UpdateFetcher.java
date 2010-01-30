package org.hyperic.hq.bizapp.server.session;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateFetcher implements Runnable {
    private static final int CHECK_INTERVAL = 1000 * 60 * 60 * 24;
    private final Log _log = LogFactory.getLog(UpdateFetcher.class);
    private UpdateBoss updateBoss;

    @Autowired
    public UpdateFetcher(UpdateBoss updateBoss) {
        this.updateBoss = updateBoss;
    }

    public void run() {
        long interval = getCheckInterval();
        while (true) {
            try {
                updateBoss.fetchReport();
            } catch (Exception e) {
                _log.warn("Error getting update notification", e);
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private long getCheckInterval() {
        try {
            Properties p = HQApp.getInstance().getTweakProperties();
            String res = p.getProperty("hq.updateNotify.interval");
            if (res != null) {
                return Long.parseLong(res);
            }
        } catch (Exception e) {
            _log.warn("Unable to get notification interval", e);
        }
        return CHECK_INTERVAL;
    }
}
