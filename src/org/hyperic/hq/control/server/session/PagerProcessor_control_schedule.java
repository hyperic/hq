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
                    trigger =
                        scheduler.getTrigger(s.getTriggerName(),
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

                return s.getControlScheduleValue();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error converting to " +
                                            "ControlScheduleValue: " + e);
        }
        return o;
    }
}
