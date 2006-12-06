package org.hyperic.hq.ui.json.action.escalation.crud;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
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

/**
 */
public class SaveEscalation extends BaseAction
{
    private static String ID_PREFIX = "pid_row";
    private static String VERS_PREFIX = "pversion_row";
    // action type
    private static String ACTION_PREFIX = "action_row";

    // email action parameter
    private static String WHO_PREFIX = "who_row";
    private static String USER_PREFIX = "users_row";

    // syslog action parameter
    private static String META_PREFIX = "meta_row";
    private static String PRODUCT_PREFIX = "product_row";
    private static String VERSION_PREFIX = "version_row";

    // action waittime
    private static String WAITTIME_PREFIX = "time_row";

    // action list order
    private static String ORDER = "order";

    // escalation attributes
    private static String ALLOW_PAUSE = "allowPause";
    private static String NOTIFICATION = "notification";
    private static String MAX_WAITTIME = "maxwaittime";

    private static String ALERTDEF_ID = "ad";
    private static String ID = "pid";
    private static String VERSION = "pversion";
    private static String NAME = "escName";
    private static String PUPDATE = "pupdate";

    public void execute(JsonActionContext context)
        throws PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               JSONException, RemoteException
    {
        EscalationWebMediator wmed = EscalationWebMediator.getInstance();
        List actions = parseActions(context);

        Map map = context.getParameterMap();
        boolean update = false;
//            Boolean.valueOf(
//                ((String[])map.get(PUPDATE))[0]).booleanValue();

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

        if (map.get(ID) != null) {
            int id = Integer.valueOf(
                ((String[])map.get(ID))[0]).intValue();
            long version;
            if (map.get(VERSION) != null) {
                version = Long.valueOf(
                    ((String[])map.get(VERSION))[0]).longValue();
            } else {
                version = 0;
            }
            if (update && id > 0) {
                json.put("id", id)
                    .put("_version_", version);
            }
        }
        Integer alertDefId = Integer.valueOf(
            ((String[])map.get(ALERTDEF_ID))[0]);
        JSONObject escalation = new JSONObject().put("escalation", json);
        wmed.saveEscalation(context, context.getSessionId(), alertDefId,
                            escalation);
        context.getSession().setAttribute("escalationName",
                                          ((String[])map.get(NAME))[0]);
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
                        (String[])map.get(ID_PREFIX + row),
                        (String[])map.get(VERS_PREFIX + row),
                        (String[])map.get(WHO_PREFIX + row),
                        (String[])map.get(USER_PREFIX + row),
                        (String[])map.get(WAITTIME_PREFIX + row)
                    ));
                } else if ("syslog".equalsIgnoreCase(values[0])) {
                    actions.add(new SyslogActionData(
                        (String[])map.get(ID_PREFIX + row),
                        (String[])map.get(VERS_PREFIX + row),
                        (String[])map.get(META_PREFIX + row),
                        (String[])map.get(PRODUCT_PREFIX + row),
                        (String[])map.get(VERSION_PREFIX + row),
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

    private static class ActionData
    {
        int id;
        long _version_;
        long waitTime;

        ActionData(String[]idarr, String[]varr, String[] timearr)
        {
            if (idarr != null) {
                id = Integer.valueOf(idarr[0]).intValue();
                _version_ = Long.valueOf(varr[0]).longValue();
            }
            waitTime = Long.valueOf(timearr[0]).longValue();
        }

        JSONObject toJSON() throws JSONException
        {
            JSONObject json = new JSONObject();
            if (id > 0) {
                json.put("id", id)
                    .put("_version_", _version_);
            }
            return json;
        }
    }

    private static class EmailActionData extends ActionData
    {
        int listType;
        String names;

        EmailActionData(String[] ida, String[] vers, String[] type,
                        String[] narr, String[] time)
        {
            super(ida, vers, time);

            if ("users".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_USERS;
            } else if ("roles".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_ROLES;
            } else if ("others".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_EMAILS;
            }

            // name list
            if (narr != null) {
                StringTokenizer tokens = new StringTokenizer(narr[0], " ,");
                StringBuffer buf = new StringBuffer();
                while(tokens.hasMoreTokens()) {
                    if (buf.length() > 0) {
                        buf.append(",");
                    }
                    buf.append(tokens.nextToken());
                }
                names = buf.toString();
            }
        }

        public JSONObject toJSON() throws JSONException
        {
            JSONObject action = super.toJSON()
                .put("className", "EmailAction");

            JSONObject config =  new JSONObject()
                .put("listType", listType)
                .put("names", names);

            action.put("config", config);

            return action;
        }
    }

    private static class SyslogActionData extends ActionData
    {
        String meta;
        String product;
        String version;

        SyslogActionData(String[] ida, String[] vs, String[] met,
                         String[] prod, String[] vers,
                         String[] time)
        {
            super(ida, vs, time);

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
