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

package org.hyperic.hq.ui.json.action.escalation.crud;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.ejb.FinderException;

public class SaveEscalation extends BaseAction
{
    // action type
    private static String ACTION_PREFIX = "action_row";

    // email action parameter
    private static String WHO_PREFIX = "who_row";
    private static String USER_PREFIX = "users_row";
    private static String ROLE_PREFIX = "roles_row";
    private static String OTHER_PREFIX = "emailinput_row";

    // syslog action parameter
    private static String META_PREFIX = "meta_row";
    private static String PRODUCT_PREFIX = "product_row";
    private static String VERSION_PREFIX = "version_row";

    // action waittime
    private static String WAITTIME_PREFIX = "waittime_row";

    // action list order
    private static String ORDER = "rowOrder";

    // escalation attributes
    private static String ALLOW_PAUSE = "allowPause";
    private static String NOTIFICATION = "notification";
    private static String MAX_WAITTIME = "maxwaittime";

    private static String ALERTDEF_ID = "ad";
    private static String GALERTDEF_ID = "gad";
    private static String NAME = "escName";
    private static String DESCRIPTION = "description";

    public void execute(JsonActionContext context)
        throws PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               JSONException, RemoteException
    {
        EscalationWebMediator wmed = EscalationWebMediator.getInstance();
        List actions = parseActions(context);

        Map map = context.getParameterMap();

        JSONArray jarr = new JSONArray();
        for (Iterator i=actions.iterator(); i.hasNext();) {
            Object obj = i.next();
            JSONObject action;
            if (obj instanceof EmailActionData) {
                EmailActionData email = (EmailActionData)obj;
                action = email.toJSON();
            } else if (obj instanceof SyslogActionData) {
                SyslogActionData slog = (SyslogActionData)obj;
                action = slog.toJSON();
            } else if (obj instanceof NoOpActionData) {
                NoOpActionData noop = (NoOpActionData)obj;
                action = noop.toJSON();
            } else {
                throw new IllegalArgumentException("Unsupported object type "
                                                   +obj.getClass().getName());
            }
            ActionData data = (ActionData)obj;
            JSONObject escalationAction = new JSONObject()
                .put("action", action)
                .put("waitTime", data.waitTime);

            jarr.put(escalationAction);
        }

        JSONObject json = new JSONObject()
            .put("name", ((String[])map.get(NAME))[0])
            .put("description", ((String[])map.get(DESCRIPTION))[0])
            .put("allowPause",
                 Boolean.valueOf(
                     ((String[])map.get(ALLOW_PAUSE))[0]).booleanValue())
            .put("notifyAll",
                 Boolean.valueOf(
                     ((String[])map.get(NOTIFICATION))[0]).booleanValue())
            .put("maxWaitTime",
                 Long.valueOf(
                     ((String[])map.get(MAX_WAITTIME))[0]).longValue())
            .put("actions", jarr);
        
        String[] ad = (String[])map.get(ALERTDEF_ID);
        String[] gad = (String[])map.get(GALERTDEF_ID);
        JSONObject result;
        JSONObject escalation = new JSONObject().put("escalation", json);

        try {
            if (ad != null && !"undefined".equals(ad[0])) {
                Integer alertDefId = Integer.valueOf((ad)[0]);
                result = wmed.saveEscalation(context, context.getSessionId(),
                                             alertDefId, 
                                             ClassicEscalationAlertType.CLASSIC,
                                             escalation);
            } else if (gad != null && !"undefined".equals(gad[0])) {
                Integer alertDefId = Integer.valueOf((gad)[0]);
                result = wmed.saveEscalation(context, context.getSessionId(),
                                             alertDefId,
                                             GalertEscalationAlertType.GALERT,
                                             escalation);
            } else {
                result = wmed.saveEscalation(context, context.getSessionId(),
                                             null, null, escalation);
            }
        } catch(SessionException e) {
            throw new SystemException(e);
        } catch(FinderException e) {
            throw new SystemException(e);
        } catch(DuplicateObjectException e) {
            throw new SystemException(e);
        }
        context.setJSONResult(new JSONResult(result));
        context.getRequest().setAttribute("escalation", result);
    }

