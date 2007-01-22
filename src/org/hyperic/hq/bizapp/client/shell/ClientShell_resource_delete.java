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

import java.io.IOException;
import java.io.PrintStream;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;

import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_resource_delete
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
        ClientShell_resource.PARAM_GROUP,
        ClientShell_resource.PARAM_APP
    };

    private ClientShellEntityFetcher entityFetcher;
    private ClientShell shell= null;

    public ClientShell_resource_delete(ClientShell shell){
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
        String confirmDelete;
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

        switch (type) {
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            try {
                getOutStream().println("\nRemoving Application...");
                AppdefBoss appdefBoss = shell.getBossManager().getAppdefBoss();
                appdefBoss.removeApplication(authToken, id.getId());
                return;
            } catch (Exception e) {
                throw new ShellCommandExecException("Unable to remove " +
                                                    "application", e);
            }
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            // Confirm group delete
            confirmDelete = null;
            while (true) {
                try {
                    confirmDelete = ((ClientShell)getShell()).getInput(
                        "Removing Group " + id.getId()
                        + " will also remove any control actions "
                        + "scheduled for that group.\n"
                        + "Do you wish to continue (Y/N)? ");
                } catch (IOException e) {
                    throw new ShellCommandExecException
                        ("An IOException occurred reading the response.", e);
                }
                if (confirmDelete.toLowerCase().startsWith("n")) {
                    out.println("\nAborting remove...");
                    return;
                }
                if (confirmDelete.toLowerCase().startsWith("y")) {
                    try {
                        out.println("\nRemoving Group...");
                        AppdefBoss appdefBoss = shell.getBossManager().
                            getAppdefBoss();
                        appdefBoss.deleteGroup(authToken, id.getId());
                        return;
                    } catch (Exception e) {
                        throw new ShellCommandExecException("Unable to " +
                                                            "remove group",
                                                            e);
                    }
                }
                out.println("\nPlease press Y or N and hit Enter.\n");
            }
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            // Confirm platform delete
            confirmDelete = null;
            while (true) {
                try {
                    confirmDelete = ((ClientShell)getShell()).getInput(
                        "Removing Platform " + id.getId()
                        + " will also remove any servers or services "
                        + "defined for that platform.\n" 
                        + "Do you wish to continue (Y/N)? ");
                } catch (IOException e) {
                    throw new ShellCommandExecException
                        ("An IOException occurred reading the response.", e);
                }
                if (confirmDelete.toLowerCase().startsWith("n")) {
                    out.println("\nAborting remove...");
                    return;
                }
                if (confirmDelete.toLowerCase().startsWith("y")) {
                    try {
                        out.println("\nRemoving Platform...");
                        AppdefBoss appdefBoss = shell.getBossManager().
                            getAppdefBoss();
                        appdefBoss.removePlatform(authToken, id.getId());
                        return;
                    } catch (Exception e) {
                        throw new ShellCommandExecException("Unable to " +
                                                            "remove platform",
                                                            e);
                    }
                }
                out.println("\nPlease press Y or N and hit Enter.\n");
            }
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            // Confirm server delete
            confirmDelete = null;
            while (true) {
                try {
                    confirmDelete = ((ClientShell)getShell()).getInput(
                        "Removing Server " + id.getId()
                        + " will also remove any services "
                        + "defined for that server.\n" 
                        + "Do you wish to continue (Y/N)? ");
                } catch (IOException e) {
                    throw new ShellCommandExecException
                        ("An IOException occurred reading the response.", e);
                }
                if (confirmDelete.toLowerCase().startsWith("n")) {
                    out.println("\nAborting remove...");
                    return;
                }
                if (confirmDelete.toLowerCase().startsWith("y")) {
                    try {
                        out.println("\nRemoving Server...");
                        AppdefBoss appdefBoss = shell.getBossManager().
                            getAppdefBoss();
                        appdefBoss.removeServer(authToken, id.getId());
                        return;
                    } catch (Exception e) {
                        throw new ShellCommandExecException("Unable to " +
                                                            "remove server", e);
                    }
                }
                out.println("\nPlease press Y or N and hit Enter.\n");
            }
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            try {
                getOutStream().println("\nRemoving Service...");
                AppdefBoss appdefBoss = shell.getBossManager().getAppdefBoss();
                appdefBoss.removeService(authToken, id.getId());
                return;
            } catch (Exception e) {
                throw new ShellCommandExecException("Unable to remove " +
                                                    "service", e);
            }
        default:
            throw new ShellCommandUsageException("Removing resource of type " +
                                                 type + " not supported");
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Delete a resource";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    Command syntax:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) + ">" +
            " <resource>\n\n" +
            "    The resource given will be removed from the inventory.  If " +
            "a platform\n" +
            "    is given, the user will be asked to confirm that all " +
            "servers and\n" +
            "    services on that platform can be removed.\n";
    }
}
