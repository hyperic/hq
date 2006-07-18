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

import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_resourcetype_view 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };

    private static final String PLATFORMTYPE_FORMAT =
        "Platform Type:     %s (id=%s)\n" +
        "Plugin:            %s\n" +
        "Last Modified:     %s\n" +
        "Created:           %s";
    private static final int[] PLATFORMTYPE_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_LONGDATE, ValuePrinter.ATTRTYPE_LONGDATE };
    private static final int[] PLATFORMTYPE_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,  ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_PLUGIN,
        ValueWrapper.ATTR_MTIME, ValueWrapper.ATTR_CTIME };
    private static final String SERVERTYPE_FORMAT =
        "Server Type:    %s (id=%s)\n" +
        "Description:    %s\n" +
        "Plugin:         %s\n" +
        "Last Modified:  %s\n" +
        "Created:        %s";
    private static final int[] SERVERTYPE_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING,
        ValuePrinter.ATTRTYPE_LONGDATE, ValuePrinter.ATTRTYPE_LONGDATE };
    private static final int[] SERVERTYPE_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,        ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_DESCRIPTION, ValueWrapper.ATTR_PLUGIN,   
        ValueWrapper.ATTR_MTIME,       ValueWrapper.ATTR_CTIME };
    private static final String SERVICETYPE_FORMAT =
        "Service Type:   %s (id=%s)\n" +
        "Description:    %s\n" +
        "Plugin:         %s\n" +
        "Last Modified:  %s\n" +
        "Created:        %s";
    private static final int[] SERVICETYPE_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_STRING,   ValuePrinter.ATTRTYPE_STRING, 
        ValuePrinter.ATTRTYPE_LONGDATE, ValuePrinter.ATTRTYPE_LONGDATE };
    private static final int[] SERVICETYPE_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,        ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_DESCRIPTION, ValueWrapper.ATTR_PLUGIN, 
        ValueWrapper.ATTR_MTIME,       ValueWrapper.ATTR_CTIME };
    private static final String TEMPLATES_FORMAT = 
        "    %-7s %-25s %s";
    private static final int[] TEMPLATES_ATTRS = new int[] {
        ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_TEMPLATEALIAS,
        ValueWrapper.ATTR_NAME,
    };

    private static final String PROPKEY_FORMAT = 
        "    %-20s %s";
    private static final int[] PROPKEY_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME,
        ValueWrapper.ATTR_DESCRIPTION,
    };

    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_resourcetype_view(ClientShell shell){
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        String format, type, tag, mtype;
        int[] attrTypes, attrs;
        ValuePrinter printer;
        ValueWrapper val;
        PrintStream out;
        List templates, propKeys;

        if(args.length != 2)
            throw new ShellCommandUsageException(this.getSyntax());
        
        type = args[0];
        tag  = args[1];

        if(!ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, type))
            throw new ShellCommandUsageException(this.getSyntax());

        try {
            Integer adId;
            int id = ClientShell_resource.convertParamToInt(type), adType;

            switch(id){
            case ClientShell_resource.PARAM_PLATFORM:
                PlatformTypeValue platValue = 
                    this.entityFetcher.getPlatformTypeValue(tag);

                adId      = platValue.getId();
                val       = new ValueWrapper(platValue);
                format    = PLATFORMTYPE_FORMAT;
                attrTypes = PLATFORMTYPE_ATTRTYPES;
                attrs     = PLATFORMTYPE_ATTRS;
                mtype     = platValue.getName();
                break;
            case ClientShell_resource.PARAM_SERVER:
                ServerTypeValue servVal = 
                    this.entityFetcher.getServerTypeValue(tag);

                adId      = servVal.getId();
                val       = new ValueWrapper(servVal);
                format    = SERVERTYPE_FORMAT;
                attrTypes = SERVERTYPE_ATTRTYPES;
                attrs     = SERVERTYPE_ATTRS;
                mtype     = servVal.getName();
                break;
            case ClientShell_resource.PARAM_SERVICE:
                ServiceTypeValue serviceVal = 
                    this.entityFetcher.getServiceTypeValue(tag);

                adId      = serviceVal.getId();
                val       = new ValueWrapper(serviceVal);
                format    = SERVICETYPE_FORMAT;
                attrTypes = SERVICETYPE_ATTRTYPES;
                attrs     = SERVICETYPE_ATTRS;
                mtype     = serviceVal.getName();
                break;
            default:
                throw new IllegalStateException("Unhandled resource");
            }

            templates = 
                this.entityFetcher.getMetricTemplatesForMonitorableType(mtype);
            adType   = ClientShell_resource.paramToEntityType(type);
            propKeys = this.entityFetcher.getCPropKeys(adType, 
                                                       adId.intValue());
                
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        out = this.getOutStream();
        printer = new ValuePrinter(out, format, attrs, attrTypes);
        printer.printItem(val);
        
        out.println("\nCustom Property Keys:");
        if(propKeys != null && propKeys.size() != 0){
            printer = new ValuePrinter(out, PROPKEY_FORMAT, PROPKEY_ATTRS);
            printer.printList(propKeys);
        } else {
            out.println("    No keys exist for this resource");
        }
        
        out.println("\nAvailable Metric Templates:");
        if(templates != null && templates.size() != 0){
            printer = new ValuePrinter(out, TEMPLATES_FORMAT, TEMPLATES_ATTRS);
            printer.printList(templates);
        } else {
            out.println("    No metric templates exist for this resource");
        }
    }

    public String getSyntaxArgs(){
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource type>";
    }

    public String getUsageShort(){
        return "View a specific " + ClientShell.PRODUCT + " resource type";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".  The type can be specified" +
            "\n    by name or by ID number.";
    }
}
