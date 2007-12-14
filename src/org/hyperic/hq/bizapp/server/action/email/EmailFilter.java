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

package org.hyperic.hq.bizapp.server.action.email;

import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.scheduler.server.session.SchedulerEJBImpl;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.collection.IntHashMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

public class EmailFilter {
    private Log _log = LogFactory.getLog(EmailFilter.class);

    private static final String     JOB_GROUP  = "EmailFilterGroup";
    private static final IntHashMap _alertBuffer = new IntHashMap();
    private static final Object SCHEDULER_LOCK = new Object();

    public EmailFilter() {}

    public String getAppdefEntityName(AppdefEntityID appEnt) {
        AuthzSubjectValue overlord =
            AuthzSubjectManagerEJBImpl.getOne().getOverlord();
        try {
            AppdefEntityValue entVal =
                new AppdefEntityValue(appEnt, overlord);
            return entVal.getName();
        } catch (AppdefEntityNotFoundException e) {
            _log.error("Entity ID invalid: " + e);
        } catch (PermissionException e) {
            // Should never happen, because we are overlord
            _log.error("Overlord not allowed to lookup resource: " + e);
        }

        return appEnt.toString();
    }
    
    private void replaceAppdefEntityHolders(AppdefEntityID appEnt,
                                            String[] strs) {
        AuthzSubjectValue overlord =
            AuthzSubjectManagerEJBImpl.getOne().getOverlord();

        try {
            AppdefEntityValue entVal =
                new AppdefEntityValue(appEnt, overlord);
            String name = entVal.getName();
            String desc = entVal.getDescription();

            if (desc == null) {
                desc = "";
            }

            for (int i = 0; i < strs.length; i++) {
                strs[i] = strs[i].replaceAll(EmailAction.RES_NAME_HOLDER,
                                             name);
                strs[i] = strs[i].replaceAll(EmailAction.RES_DESC_HOLDER,
                                             desc);
            }
        } catch (AppdefEntityNotFoundException e) {
            _log.error("Entity ID invalid", e);
        } catch (PermissionException e) {
            // Should never happen, because we are overlord
            _log.error("Overlord not allowed to lookup resource", e);
        }
    }
    
