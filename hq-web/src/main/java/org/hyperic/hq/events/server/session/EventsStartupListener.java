/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.events.server.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventsStartupListener implements StartupListener {
    private static final Log _log = LogFactory.getLog(EventsStartupListener.class);
    private static final Object LOCK = new Object();
    private static AlertDefinitionChangeCallback _alertDefChangeCallback;
    private DBUtil dbUtil;
    private HQApp app;
    private ZeventEnqueuer zEventManager;
    private TriggersCreatedListener triggersCreatedListener;
    private AlertConditionsSatisfiedListener alertConditionsSatisfiedListener;
    private AvailabilityDownAlertDefinitionCache cache;

    @Autowired
    public EventsStartupListener(DBUtil dbUtil, HQApp app, ZeventEnqueuer zEventManager,
                                 TriggersCreatedListener triggersCreatedListener,
                                 AlertConditionsSatisfiedListener alertConditionsSatisfiedListener,
                                 AvailabilityDownAlertDefinitionCache cache) {

        this.dbUtil = dbUtil;
        this.app = app;
        this.zEventManager = zEventManager;
        this.triggersCreatedListener = triggersCreatedListener;
        this.alertConditionsSatisfiedListener = alertConditionsSatisfiedListener;
        this.cache = cache;
    }

    private void registerAlertDefCacheCleanup() {
        app.registerCallbackListener(AlertDefinitionChangeCallback.class, new AlertDefinitionChangeCallback() {
            public void postCreate(AlertDefinition def) {
                removeFromCache(def);
            }

            public void postDelete(AlertDefinition def) {
                removeFromCache(def);
            }

            public void postUpdate(AlertDefinition def) {
                removeFromCache(def);
            }

            private void removeFromCache(AlertDefinition def) {
                synchronized (cache) {
                    cache.remove(def.getAppdefEntityId());

                    for (AlertDefinition childDef : def.getChildren()) {
                        cache.remove(childDef.getAppdefEntityId());
                    }
                }
            }
        });
    }

    @PostConstruct
    public void hqStarted() {
        // Make sure the escalation enumeration is loaded and registered so
        // that the escalations run
        ClassicEscalationAlertType.class.getClass();
        AlertableRoleCalendarType.class.getClass();

        synchronized (LOCK) {
            _alertDefChangeCallback = (AlertDefinitionChangeCallback) app
                .registerCallbackCaller(AlertDefinitionChangeCallback.class);
        }

        registerAlertDefCacheCleanup();
        zEventManager.registerEventClass(AlertConditionsSatisfiedZEvent.class);
        Set<Class<?>> alertEvents = new HashSet<Class<?>>();
        alertEvents.add(AlertConditionsSatisfiedZEvent.class);

        zEventManager.addBufferedListener(alertEvents, alertConditionsSatisfiedListener);

        Set<Class<?>> triggerEvents = new HashSet<Class<?>>();
        triggerEvents.add(TriggersCreatedZevent.class);

        zEventManager.addBufferedListener(triggerEvents, triggersCreatedListener);

        cleanupRegisteredTriggers();
    }

    private void cleanupRegisteredTriggers() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dbUtil.getConnection();
            stmt = conn.createStatement();
            int rows = stmt.executeUpdate("update EAM_ALERT_CONDITION set trigger_id = null " + "WHERE exists ("
                                          + "select 1 from EAM_ALERT_DEFINITION WHERE deleted = '1' "
                                          + "AND EAM_ALERT_CONDITION.alert_definition_id = id" + ")");
            _log.info("disassociated " + rows + " triggers in EAM_ALERT_CONDITION" +
                      " from their deleted alert definitions");
            rows = stmt.executeUpdate("delete from EAM_REGISTERED_TRIGGER WHERE exists ("
                                      + "select 1 from EAM_ALERT_DEFINITION WHERE deleted = '1' "
                                      + "AND EAM_REGISTERED_TRIGGER.alert_definition_id = id" + ")");
            _log.info("deleted " + rows + " rows from EAM_REGISTERED_TRIGGER");
        } catch (SQLException e) {
            _log.error(e, e);
        } catch (NamingException e) {
            _log.error(e, e);
        } finally {
            DBUtil.closeJDBCObjects(EventsStartupListener.class.getName(), conn, stmt, null);
        }
    }

    static AlertDefinitionChangeCallback getAlertDefinitionChangeCallback() {
        synchronized (LOCK) {
            return _alertDefChangeCallback;
        }
    }
}
