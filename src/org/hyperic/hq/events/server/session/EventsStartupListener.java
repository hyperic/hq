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
import java.util.StringTokenizer;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.PropertyNotFoundException;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.jdbc.DBUtil;

public class EventsStartupListener
    implements StartupListener
{
    private static final Log _log =
        LogFactory.getLog(EventsStartupListener.class);
    private static final Object LOCK = new Object();
    private static AlertDefinitionChangeCallback _alertDefChangeCallback;

    public void hqStarted() {
        // Make sure the escalation enumeration is loaded and registered so
        // that the escalations run
        ClassicEscalationAlertType.class.getClass();
        AlertableRoleCalendarType.class.getClass();

        HQApp app = HQApp.getInstance();

        synchronized (LOCK) {
            _alertDefChangeCallback = (AlertDefinitionChangeCallback)
                app.registerCallbackCaller(AlertDefinitionChangeCallback.class);
        }

        AlertDefinitionManagerEJBImpl.getOne().startup();

        loadConfigProps("triggers");
        loadConfigProps("actions");

        ZeventManager.getInstance().registerEventClass(AlertConditionsSatisfiedZEvent.class);
        Set alertEvents = new HashSet();
        alertEvents.add(AlertConditionsSatisfiedZEvent.class);

        ZeventManager.getInstance().addBufferedListener(
            alertEvents, new AlertConditionsSatisfiedListener());


        Set triggerEvents = new HashSet();
        triggerEvents.add(TriggersCreatedZevent.class);

        ZeventManager.getInstance().addBufferedListener(
                                                        triggerEvents, new TriggersCreatedListener());

        cleanupRegisteredTriggers();
    }

    private void cleanupRegisteredTriggers() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBUtil.getConnByContext(
                new InitialContext(), HQConstants.DATASOURCE);
            stmt = conn.createStatement();
            int rows = stmt.executeUpdate(
                "update EAM_ALERT_CONDITION set trigger_id = null " +
                "WHERE exists (" +
                    "select 1 from EAM_ALERT_DEFINITION WHERE deleted = '1' " +
                    "AND EAM_ALERT_CONDITION.alert_definition_id = id" +
                ")");
            _log.info("disassociated " + rows + " triggers in EAM_ALERT_CONDITION" +
                " from their deleted alert definitions");
            rows = stmt.executeUpdate(
                "delete from EAM_REGISTERED_TRIGGER WHERE exists (" +
                    "select 1 from EAM_ALERT_DEFINITION WHERE deleted = '1' " +
                    "AND EAM_REGISTERED_TRIGGER.alert_definition_id = id" +
                ")");
            _log.info("deleted " + rows + " rows from EAM_REGISTERED_TRIGGER");
        } catch (SQLException e) {
            _log.error(e, e);
        } catch (NamingException e) {
            _log.error(e, e);
        } finally {
            DBUtil.closeJDBCObjects(
                EventsStartupListener.class.getName(), conn, stmt, null);
        }
    }

    private void loadConfigProps(String prop) {
        try {
            String property = System.getProperty(prop);

            if (property == null) {
                throw new PropertyNotFoundException(prop + " list not found");
            }

            _log.info(prop + " list: " + property);

            StringTokenizer tok = new StringTokenizer(property, ", ");
            while (tok.hasMoreTokens()) {
                String className = tok.nextToken();
                    _log.debug("Initialize class: " + className);

                try {
                    Class classObj = Class.forName(className);
                    classObj.newInstance();
                } catch (ClassNotFoundException e) {
                    _log.error("Class: " + className + " not found");
                } catch (InstantiationException e) {
                    _log.error("Error instantiating class: " + className);
                } catch (IllegalAccessException e) {
                    _log.error("Error instantiating class: " + className);
                }
            }
        } catch (Exception e) {
            // Swallow all exceptions
            _log.error("Encountered error initializing " + prop, e);
        }
    }

    static AlertDefinitionChangeCallback getAlertDefinitionChangeCallback() {
        synchronized (LOCK) {
            return _alertDefChangeCallback;
        }
    }
}
