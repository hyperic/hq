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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_autoinventory_runtimescan
    extends ClientShell_autoinventory_subcommand {

    public ClientShell_autoinventory_runtimescan(ClientShell shell) { 
        super(shell); 
    }

    public String getSyntaxEx(){
        return "Use 'help " + getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Enable or disable runtime discovery";
    }

    public String getUsageHelp(String[] args) {
        return "    " + getUsageShort() + ".\n\n" +
            "    To enable or disable runtime discovery:\n" +
            "      " + getCommandName() + " <enable | disable> <" +
            ClientShell_resource.
            generateArgList(PARAM_VALID_RUNTIME_SCAN_RESOURCE) +
            "> <resource> \n";
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException {
        
        if (args.length != 3 ||
            !ClientShell_resource.paramIsValid(PARAM_VALID_RUNTIME_SCAN_RESOURCE, 
                                               args[1])) {
            throw new ShellCommandUsageException(getSyntax());
        }
        
        String action = args[0];
        boolean doEnable;
        if (action.equalsIgnoreCase("enable")) {
            doEnable = true;
        } else if (action.equalsIgnoreCase("disable")) {
            doEnable = false;
        } else {
            throw new ShellCommandUsageException(getSyntax());
        }

        AppdefEntityID id;
        int type;

        type = ClientShell_resource.paramToEntityType(args[1]);
        try {
            id  = entityFetcher.getID(type, args[2]);
        } catch (Exception e) {
            throw new ShellCommandExecException("Unable to fetch id: " + 
                                                args[2], e);
        }

        try {
            entityFetcher.toggleRuntimeScan(id, doEnable);
        } catch (Exception e) {
            getErrStream().println("Unable to " + action.toLowerCase()
                                        + " runtime autodiscover scans "
                                        + " on resource: " + id + ": " + e);
            return;
        }
    }
}
