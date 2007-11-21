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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_autoinventory_queue 
    extends ClientShell_autoinventory_subcommand {

    public ClientShell_autoinventory_queue(ClientShell shell) { 
        super(shell); 
    }

    public String getSyntaxEx(){
        return "Use 'help " + getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "View and process the auto-inventory queue";
    }

    public String getUsageHelp(String[] args) {

        return "    " + getUsageShort() + ".\n\n"
            + "    To view the auto-inventory queue:\n"
            + "    " + getCommandName() + " view [-all | -ph | "
            + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE)
            + " <resource>] \n"
            + "\n    Use -all to view everything including ignored resources. Use the -ph flag"
            + "\n    to view placeholder resources.  Placeholder are resources that are unchanged "
            + "\n    relative to the current inventory.  Use "
            + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE)
            + " to view auto-inventory"
            + "\n    information for a single platform."
            + "\n"
            + "\n    To process the auto-inventory queue:"
            + "\n    " + getCommandName() + " process <action> [-aiplatform <aiplatform-id-list>] "
            + "\n                                         [-aiserver <aiserver-id-list>]"
            + "\n                                         [-aiip <aiip-id-list>]"
            + "\n"
            + "\n    The lists of resources should be seperated by spaces."
            + "\n"
            + "\n    Where <action> is one of: "
            + "\n            approve  : merge queue data into the inventory"
            + "\n            ignore   : mark queued resources as ignored, appdef is unaffected"
            + "\n";
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException {

        if (args.length < 1) {
            throw new ShellCommandUsageException("Invalid autoinventory " +
                                                 "queue command, must " +
                                                 "specify " +
                                                 "view or process.");
        }

        if (args[0].equalsIgnoreCase("view")) {
            doQueueView(args);
            
        } else if (args[0].equalsIgnoreCase("process")) {
            doQueueProcess(args);

        } else {
            throw new ShellCommandUsageException("Invalid autoinventory " +
                                                 "queue command.");
        }
    }

    private void doQueueView ( String[] args ) 
        throws ShellCommandExecException, ShellCommandUsageException {

        if (args.length < 1 || args.length > 4) {
            throw new ShellCommandUsageException("Wrong number of arguments.");
        }

        boolean showIgnored = false;
        boolean showPlaceholders = false;
        AppdefEntityID id = null;
        int i;
        for (i=1; i<args.length; i++) {
            if (args[i].equalsIgnoreCase("-all")) {
                showIgnored = true;

            } else if (args[i].equalsIgnoreCase("-ph")) {
                showPlaceholders = true;

            } else {
                if(!ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE,
                                                      args[i])) {
                    throw new ShellCommandUsageException("Invalid option for " +
                                                         "autoinventory " +
                                                         "queue view: " + 
                                                         args[i]);
                }
                if (++i >= args.length) {
                    throw new ShellCommandUsageException("No platform id");
                }
                try {
                    id = entityFetcher.getID(AppdefEntityConstants.
                                             APPDEF_TYPE_AIPLATFORM,
                                             args[i]);
                } catch (Exception e) {
                    throw new ShellCommandExecException("Error loading " +
                                                        "resource: " + args[i]);
                }
            }
        }

        AIBoss aiBoss;
        PrintStream out = getOutStream();

        if ( id == null ) {
            // List all platforms in the queue
            List queue = null;
            try {
                aiBoss = shell.getBossManager().getAIBoss();
                queue = aiBoss.getQueue(auth.getAuthToken(),
                                             showIgnored, 
                                             showPlaceholders,
                                             null);
            } catch ( Exception e ) {
                e.printStackTrace();
                throw new ShellCommandExecException("Error retrieving " +
                                                    "autoinventory queue " +
                                                    "from server: " + e, e); 
            }

            ValuePrinter printer = 
                new ValuePrinter(out,
                                 new int[] { ValueWrapper.ATTR_ID,
                                             ValueWrapper.ATTR_FQDN,
                                             ValueWrapper.ATTR_QUEUESTATUS,
                                             ValueWrapper.ATTR_DIFF });
            printer.setHeaders(new String[] { "ID", 
                                              "FQDN",
                                              "Status", 
                                              "Differences" });
            printer.printList(queue);

        } else {
            // Just list info for the specified platform.
            AIPlatformValue aiplatform;
            Long timeLong;
            String ts;
            
            try {
                aiBoss = shell.getBossManager().getAIBoss();
                aiplatform = aiBoss.findAIPlatformById(auth.getAuthToken(), 
                                                       id.getID());
            } catch (Exception e) {
                throw new ShellCommandExecException("Error looking up " +
                                                    "platform: " + e, e);
            }

            if (aiplatform == null) {
                out.println("Platform not found in autoinventory queue.");
                return;
            }

            // Print out general platform information
            int qstat = aiplatform.getQueueStatus();
            out.println("Platform Attributes");
            out.println("-------------------");
            out.println("ID:\t\t" + aiplatform.getId());
            out.println("Name:\t\t" + aiplatform.getName());
            out.println("FQDN:\t\t" + aiplatform.getFqdn());
            out.println("CertDN:\t\t" + aiplatform.getCertdn());
            out.println("Agent-Token:\t" + aiplatform.getAgentToken());
            out.println("Status:\t\t" + 
                        AIQueueConstants.getQueueStatusString(qstat));
            out.println("Differences:\t" + 
                        AIQueueConstants.getPlatformDiffString(qstat,
                                                               aiplatform.
                                                               getDiff()));
            if (showIgnored) {
                out.println("Ignored?:\t" + aiplatform.getIgnored());
            }

            timeLong = aiplatform.getCTime();
            if (timeLong != null)
                ts = (new Date(timeLong.longValue())).toString();
            else 
                ts = "N/A";

            out.println("Created:\t" + ts);
            timeLong = aiplatform.getMTime();

            if (timeLong != null)
                ts = (new Date(timeLong.longValue())).toString();
            else 
                ts = "N/A";

            out.println("Modified:\t" + ts);
            out.println("");

            AIIpValue[] ips = aiplatform.getAIIpValues();
            if (ips.length == 0) {
                out.println("No IP addresses present in autoinventory data");
            } else {
                out.println("IP Addresses");
                out.println("------------");
                for (i=0; i<ips.length; i++) {
                    out.println("\tID:\t\t" + ips[i].getId());
                    out.println("\tIP Address:\t" + 
                                ips[i].getAddress());
                    out.println("\tNetmask:\t" + 
                                ips[i].getNetmask());
                    out.println("\tMAC Address:\t" + 
                                ips[i].getMACAddress());
                    qstat = ips[i].getQueueStatus();
                    out.println("\tStatus:\t\t" + 
                                AIQueueConstants.getQueueStatusString(qstat));
                    out.println("\tDifferences:\t" +
                                AIQueueConstants.
                                getIPDiffString(qstat, ips[i].getDiff()));
                    if (showIgnored) {
                        out.println("\tIgnored?:\t" + ips[i].getIgnored());
                    }
                    
                    timeLong = ips[i].getCTime();
                    if (timeLong != null) 
                        ts = (new Date(timeLong.longValue())).toString();
                    else 
                        ts = "N/A";
                    out.println("\tCreated:\t" + ts);

                    timeLong = ips[i].getMTime();
                    if (timeLong != null)
                        ts = (new Date(timeLong.longValue())).toString();
                    else 
                        ts = "N/A";

                    out.println("\tModified:\t" + ts);
                    out.println("");
                }
            }

            AIServerValue[] servers = aiplatform.getAIServerValues();
            if (servers.length == 0) {
                out.println("No servers present in autoinventory data\n");
            } else {
                out.println("Servers");
                out.println("-------");
                for ( i=0; i<servers.length; i++ ) {
                    out.println("\tID:\t\t" + 
                                servers[i].getId());
                    out.println("\tType:\t\t" +
                                servers[i].getServerTypeName());
                    out.println("\tName:\t\t" + 
                                servers[i].getName());
                    out.println("\tInstall Path:\t" +
                                servers[i].getInstallPath());
                    out.println("\tAIID:\t\t" + 
                                servers[i].getAutoinventoryIdentifier());
                    qstat = servers[i].getQueueStatus();
                    out.println("\tStatus:\t\t" + 
                                AIQueueConstants.getQueueStatusString(qstat));
                    out.println("\tDifferences:\t" +
                                AIQueueConstants.
                                getServerDiffString(qstat, 
                                                    servers[i].getDiff()));
                    if (showIgnored) {
                        out.println("\tIgnored?:\t" + servers[i].getIgnored());
                    }
                    
                    timeLong = servers[i].getCTime();
                    if (timeLong != null)
                        ts = (new Date(timeLong.longValue())).toString();
                    else 
                        ts = "N/A";

                    out.println("\tCreated:\t" + ts);
                    timeLong = servers[i].getMTime();

                    if (timeLong != null) 
                        ts = (new Date(timeLong.longValue())).toString();
                    else 
                        ts = "N/A";

                    out.println("\tModified:\t" + ts);
                    out.println("");
                }
            }
        }
    }

    protected void doQueueProcess (String[] args)
        throws ShellCommandExecException, ShellCommandUsageException {

        if (args.length < 2) {
            throw new ShellCommandUsageException("Wrong number of arguments.");
        }
 
        AIBoss aiBoss;
        try {
            aiBoss = shell.getBossManager().getAIBoss();
        } catch (Exception e) {
            throw new ShellCommandExecException("Error getting " +
                                                "auto-inventory boss " + e);
        }
        
        String actionString = args[1];
        int action = AIQueueConstants.getActionValue(actionString);

        int i, type = -1;
        Integer id;
        List platformList = new ArrayList();
        List serverList   = new ArrayList();
        List ipList       = new ArrayList();
        List activeList = null;
        for (i = 2; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (activeList != null && activeList.size() == 0) {
                    throw new ShellCommandUsageException("Expected id list.");
                }

                type = ClientShell_resource.paramToEntityType(args[i]);
                switch (type) {
                case AppdefEntityConstants.APPDEF_TYPE_AIPLATFORM:
                    activeList = platformList;
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_AISERVER:
                    activeList = serverList;
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_AIIP:
                    activeList = ipList;
                    break;
                }
            } else {
                if (activeList == null || type == -1) {
                    throw new ShellCommandUsageException("Unexpected " +
                                                         "argument: " +
                                                         args[i]);
                }
                try {
                    id = new Integer(entityFetcher.getID(type, 
                                                         args[i]).getID());
                } catch (Exception e) {
                    throw new ShellCommandExecException("Error loading " +
                                                        "resource: " +
                                                        args[i]);
                }
                
                activeList.add(id);

                if (activeList.equals(platformList)) {
                    // Add the servers and ips, too
                    try {
                        AIPlatformValue aiplatform =
                            aiBoss.findAIPlatformById(auth.getAuthToken(),
                                                      id.intValue());
                        // Add the servers
                        AIServerValue[] aiServers =
                            aiplatform.getAIServerValues();
                        for (int j = 0; j < aiServers.length; j++)
                            serverList.add(aiServers[j].getId());

                        // Add the IPs
                        AIIpValue[] ips = aiplatform.getAIIpValues();
                        for (int j = 0; j < ips.length; j++)
                            ipList.add(ips[j].getId());
                    } catch (Exception e) {
                        throw new ShellCommandExecException("Error looking " +
                                                            "up platform: " +
                                                            e, e);
                    }
                }
            }
        }

        if (platformList.size() 
            + serverList.size()
            + ipList.size() == 0) {
            throw new ShellCommandUsageException("You must specify one of "
                                                 + "-aiplatform, -aiip, or "
                                                 + "-aiserver");
        }

        PrintStream out = getOutStream();

        try {
            aiBoss.processQueue(auth.getAuthToken(),
                                platformList, serverList, ipList,
                                action);
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new ShellCommandExecException("Error approving "
                                                + " autoinventory data: " + e);
        }
    }
}
