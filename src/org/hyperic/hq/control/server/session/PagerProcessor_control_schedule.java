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
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.hq.scheduler.shared.SchedulerUtil;
import org.hyperic.util.pager.PagerProcessor;
import org.quartz.Trigger;

public class PagerProcessor_control_schedule implements PagerProcessor {

    protected static Log log =
        LogFactory.getLog( PagerProcessor_control_schedule.class.getName() );
    
    public PagerProcessor_control_schedule() {}

    public Object processElement(Object o) {
        if (o == null) return null;
        try {
            if (o instanceof ControlSchedule) {
                ControlScheduleDAO cdao = DAOFactory.getDAOFactory()
                    .getControlScheduleDAO();
                SchedulerLocal scheduler = 
                    SchedulerUtil.getLocalHome().create();
                ControlSchedule s = (ControlSchedule)o;
                Trigger trigger;
                try {
                    trigger = scheduler.getTrigger(s.getTriggerName(),
                                                   ControlConstants.GROUP);
                    if (trigger == null) {
                        // Job no longer exists
                        try {
                            cdao.remove(s);
                            return null;
                        } catch (Exception e) {
                        // Ignore RemoveException
                        }
                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled())
                        log.debug("exception:", e);                    
                    return null;
                }

                Date nextFire = trigger.getFireTimeAfter(new Date());
                if (nextFire != null) {
                    // Update next fire time
                    s.setNextFireTime(nextFire.getTime());
                } else {
                    // Schedule will not fire again
                    cdao.remove(s);
                    return null;
                }

                return s;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error converting to " +
                                            "ControlScheduleValue: " + e);
        }
        return o;
    }
}
