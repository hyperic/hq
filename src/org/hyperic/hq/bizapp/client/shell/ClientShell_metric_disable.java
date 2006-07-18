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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.IntegerParser;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.paramParser.StringParser;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_metric_disable 
    extends ClientShellCommand 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    
    private static final String BLOCK_METRIC = "MetricBlock";

    private static final String PARAM_FORMAT =
        "<" + ClientShellParseUtil.makeResourceBlock(PARAM_VALID_RESOURCE) + 
        "> [$" + BLOCK_METRIC + " <#Integer> | <#String>]";

    public ClientShell_metric_disable(ClientShell shell){
        super(shell, PARAM_FORMAT);
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        String blockName;

        ClientShellParseUtil.bubbleUpResourceBlock(result, blockVals);
        
        blockName = result.getBlockName();
        if(blockName.equals(BLOCK_METRIC)){
            FormatParser fp = blockVals[0];

            if(fp instanceof IntegerParser){
                result.getRoot().setValue(blockName, 
                                  new Integer(((IntegerParser)fp).getValue()));
            } else {
                result.getRoot().setValue(blockName, 
                                          ((StringParser)fp).getValue());
            }
        }
    }

    public void processCommand(ParseResult parseRes)
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        ClientShellEntityFetcher fetcher;
        AppdefEntityID id;
        Integer[] disable;
        Object oMetricTag;

        fetcher = this.getEntityFetcher();

        id = (AppdefEntityID)parseRes.getValue(ClientShellParseUtil.KEY_RESOURCE);
        oMetricTag = parseRes.getValue(BLOCK_METRIC);
        if(oMetricTag == null) {
            try {
                fetcher.disableMetrics(id);
            } catch(Exception exc){
                throw new ShellCommandExecException(exc);
            }
            this.getOutStream().println("Metrics for " + id + " disabled");
            return;
        } else {
            String metAlias;
            boolean found;
            List templs;
            
            disable = new Integer[1];

            try {
                templs = fetcher.getMetricsForID(id);
            } catch(Exception exc){
                throw new ShellCommandExecException(exc);
            }

            found    = false;
            metAlias = null;
            for(Iterator i=templs.iterator(); i.hasNext(); ){
                DerivedMeasurementValue dmv;

                dmv = (DerivedMeasurementValue)i.next();
                if(oMetricTag instanceof Integer){
                    if(dmv.getId().equals((Integer)oMetricTag)){
                        disable[0] = dmv.getId();
                        metAlias   = dmv.getTemplate().getAlias();
                        found      = true;
                    }
                } else {
                    String sMetric = (String) oMetricTag;

                    if(dmv.getTemplate().getAlias().equalsIgnoreCase(sMetric)){
                        disable[0] = dmv.getId();
                        metAlias   = dmv.getTemplate().getAlias();
                        found      = true;
                        break;
                    }
                }
            }
            
            if(!found){
                this.getErrStream().println("Unable to find metric '" + 
                                            oMetricTag + "' for " + id);
                return;
            }

            try {
                fetcher.disableMetrics(disable);
            } catch(Exception exc){
                throw new ShellCommandExecException(exc);
            }
            this.getOutStream().println("Metric '" + metAlias + 
                                        "' on " + id + " disabled");
        }
    }

    public String getSyntaxArgs(){
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource> [metric]";
    }

    public String getUsageShort(){
        return "Disable metric collection for a resource";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".  To disable a\n" +
            "    single metric, specify 'metric' as an alias or ID of a " + 
            "metric from\n" +
            "    the 'metric list' command.";
    }
}
