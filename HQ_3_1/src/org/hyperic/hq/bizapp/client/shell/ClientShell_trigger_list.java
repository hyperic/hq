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

package org.hyperic.hq.bizapp.client.shell;

import org.hyperic.util.PrintfFormat;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.events.shared.ActionValue;

import java.io.PrintStream;
import java.util.Collection;

public class ClientShell_trigger_list extends ShellCommandBase {
    private ClientShellAuthenticator auth = null;
    private ClientShell_trigger owner;

    public ClientShell_trigger_list(ClientShell_trigger owner,
                                 ClientShellAuthenticator auth)
    {
        this.owner = owner;
        this.auth  = auth;
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        PrintStream out = getOutStream();
        EventsBoss eventsBoss = this.owner.getEventsBoss();
        int authToken = this.auth.getAuthToken();
        Collection c;

        try {
            c = eventsBoss.getAllRegisteredTriggers(authToken);
        } catch(Exception exc){
            throw new ShellCommandExecException("Error getting registered " +
                                                "triggers", exc);
        }

        PrintfFormat pFmt = new PrintfFormat("%-6s %s");
        PrintfFormat oFmt = new PrintfFormat("%-6d %s\n       %s\n");
        Object[] fArgs = new Object[3];
        RegisteredTriggerValue vals[] = 
           (RegisteredTriggerValue[]) c.toArray(new RegisteredTriggerValue[0]);

        out.println(pFmt.sprintf(new String[] { "ID", "Class" }));
        out.println(pFmt.sprintf(new String[] { "--", "-----" }));
        for(int i=0; i<vals.length; i++){
            RegisteredTriggerValue val = vals[i];

            fArgs[0] = val.getId();
            fArgs[1] = val.getClassname();
            try {
                fArgs[2] = ConfigResponse.decode(val.getConfig());
            } catch(EncodingException exc){
                fArgs[2] = "<Unparsable data>";
            }

            out.println(oFmt.sprintf(fArgs));
        }
    }

    public String getUsageShort(){
        return "List all triggers";
    }

    public String getUsageHelp(String[] args) {
        return "    " + getUsageShort();
    }
}