    public void sendAlert(AppdefEntityID appEnt, EmailRecipient[] addresses,
                          String subject, String body, String htmlBody, 
                          boolean filter)
    {
        if (appEnt == null) {
            // Go ahead and just send the alert
            sendEmail(addresses, subject, body, htmlBody);
            return;
        }

        // Replace the resource name
        String[] replStrs = new String[] { subject, body };
        replaceAppdefEntityHolders(appEnt, replStrs);
        subject = replStrs[0];
        body = replStrs[1];
            
        // See if alert needs to be filtered
        if (filter) {
            PlatformManagerLocal pltMan =
                PlatformManagerEJBImpl.getOne();
            try {
                // Now let's look up the platform ID
                Integer platId;
                    
                switch (appEnt.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    platId = appEnt.getId();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    platId = pltMan.getPlatformIdByServer(appEnt.getId());
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    platId = pltMan.getPlatformIdByService(appEnt.getId());
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
                            Map cache = (Map) _alertBuffer.get(platId.intValue());
                            
                            if (cache == null) {
                                // Make sure we check again in 5 minutes
                                cache = new Hashtable();
                                _alertBuffer.put(platId.intValue(), cache);
                            }

                            for (int i = 0; i < addresses.length; i++) {
                                FilterBuffer msg;
                                if (cache.containsKey(addresses[i])) {
                                    // Create new buffer with previous body
                                    msg = (FilterBuffer)cache.get(addresses[i]); 
                                    msg.append("\n", "\n");
                                } else {
                                    msg = new FilterBuffer();
                                }
    
                                msg.incrementEntries();
                                msg.append(body, htmlBody);
                                cache.put(addresses[i], msg);
                            }
    
                            filter = true;
                        } else {
                            // Add a new queue
                            _alertBuffer.put(platId.intValue(), new Hashtable());
                        }
                    }
                }

                try {
                    scheduleJob(platId);
                } catch (SchedulerException e) {
                    //  Job probably already exists
                    _log.error("Unable to reschedule job " + platId, e);
                }
    
                if (filter)
                    return;
            } catch (PlatformNotFoundException e) {
                _log.error("Entity ID invalid: " + e);
            }
        }
            
        sendEmail(addresses, subject, body, htmlBody);
    }
    
    private InternetAddress getFromAddress() {
        ServerConfigManagerLocal configMan =
            ServerConfigManagerEJBImpl.getOne();
        try {
            Properties props = configMan.getConfig();
            String from = props.getProperty(HQConstants.EmailSender);
            if (from != null) {
                return new InternetAddress(from);
            }
        } catch (ConfigPropertyException e) {
            _log.error("ConfigPropertyException fetch FROM address", e);
        } catch (AddressException e) {
            _log.error("Bad FROM address", e);
        }
        return null;
    }
    
    private void sendEmail(EmailRecipient[] addresses, String subject, 
                           String body, String htmlBody)
    {
        Session session;
        try {
            session = (Session) 
            PortableRemoteObject.narrow(
                               new InitialContext().lookup("java:/SpiderMail"),
                               Session.class);
        } catch(NamingException e) {
            throw new SystemException(e);
        }

        MimeMessage m = new MimeMessage(session);
        
        try {
            InternetAddress from = this.getFromAddress();
            if (from == null) {
                m.setFrom();
            } else {
                m.setFrom(from);
            }

            m.setSubject(subject);
            
            if (_log.isDebugEnabled()) {
                _log.debug("Sending Alert Email: " + body);
                _log.debug("Sending HTML Alert Email: " + htmlBody);
            }

            // Send to each recipient individually (for D.B. SMS)
            for (int i = 0; i < addresses.length; i++) {
                m.setRecipient(Message.RecipientType.TO, 
                               addresses[i].getAddress());
                
                if (addresses[i].useHtml()) {
                    m.setContent(htmlBody, "text/html");
                } else {
                    m.setContent(body, "text/plain");
                }
                
                Transport.send(m);
            }
        } catch (MessagingException e) {
            _log.error("Error sending email: " + subject);
            _log.debug("Messaging Error sending email", e);
        }
    }
    
    void sendFiltered(int platId) {
        Hashtable cache;
        
        synchronized (_alertBuffer) {
            if (!_alertBuffer.containsKey(platId))
                return;

            cache = (Hashtable) _alertBuffer.remove(platId);
        
            if (cache == null || cache.size() == 0)
                return;
        
            // Insert key again so that we continue filtering
            _alertBuffer.put(platId, null);
        }
        
        AppdefEntityID platEntId = AppdefEntityID.newPlatformID(platId); 
        String platName = getAppdefEntityName(platEntId);
    
        // The cache is organized by addresses
        for (Iterator i = cache.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            EmailRecipient addr = (EmailRecipient)ent.getKey();
            FilterBuffer msg = (FilterBuffer)ent.getValue();
            
            if (msg.getNumEnts() == 1 && addr.useHtml()) {
                sendEmail(new EmailRecipient[] { addr },
                          "[HQ] Filtered Notifications for " + platName,
                          "", msg.getHtml());
            } else {
                addr.setHtml(false);
                sendEmail(new EmailRecipient[] { addr },
                          "[HQ] Filtered Notifications for " + platName,
                          msg.getText(), "");
            }
        }
    }

    private void scheduleJob(Integer platId) throws SchedulerException {
        // Create new job name with the appId
        String name = EmailFilterJob.class.getName() + platId + "Job";

        SchedulerLocal scheduler = SchedulerEJBImpl.getOne();

        synchronized (SCHEDULER_LOCK) {

            Trigger[] triggers = scheduler.getTriggersOfJob(name, JOB_GROUP);
            if (triggers.length == 0) {
                JobDetail jobDetail = new JobDetail(name, JOB_GROUP,
                                                    EmailFilterJob.class);

                String appIdStr = platId.toString();

                jobDetail.getJobDataMap().put(EmailFilterJob.APP_ID, appIdStr);

                // XXX: Make this time configurable?
                GregorianCalendar next = new GregorianCalendar();
                next.add(GregorianCalendar.MINUTE, 5);
                SimpleTrigger t = new SimpleTrigger(name + "Trigger", JOB_GROUP,
                                                    next.getTime());

                Date nextfire = scheduler.scheduleJob(jobDetail, t);
                _log.debug("Will queue alerts for platform " +
                           platId + " until " + nextfire);
            } else {
                // Already scheduled, there will only be a single trigger.
                _log.debug("Already queing alerts for platform " +
                           platId + ", will fire at " +
                           triggers[0].getNextFireTime());
            }
        }
    }
}
