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

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.server.session.EscalationStateChange;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.Notify;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.units.FormattedNumber;

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

    private String createLink(AppdefEntityID aeid, Integer aid)
        throws ConfigPropertyException
    {
        StringBuffer text = new StringBuffer();

        // Create link
        Object[] args = { new Integer(aeid.getType()), aeid.getId(), aid };
        String alertUrl = MessageFormat.format(LINK_FORMAT, args);
        text.append(getBaseURL()).append(alertUrl);

        return text.toString();
    }

    private String createText(AlertDefinitionInterface alertdef,
                              ActionExecutionInfo info,
                              AppdefEntityID aeid, AlertInterface alert)
        throws MeasurementNotFoundException
    {
        StringBuffer text = new StringBuffer("The ")
            .append(RES_NAME_HOLDER).append(" ")
            .append(aeid.getTypeName())
            .append(" has generated the following alert -\n")
            .append(info.getShortReason())
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
        text.append(info.getLongReason());

        // XXX: Ashamed of myself, this is definitely not localizable
        // um, when we figger out how we want to make the user's locale
        // something that is accessible to the backend, this should be
        // un-hardcoded
        SimpleDateFormat dformat = new SimpleDateFormat("MM/dd/yyyy hh:mm aaa");
        String alertTime = dformat.format(new Date(alert.getTimestamp()));

        // The rest of the alert details
        text.append("\n- Alert Severity: ")
            .append(EventConstants.getPriority(alertdef.getPriority()))
            .append("\n- Alert Date / Time: ").append(alertTime);

        if (!info.getAuxLogs().isEmpty()) {
            text.append("\nAdditional information:\n");
            addAuxLogs("  ", info.getAuxLogs(), text);
        }
        
        // See if we can get the previous fix
        if (alertdef.performsEscalations()) {
            String lastFix = EscalationManagerEJBImpl.getOne()
                .getLastFix((PerformsEscalations) alertdef);
            if (lastFix != null) {
                text.append("\n- Previous Fix: ")
                    .append(lastFix);
            }
        }
        
        // See if we can get the indicator metrics
        if (aeid.isPlatform() || aeid.isServer() || aeid.isService()) {
            DerivedMeasurementManagerLocal dmMan =
                DerivedMeasurementManagerEJBImpl.getOne();
            List designates = dmMan.findDesignatedMeasurements(aeid);
            
            // Gather the IDs to get the last data points
            Integer[] mids = new Integer[designates.size()];
            int i = 0;
            for (Iterator it = designates.iterator(); it.hasNext(); i++) {
                DerivedMeasurement m = (DerivedMeasurement) it.next();
                mids[i] = m.getId();
            }
            
            DataManagerLocal dMan = DataManagerEJBImpl.getOne();
            Map values =
                dMan.getLastDataPoints(mids,
                                   MeasurementConstants.ACCEPTABLE_LIVE_MILLIS);
            
            // Now output the values
            if (values.size() > 0) {
                text.append("\n- Previous Indicator Metrics: ");
                
                for (Iterator it = designates.iterator(); it.hasNext(); ) {
                    DerivedMeasurement m = (DerivedMeasurement) it.next();
                    if (values.containsKey(m.getId())) {
                        text.append("\n    ")
                            .append(m.getTemplate().getName())
                            .append(": ");
                        MetricValue val = (MetricValue) values.get(m.getId());
                        FormattedNumber th =
                            UnitsConvert.convert(val.getValue(),
                                                 m.getTemplate().getUnits());
                        text.append(th);
                    }
                }
            }
        }

        text.append(SEPARATOR);
        
        try {
            // Create the links
            text.append("\nFor additional detail about this alert, go to ")
                .append(createLink(aeid, alert.getId()))
                .append(SEPARATOR);

            // Public Service Announcement
            text.append("This message was delivered to you by Hyperic HQ.")
                .append("\nTo view the HQ Dashboard, go to ")
                .append(getBaseURL())
                .append("Dashboard.do")
                .append(SEPARATOR);
        } catch (ConfigPropertyException e) {
            _log.error("Error getting HQ config.  Can't add link to email.", e);
        } catch (ArrayIndexOutOfBoundsException e) { // Retarded ... XXX
            _log.error("Error finding event id.  Can't add link to email.", e);
        }
        
        return text.toString();
    }
    
    private void addAuxLogs(String prefix, List logs, StringBuffer text) {
        String baseUrl;
        
        try {
            baseUrl = getBaseURL();
        } catch(ConfigPropertyException e) {
            _log.warn("Unable to get base url");
            return;
        }
        
        for (Iterator i=logs.iterator(); i.hasNext(); ) { 
            AlertAuxLog a = (AlertAuxLog)i.next();
            
            text.append(prefix)
                .append(a.getDescription())
                .append("\n");
            
            if (a.getURL() != null) {
                text.append(prefix)
                    .append("  - ")
                    .append(baseUrl)
                    .append(a.getURL())
                    .append("\n");
            }
            
            addAuxLogs(prefix + "  ", a.getChildren(), text);
        }
    }
    
    private AppdefEntityID getResource(AlertDefinitionInterface def) {
        return new AppdefEntityID(def.getAppdefType(), def.getAppdefId());
    }

    public String execute(AlertInterface alert, ActionExecutionInfo info) 
        throws ActionExecuteException 
    {
        try {
            InternetAddress[] to = lookupEmailAddr();
            
            if (to.length == 0) {
                return "No valid users or emails found to send alert";
            }

            EmailFilter filter = EmailFilter.getInstance();

            AlertDefinitionInterface alertDef =
                alert.getAlertDefinitionInterface();
            AppdefEntityID appEnt = getResource(alertDef);

            String body = isSms() ? info.getShortReason() :
                                    createText(alertDef, info, appEnt, alert);

            filter.sendAlert(appEnt, to, createSubject(alertDef), body,
                             alertDef.isNotifyFiltered());

            StringBuffer result = getLog(to);
            return result.toString();
        } catch (Exception e) {
            throw new ActionExecuteException(e);
        }
    }

    protected StringBuffer getLog(InternetAddress[] to) {
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
            result.append(to[i].getPersonal());
            if (i < to.length - 1) {
                result.append(", ");
            }
        }

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
                InternetAddress addr;
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
                    break;
                default:
                case TYPE_EMAILS:
                    addr = new InternetAddress((String) it.next(), true);
                    addr.setPersonal(addr.getAddress());
                    break;
                }
                validAddresses.add(addr);
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
        InternetAddress[] to = (InternetAddress[])
            validAddresses.toArray(new InternetAddress[0]);
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
        
        InternetAddress[] to = lookupEmailAddr();

        EmailFilter filter = EmailFilter.getInstance();

        AlertDefinitionInterface defInfo = def.getDefinitionInfo();
        filter.sendAlert(getResource(defInfo), to, 
                         createSubject(defInfo) + " " + change.getDescription(), 
                         message, false);
    }
}
