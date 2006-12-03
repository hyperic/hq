package org.hyperic.hq.events.test;

import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.hq.test.HQCactusBase;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.EscalationMediator;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.common.shared.TransactionManagerUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hibernate.ObjectNotFoundException;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionFormBean;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.action.ActionMapping;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;

import javax.naming.NamingException;
import javax.ejb.EJBException;
import javax.ejb.CreateException;
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

public class EscalationCactusTest
    extends HQCactusBase
{
    private final String BOGUS_NAME1 =
        "bogus1 " + (new Random()).nextInt(10000);

    SessionManager sessionMgr = SessionManager.getInstance();

    private EventsBossLocal eventsBoss;
    private AuthzSubjectValue overlord;

    public EscalationCactusTest(String testName)
    {
        super(testName);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        if (!isCactusMode()) {
            throw new Exception("this test is for in-container testing");
        }
        eventsBoss = EventsBossUtil.getLocalHome().create();
        overlord = AuthzSubjectManagerUtil.getLocalHome().create()
            .getOverlord();
    }

    private ActionForm createForm()
    {
        return new DynaActionForm();
    }

    private ActionMapping createMap()
    {
        return new ActionMapping();
    }

    public void testCreateEscalation() throws Exception
    {
        int sessionID = sessionMgr.put(overlord);
        String jsonString = makeJsonEscalation();

        JSONObject json = new JSONObject(jsonString);
        eventsBoss.saveEscalation(sessionID, json);

        // read after save
        JSONObject json_saved =
            eventsBoss.jsonByEscalationName(sessionID, BOGUS_NAME1);

        assertNotNull(json_saved);

        int id = json_saved.getJSONObject("escalation")
            .getInt("id");
        eventsBoss.deleteEscalationById(sessionID, new Integer(id));

        json_saved = eventsBoss.jsonByEscalationName(sessionID, BOGUS_NAME1);
        assertNull(json_saved);

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

        return e.toJSON().toString();
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
