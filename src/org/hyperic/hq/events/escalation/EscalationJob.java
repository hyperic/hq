/**
 *
 */
package org.hyperic.hq.events.escalation;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.SchedulerException;
import org.quartz.JobDataMap;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.hq.events.escalation.command.EscalateCommand;
import org.hyperic.hq.events.escalation.mediator.EscalationMediator;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.hq.scheduler.shared.SchedulerUtil;
import org.hyperic.hq.CommandContext;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import java.util.GregorianCalendar;

public class EscalationJob implements Job {
    public static final String JOB_GROUP = "EscalationJobGroup";
    public static final String APP_ID = "appId";
    public static final String ALERT_ID = "alertId";

    private static SchedulerLocal scheduler;
    static {
        try {
            scheduler = SchedulerUtil.getLocalHome().create();
        } catch (CreateException e) {
            scheduler = null;
            throw new RuntimeException(
                "Can't instantiate Scheduler Session Bean"
            );
        } catch (NamingException e) {
            scheduler = null;
            throw new RuntimeException(
                "Can't lookup Scheduler Session Bean"
            );
        }
    }

    public static void scheduleJob(Integer escalationId,
                                   Integer alertId,
                                   long waitTime) {
        // create job name with escalation id
        String name = EscalationJob.class.getName() + escalationId;

        // create job detail
        JobDetail jd =
            new JobDetail(name + "Job", JOB_GROUP, EscalationJob.class);
        jd.getJobDataMap().put(APP_ID, escalationId);
        jd.getJobDataMap().put(ALERT_ID, alertId);
        // create trigger
        GregorianCalendar next = new GregorianCalendar();
        next.add(GregorianCalendar.MILLISECOND, (int)waitTime);
        SimpleTrigger trigger =
            new SimpleTrigger(name +"Trigger", JOB_GROUP, next.getTime());

        try {
            scheduler.scheduleJob(jd, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException("Can't schedule escalation job");
        }
    }

    public void execute(JobExecutionContext jobContext)
        throws JobExecutionException
    {
        JobDataMap map = jobContext.getJobDetail().getJobDataMap();
        Integer escalationId = (Integer)map.get(APP_ID);
        Integer alertId = (Integer)map.get(ALERT_ID);
        
        CommandContext context = CommandContext.createContext(
            EscalateCommand.setInstance(escalationId, alertId)
        );
        context.execute();
    }
}
