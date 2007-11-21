/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandInitException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_autoinventory_command
    extends ClientShell_autoinventory_subcommand {

    protected ClientShell_autoinventory_remote remoteHandler = null;

    public ClientShell_autoinventory_command(ClientShell shell) { 
        super(shell); 
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException
    {   
        super.init(commandName, shell);
        remoteHandler = new ClientShell_autoinventory_remote(this);
        remoteHandler.init(commandName, shell);
    }

    public String getSyntaxEx(){
        return "Use 'help " + getCommandName() + "' for details";
    }

    public String getUsageShort(){
        String commandName = getCommandName();

        if (commandName.endsWith("start")) {
            return "Start an auto-inventory scan";
        } else if (commandName.endsWith("stop")) {
            return "Stop an auto-inventory scan";
        } else if (commandName.endsWith("status")) {
            return "Report status of a running auto-inventory scan";
        }

        throw new IllegalArgumentException("Unknown command name " +
                                           commandName);
    }

    public String getUsageHelp(String[] args) {
        return "    " + getUsageShort() + ".\n\n" +
            "    To run the auto-inventory command:\n" +
            "      " + getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource> \n";
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException {

        remoteHandler.processCommand(args);
    }
}