    private List parseActions(JsonActionContext context)
    {
        ArrayList actions = new ArrayList();

        Map map = context.getParameterMap();
        Map rows = groupKeysByRow(context);
        for(Iterator i = rows.keySet().iterator(); i.hasNext();) {
            Integer row = (Integer)i.next();
            List keys = (List)rows.get(row);
            if (keys.contains(ACTION_PREFIX + row)) {
                String[] values = (String[])map.get(ACTION_PREFIX + row);
                if ("email".equalsIgnoreCase(values[0])) {
                    actions.add(new EmailActionData(
                        (String[])map.get(WHO_PREFIX + row),
                        (String[])map.get(USER_PREFIX + row),
                        (String[])map.get(ROLE_PREFIX + row),
                        (String[])map.get(OTHER_PREFIX + row),
                        false,
                        (String[])map.get(WAITTIME_PREFIX + row)
                    ));
                } else if ("sms".equalsIgnoreCase(values[0])) {
                    actions.add(new EmailActionData(
                        (String[])map.get(WHO_PREFIX + row),
                        (String[])map.get(USER_PREFIX + row),
                        (String[])map.get(ROLE_PREFIX + row),
                        (String[])map.get(OTHER_PREFIX + row),
                        true,
                        (String[])map.get(WAITTIME_PREFIX + row)
                    ));
                } else if ("syslog".equalsIgnoreCase(values[0])) {
                    actions.add(new SyslogActionData(
                        (String[])map.get(META_PREFIX + row),
                        (String[])map.get(PRODUCT_PREFIX + row),
                        (String[])map.get(VERSION_PREFIX + row),
                        (String[])map.get(WAITTIME_PREFIX + row)
                    ));
                } else if ("noop".equalsIgnoreCase(values[0])) {
                    actions.add(new NoOpActionData(
                        (String[])map.get(WAITTIME_PREFIX + row)
                    ));
                } else {
                    throw new IllegalArgumentException(
                        "Unsupported action type " + values[0]);
                }
            }
        }
        return actions;
    }

    private Map groupKeysByRow(JsonActionContext context)
    {
        TreeMap rowMap = new TreeMap();
        Map map = context.getParameterMap();
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            if (key.startsWith(ACTION_PREFIX) ||
                key.startsWith(WHO_PREFIX) ||
                key.startsWith(USER_PREFIX) ||
                key.startsWith(ROLE_PREFIX) ||
                key.startsWith(OTHER_PREFIX) ||
                key.startsWith(META_PREFIX) ||
                key.startsWith(PRODUCT_PREFIX) ||
                key.startsWith(VERSION_PREFIX) ||
                key.startsWith(WAITTIME_PREFIX)
                ) {
                String rowString = key.substring(key.indexOf("_row")+4);
                Integer row = Integer.valueOf(rowString);
                List content = (List)rowMap.get(row);
                if (content == null) {
                    content = new ArrayList();
                }
                content.add(key);
                rowMap.put(row, content);
            }
        }
        return rowMap;
    }
    
    private static class NoOpActionData extends ActionData
    {
        NoOpActionData(String[] timearr) {
            super(null, null, timearr);
        }
        
        public JSONObject toJSON() throws JSONException
        {
            JSONObject action = super.toJSON().put("className", "NoOpAction");
            action.put("config", new JSONObject());

            return action;
        }        
    }

    private static class EmailActionData extends ActionData
    {
        int listType;
        String names;
        boolean _sms;

        EmailActionData(String[] type, String[] narr, String[] roles,
                        String[] others, boolean sms, String[] time)
        {
            super(null, null, time);

            String[] namesarr;
            if ("users".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_USERS;
                namesarr = narr;
            } else if ("roles".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_ROLES;
                namesarr = roles;
            } else if ("others".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_EMAILS;
                namesarr = others;
            } else {
                throw new IllegalArgumentException("Invalid type " + type[0]);
            }

            // name list
            if (namesarr != null) {
                StringTokenizer tokens = new StringTokenizer(namesarr[0], " ,");
                StringBuffer buf = new StringBuffer();
                while(tokens.hasMoreTokens()) {
                    if (buf.length() > 0) {
                        buf.append(",");
                    }
                    buf.append(tokens.nextToken());
                }
                names = buf.toString();
            }
            
            _sms = sms;
        }

        public JSONObject toJSON() throws JSONException
        {
            JSONObject action = super.toJSON()
                .put("className", "EmailAction");

            JSONObject config =  new JSONObject()
                .put("listType", listType)
                .put("names", names)
                .put("sms", _sms);

            action.put("config", config);

            return action;
        }
    }

    private static class SyslogActionData extends ActionData
    {
        String meta;
        String product;
        String version;

        SyslogActionData(String[] met, String[] prod, String[] vers,
                         String[] time)
        {
            super(null, null, time);

            meta = met[0];
            product = prod[0];
            version = vers[0];
        }

        public JSONObject toJSON() throws JSONException
        {
            JSONObject action = super.toJSON()
                .put("className", "SyslogAction");

            JSONObject config =  super.toJSON()
                .put("meta", meta)
                .put("version", version)
                .put("product", product);
            
            action.put("config", config);

            return action;
        }
    }
}
