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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

import javax.ejb.FinderException;
import java.io.EOFException;
import java.io.PrintStream;

public class ClientShell_resource_configure 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };

    private ClientShell              shell;
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_resource_configure(ClientShell shell){
        this.shell         = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    public String getPluginType(){
        return ProductPlugin.TYPE_PRODUCT;
    }
    
    public String getPluginTypeName(){
        return ClientShell_resource.sanitizePluginTypeName(this.getPluginType());
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        PrintStream out = this.getOutStream();
        ConfigResponse response, oldOpts;
        String pluginType, pluginTypeName;
        ConfigSchema config;
        AppdefEntityID id;
        int appdefType;

        if(args.length != 2 ||
           !ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, args[0]))
            throw new ShellCommandUsageException(this.getSyntax());

        pluginType     = this.getPluginType();
        pluginTypeName = this.getPluginTypeName();

        appdefType = ClientShell_resource.paramToEntityType(args[0]);

        try {
            id = this.entityFetcher.getID(appdefType, args[1]);
        } catch(Exception exc){
            throw new ShellCommandExecException("Unable to get " + args[0] + 
                                                " " + args[1], exc);
        }

        out.println("[ Configuring " + pluginTypeName + ": " + id + " ]");

        try {
            config = this.entityFetcher.getConfigSchema(id, pluginType);
        } catch(FinderException exc){
            out.println(id + " does not support " + pluginTypeName + 
                        " configuration");
            return;
        } catch(PluginNotFoundException exc){
            out.println(id + " does not support " + pluginTypeName + 
                        " configuration");
            return;
        } catch(ConfigFetchException exc){
            out.println(exc.getMessage());
            return;
        } catch(Exception exc) {
            throw new ShellCommandExecException(exc);
        }

        try {
            oldOpts = this.entityFetcher.getMergedConfigResponse(pluginType,id,
                                                                 false);
            response = this.shell.processConfigSchema(config, oldOpts);
            
            this.entityFetcher.setConfigResponse(id, response, pluginType);
        } catch(InvalidConfigException exc){
            out.println("Unable to configure " + id + ": " + exc.getMessage());
            return;
        } catch(EOFException exc){
            out.println("\nConfiguration aborted");
            return;
        } catch(Exception exc){
            throw new ShellCommandExecException("Unable to configure " +
                                                pluginTypeName, exc);
        }
        out.println(id + " configured");
    }

    public String getSyntaxArgs(){
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <name>";
    }

    public String getUsageShort(){
        return "Setup " + this.getPluginTypeName() + " configuration";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".  If the resource requires" +
            "\n    configuration, the shell will prompt for the values.";
    }
}
