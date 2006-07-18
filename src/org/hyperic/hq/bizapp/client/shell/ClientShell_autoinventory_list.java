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
import java.util.Date;
import java.util.List;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAIJobFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAIScheduleFetcher;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_autoinventory_list
    extends ClientShell_autoinventory_subcommand {

    protected static final String PARAM_HISTORY =  "-history";
    protected static final String PARAM_SCHEDULE = "-schedule";

    public ClientShell_autoinventory_list(ClientShell shell) { 
        super(shell); 
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "List auto-inventory schedules and history";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".\n\n" +
            "    To list a history of actions performed on a resource:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_SCHEDULE_RESOURCE) +
            "> <resource> " + PARAM_HISTORY + "\n\n" + 
            "    To list a schedule of future actions:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_SCHEDULE_RESOURCE) +
            "> <resource> " + PARAM_SCHEDULE + "\n";
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException
    {
        AppdefEntityID id;
        int type;

        PrintStream out = getOutStream();

        if((args.length != 3) ||
           !ClientShell_resource.paramIsValid(PARAM_VALID_SCHEDULE_RESOURCE, 
                                              args[0])) {
            throw new ShellCommandUsageException(this.getSyntax());
        }

        type = ClientShell_resource.paramToEntityType(args[0]);

        if(!args[2].equals(PARAM_HISTORY) &&
           !args[2].equals(PARAM_SCHEDULE))
            throw new ShellCommandUsageException(this.getSyntax());

        try {
            id = this.entityFetcher.getID(type, args[1]);
        } catch (Exception e) {
            throw new ShellCommandExecException(e);
        }

        try {
            if (args[2].equals(PARAM_HISTORY)) {
                this.doListHistory(id);
            } else {
                this.doListSchedule(id);
            }
        } catch ( Exception e ) {
            throw new ShellCommandExecException(e);
        }
    }

    private void doListHistory(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException {
        FindAIJobFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;

        pFetch = this.entityFetcher.findAIJobFetcher(id);
        ValuePrinter printer = 
            new ValuePrinter("%-10s %-20s %-15s %-15s %s", 
                             new int[] {
                                 ValueWrapper.ATTR_SUBJECT,
                                 ValueWrapper.ATTR_STARTTIME,
                                 ValueWrapper.ATTR_DURATION,
                                 ValueWrapper.ATTR_STATUS,
                                 ValueWrapper.ATTR_DESCRIPTION,
                             },
                             new int[] {
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_LONGDATE,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING
                             });

        PageControl pc = this.shell.getDefaultPageControl();
        pc.setSortorder(PageControl.SORT_DESC);

        printer.setHeaders(new String[] {"User", "Date Started", 
                                         "Duration", "Status", "Description"});
        printer.setPrologue("[ Autoinventory scan history listing ]");
        
        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.shell.performPaging(vpFetch, pc);
    }

    private void doListSchedule(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException {
        FindAIScheduleFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;

        pFetch = this.entityFetcher.findAIScheduleFetcher(id);

        ValuePrinter printer = 
            new ValuePrinter("%-7s %-30s %-20s %s", 
                             new int[] {
                                 ValueWrapper.ATTR_ID,
                                 ValueWrapper.ATTR_SCHEDULE_STRING,
                                 ValueWrapper.ATTR_NEXTFIRE,
                                 ValueWrapper.ATTR_DESCRIPTION
                             },
                             new int[] {
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_LONGDATE,
                                 ValuePrinter.ATTRTYPE_STRING
                             });
        printer.setHeaders(new String[] { "Id", "Schedule", 
                                          "Next Fire Time", "Description"});

        PageControl pc = this.shell.getDefaultPageControl();
        pc.setSortorder(PageControl.SORT_ASC);

        printer.setPrologue("[ Autoinventory schedule listing ]");

        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, pc);
    }
}

