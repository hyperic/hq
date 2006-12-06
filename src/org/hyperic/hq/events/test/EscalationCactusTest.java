package org.hyperic.hq.events.test;

import org.hyperic.hq.test.HQCactusBase;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.action.ActionMapping;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Random;

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

public class EscalationCactusTest extends HQCactusBase
{
    private final String BOGUS_NAME1 =
        "bogus1 " + (new Random()).nextInt(10000);

    private EventsBossLocal eventsBoss;

    private int sessionID;

    public EscalationCactusTest(String testName)
    {
        super(testName);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        eventsBoss = EventsBossUtil.getLocalHome().create();
        sessionID = SessionManager.getInstance().put(getOverlord());
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    private void createEscalation() throws Exception
    {
        String jsonString = makeJsonEscalation();

        JSONObject json = new JSONObject(jsonString);
        eventsBoss.saveEscalation(sessionID, null, json);

        // read after save
        JSONObject json_saved =
            eventsBoss.jsonByEscalationName(sessionID, BOGUS_NAME1);
        assertNotNull(json_saved);
    }

    public void testEscalationCrud() throws Exception
    {
        createEscalation();

        JSONObject json;
        String ename = Escalation.newInstance().getJsonName();

        // flip notify bit
        json =
            eventsBoss.jsonByEscalationName(sessionID, BOGUS_NAME1);
        boolean notify = json.getJSONObject(ename)
            .getBoolean("notifyAll");
        json.getJSONObject(ename)
            .put("notifyAll", !notify);
        eventsBoss.saveEscalation(sessionID, null, json);
        json = eventsBoss.jsonByEscalationName(sessionID, BOGUS_NAME1);
        boolean notify_update = json.getJSONObject(ename)
            .getBoolean("notifyAll");
        assertTrue(notify != notify_update);

        // test for null update
        long creationTime = json.getJSONObject(ename)
            .getLong("creationTime");
        long modifiedTime = json.getJSONObject(ename)
            .getLong("modifiedTime");
        eventsBoss.saveEscalation(sessionID, null, json);
        json = eventsBoss.jsonByEscalationId(sessionID,
                                             new Integer(
                                                 json.getJSONObject(ename)
                                                     .getInt("id")));
        long creationTime1 = json.getJSONObject(ename)
            .getLong("creationTime");
        long modifiedTime1 = json.getJSONObject(ename)
            .getLong("modifiedTime");
        assertTrue(
            creationTime == creationTime1 &&
            modifiedTime == modifiedTime1);

        // test for real update
        json.getJSONObject(ename).put("maxWaitTime", 120000);
        eventsBoss.saveEscalation(sessionID, null, json);
        json = eventsBoss.jsonByEscalationName(sessionID, BOGUS_NAME1);
        creationTime1 = json.getJSONObject(ename)
            .getLong("creationTime");
        modifiedTime1 = json.getJSONObject(ename)
            .getLong("modifiedTime");
        long maxWaitTime = json.getJSONObject(ename)
            .getLong("maxWaitTime");
        assertTrue(
            creationTime == creationTime1 &&
            modifiedTime < modifiedTime1 &&
            maxWaitTime == 120000);

        int id = json.getJSONObject(ename).getInt("id");
        removeEscalation(id);
    }

    public void testWebEscalationCrud() throws Exception
    {
        // login
        webLogin();

        // should stream result directly to the response.writer
        setRequestPathInfo("/escalation/saveEscalation");
        setSaveEscalationParams();
        actionPerform();
        assertNotNull(getSession().getAttribute("escalationName"));

        String name = (String)getSession().getAttribute("escalationName");

        // should stream result directly to the response.writer
        setRequestPathInfo("/escalation/jsonByEscalationName/" + name);
        actionPerform();

        JSONObject json =
            eventsBoss.jsonByEscalationName(sessionID, name);
        assertNotNull(json);

//        String ename = Escalation.newInstance().getJsonName();
//        setRequestPathInfo("/escalation/removeEscalation/" +
//                           json.getJSONObject(ename).getInt("id"));
//        actionPerform();
//        json =
//            eventsBoss.jsonByEscalationName(sessionID, name);
//        assertNull(json);
    }

    private WebUser webLogin()
    {
        setRequestPathInfo("/j_security_check");
        addRequestParameter("j_username", "hqadmin");
        addRequestParameter("j_password", "hqadmin");
        actionPerform();
        assertNotNull(getSession().getAttribute(Constants.WEBUSER_SES_ATTR));
        return (WebUser)getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
    }

    private void setSaveEscalationParams()
    {
        addRequestParameter("action_row2", new String[]{"Email"});
        addRequestParameter("who_row2", new String[]{"Others"});
        addRequestParameter("users_row2", new String[]{"hqadmin@hyperic.com," +
                                                       " admin@cypress.com" +
                                                       " joe@kinko.com"});
        addRequestParameter("time_row2", new String[]{"120000"});

        addRequestParameter("action_row0", new String[]{"Email"});
        addRequestParameter("who_row0", new String[]{"Others"});
        addRequestParameter("users_row0", new String[]{"joe@gmail.com polly@yahoo.com," +
                                          "kim@mac.com"});
        addRequestParameter("time_row0", new String[]{"120000"});

        addRequestParameter("action_row1", new String[]{"Syslog"});
        addRequestParameter("who_row1", new String[]{"Others"});
        addRequestParameter("meta_row1", new String[]{"meta data"});
        addRequestParameter("product_row1", new String[]{"product info"});
        addRequestParameter("version_row1", new String[]{"version number"});
        addRequestParameter("time_row1", new String[]{"120000"});

        addRequestParameter("allowPause", new String[]{"true"});
        addRequestParameter("notification", new String[]{"0"});
        addRequestParameter("maxwaittime", new String[]{"300000"});
        addRequestParameter("escName", new String[]{"My escalation"});
        addRequestParameter("ad", new String[]{"10001"});
    }

    private void removeEscalation(int id)
        throws Exception
    {
        eventsBoss.deleteEscalationById(sessionID, new Integer(id));
        JSONObject json =
            eventsBoss.jsonByEscalationName(sessionID, BOGUS_NAME1);
        assertNull(json);
    }

    private String makeJsonEscalation() throws JSONException
    {
        EscalationAction act1 = createEmailAction(
            new String[] {"joe@gmail.com", "bob@yahoo.com"});

        EscalationAction act2 = createEmailAction(
            new String[] {"paul@att.com", "bill@google.com"});

        EscalationAction act3 = createSyslogAction("meta", "tomcat", "5.0");

        Escalation e = Escalation.newInstance(BOGUS_NAME1);
        e.getActions().add(act1);
        e.getActions().add(act2);
        e.getActions().add(act3);

        return new JSONObject().put(e.getJsonName(), e.toJSON()).toString();
    }

    private EscalationAction createEmailAction(String[] users)
    {
        HashSet u = new HashSet();
        for (int i=0; i<users.length; i++) {
            u.add(users[i]);
        }
        return EscalationAction.newEmailAction(
            EmailActionConfig.TYPE_EMAILS, u, 60000);
    }

    private EscalationAction createSyslogAction(String metaProject, String proj,
                                                String version)
    {
        return EscalationAction.newSyslogAction(metaProject, proj, version,
            60000);
    }
}
