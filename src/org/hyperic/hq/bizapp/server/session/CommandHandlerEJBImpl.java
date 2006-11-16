package org.hyperic.hq.bizapp.server.session;


import org.hyperic.hq.Command;
import org.hyperic.hq.CommandContext;
import org.hyperic.hq.CommandHandler;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.EJBException;
import java.util.List;
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
 * Generic command handler, commands are executed in JTA transaction context
 *
 * @ejb:bean name="CommandHandler"
 *      jndi-name="ejb/bizapp/CommandHandler"
 *      local-jndi-name="LocalCommandHandler"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class CommandHandlerEJBImpl implements SessionBean, CommandHandler {
    /**
     * @ejb:interface-method
     */
	public void executeHandler(CommandContext context) {
        List commands = context.getCommands();
        for(Iterator i = commands.iterator(); i.hasNext(); ) {
            Command command = (Command)i.next();
            command.execute(context);
        }
    }

    public void setSessionContext(SessionContext sessionContext)
        throws EJBException {
    }

    public void ejbRemove() throws EJBException {
    }

    public void ejbActivate() throws EJBException {
    }

    public void ejbPassivate() throws EJBException {
    }

    public void ejbCreate() {}
}
