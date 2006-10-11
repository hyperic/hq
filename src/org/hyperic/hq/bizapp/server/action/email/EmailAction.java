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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.trigger.conditional.ValueChangeTrigger;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionBasicValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.NumberUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.units.FormattedNumber;

/**
 */
public class EmailAction extends EmailActionConfig implements ActionInterface {
    private static final String LINK_FORMAT =
        "alerts/Alerts.do?mode=viewAlert&eid={0,number,#}:{1,number,#}&a={2,number,#}";

    private static final String NOTAVAIL = "Not Available";
    private static final String SEPARATOR =
        "\n\n------------------------------------------\n\n";

    public static final String RES_NAME_HOLDER = "RES_NAME_REPL";

    private static String baseUrl = null;

    private Log log = LogFactory.getLog( EmailAction.class.getName() );

    /** Holds value of property subjMan. */
    private AuthzSubjectManagerLocal subjMan = null;

    /** Creates a new instance of SharedEmailAction */
    public EmailAction() {
    }

    private AuthzSubjectManagerLocal getSubjMan()
        throws NamingException, CreateException {
        if (subjMan == null) {
            subjMan = AuthzSubjectManagerUtil.getLocalHome().create();
        }
        return subjMan;
    }

    private String createPriority(AlertDefinitionBasicValue alertdef) {
        StringBuffer pri = new StringBuffer();
        for (int i = 0; i < alertdef.getPriority(); i++) {
            pri.append('!');
        }
        return pri.toString();
    }

    private String createSubject(AlertDefinitionBasicValue alertdef) {
        // XXX - Where can I get product name?
        StringBuffer subj = new StringBuffer("[HQ] ")
            .append(createPriority(alertdef))
            .append(" - ")
            .append(alertdef.getName())
            .append(" ")
            .append(RES_NAME_HOLDER);
        return subj.toString();
    }

    private String createConditions(AlertConditionValue[] conds,
                                    HashMap eventMap,
                                    String indent)
        throws NamingException, CreateException, MeasurementNotFoundException
    {
        StringBuffer text = new StringBuffer();

        for (int i = 0; i < conds.length; i++) {
            if (i == 0) {
                text.append("\n").append(indent).append("If Condition: ");
            }
            else {
                text.append("\n").append(indent)
                    .append(conds[i].getRequired() ? "AND " : "OR ");
            }

            TriggerFiredEvent event = (TriggerFiredEvent)
            eventMap.get( conds[i].getTriggerId() );

            switch (conds[i].getType()) {
                case EventConstants.TYPE_THRESHOLD:
                case EventConstants.TYPE_BASELINE:
                    text.append(conds[i].getName()).append(" ")
                        .append(conds[i].getComparator()).append(" ");

                    DerivedMeasurementValue dmv =
                        DerivedMeasurementManagerUtil.getLocalHome().create()
                        .getMeasurement(
                            new Integer(conds[i].getMeasurementId()));

                    if (conds[i].getType() == EventConstants.TYPE_BASELINE) {
                        text.append(conds[i].getThreshold());
                        text.append("% of ");

                        if (MeasurementConstants.BASELINE_OPT_MAX
                            .equals(conds[i].getOption())) {
                            text.append("Max Value");
                        }
                        else if (MeasurementConstants.BASELINE_OPT_MIN
                            .equals(conds[i].getOption())) {
                            text.append("Min Value");
                        }
                        else {
                            text.append("Baseline");
                        }
                    }
                    else {
                        FormattedNumber th =
                            UnitsConvert.convert(conds[i].getThreshold(),
                                                 dmv.getTemplate().getUnits());
                        text.append(th.toString());
                    }
                    
                    // Make sure the event is present to be displayed
                    if (eventMap.containsKey(conds[i].getTriggerId())) {
                        double val = NumberUtil.stringAsNumber
                            ( event.toString() ).doubleValue();
                        FormattedNumber av = UnitsConvert.convert
                            ( val, dmv.getTemplate().getUnits() );
                        text.append(" (actual value = ")
                            .append( av.toString() )
                            .append(")");
                    }
                    break;
                case EventConstants.TYPE_CONTROL:
                    text.append(conds[i].getName());
                    break;
                case EventConstants.TYPE_CHANGE:
                    DerivedMeasurementValue dmv2 =
                        DerivedMeasurementManagerUtil.getLocalHome().create()
                            .getMeasurement(
                                new Integer(conds[i].getMeasurementId()));
                    text.append(conds[i].getName()).append(" value changed");
                    // Parse out old value.  This is a hack.
                    // Basically, we use the MessageFormat from the
                    // ValueChangeTrigger class to parse out the
                    // arguments from the event's message which was
                    // created from the same message format.  This is
                    // the best we can do until we track previous
                    // values more explicitly. (JW)
                    if (eventMap.containsKey(conds[i].getTriggerId())) {
                        text.append(" (");
                        try {
                            Object[] values = ValueChangeTrigger.MESSAGE_FMT
                                .parse(event.getMessage());
                            text.append("old value = ");
                            if ( log.isTraceEnabled() ) {
                                log.trace("event message = " +
                                          event.getMessage());
                                for (int x=0; x<values.length; ++x) {
                                    log.trace("values["+x+"] = " + values[x]);
                                }
                            }
                            if (2 == values.length) {
                                text.append(values[1]);
                            } else {
                                text.append(NOTAVAIL);
                            }
                        } catch (ParseException e) {
                            text.append(NOTAVAIL);
                        }

                        double val = NumberUtil.stringAsNumber
                            ( event.toString() ).doubleValue();
                        FormattedNumber av = UnitsConvert.convert
                            ( val, dmv2.getTemplate().getUnits() );
                        text.append(", new value = ")
                            .append( av.toString() )
                            .append(")");
                    }
                    break;
                case EventConstants.TYPE_CUST_PROP:
                    text.append(conds[i].getName())
                        .append(" value changed");
                    text.append("\n").append(indent).append(event);
                    break;
                case EventConstants.TYPE_LOG:
                    text.append("Event/Log Level(")
                        .append(ResourceLogEvent.getLevelString(
                                Integer.parseInt(conds[i].getName())))
                        .append(")");
                    if (conds[i].getOption() != null &&
                        conds[i].getOption().length() > 0) {
                        text.append(" and matching substring ")
                            .append('"')
                            .append(conds[i].getOption())
                            .append('"');
                    }
                    
                    text.append("\n").append(indent).append("Log: ")
                        .append(event);
                    break;
                default:
                    break;
            }
        }

        return text.toString();
    }

