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

import java.io.PrintStream;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_alertdef_list extends ShellCommandBase {
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_GROUP,
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    
    private static final String PARAM_ACTIVE  = "-active";
    private static final String PARAM_INACTIVE = "-inactive";

    private ClientShell              shell;
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_alertdef_list(ClientShell shell){
        this.shell         = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        PrintStream out = getOutStream();
        List data = null;

        Boolean active = null;

        // Need at least -type <id> arguments
        if (args.length < 2) {
            throw new ShellCommandUsageException(this.getSyntax());
        }

        for (int i = 0; i < args.length; i++) {
            if (ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE,
                    args[i])) {
                String resourceType = args[i];
                if(ClientShell_resource.convertParamToInt(resourceType) ==
                   ClientShell_resource.PARAM_GROUP) {
                    throw new ShellCommandExecException("Not yet implemented");
                } 

                int appdefType =
                    ClientShell_resource.paramToEntityType(resourceType);
                data = this.findResourceAlertDefinitions(appdefType, args[++i]);
            }
            else if (args[i].equals(PARAM_ACTIVE) ||
                     args[i].equals(PARAM_INACTIVE)) {
                active = new Boolean(args[i].equals(PARAM_ACTIVE));
            }
            else {
                throw new ShellCommandUsageException(this.getUsageHelp(null));
            }
        }
        
        if (data == null) {
            // Just return all alerts
            data = findAllAlertDefinitions();
        }

        // Check for 0
        if (data.size() == 0) {
            out.println("0 alert definition found");
            return;
        }
        
        PrintfFormat pFmt = new PrintfFormat("%-6s %-15s %-7s %s");
        out.println(pFmt.sprintf(new String[] { "ID", "Name", "Active",
                                                "Description"}));
        out.println(pFmt.sprintf(new String[] { "--", "----", "-------",
                                                "-----------"}));

        PrintfFormat oFmt = new PrintfFormat("%-6d %-15s %-7s %s\n" +
                                             "       -> Conditions: %s\n" +
                                             "       -> Actions: %s\n");
        Object[] fArgs = new Object[6];
        AlertDefinitionValue vals[] = 
           (AlertDefinitionValue[]) data.toArray(new AlertDefinitionValue[0]);

        for(int i=0; i<vals.length; i++){
            AlertDefinitionValue val = vals[i];
            
            if (active != null && active.booleanValue() != val.getActive())
                continue;

            fArgs[0] = val.getId();
            fArgs[1] = val.getName();
            
        	// HHQ-1396: When we expose the difference between 
        	// active and enabled states in the UI, need to 
        	// get enabled correctly.
        	// Uncomment this code at this point and remove 
        	// the other fArgs[2] initialization.
            // fArgs[2] = new Boolean(val.getActive());
            fArgs[2] = new Boolean(val.getEnabled());
            
            fArgs[3] = val.getDescription();
            try {
                StringBuffer buf = new StringBuffer();
                AlertConditionValue[] acs = val.getConditions();

                for(int j=0; j<acs.length; j++){
                    buf.append(acs[j]);
                    buf.append(", ");
                }
                fArgs[4] = buf.toString();
            } catch(Exception exc){
                fArgs[4] = "<Error getting conditions: " + exc + ">";
            }

            try {
                StringBuffer buf = new StringBuffer();
                ActionValue[] loc = val.getActions();

                for(int j=0; j<loc.length; j++){
                    buf.append(loc[j]);
                    buf.append(", ");
                }
                fArgs[5] = buf.toString();
            } catch(Exception exc){
                fArgs[5] = "<Error getting actions: " + exc + ">";
            }

            out.println(oFmt.sprintf(fArgs));
        }
    }

    private List findAllAlertDefinitions() throws ShellCommandExecException {
        try {
            return this.entityFetcher.findAllAlertDefinitions();
        } catch(Exception exc){
            throw new ShellCommandExecException("Error getting alerts", exc);
        }
    }
    
    private List findResourceAlertDefinitions(int appdefType, String resourceId)
        throws ShellCommandExecException 
    {
        try {
            AppdefEntityID id =
                this.entityFetcher.getID(appdefType, resourceId);

            return this.entityFetcher.findResourceAlertDefinitions(id);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }
    
    public String getSyntaxArgs(){
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            " > <resource> [-active | -inactive]";
    }

    public String getUsageShort(){
        return "List alert definitions";
    }

    public String getUsageHelp(String[] args) {
        return "\n    " + this.getUsageShort() + ".\n\n" +
               "    -active    Show active alert definitions only\n" +
               "    -inactive   Show inactive alert definitions only\n";
    }
}
