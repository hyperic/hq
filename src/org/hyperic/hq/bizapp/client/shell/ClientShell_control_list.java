package org.hyperic.hq.bizapp.client.shell;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import javax.ejb.ObjectNotFoundException;
import javax.naming.NamingException;

import org.hyperic.hq.bizapp.client.pageFetcher.FindControlJobFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindControlScheduleFetcher;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.client.shell.ClientShellAuthenticationException;
import org.hyperic.hq.bizapp.client.shell.ClientShell_resource;
import org.hyperic.hq.bizapp.client.shell.ValuePrinterPageFetcher;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.util.NestedException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_control_list 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
        ClientShell_resource.PARAM_GROUP
    };

    private static final String PARAM_HISTORY =  "-history";
    private static final String PARAM_SCHEDULE = "-schedule";

    private ClientShell shell;
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_control_list(ClientShell shell){
        this.shell = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                         shell.getAuthenticator());
    }

    private void doListActions(AppdefEntityID id)
        throws ShellCommandExecException
    {
        PrintStream out = this.getOutStream();
        List actions;

        try {
            actions = this.entityFetcher.getActions(id);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        out.println("Actions available for " + id + ":");
        for(Iterator i=actions.iterator(); i.hasNext(); ){
            String val = (String)i.next();
            
            out.println("    " + val);
        }
    }

    private void doListHistory(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindControlJobFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;

        pFetch = this.entityFetcher.findControlJobFetcher(id);
        ValuePrinter printer = 
            new ValuePrinter("%-10s %-20s %-10s %-15s %-10s %s", 
                             new int[] {
                                 ValueWrapper.ATTR_SUBJECT,
                                 ValueWrapper.ATTR_STARTTIME,
                                 ValueWrapper.ATTR_ACTION,
                                 ValueWrapper.ATTR_DURATION,
                                 ValueWrapper.ATTR_STATUS,
                                 ValueWrapper.ATTR_MESSAGEORDESC,
                             },
                             new int[] {
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_LONGDATE,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING
                             });

        PageControl pc = this.shell.getDefaultPageControl();
        pc.setSortorder(PageControl.SORT_DESC);

        printer.setHeaders(new String[] {"User", "Date Started", "Action", 
                                         "Duration", "Status", "Description"});
        printer.setPrologue("[ Control history listing ]");
        
        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, pc);
    }

    private void doListSchedule(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindControlScheduleFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;

        pFetch = this.entityFetcher.findControlScheduleFetcher(id);

        ValuePrinter printer = 
            new ValuePrinter("%-7s %-10s %-30s %-20s %s", 
                             new int[] {
                                 ValueWrapper.ATTR_ID,
                                 ValueWrapper.ATTR_ACTION,
                                 ValueWrapper.ATTR_SCHEDULE_STRING,
                                 ValueWrapper.ATTR_NEXTFIRE,
                                 ValueWrapper.ATTR_DESCRIPTION
                             },
                             new int[] {
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_LONGDATE,
                                 ValuePrinter.ATTRTYPE_STRING
                             });
        printer.setHeaders(new String[] { "Id", "Action", "Schedule", 
                                          "Next Fire Time", "Description"});

        PageControl pc = this.shell.getDefaultPageControl();
        pc.setSortorder(PageControl.SORT_ASC);

        printer.setPrologue("[ Control schedule listing ]");

        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, pc);
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        AppdefEntityID id;
        int type;

        if ((args.length != 2 && args.length != 3) ||
            !ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, args[0]))
            throw new ShellCommandUsageException(this.getSyntaxEx());

        type = ClientShell_resource.paramToEntityType(args[0]);
        if(args.length == 3 &&
           !args[2].equals(PARAM_HISTORY) &&
           !args[2].equals(PARAM_SCHEDULE))
            throw new ShellCommandUsageException(this.getSyntaxEx());

        try {
            id = this.entityFetcher.getID(type, args[1]);
        } catch (Exception e) {
            throw new ShellCommandExecException(e);
        }

        try {
            if(args.length == 2){
                this.doListActions(id);
            } else if (args[2].equals(PARAM_HISTORY)) {
                this.doListHistory(id);
            } else {
                this.doListSchedule(id);
            }
        } catch (Exception e) {
            if (e instanceof NestedException) {
                PluginNotFoundException pnfe = 
                    (PluginNotFoundException)((NestedException)e).
                    getExceptionOfType(PluginNotFoundException.class);
                if (pnfe != null) {
                    this.getOutStream().println(id + " does not have " +
                                                "control support");
                    return;
                }

                ObjectNotFoundException onfe =
                    (ObjectNotFoundException)((NestedException)e).
                    getExceptionOfType(ObjectNotFoundException.class);
                if (onfe != null) {
                    this.getOutStream().println("Error finding resource: " +
                                                onfe.getMessage());
                    return;
                }
            }
       
            throw new ShellCommandExecException(e);
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "List actions and history for controllable resources";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".\n\n" +
            "    To list the actions which can be performed on a resource:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource>\n\n" +
            "    To list a history of actions performed on a resource:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource> " + PARAM_HISTORY + "\n\n" +
            "    To list a schedule of future actions:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource> " + PARAM_SCHEDULE + "\n";
    }
}
