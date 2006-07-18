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
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_sigar
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
    };

    private ClientShellEntityFetcher entityFetcher;
    private ClientShell shell= null;

    public ClientShell_sigar(ClientShell shell){
        this.shell = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                         shell.getAuthenticator());
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        PrintStream out = getOutStream();
        PrintStream err = getErrStream();
        AppdefEntityID id;
        int type;

        if(args.length != 3)
            throw new ShellCommandUsageException(this.getSyntaxEx());

        type = ClientShell_resource.paramToEntityType(args[0]);

        try {
            id = this.entityFetcher.getID(type, args[1]);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
        
        int authToken = this.shell.getAuthenticator().getAuthToken();

        try {
            MeasurementBoss boss;

            out.println("Running command " + args[2]);

            boss = this.shell.getBossManager().getMeasurementBoss();
            List res = boss.sigarCmd(authToken, id, args[2]);

            for (Iterator i = res.iterator(); i.hasNext(); ) {
                List row = (List)i.next();
                for (Iterator j = row.iterator(); j.hasNext(); ) {
                    String item = (String)j.next();
                    out.print(item);
                    out.print(" ");
                }
                out.print("\n");
            }
            
        } catch (Exception e) {
            throw new ShellCommandExecException(e);
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Run a sigar command on a remote agent";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    Command syntax:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) + ">" +
            " <resource> <cmd>\n\n";
    }
}
