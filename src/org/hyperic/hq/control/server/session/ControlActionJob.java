package org.hyperic.hq.control.server.session;
 
import java.util.Date;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;

import org.hyperic.hq.product.PluginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

/**
 * A quartz job class for handling control actions on a single entity
 *
 */
public class ControlActionJob extends ControlJob {

    protected Log log = LogFactory.getLog(ControlActionJob.class.getName());

    // Public interface for quartz
    public void execute(JobExecutionContext context)
     {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        Integer idVal = new Integer(dataMap.getString(PROP_ID));
        Integer type = new Integer(dataMap.getString(PROP_TYPE));
        AppdefEntityID id = new AppdefEntityID(type.intValue(), idVal.intValue());
        Integer subjectId = new Integer(dataMap.getString(PROP_SUBJECT));
        AuthzSubjectValue subject = null;
        try {
            subject = getSubject(subjectId);
        } catch (JobExecutionException e1) {
            log.error(e1.getMessage(), e1);
        }
        String action = dataMap.getString(PROP_ACTION);
        String args = dataMap.getString(PROP_ARGS);

        Boolean scheduled = new Boolean(dataMap.getString(PROP_SCHEDULED));
        String description = dataMap.getString(PROP_DESCRIPTION);

        Trigger trigger = context.getTrigger();
        Date dateScheduled = trigger.getPreviousFireTime();

        try {
            doAgentControlCommand(id, null, null, subject, dateScheduled, 
                                  scheduled, description, action, args);
        } catch(PluginException e) {
            log.error(e.getMessage(), e);
        }
    }
}
