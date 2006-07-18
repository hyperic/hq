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
import java.text.SimpleDateFormat;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_autoinventory_schedule
    extends ClientShell_autoinventory_subcommand {

    protected static ScheduleShellUtil schedUtil = new ScheduleShellUtil();

    public ClientShell_autoinventory_schedule(ClientShell shell) { 
        super(shell); 
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Manage scheduled auto-inventory scans";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    Command syntax:\n" +
            "      " + this.getCommandName() + " " + "delete" + " " +
            "<id>\n\n" +
            "    The <id> argument is a vaild schedule id.  View id's " +
            "using the\n" +
            "    'autoinventory list' command.\n\n" +
            "      " + this.getCommandName() + " <" + 
            ClientShell_resource.
            generateArgList(PARAM_VALID_SCHEDULE_RESOURCE) +
            "> <resource> \n" + 
            "       " + cmdSpace + schedUtil.getScheduleOptions() + 
            "\n";
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException
    {
        PrintStream out = getOutStream();
        AppdefEntityID id;
        int type;
        ScheduleValue schedValue;

        if (args.length !=2 && args.length != 3) {
            throw new ShellCommandUsageException("Wrong number of arguments");
        }
         
        // Handle delete command
        if (args[0].equals("delete")) {
            Integer toDelete;
            try {
                toDelete = Integer.valueOf(args[1]);
            } catch (NumberFormatException e) {
                this.getErrStream().println("Invalid id: " + args[1]);
                return;
            }

            try {
                this.entityFetcher.deleteAIJob(new Integer[] {toDelete});
                this.getOutStream().println("Scheduled autoinventory scan " +
                                            "deleted");
            } catch (Exception e) {
                this.getErrStream().println("Unable to delete scheduled " +
                                            "autoinventory scan: " + e);
            }

            return;
        }

        // Schedule command
        if((args.length != 3) ||
           !ClientShell_resource.paramIsValid(PARAM_VALID_SCHEDULE_RESOURCE, 
                                              args[0])) {
            throw new ShellCommandUsageException("Invalid resource: '" +
                                                 args[0] + "'");
        }

        type = ClientShell_resource.paramToEntityType(args[0]);

        schedValue = schedUtil.parseOptions(args, 2, 
                                            new SimpleDateFormat(), 
                                            "autoinventory scan",
                                            this);

        try {
            id  = this.entityFetcher.getID(type, args[1]);
        } catch (Exception e) {
            throw new ShellCommandExecException("Unable to fetch id: " + 
                                                args[1], e);
        }

        AIBoss aiBoss;
        AppdefBoss appdefBoss;
        AppdefGroupValue gval;

        try {
            aiBoss = shell.getBossManager().getAIBoss();
            appdefBoss = shell.getBossManager().getAppdefBoss(); 
        } catch (Exception e) {
            throw new ShellCommandExecException("Error loading bosses: " +
                                                e.getMessage(), e);
        }

        int platformID;
        if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            try {
                gval = entityFetcher.getGroupValue(String.valueOf(id.getID()));
            } catch (Exception e) {
                throw new ShellCommandExecException("Error loading group: "
                                                    + id + ": "
                                                    + e.getMessage(), e);
            }

            if (gval.getGroupEntType() == 
                AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {

                List entries = gval.getAppdefGroupEntries();
                if (entries == null || entries.size() == 0) {
                    throw new ShellCommandExecException("Group is empty");
                }
                AppdefEntityID appdefID = (AppdefEntityID) entries.get(0);
                platformID = appdefID.getID();

            } else {
                throw new ShellCommandExecException("Group did not contain " +
                                                    "platforms");
            }
        } else {
            platformID = id.getID();
        }

        AutoinventoryShellConfigDriver configDriver
            = new AutoinventoryShellConfigDriver_remote(this, 
                                                        platformID,
                                                        aiBoss,
                                                        appdefBoss);
        ScanConfigurationCore scanConfig;
        try {
            scanConfig = configDriver.getScanConfiguration();
        } catch (Exception e) {
            throw new ShellCommandExecException("Unable to generate " +
                                                "autoinventory scan " +
                                                "configuration: ", e);
        }

        try {
            id  = this.entityFetcher.getID(type, args[1]);
            this.entityFetcher.scheduleAIScan(id, 
                                              scanConfig, 
                                              "CLI-scan-"
                                              + System.currentTimeMillis(),
                                              schedValue.getDescription(),
                                              schedValue);
            
        } catch (Exception e) {
            throw new ShellCommandExecException("Unable to schedule " +
                                                "autoinventory scan", e);
        }
        out.println("Scheduling autoinventory scan on " + id);
        out.println("Scan will recur: " + schedValue.getScheduleString());
        out.println("Scan Configuration that will be used: "
                    + scanConfig);
    }
}
