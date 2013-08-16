/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.bizapp.server.session;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.server.action.email.EmailFilterJob;
import org.hyperic.hq.bizapp.server.action.email.EmailRecipient;
import org.hyperic.hq.bizapp.shared.EmailManager;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.timer.StopWatch;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class EmailManagerImpl implements EmailManager {
    private JavaMailSender mailSender;
    private ServerConfigManager serverConfigManager;
    private PlatformManager platformManager;
    private ResourceManager resourceManager;
    private Session mailSession;
    private ConcurrentStatsCollector concurrentStatsCollector;
    
    final Log log = LogFactory.getLog(EmailManagerImpl.class);

    public static final String JOB_GROUP = "EmailFilterGroup";
    private static final IntHashMap _alertBuffer = new IntHashMap();
    public static final Object SCHEDULER_LOCK = new Object();

    @Autowired
    public EmailManagerImpl(JavaMailSender mailSender, ServerConfigManager serverConfigManager,
                            PlatformManager platformManager, ResourceManager resourceManager,
                            Session mailSession, ConcurrentStatsCollector concurrentStatsCollector) {
        this.mailSender = mailSender;
        this.mailSession = mailSession;
        this.serverConfigManager = serverConfigManager;
        this.platformManager = platformManager;
        this.resourceManager = resourceManager;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }
    
    @PostConstruct
    public void initStats() {
    	concurrentStatsCollector.register(ConcurrentStatsCollector.SEND_ALERT_TIME);
    }

    public void sendEmail(EmailRecipient[] addresses, String subject, String[] body, String[] htmlBody, Integer priority) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        final StopWatch watch = new StopWatch();
        try {
            InternetAddress from = getFromAddress();
            if (from == null) {
                mimeMessage.setFrom();
            } else {
                mimeMessage.setFrom(from);
            }

            // HHQ-5708
            // remove any possible new line from the subject
            // the subject can be render form 'subject.gsp' file
            mimeMessage.setSubject(subject.replace("\r", "").replace("\n", ""));

            // If priority not null, set it in body 
            if (priority != null) {
                switch (priority.intValue()) {
                    case EventConstants.PRIORITY_HIGH:
                        mimeMessage.addHeader("X-Priority", "1");
                        break;
                    case EventConstants.PRIORITY_MEDIUM:
                        mimeMessage.addHeader("X-Priority", "2");
                        break;
                    default:
                        break;
                }
            }

            // Send to each recipient individually (for D.B. SMS)
            for (int i = 0; i < addresses.length; i++) {
                mimeMessage.setRecipient(Message.RecipientType.TO, addresses[i].getAddress());

                if (addresses[i].useHtml()) {
                    mimeMessage.setContent(htmlBody[i], "text/html; charset=UTF-8");
                    if (log.isDebugEnabled()) {
                        log.debug("Sending HTML Alert notification: " + subject + " to " +
                                  addresses[i].getAddress().getAddress() +
                                  "\n" + htmlBody[i]);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Sending Alert notification: " + subject + " to " +
                                  addresses[i].getAddress().getAddress() +
                                  "\n" + body[i]);
                    }
                    mimeMessage.setContent(body[i], "text/plain; charset=UTF-8");
                }

                mailSender.send(mimeMessage);
            }
        } catch (MessagingException e) {
            log.error("Error sending email: " + subject);
            log.debug("MessagingException sending email", e);
        } catch (MailException me) {
            log.error("Error sending email: " + subject);
            log.debug("MailException sending email", me);
        } finally {
            if (watch.getElapsed() >= MeasurementConstants.MINUTE) {
                log.warn("sending email using mailServer=" + mailSession.getProperties() + 
                         " took " + watch.getElapsed() + " ms.  Please check with your mail administrator.");
            }
        }
    }

    private InternetAddress getFromAddress() {
        try {
            Properties props = serverConfigManager.getConfig();
            String from = props.getProperty(HQConstants.EmailSender);
            if (from != null) {
                return new InternetAddress(from);
            }
        } catch (ConfigPropertyException e) {
            log.error("ConfigPropertyException fetch FROM address", e);
        } catch (AddressException e) {
            log.error("Bad FROM address", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void sendFiltered(Integer platId) {
        Hashtable<EmailRecipient, FilterBuffer> cache;
        synchronized (_alertBuffer) {
            if (!_alertBuffer.containsKey(platId)) {
                return;
            }
            cache = (Hashtable<EmailRecipient, FilterBuffer>) _alertBuffer.remove(platId);
            if (cache == null || cache.size() == 0) {
                return;
            }
            // Insert key again so that we continue filtering
            _alertBuffer.put(platId, null);
        }

        AppdefEntityID platEntId = AppdefEntityID.newPlatformID(platId);
        String platName = resourceManager.getAppdefEntityName(platEntId);

        // The cache is organized by addresses
        for (Entry<EmailRecipient, FilterBuffer> ent : cache.entrySet()) {
            EmailRecipient addr = (EmailRecipient) ent.getKey();
            FilterBuffer msg = (FilterBuffer) ent.getValue();
            if (msg.getNumEnts() == 1 && addr.useHtml()) {
                sendEmail(new EmailRecipient[] { addr }, "[HQ] Filtered Notifications for " + platName,
                    new String[] { "" }, new String[] { msg.getHtml() }, null);
            } else {
                addr.setHtml(false);
                sendEmail(new EmailRecipient[] { addr }, "[HQ] Filtered Notifications for " + platName,
                    new String[] { msg.getText() }, new String[] { "" }, null);
            }
        }
    }

    public void sendAlert(AppdefEntityID appEnt, EmailRecipient[] addresses, String subject, String[] body,
                          String[] htmlBody, int priority, boolean filter) {
        final StopWatch watch = new StopWatch();
        try {
            _sendAlert(appEnt, addresses, subject, body, htmlBody, priority, filter);
        } finally {
            concurrentStatsCollector.addStat(watch.getElapsed(), ConcurrentStatsCollector.SEND_ALERT_TIME);
        }
    }

    private void _sendAlert(AppdefEntityID appEnt, EmailRecipient[] addresses, String subject, String[] body,
                            String[] htmlBody, int priority, boolean filter) {
        if (appEnt == null) {
            // Go ahead and just send the alert
            sendEmail(addresses, subject, body, htmlBody, new Integer(priority));
            return;
        }

        // See if alert needs to be filtered
        if (filter) {
            
            try {
                // Now let's look up the platform ID
                Integer platId;

                switch (appEnt.getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                        platId = appEnt.getId();
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                        platId = platformManager.getPlatformIdByServer(appEnt.getId());
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                        platId = platformManager.getPlatformIdByService(appEnt.getId());
                        break;
                    default:
                        platId = null;
                        break;
                }

                filter = false;

                // Let's see if we are adding or sending
                if (platId != null) {
                    synchronized (_alertBuffer) {
                        if (_alertBuffer.containsKey(platId.intValue())) {
                            // Queue it up
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            Map<EmailRecipient, FilterBuffer> cache = (Map) _alertBuffer.get(platId.intValue());

                            if (cache == null) {
                                // Make sure we check again in 5 minutes
                                cache = new Hashtable<EmailRecipient, FilterBuffer>();
                                _alertBuffer.put(platId.intValue(), cache);
                            }

                            for (int i = 0; i < addresses.length; i++) {
                                FilterBuffer msg;
                                if (cache.containsKey(addresses[i])) {
                                    // Create new buffer with previous body
                                    msg = (FilterBuffer) cache.get(addresses[i]);
                                    msg.append("\n", "\n");
                                } else {
                                    msg = new FilterBuffer();
                                }

                                msg.incrementEntries();
                                msg.append(body[i], htmlBody[i]);
                                cache.put(addresses[i], msg);
                            }

                            filter = true;
                        } else {
                            // Add a new queue
                            _alertBuffer.put(platId.intValue(), new Hashtable<EmailRecipient, FilterBuffer>());
                        }
                    }
                }

                try {
                    scheduleJob(platId);
                } catch (SchedulerException e) {
                    // Job probably already exists
                    log.error("Unable to reschedule job " + platId, e);
                }
                if (filter) {
                    return;
                }
            } catch (PlatformNotFoundException e) {
                log.error("Entity ID invalid: " + e);
            }
        }

        sendEmail(addresses, subject, body, htmlBody, new Integer(priority));
    }

    private void scheduleJob(Integer platId) throws SchedulerException {
        // Create new job name with the appId
        String name = EmailFilterJob.class.getName() + platId + "Job";

        Scheduler scheduler = Bootstrap.getBean(Scheduler.class);

        synchronized (SCHEDULER_LOCK) {

            Trigger[] triggers = scheduler.getTriggersOfJob(name, JOB_GROUP);
            if (triggers.length == 0) {
                JobDetail jobDetail = new JobDetail(name, JOB_GROUP, EmailFilterJob.class);

                String appIdStr = platId.toString();

                jobDetail.getJobDataMap().put(EmailFilterJob.APP_ID, appIdStr);

                // XXX: Make this time configurable?
                GregorianCalendar next = new GregorianCalendar();
                next.add(GregorianCalendar.MINUTE, 5);
                SimpleTrigger t = new SimpleTrigger(name + "Trigger", JOB_GROUP, next.getTime());

                Date nextfire = scheduler.scheduleJob(jobDetail, t);
                log.debug("Will queue alerts for platform " + platId + " until " + nextfire);
            } else {
                // Already scheduled, there will only be a single trigger.
                log.debug("Already queing alerts for platform " + platId + ", will fire at " +
                          triggers[0].getNextFireTime());
            }
        }
    }
}
