/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.measurement.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.shared.AlertDefinitionBasicValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.hq.dao.MetricProblemDAO;
import org.hyperic.dao.DAOFactory;

/**
 * Log the fact that an alert was generated due to some measurement
 */
public class MetricAlertAction implements ActionInterface {
    private Log log = LogFactory.getLog(MetricAlertAction.class);

    public String execute(AlertDefinitionBasicValue alertdef,
                          TriggerFiredEvent event, Integer alertId)
        throws ActionExecuteException {
        MetricProblemDAO dao =
            DAOFactory.getDAOFactory().getMetricProblemDAO();
        StringBuffer actLog = new StringBuffer();

        // Organize the events by trigger
        TriggerFiredEvent[] firedEvents = event.getRootEvents();
        for (int i = 0; i < firedEvents.length; i++) {
            // Go through the TriggerFiredEvent's root events
            AbstractEvent[] events = firedEvents[i].getEvents();
            for (int j = 0; j < events.length; j++) {
                if (events[j] instanceof MeasurementEvent) {
                    dao.create(events[j].getInstanceId(),
                               events[j].getTimestamp(),
                               MeasurementConstants.PROBLEM_TYPE_ALERT,
                               alertId);

                    // Append to action log
                    actLog.append("MeasurementAlert added for mid: ");
                    actLog.append(events[j].getInstanceId());
                    actLog.append(" aid: ");
                    actLog.append(alertId);
                    actLog.append("\n");
                }
            }
        }

        return actLog.toString();
    }

    public ConfigSchema getConfigSchema() {
        return new ConfigSchema();
    }


    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException {
        return new ConfigResponse();
    }

    public void init(ConfigResponse config) throws InvalidActionDataException {
    }

    public String getImplementor() {
        return MetricAlertAction.class.getName();
    }

    public void setImplementor(String implementor) {
        // This is not an action to be overwritten
    }

    public void setParentActionConfig(AppdefEntityID aeid,
                                      ConfigResponse config)
        throws InvalidActionDataException {
        this.init(config);
    }
}
