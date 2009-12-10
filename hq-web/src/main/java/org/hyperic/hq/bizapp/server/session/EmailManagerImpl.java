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

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.server.action.email.EmailFilter;
import org.hyperic.hq.bizapp.server.action.email.EmailRecipient;
import org.hyperic.hq.bizapp.shared.EmailManager;
import org.hyperic.hq.context.Bootstrap;
import org.jboss.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This SessionEJB is used to ensure that EmailFilter does not fail since it
 * requires an associated session. Class uses transaction type NotSupported so
 * that callers don't get hung up on Email since it is an I/O operation.
 */
@Service
@Transactional
// Really? TODO
public class EmailManagerImpl implements EmailManager {

    private final Log log = LogFactory.getLog(EmailManagerImpl.class.getName());
    private MBeanServer server;

    @Autowired
    public EmailManagerImpl(MBeanServer server) {
        this.server = server;
    }

    @PostConstruct
    public void initJBossMailServer() {
        MailService mailService = new MailService();
        mailService.setJNDIName("java:/SpiderMail");
        mailService.setUser("EAM Application");
        mailService.setPassword("password");
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                getClass().getClassLoader().getResourceAsStream("jboss-mail-config.xml"));
            Element config = (Element) document.getFirstChild();
            mailService.setConfiguration(config);
        } catch (Exception e) {
            log.error("Error parsing JBoss mail service configuration.  Unable to create mail service.", e);
            return;
        }
        try {
            server.registerMBean(mailService, new ObjectName("jboss:service=SpiderMail"));
        } catch (Exception e) {
            log.error("Error registering JBoss mail service MBean", e);
        }

    }

    /**
     */
    public void sendAlert(EmailFilter filter, AppdefEntityID appEnt, EmailRecipient[] addresses, String subject,
                          String[] body, String[] htmlBody, int priority, boolean filterNotifications) {
        filter.sendAlert(appEnt, addresses, subject, body, htmlBody, priority, filterNotifications);
    }

    public static EmailManager getOne() {
        return Bootstrap.getBean(EmailManager.class);
    }
}
