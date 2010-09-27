/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.control.server.session;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.PluginException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

/**
 * A quartz job class for handling control actions on a single entity
 * 
 */
public class ControlActionJob
    extends ControlJob {

    protected Log log = LogFactory.getLog(ControlActionJob.class.getName());

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();

            Integer idVal = new Integer(dataMap.getString(PROP_ID));
            Integer type = new Integer(dataMap.getString(PROP_TYPE));
            AppdefEntityID id = new AppdefEntityID(type.intValue(), idVal);
            Integer subjectId = new Integer(dataMap.getString(PROP_SUBJECT));

            String action = dataMap.getString(PROP_ACTION);
            String args = dataMap.getString(PROP_ARGS);

            Boolean scheduled = Boolean.valueOf(dataMap.getString(PROP_SCHEDULED));
            String description = dataMap.getString(PROP_DESCRIPTION);

            Trigger trigger = context.getTrigger();
            Date dateScheduled = trigger.getPreviousFireTime();
            final String subjectName = Bootstrap.getBean(AuthzSubjectManager.class)
                .findSubjectName(subjectId);
            try {
                Bootstrap.getBean(ControlActionExecutor.class).executeControlAction(id,
                    subjectName, dateScheduled, scheduled, description, action, args);
            } catch (PluginException e) {
                log.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
}