    private String getBaseURL()
        throws ConfigPropertyException, CreateException, NamingException {
        if (baseUrl == null) {
            baseUrl = ServerConfigManagerUtil.getLocalHome().create().
                getConfig().getProperty(HQConstants.BaseURL);

            // make sure no extra slashes (/)
            if ( baseUrl.charAt(baseUrl.length() - 1) != '/') {
                baseUrl += '/';
            }
        }
        return baseUrl;
    }

    private String createLink(AlertDefinitionBasicValue alertdef, Integer aid)
        throws ConfigPropertyException, CreateException, NamingException {
        StringBuffer text = new StringBuffer();

            // Create link
            Object[] args = {
                new Integer( alertdef.getAppdefType() ),
                new Integer( alertdef.getAppdefId() ),
                aid
            };
            String alertUrl = MessageFormat.format(LINK_FORMAT, args);
            text.append(getBaseURL()).append(alertUrl);

        return text.toString();
    }

    private String createText(AlertDefinitionBasicValue alertdef,
                              TriggerFiredEvent event, AppdefEntityID aeid,
                              Integer alertId)
        throws NamingException, CreateException, MeasurementNotFoundException
    {
        // XXX: Ashamed of myself, this is definitely not localizable
        // um, when we figger out how we want to make the user's locale something
        // that is accessible to the backend, this should be un-hardcoded
        SimpleDateFormat dformat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aaa");
        String alertTime = dformat.format(new Date(event.getTimestamp()));

        // Organize the events by trigger
        TriggerFiredEvent[] firedEvents = event.getRootEvents();
        HashMap eventMap = new HashMap();
        for (int i = 0; i < firedEvents.length; i++) {
            eventMap.put(firedEvents[i].getInstanceId(),
                         firedEvents[i]);
        }

        StringBuffer text = new StringBuffer("The ")
            .append(RES_NAME_HOLDER).append(" ")
            .append(aeid.getTypeName())
            .append(" has generated the following alert -\n")
            .append(this.createConditions(alertdef.getConditions(), eventMap, ""))
            .append(SEPARATOR);

        text.append("ALERT DETAIL")
            .append("\n- Resource Name: ").append(RES_NAME_HOLDER)
            .append("\n- Alert Name: ").append(alertdef.getName());
        
        if (alertdef.getDescription() != null &&
            alertdef.getDescription().length() > 0) {
            text.append("\n- Description: ").append(alertdef.getDescription());
        }

        // Now go through the condition set
        text.append("\n- Condition Set: ");

        // Get the conditions
        text.append(this.createConditions(alertdef.getConditions(), eventMap, "    "));

        // The rest of the alert details
        text.append("\n- Alert Severity: ")
            .append(EventConstants.getPriority(alertdef.getPriority()))
            .append("\n- Alert Date / Time: ").append(alertTime)
            .append(SEPARATOR);

        try {
            // Create the links
            text.append("\nFor additional detail about this alert, go to ")
                .append(this.createLink(alertdef, alertId))
                .append(SEPARATOR);

            // Public Service Announcement
            text.append("This message was delivered to you by Hyperic HQ.")
                .append("\nTo view the HQ Dashboard, go to ")
                .append(getBaseURL())
                .append("Dashboard.do")
                .append(SEPARATOR);
        } catch (NamingException e) {
            log.error("Error getting HQ config.  Can't add link to email.", e);
        } catch (CreateException e) {
            log.error("Error getting HQ config.  Can't add link to email.", e);
        } catch (ConfigPropertyException e) {
            log.error("Error getting HQ config.  Can't add link to email.", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Error finding event id.  Can't add link to email.", e);
        }

        return text.toString();
    }

    /** Execute the action
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if execution causes an error
     *
     */
    public String execute(AlertDefinitionBasicValue alertdef,
                          TriggerFiredEvent event, Integer alertId)
        throws ActionExecuteException {
        AlertManagerLocal aman = null;

        try {
            aman = AlertManagerUtil.getLocalHome().create();
        } catch (CreateException e1) {
            // Don't let it affect the action execution
        } catch (NamingException e1) {
            // Don't let it affect the action execution
        }

        try {
            // First, look up the addresses
            String smsAddr;
            Integer uid;
            int i = 0;
            List validAddresses = new ArrayList();
            for (Iterator it = this.getUsers().iterator(); it.hasNext(); i++) {
                try {
                    AuthzSubjectValue overlord = getSubjMan().getOverlord();
                    switch (this.getType()) {
                        case TYPE_USERS :
                            uid = (Integer) it.next();
                            AuthzSubjectValue who = 
                                getSubjMan().findSubjectById(overlord, uid);
                            validAddresses.add(
                                new InternetAddress(who.getEmailAddress()));
                            smsAddr = who.getSMSAddress();    
                            if(smsAddr != null && !smsAddr.equals("")) {
                                validAddresses.add(
                                    new InternetAddress(smsAddr));
                            }
                            // Add an alert to the user
                            if (aman != null) {
                                aman.addSubjectAlert(uid, alertId);
                            }
                            break;
                        default :
                        case TYPE_EMAILS :
                            validAddresses.add(
                                new InternetAddress((String) it.next(), true));
                            break;
                    }
                } catch (FinderException e) {
                    // This one is no good, continue
                    continue;
                } catch (CreateException e) {
                    throw new ActionExecuteException("Session EJB error", e);
                } catch (PermissionException e) {
                    // authz failure...should not happen since its the overlord
                    // doing the user lookup
                    continue;
                }
            }

            // Convert the valid addresses
            InternetAddress[] to = (InternetAddress[])
                validAddresses.toArray(new InternetAddress[0]);

            EmailFilter filter = EmailFilter.getInstance();

            AppdefEntityID appEnt = new AppdefEntityID(alertdef.getAppdefType(),
                                                       alertdef.getAppdefId());

            filter.sendAlert(appEnt, to, createSubject(alertdef),
                             createText(alertdef, event, appEnt, alertId),
                             alertdef.getNotifyFiltered());

            StringBuffer result = new StringBuffer();
            // XXX: Should get this strings into a resource file
            switch (this.getType()) {
                case TYPE_USERS :
                    result.append("HQ Users Notified: ");
                    break;
                default :
                case TYPE_EMAILS :
                    result.append("Other Recipients Notified: ");
                    break;
            }
            result.append(this.getNames());

            return result.toString();
        } catch (javax.ejb.CreateException e) {
            throw new ActionExecuteException(e);
        } catch (javax.naming.NamingException e) {
            throw new ActionExecuteException(e);
        } catch (javax.mail.MessagingException e) {
            throw new ActionExecuteException(e);
        } catch (MeasurementNotFoundException e) {
            throw new ActionExecuteException(e);
        } catch (SystemException e) {
            throw new ActionExecuteException(e);
        }
    }

    public void setParentActionConfig(AppdefEntityID aeid,
                                      ConfigResponse config)
        throws InvalidActionDataException {
        this.init(config);
    }

}
