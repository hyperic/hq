package org.hyperic.hq.bizapp.client.shell;

import java.io.PrintStream;

import java.text.SimpleDateFormat;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.client.shell.ClientShell_resource;
import org.hyperic.hq.bizapp.client.shell.ScheduleShellUtil;
import org.hyperic.hq.scheduler.ScheduleValue;

import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_control_schedule 
    extends ShellCommandBase 
{
    private static final String PARAM_DELETE = "delete";

    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
        ClientShell_resource.PARAM_GROUP
    };

    private ClientShell shell;
    private ClientShellEntityFetcher entityFetcher;
    private static ScheduleShellUtil schedUtil = new ScheduleShellUtil();

    public ClientShell_control_schedule(ClientShell shell){
        this.shell = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                         shell.getAuthenticator());
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        PrintStream out = getOutStream();
        AppdefEntityID id;
        int type;
        int[] orderSpec;
        ScheduleValue schedValue;

        // Handle 'control schedule delete <id>'
        if(args.length == 2 && args[0].equals(PARAM_DELETE)) {
            doScheduleDelete(args[1]);
            return;
        }

        if((args.length != 4 && args.length != 5) ||
           !ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, args[0]))
           throw new ShellCommandUsageException(this.getSyntaxEx());

        type = ClientShell_resource.paramToEntityType(args[0]);

        // If provided, the 4th argument specifies the control job
        // ordering. Order specification String would contain something
        // like: 10001,10002,10003
        if (args.length == 5) {
            orderSpec = ClientShell.commaSepStrToIntArr(args[4]);
        } else {
            orderSpec = null;
        }

        schedValue = schedUtil.parseOptions(args, 3, 
                                            new SimpleDateFormat(), 
                                            "control action",
                                            this);
        try {
            id  = this.entityFetcher.getID(type, args[1]);
            this.entityFetcher.doAction(id,args[2], schedValue, orderSpec);
            
        } catch (Exception e) {
            throw new ShellCommandExecException("Unable to schedule " +
                                                "control action", e);
        }
        out.println("Scheduling '" + args[2] + "' on " + id);
        out.println("Action will recur: " + schedValue.getScheduleString());
    }

    private void doScheduleDelete(String strId)
    {
        Integer id;
        try {
            id = Integer.valueOf(strId);
        } catch (NumberFormatException e) {
            this.getErrStream().println("Invalid id: " + strId);
            return;
        }

        try {
            this.entityFetcher.deleteControlJob(new Integer[] {id});
        } catch (Exception e) {
            this.getErrStream().println("Unable to delete scheduled job");
            return;
        }
        this.getOutStream().println("Scheduled job deleted");
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Manage scheduled control action on a resource";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    Command syntax:\n" +
            "      " + this.getCommandName() + " " + PARAM_DELETE + " " +
            "<id>\n\n" +
            "    The <id> argument is a vaild schedule id.  View id's " +
            "using the\n " +
            "    'control list' command.\n\n" +
            "      " + this.getCommandName() + " <" + 
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <name> <cmd>\n" + 
            "       " + cmdSpace + schedUtil.getScheduleOptions() + 
            " [order]\n\n" +
            "    The <cmd> argument is a valid command which the resource " +
            "understands.\n" +
            "    Use the `control list` command to get a list of " +
            "valid commands.\n" +
            "    The optional \"order specification\" argument will cause "+
            "the control jobs\n" +
            "    to execute synchronously in the order specified. Use a "+
            "comma separated list\n" +
            "    of compatible resource ids (i.e. 10003,10002,10001).\n";
    }
}
