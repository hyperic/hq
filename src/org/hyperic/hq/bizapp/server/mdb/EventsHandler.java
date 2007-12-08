package org.hyperic.hq.bizapp.server.mdb;

import java.util.List;

interface EventsHandler {
    void handleEvents(List events);
}
