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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.Notify;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;

public class EmailAction extends EmailActionConfig
    implements ActionInterface, Notify
{
    protected static final String LINK_FORMAT =
        "alerts/Alerts.do?mode=viewAlert&eid={0,number,#}:{1,number,#}&a={2,number,#}";

    private static final String SEPARATOR =
        "\n\n------------------------------------------\n\n";

    public static final String RES_NAME_HOLDER = "RES_NAME_REPL";
    public static final String RES_DESC_HOLDER = "RES_DESC_REPL";

    protected static String baseUrl = null;

    private Log log = LogFactory.getLog( EmailAction.class.getName() );

    private AuthzSubjectManagerLocal subjMan;

    public EmailAction() {
    }

    protected AuthzSubjectManagerLocal getSubjMan() {
        if (subjMan == null) {
            subjMan = AuthzSubjectManagerEJBImpl.getOne();
        }
        return subjMan;
    }

    protected String createPriority(AlertDefinitionInterface alertdef) {
        StringBuffer pri = new StringBuffer();
        for (int i = 0; i < alertdef.getPriority(); i++) {
            pri.append('!');
        }
        return pri.toString();
    }

    protected String createSubject(AlertDefinitionInterface alertdef) {
        // XXX - Where can I get product name?
        StringBuffer subj = new StringBuffer("[HQ] ")
            .append(createPriority(alertdef))
            .append(" - ")
            .append(alertdef.getName())
            .append(" ")
            .append(RES_NAME_HOLDER);
        return subj.toString();
    }

    private String getBaseURL()
        throws ConfigPropertyException
    {
        if (baseUrl == null) {
            baseUrl = ServerConfigManagerEJBImpl.getOne()
                .getConfig().getProperty(HQConstants.BaseURL);

            // make sure no extra slashes (/)
            if ( baseUrl.charAt(baseUrl.length() - 1) != '/') {
                baseUrl += '/';
            }
        }
        return baseUrl;
    }

    protected String createLink(AppdefEntityID aeid, Integer aid)
        throws ConfigPropertyException
    {
        StringBuffer text = new StringBuffer();

        // Create link
        Object[] args = { new Integer(aeid.getType()), aeid.getId(), aid };
        String alertUrl = MessageFormat.format(LINK_FORMAT, args);
        text.append(getBaseURL()).append(alertUrl);

        return text.toString();
    }

    protected String createText(AlertDefinitionInterface alertdef,
                                String longReason, AppdefEntityID aeid,
                                Integer alertId)
        throws MeasurementNotFoundException
    {
//        String alertTime = dformat.format(new Date(event.getTimestamp()));

        // Organize the events by trigger
//        TriggerFiredEvent[] firedEvents = event.getRootEvents();
//        HashMap eventMap = new HashMap();
//        for (int i = 0; i < firedEvents.length; i++) {
//            eventMap.put(firedEvents[i].getInstanceId(),
//                         firedEvents[i]);
//        }

        StringBuffer text = new StringBuffer("The ")
            .append(RES_NAME_HOLDER).append(" ")
            .append(aeid.getTypeName())
            .append(" has generated the following alert -\n")
            .append(longReason)
            .append(SEPARATOR);

        text.append("ALERT DETAIL")
            .append("\n- Resource Name: ").append(RES_NAME_HOLDER)
            .append("\n- Resource Description: ").append(RES_DESC_HOLDER)
            .append("\n- Alert Name: ").append(alertdef.getName());
        
        if (alertdef.getDescription() != null &&
            alertdef.getDescription().length() > 0) {
            text.append("\n- Description: ").append(alertdef.getDescription());
        }

        // Now go through the condition set
        text.append("\n- Condition Set: ");

        // Get the conditions
        text.append(longReason);

        // The rest of the alert details
        text.append("\n- Alert Severity: ")
            .append(EventConstants.getPriority(alertdef.getPriority()))
//            .append("\n- Alert Date / Time: ").append(alertTime)
            .append(SEPARATOR);

        try {
            // Create the links
            text.append("\nFor additional detail about this alert, go to ")
                .append(createLink(aeid, alertId))
                .append(SEPARATOR);

            // Public Service Announcement
            text.append("This message was delivered to you by Hyperic HQ.")
                .append("\nTo view the HQ Dashboard, go to ")
                .append(getBaseURL())
                .append("Dashboard.do")
                .append(SEPARATOR);
        } catch (ConfigPropertyException e) {
            log.error("Error getting HQ config.  Can't add link to email.", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Error finding event id.  Can't add link to email.", e);
        }

        return text.toString();
    }

    public String execute(AlertInterface alert, ActionExecutionInfo info) 
        throws ActionExecuteException 
    {
        try {
            InternetAddress[] to = lookupEmailAddr();

            EmailFilter filter = EmailFilter.getInstance();

            AlertDefinitionInterface alertDef =
                alert.getAlertDefinitionInterface();
            AppdefEntityID appEnt = new AppdefEntityID(alertDef.getAppdefType(),
                                                       alertDef.getAppdefId());

            String body = isSms() ? info.getShortReason() :
                                    createText(alertDef, info.getLongReason(), 
                                               appEnt, alert.getId()); 

            filter.sendAlert(appEnt, to, createSubject(alertDef), body,
                             alertDef.isNotifyFiltered());

            StringBuffer result = getLog();
            return result.toString();
        } catch (Exception e) {
            throw new ActionExecuteException(e);
        }
    }

    protected StringBuffer getLog() {
        StringBuffer result = new StringBuffer();
        // XXX: Should get this strings into a resource file
        switch (getType()) {
        case TYPE_USERS :
            result.append("HQ Users Notified: ");
            break;
        default :
        case TYPE_EMAILS :
            result.append("Other Recipients Notified: ");
            break;
        }
        result.append(getNames());
        return result;
    }

    protected InternetAddress[] lookupEmailAddr()
        throws ActionExecuteException
    {
        // First, look up the addresses
        Integer uid;
        int i = 0;
        List validAddresses = new ArrayList();
        for (Iterator it = getUsers().iterator(); it.hasNext(); i++) {
            try {
                AuthzSubjectValue overlord = getSubjMan().getOverlord();
                switch (getType()) {
                case TYPE_USERS:
                    uid = (Integer) it.next();
                    AuthzSubjectValue who =
                        getSubjMan().findSubjectById(overlord, uid);
                    if (isSms()) {
                        validAddresses.add(
                            new InternetAddress(who.getSMSAddress()));
                    } else {
                        validAddresses.add(
                            new InternetAddress(who.getEmailAddress()));
                    }
                    break;
                default:
                case TYPE_EMAILS:
                    validAddresses.add(new InternetAddress((String) it.next(),
                                                           true));
                    break;
                }
            } catch (AddressException e) {
                log.warn("Mail address invalid", e);
                continue;
            } catch (PermissionException e) {
                // authz failure...should not happen since its the overlord
                // doing the user lookup
                continue;
            }
        }

        // Convert the valid addresses
        InternetAddress[] to = (InternetAddress[])
            validAddresses.toArray(new InternetAddress[0]);
        return to;
    }

    public void setParentActionConfig(AppdefEntityID aeid,
                                      ConfigResponse config)
        throws InvalidActionDataException {
        init(config);
    }

    public void send(Escalatable alert, String message)
        throws ActionExecuteException
    {
        PerformsEscalations def = alert.getDefinition();
        
        InternetAddress[] to = lookupEmailAddr();

        EmailFilter filter = EmailFilter.getInstance();

        filter.sendAlert(null, to, createSubject(def.getDefinitionInfo()), 
                         message, false);
    }
}
