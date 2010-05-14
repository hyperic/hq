package org.hyperic.hq.events.server.session;

public interface AlertRegulator {

    boolean alertNotificationsAllowed();

    boolean alertsAllowed();
}
