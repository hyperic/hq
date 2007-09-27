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

import java.io.File;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalationStateChange;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.Notify;
import org.hyperic.hq.hqu.rendit.RenditServer;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.util.config.ConfigResponse;

public class EmailAction extends EmailActionConfig
    implements ActionInterface, Notify
{
    public static final String RES_NAME_HOLDER = "RES_NAME_REPL";
    public static final String RES_DESC_HOLDER = "RES_DESC_REPL";

    protected static String baseUrl = null;

    private Log _log = LogFactory.getLog(EmailAction.class);

    private AuthzSubjectManagerLocal subjMan;

    public EmailAction() {
    }

    protected AuthzSubjectManagerLocal getSubjMan() {
        if (subjMan == null) {
            subjMan = AuthzSubjectManagerEJBImpl.getOne();
        }
        return subjMan;
    }

    private String createPriority(AlertDefinitionInterface alertdef) {
        StringBuffer pri = new StringBuffer();
        for (int i = 0; i < alertdef.getPriority(); i++) {
            pri.append('!');
        }
        return pri.toString();
    }

    private String createSubject(AlertDefinitionInterface alertdef) {
        // XXX - Where can I get product name?
        StringBuffer subj = new StringBuffer("[HQ] ")
            .append(createPriority(alertdef))
            .append(" - ")
            .append(alertdef.getName())
            .append(" ")
            .append(RES_NAME_HOLDER);
        return subj.toString();
    }

    private String createText(AlertDefinitionInterface alertdef,
                              ActionExecutionInfo info, AppdefEntityID aeid, 
                              AlertInterface alert, String templateName)
        throws MeasurementNotFoundException
    {
        File templateDir = new File(HQApp.getInstance().getResourceDir(),
                                    "alertTemplates");
        File templateFile = new File(templateDir, templateName);
        StringWriter output = new StringWriter();
        Map params = new HashMap();
        
        ResourceDAO rDao = new ResourceDAO(DAOFactory.getDAOFactory());
        
        params.put("alertDef", alertdef);
        params.put("alert", alert);
        params.put("action", info);
        params.put("resource", rDao.findByInstanceId(aeid.getAuthzTypeId(),
                                                     aeid.getId()));
        
        try {
            RenditServer.getInstance().renderTemplate(templateFile, params, 
                                                      output);
            
            if (_log.isDebugEnabled())
                _log.debug("Template rendered\n" + output.toString());
        } catch(Exception e) {
            _log.warn("Unable to render template", e);
        }
        return output.toString();
    }
    
    private AppdefEntityID getResource(AlertDefinitionInterface def) {
        return new AppdefEntityID(def.getAppdefType(), def.getAppdefId());
    }

    public String execute(AlertInterface alert, ActionExecutionInfo info) 
        throws ActionExecuteException 
    {
        try {
            EmailRecipient[] to = lookupEmailAddr();
            
            if (to.length == 0) {
                return "No valid users or emails found to send alert";
            }

            EmailFilter filter = new EmailFilter();

            AlertDefinitionInterface alertDef =
                alert.getAlertDefinitionInterface();
            AppdefEntityID appEnt = getResource(alertDef);

            String body, htmlBody;
            
            if (isSms()) {
                body = createText(alertDef, info, appEnt, alert, 
                                  "sms_email.gsp");
                htmlBody = "";
                for (int i=0; i<to.length; i++) {
                    to[i].setHtml(false);
                }
            } else {
                body = createText(alertDef, info, appEnt, alert, 
                                  "text_email.gsp");
                htmlBody = createText(alertDef, info, appEnt, alert,
                                      "html_email.gsp");
            }

            filter.sendAlert(appEnt, to, createSubject(alertDef), body,
                             htmlBody, alertDef.getPriority(),
                             alertDef.isNotifyFiltered());

            StringBuffer result = getLog(to);
            return result.toString();
        } catch (Exception e) {
            throw new ActionExecuteException(e);
        }
    }

    protected StringBuffer getLog(EmailRecipient[] to) {
        StringBuffer result = new StringBuffer(isSms() ? "SMS" : "Notified");
        // XXX: Should get this strings into a resource file
        switch (getType()) {
        case TYPE_USERS :
            result.append(" users: ");
            break;
        default :
        case TYPE_EMAILS :
            result.append(": ");
            break;
        }
        
        for (int i = 0; i < to.length; i++) {
            result.append(to[i].getAddress().getPersonal());
            if (i < to.length - 1) {
                result.append(", ");
            }
        }

        return result;
    }

    protected EmailRecipient[] lookupEmailAddr()
        throws ActionExecuteException
    {
        // First, look up the addresses
        Integer uid;
        int i = 0;
        HashSet prevRecipients = new HashSet();
        List validRecipients = new ArrayList();
        for (Iterator it = getUsers().iterator(); it.hasNext(); i++) {
            try {
                InternetAddress addr;
                boolean useHtml = false;
                
                switch (getType()) {
                case TYPE_USERS:
                    uid = (Integer) it.next();
                    AuthzSubject who = getSubjMan().getSubjectById(uid);
                    
                    if (who == null) {
                        _log.warn("User not found: " + uid);
                        continue;
                    }

                    if (isSms()) {
                        addr = new InternetAddress(who.getSMSAddress());
                    } else {
                        addr = new InternetAddress(who.getEmailAddress());
                    }
                    addr.setPersonal(who.getName());
                    useHtml = who.isHtmlEmail();
                    break;
                default:
                case TYPE_EMAILS:
                    addr = new InternetAddress((String) it.next(), true);
                    addr.setPersonal(addr.getAddress());
                    break;
                }
                
                // Don't send duplicate notifications
                if (prevRecipients.add(addr)) {
                    validRecipients.add(new EmailRecipient(addr, useHtml));
                }
            } catch (AddressException e) {
                _log.warn("Mail address invalid", e);
                continue;
            } catch (UnsupportedEncodingException e) {
                _log.warn("Username encoding error", e);
                continue;
            } catch (Exception e) {
                _log.warn("Email lookup failed");
                _log.debug("Email lookup failed", e);
                continue;
            }
        }

        // Convert the valid addresses
        EmailRecipient[] to = (EmailRecipient[])
            validRecipients.toArray(new EmailRecipient[validRecipients.size()]);
        return to;
    }

    public void setParentActionConfig(AppdefEntityID ent, ConfigResponse cfg)
        throws InvalidActionDataException 
    {
        init(cfg);
    }

    public void send(Escalatable alert, EscalationStateChange change, 
                     String message)
        throws ActionExecuteException 
    {
        PerformsEscalations def = alert.getDefinition();
        
        EmailRecipient[] to = lookupEmailAddr();

        EmailFilter filter = new EmailFilter();

        for (int i=0; i<to.length; i++) {
            to[i].setHtml(false);
        }
        AlertDefinitionInterface defInfo = def.getDefinitionInfo();
        filter.sendAlert(getResource(defInfo), to, 
                         createSubject(defInfo) + " " + change.getDescription(), 
                         message, message, defInfo.getPriority(), false);
    }
}
