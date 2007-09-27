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

import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.shell.ShellCommand_set;

public class ClientShell_set extends ShellCommand_set {

    private ClientShell shell;

    public ClientShell_set(ClientShell shell) {
        super();
        this.shell = shell;
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        if (args.length < 1 || args.length > 2) {
            throw new ShellCommandUsageException(this.getSyntax());
        }

        if(args.length == 1){
            // Allow for foo=bar syntax
            int idx = args[0].indexOf('=');
            if (idx != -1) {
                // Allow set foo=bar syntax
                String key = args[0].substring(0, idx);
                String value = args[0].substring(idx + 1);
                String[] newArgs = new String[] { key, value };
                processCommandInternal(newArgs);
            }
        }

        processCommandInternal(args);
    }

    private void processCommandInternal(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        super.processCommand(args);
        
        //base class has already validated args
        if (args.length == 1) {
            this.shell.removeProperty(args[0]);
        } else {
            this.shell.setProperty(args[0], args[1]);
        }
    }

    public String getUsageShort(){
        return "Set shell properties";
    }
}
        
