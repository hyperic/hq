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
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public class SaveEscalation extends BaseAction
{
    private static String ID_PREFIX = "pid_";
    private static String VERS_PREFIX = "pversion_";
    // action type
    private static String ACTION_PREFIX = "action_";

    // email action parameter
    private static String WHO_PREFIX = "who_";
    private static String USER_PREFIX = "users_";

    // syslog action parameter
    private static String META_PREFIX = "meta_";
    private static String PRODUCT_PREFIX = "product_";
    private static String VERSION_PREFIX = "version_";

    // action waittime
    private static String WAITTIME_PREFIX = "waittime_";

    // action list order
    private static String ORDER = "order";

    // escalation attributes
    private static String ALLOW_PAUSE = "allowPause";
    private static String NOTIFICATION = "notification";
    private static String MAX_WAITTIME = "maxwaittime";

    private static String ID = "pid";
    private static String VERSION = "pversion";
    private static String NAME = "pname";
    private static String PUPDATE = "pupdate";

    public void execute(JsonActionContext context)
        throws PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               JSONException, RemoteException
    {
        EscalationWebMediator wmed = EscalationWebMediator.getInstance();
        List actions = parseActions(context);

        JSONArray jarr = new JSONArray();
        for (Iterator i=actions.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof EmailActionData) {
                EmailActionData email = (EmailActionData)obj;
                JSONObject config = new JSONObject()
                    .put("listType", email.listType)
                    .put("names", email.names);
                
                JSONObject action = new JSONObject()
                    .put("config", config)
                    .put("className", "EmailAction");
                if (email.id > 0) {
                    action.put("id", email.id)
                        .put("_version_", email._version_);
                }

                JSONObject escalationAction = new JSONObject()
                    .put("action", action)
                    .put("waitTime", email.waitTime);

                jarr.put(escalationAction);
            } else {
                SyslogActionData slog = (SyslogActionData)obj;
                JSONObject config = new JSONObject()
                    .put("meta", slog.meta)
                    .put("product", slog.product)
                    .put("version", slog.version);

                JSONObject action = new JSONObject()
                    .put("config", config)
                    .put("className", "SyslogAction");
                if (slog.id > 0) {
                    action.put("id", slog.id)
                        .put("_version_", slog._version_);
                }

                JSONObject escalationAction = new JSONObject()
                    .put("action", action)
                    .put("waitTime", slog.waitTime);

                jarr.put(escalationAction);
            }
        }
        Map map = context.getParameterMap();
        JSONObject escalation = new JSONObject()
            .put("allowPause",
                 Boolean.valueOf((String)map.get(ALLOW_PAUSE)).booleanValue())
            .put("notifyAll",
                 Boolean.valueOf((String)map.get(NOTIFICATION)).booleanValue())
            .put("maxWaitTime",
                 Long.valueOf((String)map.get(MAX_WAITTIME)).longValue())
            .put("actions", jarr);

        int id = Integer.valueOf((String)map.get(ID)).intValue();
        long version = Long.valueOf((String)map.get(VERSION)).longValue();
        if (id > 0) {
            escalation.put("id", id)
                .put("_version_", version);
        }
        wmed.saveEscalation(context, context.getSessionId(), escalation);
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
                } else {
                    actions.add(new SyslogActionData(
                        (String[])map.get(ID_PREFIX + row),
                        (String[])map.get(VERS_PREFIX + row),
                        (String[])map.get(META_PREFIX + row),
                        (String[])map.get(PRODUCT_PREFIX + row),
                        (String[])map.get(VERSION_PREFIX + row),
                        (String[])map.get(WAITTIME_PREFIX + row)
                    ));
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
                String rowString = key.substring(key.indexOf("_"));
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

    private static class EmailActionData {
        int id;
        long _version_;
        int listType;
        String names;
        long waitTime;

        EmailActionData(String[] ida, String[] vers, String[] type,
                        String[] narr, String[] time)
        {
            id = Integer.valueOf(ida[0]).intValue();
            _version_ = Long.valueOf(vers[0]).longValue();
            if ("users".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_USERS;
            } else if ("roles".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_ROLES;
            } else if ("others".equalsIgnoreCase(type[0])) {
                listType = EmailActionConfig.TYPE_EMAILS;
            }

            // name list
            StringTokenizer tokens = new StringTokenizer(narr[0], " ,");
            StringBuffer buf = new StringBuffer();
            while(tokens.hasMoreTokens()) {
                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(tokens.nextToken());
            }
            names = buf.toString();

            waitTime = Long.valueOf(time[0]).longValue();
        }
    }

    private static class SyslogActionData {
        int id;
        long _version_;
        String meta;
        String product;
        String version;
        long waitTime;

        SyslogActionData(String[] ida, String[] vs, String[] met,
                         String[] prod, String[] vers,
                         String[] time) {
            id = Integer.valueOf(ida[0]).intValue();
            _version_ = Long.valueOf(vs[0]).longValue();
            meta = met[0];
            product = prod[0];
            version = vers[0];
            waitTime = Long.valueOf(time[0]).longValue();
        }
    }
}
