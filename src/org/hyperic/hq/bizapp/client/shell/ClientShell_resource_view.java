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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.resourceTree.PlatformNode;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.shared.resourceTree.ServerNode;
import org.hyperic.hq.appdef.shared.resourceTree.ServiceNode;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.pager.StaticPageFetcher;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_resource_view 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_APP,
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
        ClientShell_resource.PARAM_GROUP
    };

    private static final int[] PARAM_VALID_PROP_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE
    };

    private static final String PARAM_TREE   = "-tree";
    private static final String PARAM_REPL   = "-replacements";
            static final String PARAM_CPROPS = "-properties";

    private static final String TREE_PLAT_FMT     = "%-40s %-10s %s";
    private static final String TREE_SERVER_FMT   = " \\_%-37s %-10s %s";
    private static final String TREE_SERVICE_FMT  = " |  \\_%-34s %-10s %s";
    private static final String TREE_SERVICEL_FMT = "    \\_%-34s %-10s %s";
    private static final int[]  TREE_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME, ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_TYPENAME };
    private static final String FORMAT_DOTTED_LINE =
        "------------------------------------------------------------";

    private static final String APP_FORMAT =
        "Application:         %s (id=%s)\n" +
        "Description:         %s\n" +
        "Engineering Contact: %s\n" +
        "Business Contact:    %s\n" +
        "Operations Contact:  %s\n" +
        "Owner:               %s\n" +
        "Location:            %s\n" +
        "Last Modified:       %s (by %s)\n" +
        "Created:             %s";
    private static final int[] APP_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_LONGDATE, ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_LONGDATE };
    private static final int[] APP_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME, ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_DESCRIPTION, 
        ValueWrapper.ATTR_ENGCONTACT, 
        ValueWrapper.ATTR_BUSINESSCONTACT, 
        ValueWrapper.ATTR_OPSCONTACT, 
        ValueWrapper.ATTR_OWNER,
        ValueWrapper.ATTR_LOCATION,
        ValueWrapper.ATTR_MTIME, ValueWrapper.ATTR_MODIFIEDBY,
        ValueWrapper.ATTR_CTIME };

    private static final String APP_FORMAT_SHORT =
        "    Application: Id:%s     Name: %s\n";
    private static final int[] APP_ATTRTYPES_SHORT = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING };
    private static final int[] APP_ATTRS_SHORT = new int[] {
        ValueWrapper.ATTR_ID, ValueWrapper.ATTR_NAME };


    private static final String PLATFORM_FORMAT =
        "Platform:       %s (id=%s)\n" +
        "Platform Type:  %s\n" +
        "Description:    %s\n" +
        "CPU Count:      %s\n" +
        "FQDN:           %s\n" +
        "Comment:        %s\n" +
        "Owner:          %s\n" +
        "Location:       %s\n" +
        "Last Modified:  %s (by %s)\n" +
        "Created:        %s";
    private static final int[] PLATFORM_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_LONGDATE, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_LONGDATE };
    private static final int[] PLATFORM_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,       ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_TYPENAME,   ValueWrapper.ATTR_DESCRIPTION,
        ValueWrapper.ATTR_CPUCOUNT,   ValueWrapper.ATTR_FQDN,
        ValueWrapper.ATTR_COMMENT,    ValueWrapper.ATTR_OWNER,
        ValueWrapper.ATTR_LOCATION,   ValueWrapper.ATTR_MTIME,
        ValueWrapper.ATTR_MODIFIEDBY, ValueWrapper.ATTR_CTIME };
    private static final String SERVER_FORMAT =
        "Server:         %s (id=%s)\n" +
        "Server Type:    %s\n" +
        "Description:    %s\n" +
        "Install Path:   %s\n" +
        "Owner:          %s\n" +
        "Location:       %s\n" +
        "Last Modified:  %s (by %s)\n" +
        "Created:        %s";

    private static final String PLATFORM_FORMAT_SHORT =
        "    Platform: Id:%s     Name: %s\n";
    private static final int[] PLATFORM_ATTRTYPES_SHORT = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING };
    private static final int[] PLATFORM_ATTRS_SHORT = new int[] {
        ValueWrapper.ATTR_ID, ValueWrapper.ATTR_NAME };

    private static final int[] SERVER_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING,
        ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING,
        ValuePrinter.ATTRTYPE_LONGDATE, ValuePrinter.ATTRTYPE_STRING,
        ValuePrinter.ATTRTYPE_LONGDATE };
    private static final int[] SERVER_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,     ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_TYPENAME, ValueWrapper.ATTR_DESCRIPTION,
        ValueWrapper.ATTR_INSTALLPATH,
        ValueWrapper.ATTR_OWNER,    ValueWrapper.ATTR_LOCATION,
        ValueWrapper.ATTR_MTIME,    ValueWrapper.ATTR_MODIFIEDBY,
        ValueWrapper.ATTR_CTIME };

    private static final String SERVER_FORMAT_SHORT =
        "    Server: Id:%s     Name: %s\n";
    private static final int[] SERVER_ATTRTYPES_SHORT = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING };
    private static final int[] SERVER_ATTRS_SHORT = new int[] {
        ValueWrapper.ATTR_ID, ValueWrapper.ATTR_NAME };

    private static final String SERVICE_FORMAT =
        "Service:            %s (id=%s)\n" +
        "Service Type:       %s\n" +
        "Parent Service ID:  %s\n" + 
        "Description:        %s\n" +
        "Owner:              %s\n" +
        "Location:           %s\n" +
        "Last Modified:      %s (by %s)\n" +
        "Created:            %s";
    private static final int[] SERVICE_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING,
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING,   
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_LONGDATE, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_LONGDATE };
    private static final int[] SERVICE_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,        ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_TYPENAME,    ValueWrapper.ATTR_PARENTID,
        ValueWrapper.ATTR_DESCRIPTION, ValueWrapper.ATTR_OWNER,       
        ValueWrapper.ATTR_LOCATION,    ValueWrapper.ATTR_MTIME,       
        ValueWrapper.ATTR_MODIFIEDBY,  ValueWrapper.ATTR_CTIME };

    private static final String SERVICE_FORMAT_SHORT =
        "    Service: Id:%s     Name: %s\n";
    private static final int[] SERVICE_ATTRTYPES_SHORT = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING };
    private static final int[] SERVICE_ATTRS_SHORT = new int[] {
        ValueWrapper.ATTR_ID, ValueWrapper.ATTR_NAME };

    private static final String GROUP_FORMAT =
        "Group:          %s (id=%s)\n" +
        "Group Type:     %s\n" +
        "Description:    %s\n" +
        "Owner:          %s\n" +
        "Last Modified:  %s (by %s)\n" +
        "Created:        %s";
    private static final int[] GROUP_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_LONGDATE,
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_LONGDATE };

    private static final int[] GROUP_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,         ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_TYPENAME,     ValueWrapper.ATTR_DESCRIPTION,
        ValueWrapper.ATTR_OWNER,        ValueWrapper.ATTR_MTIME,
        ValueWrapper.ATTR_MODIFIEDBY,   ValueWrapper.ATTR_CTIME };

    private static final String GROUP_FORMAT_SHORT =
        "    Group:    Id:%s     Name: %s\n";
    private static final int[] GROUP_ATTRTYPES_SHORT = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING };
    private static final int[] GROUP_ATTRS_SHORT = new int[] {
        ValueWrapper.ATTR_ID, ValueWrapper.ATTR_NAME };

    private ClientShell              shell;
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_resource_view(ClientShell shell){
        this.shell = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    private void printView(int type, String tag)
        throws ShellCommandExecException 
    {
        printView(type, tag, false);
    }

    private void printView(int type, String tag, boolean groupContext)
        throws ShellCommandExecException
    {
        ConfigResponseDB config;
        AppdefGroupValue groupVal;
        String format;
        int[] attrTypes, attrs;
        ValuePrinter printer;
        ValueWrapper val;
        
        try {
            ApplicationValue appVal;
            PlatformValue platVal;
            ServerValue servVal;
            ServiceValue serviceVal;
            AppdefEntityID id;

            groupVal = null;

            switch(type){
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                appVal    = this.entityFetcher.getApplicationValue(tag);
                val       = new ValueWrapper(appVal);
                format    = (groupContext)? APP_FORMAT_SHORT:APP_FORMAT;
                attrTypes = (groupContext)? APP_ATTRTYPES_SHORT:
                                            APP_ATTRTYPES;
                attrs     = (groupContext)? APP_ATTRS_SHORT:APP_ATTRS;
                config    = null;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                platVal   = this.entityFetcher.getPlatformValue(tag);
                id        = platVal.getEntityId();
                val       = new ValueWrapper(platVal);
                format    = (groupContext)? PLATFORM_FORMAT_SHORT :
                                            PLATFORM_FORMAT;
                attrTypes = (groupContext)? PLATFORM_ATTRTYPES_SHORT:
                                            PLATFORM_ATTRTYPES;
                attrs     = (groupContext)? PLATFORM_ATTRS_SHORT :
                                            PLATFORM_ATTRS;
                config    = this.entityFetcher.getConfigResponse(id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                servVal   = this.entityFetcher.getServerValue(tag);
                id        = servVal.getEntityId();
                val       = new ValueWrapper(servVal);
                format    = (groupContext)? SERVER_FORMAT_SHORT:SERVER_FORMAT;
                attrTypes = (groupContext)? SERVER_ATTRTYPES_SHORT:
                                            SERVER_ATTRTYPES;
                attrs     = (groupContext)? SERVER_ATTRS_SHORT:SERVER_ATTRS;
                config    = this.entityFetcher.getConfigResponse(id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                serviceVal = this.entityFetcher.getServiceValue(tag);
                id         = serviceVal.getEntityId();
                val        = new ValueWrapper(serviceVal);
                format    = (groupContext)? SERVICE_FORMAT_SHORT :
                                            SERVICE_FORMAT;
                attrTypes = (groupContext)? SERVICE_ATTRTYPES_SHORT:
                                            SERVICE_ATTRTYPES;
                attrs     = (groupContext)? SERVICE_ATTRS_SHORT:SERVICE_ATTRS;
                config     = this.entityFetcher.getConfigResponse(id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                groupVal   = this.entityFetcher.getGroupValue(tag);
                id         = groupVal.getEntityId();
                val        = new ValueWrapper(groupVal);
                format    = (groupContext)? GROUP_FORMAT_SHORT:GROUP_FORMAT;
                attrTypes = (groupContext)? GROUP_ATTRTYPES_SHORT:
                                            GROUP_ATTRTYPES;
                attrs     = (groupContext)? GROUP_ATTRS_SHORT:GROUP_ATTRS;
                config     = null;
                break;
            default:
                throw new IllegalStateException("Unhandled resource");
            }
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
        
        printer = new ValuePrinter(this.getOutStream(), format,
                                   attrs, attrTypes);
        printer.printItem(val);

        if(config != null && !groupContext){
            this.printConfig(config.getProductResponse(),     
                             ProductPlugin.TYPE_PRODUCT);
            this.printConfig(config.getControlResponse(),
                             ProductPlugin.TYPE_CONTROL);
            this.printConfig(config.getMeasurementResponse(), 
                             ProductPlugin.TYPE_MEASUREMENT);
        }

        // If this is a group recurse through the entries.
        if(type == AppdefEntityConstants.APPDEF_TYPE_GROUP){
            Iterator grpIter = groupVal.getAppdefGroupEntries().iterator();

            while(grpIter.hasNext()){
                AppdefEntityID member = (AppdefEntityID) grpIter.next();

                this.getOutStream().println(FORMAT_DOTTED_LINE);
                this.printView(member.getType(),
                               String.valueOf(member.getID()), true);
            }
        }
    }

    private void printConfig(byte[] config, String type){
        PrintfFormat pFmt;
        ConfigResponse response;
        PrintStream out;
        Object[] fArgs;
        String typeName;

        if(config == null || config.length == 0)
            return;

        try {
            response = ConfigResponse.decode(config);
        } catch(Exception exc){
            this.getErrStream().println("Error decoding " + type + 
                                        "configuration: " + exc.getMessage());
            return;
        }

        if(response.getKeys().size() == 0)
            return;

        pFmt  = new PrintfFormat("  %-20s = '%s'");
        fArgs = new Object[2];

        out = this.getOutStream();

        typeName = ClientShell_resource.sanitizePluginTypeName(type);
        out.println("\n[Configuration : " + typeName + "]");
        for(Iterator i=response.getKeys().iterator(); i.hasNext(); ){
            String key = (String)i.next();
            String val = response.getValue(key);

            fArgs[0] = key;
            fArgs[1] = val;
            out.println(pFmt.sprintf(fArgs));
        }
    }

    private void printTree(AppdefEntityID id)
        throws ShellCommandExecException
    {
        ValuePrinter platPrinter, servPrinter, servicePrinter, lservicePrinter;
        ResourceTree tree;
        PrintStream out;

        out = this.getOutStream();
        platPrinter     = new ValuePrinter(out, TREE_PLAT_FMT,     TREE_ATTRS);
        servPrinter     = new ValuePrinter(out, TREE_SERVER_FMT,   TREE_ATTRS);
        servicePrinter  = new ValuePrinter(out, TREE_SERVICE_FMT,  TREE_ATTRS);
        lservicePrinter = new ValuePrinter(out, TREE_SERVICEL_FMT, TREE_ATTRS);
        platPrinter.setHeaders(new String[] {"Resource", "ID", "Type" });

        try {
            AppdefEntityID[] ids = new AppdefEntityID[] { id };

            tree = this.entityFetcher.getResourceTree(ids,
                                     AppdefEntityConstants.RESTREE_TRAVERSE_UP);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
                                                      

        platPrinter.printHeaders();
        for(Iterator p=tree.getPlatformIterator(); p.hasNext(); ){
            PlatformNode pNode = (PlatformNode)p.next();

            platPrinter.printItem(new ValueWrapper(pNode.getPlatform()));

            for(Iterator s=pNode.getServerIterator(); s.hasNext(); ){
                ServerNode sNode = (ServerNode)s.next();

                servPrinter.printItem(new ValueWrapper(sNode.getServer()));

                for(Iterator v=sNode.getServiceIterator(); v.hasNext(); ){
                    ServiceNode vNode = (ServiceNode)v.next();
                    ServiceValue vValue;
                    
                    vValue = vNode.getService();
                    if(s.hasNext())
                        servicePrinter.printItem(new ValueWrapper(vValue));
                    else
                        lservicePrinter.printItem(new ValueWrapper(vValue));
                }
            }
        }
    }

    private void printProperties(Properties props){
        PrintfFormat pFmt;
        ArrayList keys, totList;
        Object[] fArgs;

        totList = new ArrayList();
        keys    = new ArrayList();
        for(Iterator i=props.keySet().iterator(); i.hasNext(); ){
            keys.add((String)i.next());
        }
        
        Collections.sort(keys);
        
        pFmt  = new PrintfFormat("    %-35s = %s");
        fArgs = new Object[2];
        for(Iterator i=keys.iterator(); i.hasNext(); ){
            String key = (String)i.next();
            String val;

            val = props.getProperty(key);
            fArgs[0] = key;
            fArgs[1] = val == null ? "" : val;
            totList.add(pFmt.sprintf(fArgs));
        }

        try {
            this.shell.performPaging(new StaticPageFetcher(totList));
        } catch(PageFetchException exc){
            this.getErrStream().println("Error paging: " + exc.getMessage());
        }
    }

    private void printReplacements(AppdefEntityID id)
        throws ShellCommandExecException
    {
        PrintStream out = this.getOutStream();
        Properties props;

        try {
            props = this.entityFetcher.getResourceProperties(id);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        out.println("Replacement properties available for " + id + ":");
        this.printProperties(props);
    }

    private void printProps(AppdefEntityID id)
        throws ShellCommandExecException
    {
        PrintStream out = this.getOutStream();
        Properties props;

        try {
            props = this.entityFetcher.getCPropEntries(id);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        out.println("Custom properties for " + id + ":");
        this.printProperties(props);
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        String type, tag;
        boolean isReplacementsView, isPropsView;
        int aType;

        isReplacementsView = (args.length == 3 && args[2].equals(PARAM_REPL));
        isPropsView = (args.length == 3 && args[2].equals(PARAM_CPROPS));
        if(!(args.length == 2 || 
             (args.length == 3 && args[2].equals(PARAM_TREE)) ||
             isReplacementsView ||
             isPropsView))
        {
            throw new ShellCommandUsageException(this.getSyntaxEx());
        }

        type = args[0];
        tag  = args[1];

        if(isReplacementsView){
            if(!ClientShell_resource.paramIsValid(PARAM_VALID_PROP_RESOURCE,type))
                throw new ShellCommandUsageException(this.getSyntaxEx());
        } else {
            if(!ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, type))
                throw new ShellCommandUsageException(this.getSyntaxEx());
        }

        aType = ClientShell_resource.paramToEntityType(type);
        if(args.length == 2){
            this.printView(aType, tag);
        } else {
            AppdefEntityID id;

            try {
                id = this.entityFetcher.getID(aType, tag);
            } catch(Exception exc){
                throw new ShellCommandExecException(exc);
            }

            if(isReplacementsView){
                this.printReplacements(id);
            } else if(isPropsView){
                this.printProps(id);
            } else {
                this.printTree(id);
            }
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "View a specific " + ClientShell.PRODUCT + " resource";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    To view basic entity information:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource>\n\n" +
            "    To view the entity's relationship to other entities within "+
            "HQ:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource>\n" +
            "      " + cmdSpace + " " + PARAM_TREE + "\n\n" +
            "    To view the properties which can be used for replacement "+
            "(such as\n" +
            "    for transfer operations):\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_PROP_RESOURCE) + 
            "> <resource> " + PARAM_REPL + "\n\n" +
            "    To view the custom properties set for a resource:\n"+
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) + 
            "> <resource>\n" + 
            "      " + cmdSpace + " " + PARAM_CPROPS + "\n\n" +
            "    The <resource> can be specified either by ID or by its " +
            "name.";
    }
}
