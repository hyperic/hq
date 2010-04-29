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
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.scheduler.server.session.BaseJob;

public abstract class ControlJob extends BaseJob {

    // The time to wait inbetween checking if a control action has
    // finished.  This is used to synchronize control calls to the
    // agent.
    protected static final int JOB_WAIT_INTERVAL = 500;
    
    // Configuration paramaters
    public static final String PROP_ACTION         = "action";
    public static final String PROP_ARGS           = "args";

    protected Log log = LogFactory.getLog(ControlJob.class);

    /**
     * Do a control command on a single appdef entity
     *
     * @return The job id
     */
    protected Integer doAgentControlCommand(AppdefEntityID id,
                                            AppdefEntityID gid,
                                            Integer batchId,
                                            AuthzSubject subject,
                                            Date dateScheduled,
                                            Boolean scheduled,
                                            String description,
                                            String action,
                                            String args)
        throws PluginException
    {
        return Bootstrap.getBean(ControlScheduleManager.class).doAgentControlCommand(id, gid, batchId,
            subject, dateScheduled, scheduled, description,action, args);
    }
}
