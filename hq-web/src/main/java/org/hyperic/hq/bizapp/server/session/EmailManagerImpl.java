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

package org.hyperic.hq.bizapp.server.session;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.server.action.email.EmailFilter;
import org.hyperic.hq.bizapp.server.action.email.EmailRecipient;
import org.hyperic.hq.bizapp.shared.EmailManager;
import org.hyperic.hq.common.server.session.ServerConfigManagerImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class EmailManagerImpl implements EmailManager {
    private JavaMailSender mailSender;
    private ServerConfigManager serverConfigManager;
    final Log log = LogFactory.getLog(EmailManagerImpl.class);


    @Autowired
    public EmailManagerImpl( ServerConfigManager serverConfigManager) {
        //this.mailSender = mailSender;
        this.serverConfigManager = serverConfigManager;
    }

    public void sendAlert(EmailFilter filter, AppdefEntityID appEnt, EmailRecipient[] addresses, String subject,
                          String[] body, String[] htmlBody, int priority, boolean filterNotifications) {
        filter.sendAlert(appEnt, addresses, subject, body, htmlBody, priority, filterNotifications);
    }

    public void sendEmail(EmailRecipient[] addresses, String subject, String[] body, String[] htmlBody,
                          Integer priority) {
        final Log log = LogFactory.getLog(EmailFilter.class);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            InternetAddress from = getFromAddress();
            if (from == null) {
                mimeMessage.setFrom();
            } else {
                mimeMessage.setFrom(from);
            }

            mimeMessage.setSubject(subject);

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
                mimeMessage.setRecipient(Message.RecipientType.TO,
                                         addresses[i].getAddress());

                if (addresses[i].useHtml()) {
                    mimeMessage.setContent(htmlBody[i], "text/html");
                    if (log.isDebugEnabled()) {
                        log.debug("Sending HTML Alert notification: " +
                                  subject + " to " +
                                  addresses[i].getAddress().getAddress());
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Sending Alert notification: " + subject +
                                  " to " +
                                  addresses[i].getAddress().getAddress());
                    }
                    mimeMessage.setContent(body[i], "text/plain");
                }

                mailSender.send(mimeMessage);
            }
        } catch (MessagingException e) {
            log.error("Error sending email: " + subject);
            log.debug("Messaging Error sending email", e);
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

    public static EmailManager getOne() {
        return Bootstrap.getBean(EmailManager.class);
    }
}
