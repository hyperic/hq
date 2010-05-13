package org.hyperic.hq.bizapp.server.session;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceCleanupEventListenerInitializer {

    private ResourceCleanupEventListenerRegistrar resourceCleanupEventListenerRegistrar;

    @Autowired
    public ResourceCleanupEventListenerInitializer(ResourceCleanupEventListenerRegistrar resourceCleanupEventListenerRegistrar) {
        this.resourceCleanupEventListenerRegistrar = resourceCleanupEventListenerRegistrar;
    }

    @PostConstruct
    public void init() {
        resourceCleanupEventListenerRegistrar.registerResourceCleanupListener();
    }

}
