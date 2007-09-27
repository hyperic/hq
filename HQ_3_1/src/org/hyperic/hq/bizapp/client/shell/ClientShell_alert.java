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

import java.util.Hashtable;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.util.shell.MultiwordShellCommand;
import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandInitException;

public class ClientShell_alert extends MultiwordShellCommand {
    private ClientShellAuthenticator auth       = null;
    private ClientShell              shell      = null;
    private EventsBoss               eventsBoss = null;

    public ClientShell_alert (ClientShell shell) {
        this.shell = shell;
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException 
    {
        this.auth = this.shell.getAuthenticator();

        super.init(commandName, shell);

        registerSubHandler("delete", 
                           new ClientShell_alert_delete(this.shell));
        registerSubHandler("list", 
                           new ClientShell_alert_list(this.shell));
    }

    public String getUsageShort(){
        return "List and delete alerts";
    }

    public String getUsageHelp(String args[]) {
        String res;

        if((res = super.getUsageHelp(args)) != null)
            return res;

        return "alert <list>\n" +
               "  List alerts\n" +
               "alert <delete>\n" +
               "  Delete alerts\n" +
               "  for specific help, use 'help alert list', etc.";
    }


    public EventsBoss getEventsBoss() 
        throws ShellCommandExecException 
    { 
        if(this.eventsBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();
                
                this.eventsBoss = EventsBossUtil.getHome(env).create();
            } catch(Exception e){
                throw new ShellCommandExecException("Could not getEventsBoss",
                                                    e);
            }
        }
        return this.eventsBoss; 
    }
}
