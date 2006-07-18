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

import java.io.EOFException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasurementArgValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.paramParser.StringParser;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_metric_create
    extends ClientShellCommand
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };

    // The indices of the following values needs to match up with
    // COLL_TYPE_* from MeasurementConstants
    private static final String[] COLLECTION_TYPES = {
        "Dynamic     (Value changes up and down)",
        "Static      (Value is consistent for a long time [e.g. TotalMemory])",
        "Trends up   (Value increases over time)",
        "Trends down (Value decreases over time)",
    };
    
    private static final String PROP_CATEGORY    = "category";
    private static final String PROP_DESCRIPTION = "description";
    private static final String PROP_UNITS       = "units";
    private static final String PROP_COLLTYPE    = "collectionType";
    private static final String PROP_EXPR        = "expression";

    private static final String BLOCK_METRIC = "metricName";
    private static final String PARAM_FORMAT =
        "<" + ClientShellParseUtil.makeResourceTypeBlock(PARAM_VALID_RESOURCE) +
        "> <$" + BLOCK_METRIC + " #String>";

    public ClientShell_metric_create(ClientShell shell){
        super(shell, PARAM_FORMAT);
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        String blockName;

        ClientShellParseUtil.bubbleUpResourceTypeBlock(result, blockVals);
        
        blockName = result.getBlockName();
        if(blockName.equals(BLOCK_METRIC)){
            StringParser sp = (StringParser)blockVals[0];

            result.getRoot().setValue(blockName, sp.getValue());
        }
    }

    private ConfigSchema getCreateSchema(String metricName, 
                                         AppdefResourceTypeValue resource,
                                         List templateList)
    {
        SchemaBuilder sb = new SchemaBuilder();
        String desc, expr, units, useCat;
        
        desc   = metricName + " derived metric";
        expr   = "5 + 5";
        units  = MeasurementConstants.UNITS_NONE;
        useCat = MeasurementConstants.CAT_UTILIZATION;
        
        // Try to find a utilization metric
        for(Iterator i=templateList.iterator(); i.hasNext(); ){
            MeasurementTemplateValue tmpl = (MeasurementTemplateValue)i.next();
            final String cat = tmpl.getCategory().getName();

            expr = tmpl.getAlias() + " + 1.0";
            if(cat.equals(MeasurementConstants.CAT_UTILIZATION)){
                desc   = tmpl.getAlias() + " divided by 2";
                expr   = tmpl.getAlias() + " / 2.0";
                units  = tmpl.getUnits();
                useCat = cat;
                break;
            }
        }

        sb.addEnum(PROP_CATEGORY, "Category",
                   MeasurementConstants.VALID_CATEGORIES, useCat);
        sb.add(PROP_DESCRIPTION, "Description", desc);
        sb.addEnum(PROP_UNITS, "Units", 
                   MeasurementConstants.VALID_UNITS, units);
        sb.addEnum(PROP_COLLTYPE, "Collection Type", COLLECTION_TYPES);
        sb.add(PROP_EXPR, "Expression", expr);

        return sb.getSchema();
    }

    private void createMetric(ClientShellEntityFetcher fetcher,
                              String monType, String alias, String desc,
                              String category, String expr, String units, 
                              int collectionType, List templates)
        throws ShellCommandExecException
    {
        MeasurementArgValue[] argArray;
        ArrayList args;
        int argNo;

        args = new ArrayList();
        // Perform replacement within the expression
        argNo = 1;
        for(Iterator i = templates.iterator(); i.hasNext(); ){
            MeasurementTemplateValue tmpl = (MeasurementTemplateValue)i.next();
            String newExpr;

            newExpr = StringUtil.replace(expr, tmpl.getAlias(), 
                                         "ARG" + argNo + ".doubleValue()");
            if(!newExpr.equals(expr)){ 
                MeasurementArgValue arg;
                
                expr = newExpr;

                arg = new MeasurementArgValue();
                arg.setId(tmpl.getId());
                arg.setPlacement(new Integer(argNo));
                arg.setTicks(new Integer(0));
                arg.setPrevious(new Integer(0));
                arg.setWeight(new Float(0));
                args.add(arg);
                argNo++;
            }
        }

        if(this.getClientShell().isDeveloper()){
            PrintStream out = this.getOutStream();

            out.println("Expression: " + expr);
            out.println("Args: " + args);
        }

        argArray = 
            (MeasurementArgValue[])args.toArray(new MeasurementArgValue[0]);
        try {
            fetcher.createDerivedMeasurementTemplate(desc, alias, monType,
                                                     category, expr, units,
                                                     collectionType, argArray);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    public void processCommand(ParseResult parseRes) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        final String resourceKey = ClientShellParseUtil.KEY_RESOURCETYPE;
        final ClientShellEntityFetcher fetcher = this.getEntityFetcher();
        AppdefResourceTypeValue resource;
        ConfigSchema createSchema;
        ConfigResponse resp;
        String newAlias;
        List templateList;
        int collectionType = -1;

        resource = (AppdefResourceTypeValue)parseRes.getValue(resourceKey);
        newAlias = (String)parseRes.getValue(BLOCK_METRIC);
        
        try {
            templateList = 
              fetcher.getMetricTemplatesForMonitorableType(resource.getName());
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
        
        createSchema = this.getCreateSchema(newAlias, resource, templateList);

        try {
            resp = this.getClientShell().processConfigSchema(createSchema);
        } catch(EOFException exc){
            this.getOutStream().println("\nMetric creation aborted");
            return;
        } catch(Exception exc){
            throw new ShellCommandExecException(exc.getMessage(), exc);
        }

        for(int i=0; i < COLLECTION_TYPES.length; i++){
            if(resp.getValue(PROP_COLLTYPE).equals(COLLECTION_TYPES[i])){
               collectionType = i;
               break;
            }
        }

        if(collectionType == -1){
            throw new IllegalStateException("Response does not match schema");
        }

        this.createMetric(fetcher, resource.getName(), newAlias,
                          resp.getValue(PROP_DESCRIPTION),
                          resp.getValue(PROP_CATEGORY),
                          resp.getValue(PROP_EXPR), 
                          resp.getValue(PROP_UNITS),
                          collectionType,
                          templateList);
    }

    public String getSyntaxArgs(){
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <type> <newAlias>";
    }

    public String getUsageShort(){
        return "Create a new derived metric";
    }

    public String getUsageHelp(String[] args){
        return "    " + this.getUsageShort() + ".  The command will prompt" +
            " for information\n" +
            "    about the expression, description, category, and units.\n\n" +
            "    The expression is a mathematical expression, consisting of " +
            "a combination of\n" +
            "    aliases for that resource type.  Use the 'resourcetype view'"+
            " command to\n" +
            "    list the aliases which can be used in the expression.\n\n" +
            "    e.g.  MemFree / MemTotal\n\n" +
            "    <type> = the platform, server, or service type (as from " +
            "'resourcetype list')\n" +
            "    <newAlias> = the alias to assign to the new metric";
    }
}
