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

import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.IntegerParser;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.paramParser.StringParser;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_metric_delete
    extends ClientShellCommand
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    
    private static final String BLOCK_METRIC = "metric";
    private static final String PARAM_FORMAT =
        "<<" + ClientShellParseUtil.makeResourceTypeBlock(PARAM_VALID_RESOURCE) +
        "> <$" + BLOCK_METRIC + " #String>> | " +
        " <$" + BLOCK_METRIC + " #Integer>";

    public ClientShell_metric_delete(ClientShell shell){
        super(shell, PARAM_FORMAT);
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        String blockName;

        ClientShellParseUtil.bubbleUpResourceTypeBlock(result, blockVals);
        
        blockName = result.getBlockName();
        if(blockName.equals(BLOCK_METRIC)){
            FormatParser fp = blockVals[0];

            if(fp instanceof StringParser)
                result.getRoot().setValue(blockName, 
                                          ((StringParser)fp).getValue());
            else 
                result.getRoot().setValue(blockName,
                                  new Integer(((IntegerParser)fp).getValue()));
        }
    }

    public void processCommand(ParseResult parseRes) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        AppdefResourceTypeValue resource;
        ClientShellEntityFetcher fetcher;
        Object oMetric;
        int metricId = 0;

        fetcher = this.getEntityFetcher();

        oMetric = parseRes.getValue(BLOCK_METRIC);
        if(oMetric instanceof String){
            String resourceKey = ClientShellParseUtil.KEY_RESOURCETYPE;
            String monType, sMetric;
            boolean found;
            List tmpls;

            resource = (AppdefResourceTypeValue)parseRes.getValue(resourceKey);
            monType  = resource.getName();
            sMetric  = (String)oMetric;
            try {
                tmpls    = 
                   (List)fetcher.getMetricTemplatesForMonitorableType(monType);
            } catch(Exception exc){
                throw new ShellCommandExecException(exc);
            }

            found = false;
            for(Iterator i=tmpls.iterator(); i.hasNext(); ){
                MeasurementTemplateValue mtv;

                mtv = (MeasurementTemplateValue)i.next();
                if(mtv.getAlias().equalsIgnoreCase(sMetric)){
                    metricId = mtv.getId().intValue();
                    found = true;
                    break;
                }
            }

            if(found == false){
                this.getErrStream().println("Unable to find metric '" + 
                                            sMetric + "' for '" + monType +
                                            "'");
                return;
            }
        } else {
            metricId = ((Integer)oMetric).intValue();
        }

        try {
            fetcher.removeDerivedMeasurementTemplate(metricId);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    public String getUsageShort(){
        return "Delete a derived metric";
    }

    public String getUsageHelp(String[] args){
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n\n" +
            "    To delete a template by resource and alias name:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) + 
            "> <resourceType> <alias>\n\n" + 
            "    To delete a template by its ID (as from " +
            "'resourcetype view')\n" +
            "      " + this.getCommandName() + " <templateID>";
    }
}
