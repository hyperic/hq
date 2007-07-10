package org.hyperic.hq.bizapp.client.shell;

import java.io.PrintStream;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.client.shell.ClientShell_resource;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_control_perform 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
        ClientShell_resource.PARAM_GROUP
    };

    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_control_perform(ClientShell shell){
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
        int[] orderSpec;

        if(args.length != 3 && args.length !=4)
            throw new ShellCommandUsageException(this.getSyntaxEx());

        type = ClientShell_resource.paramToEntityType(args[0]);

        // If provided, the 4th argument specifies the control job
        // ordering. Order specification String would contain something
        // like: 10001,10002,10003
        if (args.length == 4) {
            orderSpec = ClientShell.commaSepStrToIntArr(args[3]);
        } else {
            orderSpec = null;
        }

        try {
            id = this.entityFetcher.getID(type, args[1]);

            this.entityFetcher.doAction(id, args[2], orderSpec);
            out.println("Executing '" + args[2] + "' on " + id);
        } catch (PluginException e) {
            err.println("Unable to perform control action: " +
                        e.getMessage());
        } catch (GroupNotCompatibleException e) {
            err.println("Unable to perform control action on non-" +
                        "compatible group");
        } catch (Exception e) {
            throw new ShellCommandExecException("Unable to perform " +
                                                "control action", e);
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "Perform a control action on a resource";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    Command syntax:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <name> <cmd>\n" +
            "       " + cmdSpace + "[order]\n\n" + 
            "    The <cmd> argument is a valid command which the resource " +
            "has returned.\n" +
            "    Use the `control list` command to get a list of valid " +
            "commands.\n\n" +
            "    The optional \"order\" argument will cause "+
            "the control jobs to execute\n" +
            "    synchronously in the order specified.  Use a comma " +
            "separated list of \n" +
            "    compatible resource ids (i.e. 10003,10002,10001) to " +
            "specify the order.\n";
    }
}
