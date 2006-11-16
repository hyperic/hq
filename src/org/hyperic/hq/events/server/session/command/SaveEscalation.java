package org.hyperic.hq.events.server.session.command;

import org.hyperic.hq.Command;
import org.hyperic.hq.CommandContext;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationDAO;
import org.hyperic.hq.events.server.session.ActionDAO;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.dao.DAOFactory;

import java.util.Iterator;

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
 * create escalation object
 */
public class SaveEscalation extends Command {

    private Escalation escalation;

    public static SaveEscalation setInstance(Escalation e) {
        SaveEscalation n = new SaveEscalation(e);
        return n;
    }

    protected SaveEscalation() {
    }

    protected SaveEscalation(Escalation escalation) {
        this.escalation = escalation;
        verify(escalation);
    }

    public void execute(CommandContext context) {
        verify(escalation);
        EscalationDAO dao =
            DAOFactory.getDAOFactory().getEscalationDAO();
        dao.save(escalation);
    }

    private void verify(Escalation escalation) {
        if (escalation == null) {
            throw new IllegalArgumentException("No escalation to create");
        }
        if (escalation.getActions().size() == 0) {
            throw new IllegalArgumentException("There must be at least one " +
                                               "action defined in the " +
                                               "escalation policy");
        }
        if (escalation.getName() == null) {
            throw new IllegalArgumentException("Escalation name must be " +
                                               "defined");
        }
    }
}
