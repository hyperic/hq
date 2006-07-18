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
import java.io.EOFException;

import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.SchemaBuilder;

public class ClientShell_transfer
    extends ShellCommandBase 
{
    private static final String PROP_FILE      = "file";
    private static final String PROP_DEST      = "dest";
    private static final String PROP_OVERWRITE = "overwrite";

    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
    };

    private ClientShellEntityFetcher entityFetcher;
    private ClientShell shell = null;

    public ClientShell_transfer(ClientShell shell){
        this.shell = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                         shell.getAuthenticator());
    }

    private ConfigSchema getConfigSchema() {
        SchemaBuilder sb = new SchemaBuilder();

        sb.add(PROP_FILE, "File to copy", "");
        sb.add(PROP_DEST, "Destination", "");
        sb.add(PROP_OVERWRITE, "Overwrite if the file exists?", false); 
        
        return sb.getSchema();
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        PrintStream out = getOutStream();
        PrintStream err = getErrStream();
        AppdefEntityID id;
        int type;

        if(args.length != 2)
            throw new ShellCommandUsageException(this.getSyntaxEx());

        type = ClientShell_resource.paramToEntityType(args[0]);

        try {
            id = this.entityFetcher.getID(type, args[1]);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
        
        int authToken = this.shell.getAuthenticator().getAuthToken();

        ConfigSchema schema = getConfigSchema();
        ConfigResponse resp;

        //XXX: TODO: Allow multiple files in one transaction.
        out.println("Configure file transfer:");
        try {
            resp = this.shell.processConfigSchema(schema);
        } catch(EOFException exc){
            out.println("\nFile transfer aborted");
            return;
        } catch(Exception exc){
            throw new ShellCommandExecException(exc.getMessage(), exc);
        }

        String[][] files = {
            {resp.getValue(PROP_FILE), resp.getValue(PROP_DEST)}};

        int[] modes = new int[1];
        if (resp.getValue(PROP_OVERWRITE).equals("true"))
            modes[0] = FileData.WRITETYPE_CREATEOROVERWRITE;
        else
            modes[0] = FileData.WRITETYPE_CREATEONLY;

        AppdefBoss boss;

        out.println("Transferring files...");
        try {
            boss = this.shell.getBossManager().getAppdefBoss();
            FileDataResult[] res = boss.agentSendFileData(authToken, id,
                                                          files, modes);
            for (int i = 0; i < res.length; i++) {
                out.println(res[i].getFileName() + " sent in " +
                            res[i].getSendTimeSeconds() + " seconds (" +
                            res[i].getTxRate() + " Kb/s)");
            }
            out.println("Done.");
        } catch (Exception e) {
            throw new ShellCommandExecException(e);
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Transfer a files to a remote agent";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    Command syntax:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) + ">" +
            " <resource> \n\n";
    }
}
