package org.hyperic.hq.bizapp.server.session;

import javax.annotation.PostConstruct;

import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.util.thread.LoggingThreadGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BossInitializer {

    private ProductBoss productBoss;
    private UpdateFetcher updateFetcher;

    private ResourceCleanupEventListenerRegistrar resourceCleanupEventListenerRegistrar;

    @Autowired
    public BossInitializer(
                           ProductBoss productBoss,
                           UpdateFetcher updateFetcher,
                           ResourceCleanupEventListenerRegistrar resourceCleanupEventListenerRegistrar) {

        this.productBoss = productBoss;
        this.updateFetcher = updateFetcher;
        this.resourceCleanupEventListenerRegistrar = resourceCleanupEventListenerRegistrar;
    }

    @PostConstruct
    public void init() {
        resourceCleanupEventListenerRegistrar.registerResourceCleanupListener();
        productBoss.preload();
        LoggingThreadGroup grp = new LoggingThreadGroup("Update Notifier");
        Thread t = new Thread(grp, updateFetcher, "Update Notifier");
        t.start();
    }

}
