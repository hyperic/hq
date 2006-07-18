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

import org.hyperic.hq.bizapp.shared.MeasurementBoss;

import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.shell.ShellCommandExecException;

import javax.naming.NamingException;

public class ClientShell_leakConnection 
    extends ShellCommandBase 
{
    private ClientShell shell;

    public ClientShell_leakConnection(ClientShell shell){
        this.shell = shell;
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        ClientShellBossManager manager;
        MeasurementBoss boss;
        manager = this.shell.getBossManager();
        try {
            boss = manager.getMeasurementBoss();
        } catch(ClientShellAuthenticationException exc){
            throw new ShellCommandExecException("Failed to get measurement boss",
                                                exc);
        } catch(NamingException exc){
            throw new ShellCommandExecException("Failed to get measurement boss");
        }
        try {
            boss.leakConnection();
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    public String getUsageShort(){
        return "Intentionally leak a database connection.";
    }

    public String getUsageHelp(String args[]){
        return "    " + this.getUsageShort() + ".";
    }
}
