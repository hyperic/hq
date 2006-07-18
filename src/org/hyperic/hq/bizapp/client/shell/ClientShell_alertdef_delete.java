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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Properties;

import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.shell.ShellCommandExecException;

public class ClientShell_alertdef_delete extends ShellCommandBase {
    private ClientShell              shell;
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_alertdef_delete(ClientShell shell){
        this.shell         = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        PrintStream out = this.getOutStream();
        String      aClass;
        Integer     alertdefID;

        if (args.length < 1) {
            throw new ShellCommandUsageException(this.getSyntax());
        }

        Integer[] ids = new Integer[args.length];        
        try {
            for (int i = 0; i < args.length; i++) {
                ids[i] = Integer.valueOf(args[i]);
            }
        } catch(NumberFormatException exc){
            throw new ShellCommandUsageException("IDs must be integers");
        }

        try {
            this.entityFetcher.deleteAlertDefinitions(ids);
            out.println("Alert definition(s) deleted: " + Arrays.asList(ids));
        } catch(Exception exc){
            throw new ShellCommandExecException("Error processing request ", 
                                                exc);
        }
    }

    public String getSyntaxArgs(){
        return "<ID1> <ID2> ...";
    }

    public String getUsageShort(){
        return "Delete alert definition(s)";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() +
               ", where ID's are separated by spaces";
    }
}
