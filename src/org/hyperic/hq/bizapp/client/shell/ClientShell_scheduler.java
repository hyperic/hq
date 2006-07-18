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

import java.util.Hashtable;

import org.hyperic.util.shell.MultiwordShellCommand;
import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandHandler;
import org.hyperic.util.shell.ShellCommandInitException;

import org.hyperic.hq.bizapp.shared.SchedulerBoss;
import org.hyperic.hq.bizapp.shared.SchedulerBossUtil;

public class ClientShell_scheduler extends MultiwordShellCommand {
    private SchedulerBoss schedulerBoss = null;
    private ClientShell shell = null;
    private ClientShellAuthenticator auth = null;

    public ClientShell_scheduler(ClientShell shell) {
        this.shell = shell;
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException
    {
        ShellCommandHandler handler;

        super.init(commandName, shell);

        this.auth = this.shell.getAuthenticator();

        handler = new ClientShell_scheduler_list(this, this.auth);
        registerSubHandler("list", handler);

        handler = new ClientShell_scheduler_delete(this, this.auth);
        registerSubHandler("delete", handler);
    }

    public String getUsageShort() {
        return "Manage scheduler jobs and schedules";
    }

    public String getUsageHelp(String args[]) {
        String res;

        if ( (res = super.getUsageHelp(args) ) != null) {
            return res;
        }

        return "scheduler < list | delete > < jobs | schedules >\n" +
            "    jobs: list / delete jobs\n" +
            "    schedules: list / delete schedules\n";
    }

    public SchedulerBoss getSchedulerBoss() throws ShellCommandExecException {
        if (schedulerBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                schedulerBoss = SchedulerBossUtil.getHome(env).create();
            } catch(Exception e) {
                throw new ShellCommandExecException("Could not get SchedulerBoss",
                                                    e);
            }
        }
        return schedulerBoss;
    }
}

// EOF
