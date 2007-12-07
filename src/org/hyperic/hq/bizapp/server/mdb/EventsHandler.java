package org.hyperic.hq.bizapp.server.mdb;

import java.util.List;

public interface EventsHandler {
    void handleEvents(List events);
}
