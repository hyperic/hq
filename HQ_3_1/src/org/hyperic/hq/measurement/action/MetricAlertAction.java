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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.MetricProblemDAO;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * Log the fact that an alert was generated due to some measurement
 */
public class MetricAlertAction implements ActionInterface {
    private Log log = LogFactory.getLog(MetricAlertAction.class);

    public String execute(AlertInterface aIface, ActionExecutionInfo info) 
        throws ActionExecuteException 
    {
        MetricProblemDAO dao =
            DAOFactory.getDAOFactory().getMetricProblemDAO();
        StringBuffer actLog = new StringBuffer();

        // XXX -- This is probably not a safe cast.  The information here
        //        should probably be contained within the short/long reasons
        //        as well -- JMT
        Alert alert = (Alert)aIface;
        Collection logs = alert.getConditionLog();
        for (Iterator it = logs.iterator(); it.hasNext(); ) {
            AlertConditionLog log = (AlertConditionLog) it.next();
            AlertCondition cond = log.getCondition();
            if (cond.getType() == EventConstants.TYPE_THRESHOLD ||
                cond.getType() == EventConstants.TYPE_BASELINE) {
                dao.create(new Integer(cond.getMeasurementId()),
                           System.currentTimeMillis(),
                           MeasurementConstants.PROBLEM_TYPE_ALERT,
                           alert.getId());

                // Append to action log
                actLog.append("MeasurementAlert added for mid: ");
                actLog.append(cond.getMeasurementId());
                actLog.append(" aid: ");
                actLog.append(alert.getId());
                actLog.append("\n");
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

    public void init(ConfigResponse config) {
    }

    public String getImplementor() {
        return MetricAlertAction.class.getName();
    }

    public void setImplementor(String implementor) {
        // This is not an action to be overwritten
    }

    public void setParentActionConfig(AppdefEntityID aeid,
                                      ConfigResponse config)
    {
        init(config);
    }
}
