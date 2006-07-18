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
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.StringUtil;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.IntegerParser;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.paramParser.StringParser;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_metric_enable 
    extends ClientShellCommand 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    private static final String PARAM_INTERVAL = "-interval";

    private static final String BLOCK_INTERVAL = "interval";
    private static final String BLOCK_METRIC   = "metric";

    private static final String PARAM_FORMAT =
        "<" + ClientShellParseUtil.makeResourceBlock(PARAM_VALID_RESOURCE) + 
        "> [$" + BLOCK_INTERVAL + " " + PARAM_INTERVAL + " #Integer] " + 
        " [$" + BLOCK_METRIC + " #String]";

    private static final String TEMPLATE_FMT = 
        "    %s";
    private static final int[] TEMPLATE_ATTRS = new int[] {
        ValueWrapper.ATTR_NAME };

    public ClientShell_metric_enable(ClientShell shell){
        super(shell, PARAM_FORMAT);
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        String blockName;

        ClientShellParseUtil.bubbleUpResourceBlock(result, blockVals);
        
        blockName = result.getBlockName();
        if(blockName.equals(BLOCK_INTERVAL)){
            IntegerParser ip = (IntegerParser)blockVals[1];

            result.getRoot().setValue(blockName, new Integer(ip.getValue()));
        } else if(blockName.equals(BLOCK_METRIC)){
            StringParser sp = (StringParser)blockVals[0];

            result.getRoot().setValue(blockName, sp.getValue());
        }
    }

    private Integer[] templateValueToTemplateId(List templateValues){
        Integer[] res = new Integer[templateValues.size()];

        for(int i = 0; i < templateValues.size(); i++){
            MeasurementTemplateValue v =
                (MeasurementTemplateValue)templateValues.get(i);

            res[i] = v.getId();
        }
        return res;
    }

    private void enableMeasurements(AppdefEntityID id, String metricTag,
                                    int interval)
        throws ShellCommandExecException
    {
        ClientShellEntityFetcher entityFetcher;
        Integer[] templateIds;
        List templates;

        entityFetcher = this.getEntityFetcher();

        try {
            final String pType = ProductPlugin.TYPE_MEASUREMENT;

            templates = entityFetcher.getMetricTemplatesForID(id);

            if(metricTag == null){
                ValuePrinter printer;

                this.getOutStream().println("Enabling collection of the " +
                                            "following metrics, every " +
                                            interval + " seconds:");
                printer     = new ValuePrinter(this.getOutStream(),
                                               TEMPLATE_FMT, TEMPLATE_ATTRS);
                printer.printList(templates);
                templateIds = templateValueToTemplateId(templates);
            } else {
                Integer metricID = null;
                String metricName = null;

                try {
                    metricID = new Integer(metricTag);
                    for(Iterator i=templates.iterator(); i.hasNext(); ){
                        MeasurementTemplateValue mtv;

                        mtv = (MeasurementTemplateValue)i.next();
                        if(mtv.getId().equals(metricID)){
                            metricID   = mtv.getId();
                            metricName = mtv.getName();
                            break;
                        }
                    }
                } catch(NumberFormatException exc){
                    for(Iterator i=templates.iterator(); i.hasNext(); ){
                        MeasurementTemplateValue mtv;

                        mtv = (MeasurementTemplateValue)i.next();
                        if(mtv.getAlias().equalsIgnoreCase(metricTag)){
                            metricID   = mtv.getId();
                            metricName = mtv.getName();
                            break;
                        }
                    }
                }

                if(metricID == null){
                    this.getErrStream().println("Unable to find metric '" +
                                                metricTag + "' for " + id);
                    return;
                }

                this.getOutStream().println("Enabling collection of '" +
                                            metricName + "' every " +
                                            interval + " seconds");
                templateIds = new Integer[] { metricID };
            }
            entityFetcher.enableMetrics(templateIds, id, interval);
        } catch(ConfigFetchException exc){
            String saniType, prodType;

            // Don't have the info necessary to enable metrics
            prodType = exc.getProductType();
            saniType = ClientShell_resource.sanitizePluginTypeName(prodType);
            this.getErrStream().println("Error: Unable to schedule " +
                                        "metrics for " + id + " because " +
                                        exc.getEntity() + "\nrequires " +
                                        saniType + " configuration.");
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    public void processCommand(ParseResult parseRes) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        AppdefEntityID id;
        String metricTag;
        Integer interval;

        id = (AppdefEntityID)parseRes.getValue(ClientShellParseUtil.KEY_RESOURCE);
        metricTag = (String)parseRes.getValue(BLOCK_METRIC);
        interval = (Integer)parseRes.getValue(BLOCK_INTERVAL);

        this.enableMeasurements(id, metricTag, 
                   interval == null ? 
                   (int)(MeasurementConstants.INTERVAL_DEFAULT_MILLIS / 1000) :
                   interval.intValue());
    }

    public String getUsageShort(){
        return "Enable metric collection for a resource";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource>\n       " + cmdSpace + 
            "[" + PARAM_INTERVAL + " seconds] [metric]\n" +
            "    The interval indicates how often the enabled metrics are " +
            "to be\n    collected.  If the optional metric " +
            "argument is given, the specific\n" +
            "    metric will be collected, otherwise all metrics for the " +
            "resource\n    will be enabled.  If the resource requires " +
            "configuration, the command\n" +
            "    will prompt prior to enabling.\n" +
            "\n    For a listing of the available metrics for the resource, " +
            "use\n    the 'resourcetype view'" +
            " command, with the type of the resource you\n    are enabling.";
    }
}
