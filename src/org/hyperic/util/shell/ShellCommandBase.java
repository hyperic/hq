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

package org.hyperic.util.shell;

import java.io.PrintStream;
import java.util.ResourceBundle;
import java.util.Locale;

public class ShellCommandBase implements ShellCommandHandler {

    private String _commandName = null;
    private ShellBase _shell = null;

    public String getCommandName() {
        return _commandName;
    }

    public ShellBase getShell() {
        return _shell;
    }

    public PrintStream getOutStream () { 
        return this.getShell().getOutStream(); 
    }

    public PrintStream getErrStream () { 
        return this.getShell().getErrStream(); 
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException
    {
        _commandName = commandName;
        _shell = shell;
    }

    public void processCommand ( String[] args ) 
        throws ShellCommandUsageException, ShellCommandExecException {

        getOutStream().println("ShellCommandBase: not implemented: " +
                               _commandName);
    }

    public String getSyntax() {
        return "Syntax: " + this.getCommandName() + " " + this.getSyntaxArgs();
    }

    public String getSyntaxArgs() {
        return "";
    }

    public String getUsageShort () {
        return "";
    }

    public String getUsageHelp ( String[] args ) {
        return "Help not available for command " + _commandName;
    }

    /**
     * Load and initialize the ResourceBundle according to our default
     * locale.
     * @param string containing the package qualified resourcebundle name.
     * @return ResourceBundle
     * */
    public ResourceBundle getMessages (String string) {
        return getMessages(string, null);
    }

    /**
     * Load and initialize the ResourceBundle according to the specified
     * locale.
     * @param string containing the package qualified resourcebundle name.
     * @param loc specific version of the resource bundle.
     * @return ResourceBundle
     * */
    public ResourceBundle getMessages (String string, Locale loc) {
        ResourceBundle messages = null;
        try {
            if (loc != null)
                messages = ResourceBundle.getBundle(string, loc);
            else
            	messages = ResourceBundle.getBundle(string);
        } catch (Exception e){
            e.printStackTrace();
        }
        return messages;
    }
}
