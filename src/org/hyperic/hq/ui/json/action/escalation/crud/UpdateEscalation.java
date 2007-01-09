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
import org.json.JSONObject;
import org.json.JSONArray;

import java.rmi.RemoteException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.StringTokenizer;

import javax.ejb.FinderException;

/**
 */
public class UpdateEscalation extends BaseAction
{
    private static final String VERSION = "_version_";
    // action names
    private static final String LIST_TYPE_PRFIX = "listType_";
    private static final String NAMES_PREFIX = "names_";
    private static final String SMS_PREFIX = "sms_";
    private static final String VERSION_PREFIX = "_version__";
    private static final String CLASSNAME_PREFIX = "className_";
    private static final String PRODUCT_PREFIX = "product_";
    private static final String META_PREFIX = "meta_";
    private static final String VERS_PREFIX = "verson_";
    private static final String WAITTIME_PREFIX = "waitTime_";
    
    // action list order
    private static String ORDER = "rowOrder";

    public void execute(JsonActionContext context)
        throws PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               JSONException,
               RemoteException
    {
        Map map = context.getParameterMap();

        if (map.get(ID) == null) {
            throw new IllegalArgumentException("Escalation id not found");
        }

        List actions = parseActions(context);

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
                     ((String[])map.get(NOTIFY_ALL))[0]).booleanValue())
            .put("maxWaitTime",
                 Long.valueOf(
                     ((String[])map.get(MAX_WAIT_TIME))[0]).longValue())
            .put("actions", jarr);

        Integer id = Integer.valueOf(((String[]) map.get(ID))[0]);
        json.put("id", id);

        EscalationWebMediator wmed = EscalationWebMediator.getInstance();
        String[] ad = (String[])map.get(ALERTDEF_ID);
        String[] gad = (String[])map.get(GALERTDEF_ID);
        JSONObject escalation = new JSONObject().put("escalation", json);
        try {
            if (ad != null && !"undefined".equals(ad[0])) {
                Integer alertDefId = Integer.valueOf((ad)[0]);
                wmed.saveEscalation(context, context.getSessionId(),
                                             alertDefId, 
                                             ClassicEscalationAlertType.CLASSIC,
                                             escalation);
            } else if (gad != null && !"undefined".equals(gad[0])) {
                Integer alertDefId = Integer.valueOf((gad)[0]);
                wmed.saveEscalation(context, context.getSessionId(),
                                             alertDefId, 
                                             GalertEscalationAlertType.GALERT,
                                             escalation);
            } else {
                wmed.updateEscalation(context, context.getSessionId(),
                                               id, escalation);
            }
        } catch(SessionException e) {
            throw new SystemException(e);
        } catch(FinderException e) {
            throw new SystemException(e);
        } catch(DuplicateObjectException e) {
            throw new SystemException(e);
        }
        
        JSONObject result =
            wmed.jsonByEscalationId(context, context.getSessionId(), id);

        context.setJSONResult(new JSONResult(result));
        context.getRequest().setAttribute("escalation", result);
    }

    private List parseActions(JsonActionContext context)
    {
        ArrayList actions = new ArrayList();

        Map map = context.getParameterMap();
        Map rows = groupKeysByRow(context);
        for(Iterator i = rows.keySet().iterator(); i.hasNext();) {
            Integer id = (Integer)i.next();
            String[] className = (String[])map.get(CLASSNAME_PREFIX + id);
            if (className == null) {
                throw new IllegalArgumentException("action className not found.");
            }
            if (className[0].equals("EmailAction")) {
                actions.add(new EmailActionData(
                    id,
                    (String[])map.get(VERS_PREFIX + id),
                    (String[])map.get(LIST_TYPE_PRFIX + id),
                    (String[])map.get(NAMES_PREFIX + id),
                    (String[])map.get(SMS_PREFIX + id),
                    (String[])map.get(WAITTIME_PREFIX + id)
                ));
            } else if (className[0].equals("SyslogAction")) {
                actions.add(new SyslogActionData(
                    id,
                    (String[])map.get(VERS_PREFIX + id),
                    (String[])map.get(META_PREFIX + id),
                    (String[])map.get(PRODUCT_PREFIX + id),
                    (String[])map.get(VERSION_PREFIX + id),
                    (String[])map.get(WAITTIME_PREFIX + id)
                ));
            } else if (className[0].equals("NoOpAction")) {
                actions.add(new NoOpActionData(
                    (String[])map.get(WAITTIME_PREFIX + id)
                ));
            } else {
                throw new IllegalArgumentException(
                    "Unsupported action className " + className[0]);
            }
        }
        return actions;
    }

    private Map groupKeysByRow(JsonActionContext context)
    {
        TreeMap idMap = new TreeMap();
        Map map = context.getParameterMap();
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            if (key.startsWith(LIST_TYPE_PRFIX) ||
                key.startsWith(NAMES_PREFIX) ||
                key.startsWith(SMS_PREFIX) ||
                key.startsWith(VERSION_PREFIX) ||
                key.startsWith(PRODUCT_PREFIX) ||
                key.startsWith(CLASSNAME_PREFIX) ||
                key.startsWith(META_PREFIX) ||
                key.startsWith(VERS_PREFIX) ||
                key.startsWith(WAITTIME_PREFIX)
                ) {
                String idString = key.substring(key.length());
                Integer id = Integer.valueOf(idString);
                List content = (List)idMap.get(id);
                if (content == null) {
                    content = new ArrayList();
                }
                content.add(key);
                idMap.put(id, content);
            }
        }
        return idMap;
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
        boolean sms;

        EmailActionData(Integer id, String[] vers, String[] type,
                        String[] namesarr, String[] smsarr, String[] time)
        {
            super(new String[]{""+id}, vers, time);

            int ltype = Integer.valueOf(type[0]).intValue();
            switch(ltype) {
                case EmailActionConfig.TYPE_EMAILS:
                case EmailActionConfig.TYPE_ROLES:
                case EmailActionConfig.TYPE_USERS:
                    listType = ltype;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid user list type "
                                                       + ltype);
            }
            // name list
            StringTokenizer tokens = new StringTokenizer(namesarr[0], " ,");
            StringBuffer buf = new StringBuffer();
            while(tokens.hasMoreTokens()) {
                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(tokens.nextToken());
            }
            names = buf.toString();
            sms = Boolean.valueOf(smsarr[0]).booleanValue();
        }

        public JSONObject toJSON() throws JSONException
        {
            JSONObject action = super.toJSON()
                .put("className", "EmailAction");

            JSONObject config =  new JSONObject()
                .put("listType", listType)
                .put("names", names)
                .put("sms", sms);

            action.put("config", config);

            return action;
        }
    }

    private static class SyslogActionData extends ActionData
    {
        String meta;
        String product;
        String version;

        SyslogActionData(Integer id, String[] vs, String[] met,
                         String[] prod, String[] vers, String[] time)
        {
            super(new String[]{""+id}, vs, time);

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
