package org.hyperic.hq.events.test;

import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.CommandContext;
import org.hyperic.hq.command.SaveCommand;
import org.hyperic.hq.command.FindCommand;
import org.hyperic.hq.command.RemoveCommand;
import org.hyperic.dao.DAOFactory;

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

        Escalation e = Escalation.newInstance(BOGUS_NAME);
        e.getActions().add(act1);
        e.getActions().add(act2);

        CommandContext context = CommandContext.createContext();
        context.setContextDao(DAOFactory.getDAOFactory().getEscalationDAO());

        context.execute(SaveCommand.setInstance(e));
        // look it up, there should be exactly one
        List result = assertEscalation(BOGUS_NAME, 1);

        if (result != null) {
            e = (Escalation)result.get(0);
            assertTrue(e.getActions().size() == 2);
            // verify escalation order
            assertTrue(e.getActions().get(0).equals(act1));
            assertTrue(e.getActions().get(1).equals(act2));

            // alter escalation order
            e.getActions().clear();
            e.getActions().add(act2);
            e.getActions().add(act1);
            long mtime = e.getEscalationState().getModifiedTime();
            context.execute(SaveCommand.setInstance(e));
            // look it up, there should be exactly one
            result = assertEscalation(BOGUS_NAME, 1);

            e = (Escalation)result.get(0);
            // should still have 2 actions
            assertTrue(e.getActions().size() == 2);
            // verify escalation order
            assertTrue(e.getActions().get(0).equals(act2));
            assertTrue(e.getActions().get(1).equals(act1));

            // escalation state time should not be updated
            assertTrue(e.getEscalationState().getModifiedTime() == mtime);

            // update state
            mtime = e.getEscalationState().getModifiedTime();
            EscalationState clone =
                (EscalationState)e.getEscalationState().clone();
            clone.setCurrentLevel(1);
            e.setEscalationState(clone);
            context.execute(SaveCommand.setInstance(e));
            // look it up, there should be exactly one
            result = assertEscalation(BOGUS_NAME, 1);
            e = (Escalation)result.get(0);
            // escalation state time should have been  updated
            assertTrue(e.getEscalationState().getModifiedTime() > mtime);

            // remove it

            context.execute(
                RemoveCommand.setInstance(e)
            );
            // look it up again should not be there.
            assertEscalation(BOGUS_NAME, 0);
        }
    }

    private List assertEscalation(String name, int count) {
        Escalation esc = Escalation.createFinder();
        esc.setName(name);
        CommandContext context = CommandContext.createContext(
            FindCommand.setInstance(esc));
        context.setContextDao(DAOFactory.getDAOFactory().getEscalationDAO());
        context.execute();
        List result = context.getQueryResult();
        assertTrue(result.size() == count);
        return result;
    }

    private EscalationAction createEmailAction(String[] users) {
        HashSet u = new HashSet();
        for (int i=0; i<users.length; i++) {
            u.add(users[i]);
        }
        return EscalationAction.newEmailAction(
            EmailActionConfig.TYPE_EMAILS, u, 60000);
    }
}
