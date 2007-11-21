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

import java.util.Properties;

import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_alertdef_add extends ShellCommandBase {
    private final static String PARAM_ACTION         = "-action";

    private final static String EMAIL_ACTION         = "Email";
    private final static String EMAIL_ACTION_CLASS   =
        new EmailActionConfig().getImplementor();
    
    private final static String SYSLOG_ACTION        = "Syslog";
    private final static String SYSLOG_ACTION_CLASS  =
        new SyslogActionConfig().getImplementor();
    
    private static Properties types;

    static {
        types = new Properties();
        types.setProperty(EMAIL_ACTION,   EMAIL_ACTION_CLASS);
        types.setProperty(SYSLOG_ACTION,  SYSLOG_ACTION_CLASS);
    }

    private ClientShell              shell;
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_alertdef_add(ClientShell shell){
        this.shell         = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException
    {
        processCommand(args, null);
    }
    
    public void processCommand(String[] args, ConfigResponse actionResp) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        String      aClass;
        Integer     alertdefID;

        if (args.length < 3) {
            throw new ShellCommandUsageException(this.getSyntax());
        }
        
        if(args[0].equals(PARAM_ACTION)){
            if ((aClass = ClientShell_alertdef_add.types.getProperty(args[1]))
                == null) {
                throw new ShellCommandUsageException(
                    "No valid action name specified");
           }
        } else {
            throw new ShellCommandUsageException(this.getSyntax());
        }

        try {
            alertdefID = Integer.valueOf(args[2]);
        } catch(NumberFormatException exc){
            throw new ShellCommandUsageException("IDs must be integers");
        }

        try {
            if(actionResp == null) {
                ConfigSchema schema =
                    this.entityFetcher.getActionConfigSchema(aClass);
                shell.getOutStream().println("[ Configure " + args[0] + " ]");
    
                actionResp = this.shell.processConfigSchema(schema);
                shell.getOutStream().println();
                
                if (args[1].equals(EMAIL_ACTION)) {
                    while (! this.entityFetcher.ensureNamesAreIds(actionResp) ) {
                        actionResp = this.shell.processConfigSchema(schema);
                        shell.getOutStream().println();
                    }
                }
            }
            
            ActionValue aval =
                this.entityFetcher.createAction(alertdefID, aClass, actionResp);
            
        } catch(Exception exc){
            throw new ShellCommandExecException("Error creating action for Alert ID: " +
                                                alertdefID, 
                                                exc);
        }
    }
    
    public String getSyntaxArgs(){
        return PARAM_ACTION + " <actionType> <alertdefID>";
    }

    public String getUsageShort(){
        return "Add an action to an alert definition";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".\n" +
            "    actionType must be one of the following:\n" +
            "      " + types.keySet() + "\n" +
            "    alertDefID is an ID as returned 'alertdef list'";
    }
}
