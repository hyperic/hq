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

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.ejb.CreateException;
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
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.hq.scheduler.shared.SchedulerUtil;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.collection.IntHashMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

public class EmailFilter {
    protected Log log = LogFactory.getLog(EmailFilter.class);
    private static final String JOB_GROUP = "EmailFilterGroup";

    private static EmailFilter singleton = new EmailFilter();

    /** Holds value of property alertBuffer. */
    private IntHashMap alertBuffer = new IntHashMap();
    
    /** Holds value of property platform manager. */
    private PlatformManagerLocal pltMan = null;

    /** Holds value of property server config manager. */
    private ServerConfigManagerLocal configMan = null;
    
    /** Holds value of property overlord. */
    AuthzSubjectValue overlord = null;
    
    /** Holds value of property appMan. */
    private SchedulerLocal scheduler = null;
    
    public static EmailFilter getInstance() { return singleton; }

    /** Creates a new instance of EmailFilter */
    public EmailFilter() {}

    private boolean init() {
        if (pltMan != null && overlord != null)
            return true;
            
        try {
            pltMan = PlatformManagerUtil.getLocalHome().create();
            AuthzSubjectManagerLocal authzSubjectManager =
                AuthzSubjectManagerUtil.getLocalHome().create();
            overlord = authzSubjectManager.getOverlord();
            return true;
        } catch (CreateException e) {
            // Then we'll keep those values null
        } catch (NamingException e) {
            // Then we'll keep those values null
        } catch (ObjectNotFoundException e) {
            // Then we'll keep those values null
        }
        return false;
    }

    public String getAppdefEntityName(AppdefEntityID appEnt) {
        if (init()) {
            try {
                AppdefEntityValue entVal =
                    new AppdefEntityValue(appEnt, overlord);
                return entVal.getName();
            } catch (AppdefEntityNotFoundException e) {
                log.error("Entity ID invalid: " + e);
            } catch (PermissionException e) {
                // Should never happen, because we are overlord
                log.error("Overlord not allowed to lookup resource: " + e);
            }
        }
        return appEnt.toString();
    }

    public void sendAlert(AppdefEntityID appEnt, InternetAddress[] addresses,
                          String subject, String body, boolean filter)
        throws NamingException {
        if (appEnt != null) {
            String resName = this.getAppdefEntityName(appEnt);
            
            // Replace the resource name
            subject = subject.replaceAll(EmailAction.RES_NAME_HOLDER, resName);
            body = body.replaceAll(EmailAction.RES_NAME_HOLDER, resName);
            
            // See if alert needs to be filtered
            if (filter && init()) {
                try {
                    // Now let's look up the platform ID
                    Integer platId = null;
                    
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
                        break;
                    }

                    filter = false;

                    // Let's see if we are adding or sending
                    if (platId != null) {
                        if (alertBuffer.containsKey(platId.intValue())) {
                            // Queue it up
                            Map cache =
                                (Map) alertBuffer.get(platId.intValue());
                            
                            if (cache == null) {
                                // Make sure we check again in 5 minutes
                                try {
                                    cache = new Hashtable();
                                    alertBuffer.put(platId.intValue(), cache);
                                    scheduleJob(platId);
                                } catch (SchedulerException e) {
                                    // Job probably already exists
                                    log.error("Unable to reschedule job " +
                                               platId + ": " + e);
                                }

                            }

                            for (int i = 0; i < addresses.length; i++) {
                                StringBuffer msg = null;
                                if (cache.containsKey(addresses[i])) {
                                    // Create new buffer with previous body
                                    msg =
                                        (StringBuffer) cache.get(addresses[i]);
                                    msg.append("\n");
                                }
                                else {
                                    msg = new StringBuffer();
                                }
    
                                // Append the current e-mail body
                                msg.append(body);
                                cache.put(addresses[i], msg);
                            }
    
                            filter = true;
                        } else {
                            // Add new job to send filtered e-mail in 5 minutes
                            scheduleJob(platId);
    
                            // Add a new queue
                            alertBuffer.put(platId.intValue(), new Hashtable());
                        }
                    }
    
                    if (filter)
                        return;
                } catch (PlatformNotFoundException e) {
                    log.error("Entity ID invalid: " + e);
                } catch (SchedulerException e) {
                    log.error("Can't schedule the filtered alert: " + e);
                }
            }
        }

