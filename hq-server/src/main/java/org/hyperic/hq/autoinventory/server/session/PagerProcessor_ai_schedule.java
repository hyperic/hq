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

package org.hyperic.hq.autoinventory.server.session;

import java.util.Date;

import org.hyperic.hq.autoinventory.AISchedule;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.dao.AIScheduleDAO;
import org.hyperic.util.pager.PagerEventHandler;
import org.hyperic.util.pager.PagerProcessorExt;
import org.quartz.Scheduler;
import org.quartz.Trigger;

public class PagerProcessor_ai_schedule implements PagerProcessorExt {

    private final String GROUP = "autoinventory";

    public PagerProcessor_ai_schedule() {}

    public PagerEventHandler getEventHandler () { return null; }

    public boolean skipNulls () { return true; }

    public Object processElement(Object o) {
        if (o == null) return null;
        try {
            if (o instanceof AISchedule) {
                Scheduler scheduler = 
                    Bootstrap.getBean(Scheduler.class);
                AISchedule s = (AISchedule)o;
                Trigger trigger;
                try {
                    trigger =
                        scheduler.getTrigger(s.getTriggerName(), GROUP);
                    if (trigger == null) {
                        // Job no longer exists
                        try {
                            Bootstrap.getBean(AIScheduleDAO.class)
                                .remove(s);
                            return null;
                        } catch (Exception e) {
                        // Ignore
                        }
                    }
                } catch (Exception e) {
                    return null;
                }

                // Update next fire time
                Date nextFire = trigger.getFireTimeAfter(new Date());
                s.setNextFireTime(nextFire.getTime());

                return s.getAIScheduleValue();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error converting to " +
                                            "AIScheduleValue: " + e);
        }
        return o;
    }

    public Object processElement ( Object o1, Object o2 ) {
        return processElement(o1);
    }
}
