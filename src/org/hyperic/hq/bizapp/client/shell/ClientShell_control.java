package org.hyperic.hq.bizapp.client.shell;

import org.hyperic.util.shell.MultiwordShellCommand;
import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandHandler;
import org.hyperic.util.shell.ShellCommandInitException;

public class ClientShell_control 
    extends MultiwordShellCommand 
{
    private ClientShell shell;

    public ClientShell_control(ClientShell shell){
        this.shell = shell;
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException
    {
        ShellCommandHandler handler;

        super.init(commandName, shell);

        handler = new ClientShell_control_configure(this.shell);
        registerSubHandler("configure", handler);

        handler = new ClientShell_control_perform(this.shell);
        registerSubHandler("perform", handler);

        handler = new ClientShell_control_list(this.shell);
        registerSubHandler("list", handler);

        handler = new ClientShell_control_schedule(this.shell);
        registerSubHandler("schedule", handler);
    }

    public String getUsageShort(){
        return "Control resources in HQ";
    }
}
