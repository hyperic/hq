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

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.product.ProductPlugin;

import org.hyperic.util.shell.MultiwordShellCommand;
import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandHandler;
import org.hyperic.util.shell.ShellCommandInitException;

public class ClientShell_resource 
    extends MultiwordShellCommand 
{
    private ClientShell              shell;

    public ClientShell_resource(ClientShell shell){
        this.shell = shell;
    }

    public void init(String commandName, ShellBase shell)
        throws ShellCommandInitException 
    {
        ShellCommandHandler handler;

        super.init(commandName, shell);

        handler = new ClientShell_resource_list(this.shell);
        registerSubHandler("list", handler);
        handler = new ClientShell_resource_import(this.shell);
        registerSubHandler("import", handler);
        handler = new ClientShell_resource_view(this.shell);
        registerSubHandler("view", handler);
        handler = new ClientShell_resource_configure(this.shell);
        registerSubHandler("configure", handler);
        handler = new ClientShell_resource_set(this.shell);
        registerSubHandler("set", handler);
        handler = new ClientShell_resource_export(this.shell);
        registerSubHandler("export", handler);
        handler = new ClientShell_resource_delete(this.shell);
        registerSubHandler("delete", handler);
    }

    public String getUsageShort(){
        return "View and update " + ClientShell.PRODUCT + " resources";
    }

    // Common parameters
    public static final int PARAM_APP        = 0;
    public static final int PARAM_GROUP      = 1;
    public static final int PARAM_PLATFORM   = 2;
    public static final int PARAM_SERVER     = 3;
    public static final int PARAM_SERVICE    = 4;
    public static final int PARAM_AIPLATFORM = 5;
    public static final int PARAM_AISERVER   = 6;
    public static final int PARAM_AIIP       = 7;
    
    private static final String PARAMSTR_APP        = "-app";
    private static final String PARAMSTR_GROUP      = "-group";
    private static final String PARAMSTR_PLATFORM   = "-platform";
    private static final String PARAMSTR_SERVER     = "-server";
    private static final String PARAMSTR_SERVICE    = "-service";
    private static final String PARAMSTR_AIPLATFORM = "-aiplatform";
    private static final String PARAMSTR_AISERVER   = "-aiserver";
    private static final String PARAMSTR_AIIP       = "-aiip";

    /**
     * Convert a parameter (i.e. one of PARAM_*) to one of 
     * AppdefEntityConstants.APPDEF_TYPE_*.  Parameters which
     * have no match will throw an IllegalArgumentException
     */
    public static int paramToEntityType(String param){
        if(param.equals(PARAMSTR_APP))
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        else if(param.equals(PARAMSTR_PLATFORM))
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        else if(param.equals(PARAMSTR_SERVER))
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        else if(param.equals(PARAMSTR_SERVICE))
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        else if(param.equals(PARAMSTR_AIPLATFORM))
            return AppdefEntityConstants.APPDEF_TYPE_AIPLATFORM;
        else if(param.equals(PARAMSTR_AISERVER))
            return AppdefEntityConstants.APPDEF_TYPE_AISERVER;
        else if(param.equals(PARAMSTR_AIIP))
            return AppdefEntityConstants.APPDEF_TYPE_AIIP;
        else if(param.equals(PARAMSTR_GROUP))
            return AppdefEntityConstants.APPDEF_TYPE_GROUP;
        else
            throw new IllegalArgumentException("Unknown parameter type");
    }

    public static String convertParamToString(int param){
        switch(param){
        case PARAM_APP:
            return PARAMSTR_APP;
        case PARAM_GROUP:
            return PARAMSTR_GROUP;
        case PARAM_PLATFORM:
            return PARAMSTR_PLATFORM;
        case PARAM_SERVER:
            return PARAMSTR_SERVER;
        case PARAM_SERVICE:
            return PARAMSTR_SERVICE;
        case PARAM_AIPLATFORM:
            return PARAMSTR_AIPLATFORM;
        case PARAM_AISERVER:
            return PARAMSTR_AISERVER;
        case PARAM_AIIP:
            return PARAMSTR_AIIP;
        default:
            throw new IllegalArgumentException("Unknown parameter type: " +
                                               param);
        }
    }

    public static int convertParamToInt(String param){
        if(param.equals(PARAMSTR_APP))
            return PARAM_APP;
        else if(param.equals(PARAMSTR_GROUP))
            return PARAM_GROUP;
        else if(param.equals(PARAMSTR_PLATFORM))
            return PARAM_PLATFORM;
        else if(param.equals(PARAMSTR_SERVER))
            return PARAM_SERVER;
        else if(param.equals(PARAMSTR_SERVICE))
            return PARAM_SERVICE;
        else if(param.equals(PARAMSTR_AIPLATFORM))
            return PARAM_AIPLATFORM;
        else if(param.equals(PARAMSTR_AISERVER))
            return PARAM_AISERVER;
        else if(param.equals(PARAMSTR_AIIP))
            return PARAM_AIIP;
        else
            throw new IllegalArgumentException("Unknown parameter type: " +
                                               param);
    }

    /**
     * Convert an array of PARAM_* arguments to a string argument line.
     */
    public static String generateArgList(int[] params){
        StringBuffer res = new StringBuffer();

        for(int i=0; i<params.length; i++){
            res.append(convertParamToString(params[i]));
            if(i != params.length - 1)
                res.append(" | ");
        }
        return res.toString();
    }

    public static boolean paramIsValid(int[] params, String param){
        int pType;

        try {
            pType = convertParamToInt(param);
        } catch(IllegalArgumentException exc){
            return false;
        }

        for(int i=0; i<params.length; i++)
            if(params[i] == pType)
                return true;
        return false;
    }

    public static String sanitizePluginTypeName(String pluginType){
        if(pluginType.equals(ProductPlugin.TYPE_PRODUCT))
            return "resource";
        else if(pluginType.equals(ProductPlugin.TYPE_MEASUREMENT))
            return "metric";
        else if(pluginType.equals(ProductPlugin.TYPE_CONTROL))
            return "control";
        else if(pluginType.equals(ProductPlugin.TYPE_RESPONSE_TIME))
            return "response time";

        throw new IllegalArgumentException("Unhandled plugin type: " + 
                                           pluginType);
    }
}
