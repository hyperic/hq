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

import org.hyperic.util.shell.*;

public class ClientShell_scheduler_list extends MultiwordShellCommand {
    private ClientShell_scheduler owner;
    private ClientShellAuthenticator auth;

    public ClientShell_scheduler_list(ClientShell_scheduler owner,
                                   ClientShellAuthenticator eamshellauthenticator)
    {
        this.owner = owner;
        auth = eamshellauthenticator;
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException
    {
        ShellCommandHandler handler;

        super.init(commandName, shell);

        handler = new ClientShell_scheduler_list_jobs(this, this.auth);
        registerSubHandler("jobs", handler);

        handler = new ClientShell_scheduler_list_schedules(this, this.auth);
        registerSubHandler("schedules", handler);
    }

    public String getUsageHelp(String as[]) {
        return "scheduler list < jobs | schedules >";
    }

    public ClientShell_scheduler getOwner() {
        return owner;
    }
}

// EOF
