package org.hyperic.hq.events.server.session;

import org.hyperic.hq.inventory.domain.Resource;

public class ResourceEventLog {
    private Resource _r;
    private EventLog _e;

    ResourceEventLog(Resource r, EventLog e) {
        _r = r;
        _e = e;
    }

    public Resource getResource() {
        return _r;
    }

    public EventLog getEventLog() {
        return _e;
    }
}
