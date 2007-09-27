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

import org.hyperic.hq.bizapp.shared.ProductBoss;

import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.shell.ShellCommandExecException;

import javax.naming.NamingException;

public class ClientShell_version 
    extends ShellCommandBase 
{
    private ClientShell shell;

    public ClientShell_version(ClientShell shell){
        this.shell = shell;
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        ClientShellBossManager manager;
        ProductBoss boss;
        String version;

        if(args.length != 0)
            throw new ShellCommandExecException(this.getSyntax());

        manager = this.shell.getBossManager();
        try {
            boss = manager.getProductBoss();
        } catch(ClientShellAuthenticationException exc){
            throw new ShellCommandExecException("Failed to get product boss",
                                                exc);
        } catch(NamingException exc){
            throw new ShellCommandExecException("Failed to get product boss");
        }
        try {
            version = boss.getVersion();
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        this.getOutStream().println("Server reports version '" +version +"'");
                                    
    }

    public String getUsageShort(){
        return "Retrieve " + ClientShell.PRODUCT + " version information";
    }

    public String getUsageHelp(String args[]){
        return "    " + this.getUsageShort() + ".";
    }
}
