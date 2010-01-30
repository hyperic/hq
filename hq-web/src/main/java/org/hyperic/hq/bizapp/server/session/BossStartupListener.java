package org.hyperic.hq.bizapp.server.session;

import javax.annotation.PostConstruct;

import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.EventLogBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.measurement.server.session.MeasurementStartupListener;
import org.hyperic.util.thread.LoggingThreadGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BossStartupListener implements StartupListener {

    private static UpdateReportAppender updateCallback;

    private AppdefBoss appdefBoss;

    private EventsBoss eventsBoss;
    private EventLogBoss eventLogBoss;
    private ProductBoss productBoss;
    private UpdateFetcher updateFetcher;
    private HQApp hqApp;

    @Autowired
    public BossStartupListener(AppdefBoss appdefBoss, EventsBoss eventsBoss, EventLogBoss eventLogBoss,
                                ProductBoss productBoss, UpdateFetcher updateFetcher, HQApp hqApp, MeasurementStartupListener measurementStartupListener) {
        this.appdefBoss = appdefBoss;
        this.eventsBoss = eventsBoss;
        this.eventLogBoss = eventLogBoss;
        this.productBoss = productBoss;
        this.updateFetcher = updateFetcher;
        this.hqApp = hqApp;
        //TODO MeasurementStartupListener has to be initialized first to register the DefaultMetricEnableCallback handler.  Injecting the listener here purely to wait for that
    }

    @PostConstruct
    public void hqStarted() {
        eventsBoss.startup();
        eventLogBoss.startup();
        appdefBoss.startup();
        productBoss.preload();
        updateCallback = (UpdateReportAppender) hqApp.registerCallbackCaller(UpdateReportAppender.class);
        LoggingThreadGroup grp = new LoggingThreadGroup("Update Notifier");
        Thread t = new Thread(grp, updateFetcher, "Update Notifier");
        t.start();
    }

    static UpdateReportAppender getUpdateReportAppender() {
        return updateCallback;
    }

}
