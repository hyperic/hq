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

import org.hyperic.util.shell.MultiwordShellCommand;
import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandInitException;

public class ClientShell_alertdef 
    extends MultiwordShellCommand 
{
    private ClientShell _shell;

    public ClientShell_alertdef(ClientShell shell){
        _shell = shell;
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException 
    {
        super.init(commandName, shell);

        registerSubHandler("add", new ClientShell_alertdef_add(_shell));
        registerSubHandler("create", new ClientShell_alertdef_create(_shell));
        registerSubHandler("delete", new ClientShell_alertdef_delete(_shell));
        registerSubHandler("disable", new ClientShell_alertdef_disable(_shell));
        registerSubHandler("enable", new ClientShell_alertdef_enable(_shell));
        registerSubHandler("list", new ClientShell_alertdef_list(_shell));
        registerSubHandler("flush", new ClientShell_alertdef_flush(_shell));
    }

    public String getUsageShort(){
        return "Add, delete, and list alert definitions";
    }
}
