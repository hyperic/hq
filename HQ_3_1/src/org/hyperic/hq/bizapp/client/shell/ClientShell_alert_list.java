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

import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_alert_list 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
        ClientShell_resource.PARAM_GROUP,
    };
    
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_alert_list(ClientShell shell){
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        List data;
        
        if (args.length == 0) {
            throw new ShellCommandUsageException(getSyntax());
        }
        else if(ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE,
                                                  args[0])){
            String resourceType, resourceId;
            int appdefType;

            resourceType = args[0];
            resourceId   = args[1];
            if (ClientShell_resource.convertParamToInt(resourceType) ==
                ClientShell_resource.PARAM_GROUP) {
                throw new ShellCommandExecException("Not yet implemented");
            } 

            appdefType = ClientShell_resource.paramToEntityType(resourceType);
            data = findResourceAlerts(appdefType, resourceId);
        } else {
            throw new ShellCommandUsageException(getUsageHelp(null));
        }
        
        this.getOutStream().println(data.size() + " alerts found");

        ValuePrinter printer =
            new ValuePrinter(this.getOutStream(), 
                             new int[] {
                                 ValueWrapper.ATTR_ID,
                                 ValueWrapper.ATTR_CTIME,
                                 ValueWrapper.ATTR_ACTUAL},
                             new int[] {
                                 ValuePrinter.ATTRTYPE_STRING,
                                 ValuePrinter.ATTRTYPE_LONGDATE,
                                 ValuePrinter.ATTRTYPE_STRING});
 
        printer.setHeaders(new String[] { "ID", "Alert Time", "Actual Values"});
        printer.printList(data);
    }

    private List findResourceAlerts(int appdefType, String resourceId)
        throws ShellCommandExecException 
    {
        try {
            AppdefEntityID id =
                this.entityFetcher.getID(appdefType, resourceId);

            return this.entityFetcher.findResourceAlerts(id);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }
    
    public String getSyntaxArgs(){
        return ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            " <resource>";
    }

    public String getUsageShort(){
        return "List alerts and their actual values";
    }

    public String getUsageHelp(String[] args) {
        return "    " + getUsageShort() + ".";
    }
}
