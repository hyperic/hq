package org.hyperic.hq.events.test;

import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.hq.events.server.session.command.CreateEscalation;
import org.hyperic.hq.events.server.session.command.FindEscalation;
import org.hyperic.hq.events.server.session.command.RemoveEscalation;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.CommandContext;

import javax.naming.NamingException;
import java.util.HashSet;
import java.util.Random;
import java.util.List;


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
/**
 *
 */
public class EscalationTest
    extends HQEJBTestBase {
    private final int RANDOM_ID = (new Random()).nextInt(10000);
    private final String BOGUS_NAME = "escalation " + RANDOM_ID;

    public EscalationTest(String testName) {
        super(testName);
    }

    public void testCreateEscalation() throws Exception {

        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                createEscalationTest();
            }
        });

    }

    private void createEscalationTest()
        throws javax.ejb.CreateException, NamingException {
        EscalationAction act1 = createEmailAction(
            new String[] {"joe@gmail.com", "bob@yahoo.com"});

        EscalationAction act2 = createEmailAction(
            new String[] {"paul@att.com", "bill@google.com"});

        Escalation e = Escalation.createEscalation(BOGUS_NAME);
        e.getActions().add(act1);
        e.getActions().add(act2);

        CommandContext context = CommandContext.createContext(
            CreateEscalation.newEscalation(e));
        context.execute();
        // look it up, there should be exactly one
        List result = assertEscalation(BOGUS_NAME, 1);

        // remove it
        if (result != null) {
            context = CommandContext.createContext(
                RemoveEscalation.setInstance((Escalation)result.get(0)));
            context.execute();
        }
        // look it up again should not be there.
        assertEscalation(BOGUS_NAME, 0);
    }

    private List assertEscalation(String name, int count) {
        Escalation esc = Escalation.createFinder();
        esc.setName(name);
        CommandContext context = CommandContext.createContext(
            FindEscalation.createFinder(esc));
        context.execute();
        List result = (List)context.getResult();
        assertTrue(result.size() == count);
        return result;
    }

    private EscalationAction createEmailAction(String[] users) {
        HashSet u = new HashSet();
        for (int i=0; i<users.length; i++) {
            u.add(users[i]);
        }
        return EscalationAction.createEmailAction(
            EmailActionConfig.TYPE_EMAILS, u, 60000);
    }
}
