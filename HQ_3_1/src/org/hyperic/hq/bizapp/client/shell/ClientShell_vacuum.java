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

import org.hyperic.hq.bizapp.shared.ConfigBoss;

import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.shell.ShellCommandExecException;

import javax.naming.NamingException;

public class ClientShell_vacuum extends ShellCommandBase {
    private ClientShell shell;

    public ClientShell_vacuum(ClientShell shell){
        this.shell = shell;
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        ClientShellBossManager manager;
        ConfigBoss boss;
        String version;

        if(args.length != 0)
            throw new ShellCommandExecException(this.getSyntax());

        manager = this.shell.getBossManager();
        try {
            boss = manager.getConfigBoss();
        } catch(ClientShellAuthenticationException exc){
            throw new ShellCommandExecException("Failed to get meas. boss",
                                                exc);
        } catch(NamingException exc){
            throw new ShellCommandExecException("Failed to get meas. boss");
        }
        long duration = 0;
        try {
            duration = boss.vacuum(shell.getAuthenticator().getAuthToken());
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
        if (duration == -1) {
            this.getOutStream().println("Database not vacuumed (it was not a "
                                        + "PostgreSQL database).");
        } else {
            this.getOutStream().println("Database successfully vacuumed in "
                                        + StringUtil.formatDuration(duration));
        }
    }

    public String getUsageShort(){
        return "Cleans the " + ClientShell.PRODUCT 
            + " server's database if using PostgreSQL (built-in or external)";
    }

    public String getUsageHelp(String args[]){
        return "    " + this.getUsageShort() + "."
            + "\nThis command requires admin privileges."
            + "\nThis command is only meaningful if the HQ server is configured"
            + "\nto use a PostgreSQL database (including the built-in HQ "
            + "database.";
    }
}
