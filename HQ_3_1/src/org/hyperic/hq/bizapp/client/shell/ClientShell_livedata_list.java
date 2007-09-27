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

import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

import java.io.PrintStream;

public class ClientShell_livedata_list extends ShellCommandBase {
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };

    private ClientShellEntityFetcher _entityFetcher;

    public ClientShell_livedata_list(ClientShell shell) {
        _entityFetcher =  new ClientShellEntityFetcher(shell.getBossManager(),
                                                       shell.getAuthenticator());
    }

    public void processCommand(String [] args)
        throws ShellCommandUsageException, ShellCommandExecException {

        if (args.length != 2) {
            throw new ShellCommandUsageException(getSyntaxEx());
        }

        try {
            int type = ClientShell_resource.paramToEntityType(args[0]);
            AppdefEntityID id =  _entityFetcher.getID(type, args[1]);

            String[] cmds = _entityFetcher.getLiveDataCommands(id);

            PrintStream ps = this.getShell().getOutStream();
            ps.println("Commands for " + id + ":");
            for (int i = 0; i < cmds.length; i++) {
                ps.println("  " + cmds[i]);
            }

        } catch (Exception e) {
            throw new ShellCommandExecException(e);
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Get the live data commands for a resource";
    }

    public String getUsageHelp (String[] args) {
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> < name | id >";
    }
}