        // Go ahead and just send the alert
        sendEmail(addresses, subject, body);
        return;
    }
    
    private InternetAddress getFromAddress() {
        try {
            if (this.configMan == null) {
                this.configMan = ServerConfigManagerUtil.getLocalHome().create();                
            }
            
            Properties props = this.configMan.getConfig();
            String from = props.getProperty(HQConstants.EmailSender);
            if (from != null) {
                return new InternetAddress(from);
            }
        } catch (ConfigPropertyException e) {
            log.error("ConfigPropertyException fetch FROM address", e);
        } catch (CreateException e) {
            log.error("System error", e);
        } catch (NamingException e) {
            log.error("System error", e);
        } catch (AddressException e) {
            log.error("Bad FROM address", e);
        }
        return null;
    }
    
    private void sendEmail(InternetAddress[] addresses,
                          String subject, String body)
        throws NamingException {
        Session session =
            (Session) PortableRemoteObject.narrow(
                new InitialContext().lookup("java:/SpiderMail"),
                Session.class);

        MimeMessage m = new MimeMessage(session);
        
        try {
            InternetAddress from = this.getFromAddress();
            if (from == null) {
                m.setFrom();
            }
            else {
                m.setFrom(from);
            }

            m.setSubject(subject);
            m.setContent(body, "text/plain");

            // Send to each recipient individually (for D.B. SMS)
            for (int i = 0; i < addresses.length; i++) {
                m.setRecipient(Message.RecipientType.TO, addresses[i]);
                Transport.send(m);
            }
        } catch (MessagingException e) {
            log.error("Error sending email: " + subject);
            log.debug("Messaging Error sending email", e);
        }
    }
    
    public void sendFiltered(int platId)
        throws NamingException {
        if (!alertBuffer.containsKey(platId))
            return;

        AppdefEntityID platEntId = new AppdefEntityID(
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, platId);

        String platName = this.getAppdefEntityName(platEntId);
        
        Hashtable cache = (Hashtable) alertBuffer.remove(platId);
        
        if (cache == null || cache.size() == 0)
            return;
        
        // Insert key again so that we continue filtering
        alertBuffer.put(platId, null);
        
        // The cache is organized by addresses
        Collection addrs = cache.keySet();
        for (Iterator i = addrs.iterator(); i.hasNext(); ) {
            InternetAddress addr = (InternetAddress) i.next();
            sendEmail(new InternetAddress[] { addr },
                      "[HQ] Filtered Notifications for " + platName,
                      cache.get(addr).toString());
        }
    }

    private void scheduleJob(Integer platId) throws SchedulerException {
        try {
            // Create new job name with the appId
            String name = EmailFilterJob.class.getName() + platId;
            
            // Create job detail
            JobDetail jd =
                new JobDetail(name + "Job", JOB_GROUP, EmailFilterJob.class);
            
            String appIdStr = platId.toString();
            
            jd.getJobDataMap().put(EmailFilterJob.APP_ID, appIdStr);
            
            // Create trigger
            GregorianCalendar next = new GregorianCalendar();
            next.add(GregorianCalendar.MINUTE, 5);
            SimpleTrigger t =
                new SimpleTrigger(name + "Trigger", JOB_GROUP, next.getTime());
            
            // Now schedule with EJB
            if (scheduler == null) {
                scheduler = SchedulerUtil.getLocalHome().create();
            }
            
            scheduler.scheduleJob(jd, t);
        } catch (CreateException e) {
            throw new SchedulerException("Can't create scheduler: " + e);
        } catch (NamingException e) {
            throw new SchedulerException("Can't lookup scheduler: " + e);
        }
    }
}
