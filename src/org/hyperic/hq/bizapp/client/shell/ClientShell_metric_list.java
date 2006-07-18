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
import java.util.Date;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.bizapp.client.pageFetcher.FindMetricDataFetcher;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.paramParser.BlockHandler;
import org.hyperic.util.paramParser.DateParser;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.IntegerParser;
import org.hyperic.util.paramParser.IntervalParser;
import org.hyperic.util.paramParser.ParamParser;
import org.hyperic.util.paramParser.ParseException;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.paramParser.StringParser;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_metric_list 
    extends ShellCommandBase 
    implements BlockHandler
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_GROUP,
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    private static final String PARAM_VALUE    = "-value";
    private static final String PARAM_FROM     = "-from";
    private static final String PARAM_TO       = "-to";
    //private static final String PARAM_INTERVAL = "-interval";

    private static final String BLOCK_FROM     = "FromBlock";
    private static final String BLOCK_INTERVAL = "IntervalBlock";
    private static final String BLOCK_TO       = "ToBlock";
    private static final String BLOCK_VALUE    = "ValueBlock";

    private static final String RANGE_BLOCK =
        "[$" + BLOCK_FROM + " " + PARAM_FROM + " #PastDate " +
        " [$" + BLOCK_TO + " " + PARAM_TO + " #PastDate] " +
    //    " [$" + BLOCK_INTERVAL + " " + PARAM_INTERVAL + " #Interval]" +
        "]";
    private static final String PARAM_FMT =
        "<<" + ClientShellParseUtil.makeResourceBlock(PARAM_VALID_RESOURCE) + 
        "> [$ValueBlock " + PARAM_VALUE + " #String " + RANGE_BLOCK + "]> | " +
        "<$ValueBlock " + PARAM_VALUE + " #Integer " + RANGE_BLOCK + ">";


    private ClientShellEntityFetcher entityFetcher;
    private ParamParser           paramParser;

    public ClientShell_metric_list(ClientShell shell){
        ClientShellParserRetriever retriever;

        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
        retriever = new ClientShellParserRetriever(this.entityFetcher);
        this.paramParser = new ParamParser(PARAM_FMT, retriever, this);
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        String blockName;

        ClientShellParseUtil.bubbleUpResourceBlock(result, blockVals);
        
        blockName = result.getBlockName();
        if(blockName.equals(BLOCK_VALUE)){
            Object val;

            if(blockVals[1] instanceof IntegerParser)
                val = new Integer(((IntegerParser)blockVals[1]).getValue());
            else 
                val = ((StringParser)blockVals[1]).getValue();

            result.getRoot().setValue(blockName, val);
        } else if(blockName.equals(BLOCK_FROM) || blockName.equals(BLOCK_TO)){
            DateParser dp = (DateParser)blockVals[1];

            result.getRoot().setValue(blockName, new Long(dp.getValue()));
        } else if(blockName.equals(BLOCK_INTERVAL)){
            IntervalParser ip = (IntervalParser)blockVals[1];

            result.getRoot().setValue(blockName, new Long(ip.getValue()));
        }
    }

    private void printMetrics(List meas){
        if (meas.size() == 0) {
            this.getOutStream().println("No metrics found");
            return;
        }
        
        ValuePrinter printer;

        this.getOutStream().println("Metric listing: " + meas.size() + 
                                    " metrics found");
                                    
        printer = new ValuePrinter(this.getOutStream(), 
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_INTERVAL,
                                       ValueWrapper.ATTR_TEMPLATENAME,
                                       ValueWrapper.ATTR_TEMPLATEALIAS });
        printer.setHeaders(new String[] { "ID", "Interval",
                                          "Description", "Alias" });
        printer.printList(meas);
    }

    private void printGroupMetrics(List meas){
        if (meas.size() == 0) {
            this.getOutStream().println("No metrics found");
            return;
        }
        
        ValuePrinter printer;

        this.getOutStream().println("Metric listing: " + meas.size() + 
                                    " metrics found");
                                    
        printer = new ValuePrinter(this.getOutStream(), 
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_INTERVAL,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_ACTIVE_MEMBERS,
                                       ValueWrapper.ATTR_TOTAL_MEMBERS });

        printer.setHeaders(new String[] { "ID", "Interval",
                                          "Description",
                                          "# Collecting", "Total" });
        printer.printList(meas);
    }

    private void printMetricValues(DerivedMeasurementValue dVal,
                                   FindMetricDataFetcher pFetch)
        throws PageFetchException 
    {
        ValuePrinter printer;

        printer = new ValuePrinter(this.getOutStream(), 
                                   "%-25s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_TIME,
                                       ValueWrapper.ATTR_DATA},
                                   new int[] {
                                       ValuePrinter.ATTRTYPE_LONGDATE,
                                       ValuePrinter.ATTRTYPE_STRING});
        printer.setAuxData(dVal);
        printer.setHeaders(new String[] { "Time", "Value", });
        
        PageControl pc = this.getShell().getDefaultPageControl();
        ValuePrinterPageFetcher vpFetch =
            new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, pc);
    }

    private void doListMetricsForResource(AppdefEntityID id)
        throws ShellCommandExecException
    {
        List meas;

        try {
            meas = this.entityFetcher.getMetricsForID(id);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            this.printGroupMetrics(meas);
        } else {
            this.printMetrics(meas);
        }
    }

    private void doListMetricValuesForID(int metricID, Long lStart, Long lEnd,
                                         Long lInterval)
        throws ShellCommandExecException 
    {
        DerivedMeasurementValue dVal;
        List data;

        try {
            FindMetricDataFetcher fetcher;
            long start, end, interval;

            if((dVal = this.entityFetcher.getMetric(metricID)) == null){
                this.getErrStream().println("Metric " + metricID + 
                                            " not found");
                return;
            }
            
            start = 0;

            if(lEnd != null)
                end = lEnd.longValue();
            else
                end = System.currentTimeMillis();

            if(lInterval == null){
                if(lStart != null){
                    start = lStart.longValue();
                    fetcher = 
                        this.entityFetcher.findAllMetricDataFetcher(metricID,
                                                                    start,
                                                                    end);
                } else {
                    fetcher =
                        this.entityFetcher.findAllMetricDataFetcher(metricID);
                }
            } else {
                start    = lStart.longValue();
                interval = lInterval.longValue();

                fetcher = this.entityFetcher.findClosestMetricDataFetcher(
                    metricID, start, end, interval);
            }
            this.printMetricValues(dVal, fetcher);
        } catch(MeasurementNotFoundException exc){
            System.err.println("Metric " + metricID + " not found");
            return;
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    private void doListMetricValuesForAlias(AppdefEntityID id,
                                            String metricTag, Long lStart,
                                            Long lEnd, Long lInterval)
        throws ShellCommandExecException 
    {
        DerivedMeasurementValue dVal;

        try {
            dVal = this.entityFetcher.getMetricByAliasAndID(id, metricTag);
        } catch(MeasurementNotFoundException exc){
            System.err.println(StringUtil.capitalize(id + " does not have a " +
                                                     "metric alias, '" + 
                                                     metricTag + "'"));
            return;
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        this.doListMetricValuesForID(dVal.getId().intValue(), lStart, lEnd,
                                     lInterval);
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        AppdefEntityID entity;
        ParseResult parseRes;
        Long from, to, interval;
        Object oValue;

        try {
            parseRes = this.paramParser.parseParams(args);
        } catch(ParseException exc){
            if(exc.getExceptionOfType(ApplicationException.class) != null||
               exc.getExceptionOfType(SystemException.class) != null)
            {
                throw new ShellCommandExecException(exc);
            }

            throw new ShellCommandUsageException(this.getSyntaxEx());
        }

        from     = (Long)parseRes.getValue(BLOCK_FROM);
        to       = (Long)parseRes.getValue(BLOCK_TO);
        interval = (Long)parseRes.getValue(BLOCK_INTERVAL);
        oValue   = parseRes.getValue(BLOCK_VALUE);
        entity   = 
            (AppdefEntityID)parseRes.getValue(ClientShellParseUtil.KEY_RESOURCE);

        if(from != null){
            PrintStream out = this.getOutStream();

            out.println("Reading metrics from " + 
                        (new Date(from.longValue())));
                      
            if(to != null)
                out.println("                  to " + 
                            (new Date(to.longValue())));
            else
                out.println();
        }

        if(entity != null){
            if(oValue == null){
                this.doListMetricsForResource(entity);
            } else {
                this.doListMetricValuesForAlias(entity, (String)oValue,
                                                from, to, interval);
            }
        } else {
            this.doListMetricValuesForID(((Integer)oValue).intValue(),
                                         from, to, interval);
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "List metrics and their values";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".\n" +
            "    To list metrics for a resource:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource>\n" +
            "    To list values for a metric:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <resource>\n       " + cmdSpace + PARAM_VALUE + 
            " <metric name>\n" +
            "      " + cmdSpace + 
            " [" + PARAM_FROM + " <date> [" + PARAM_TO + " <date>]" +            //" [" + PARAM_INTERVAL + " <time>]" +            "]\n" +
            "    Or:\n" +
            "      " + this.getCommandName() + " " + PARAM_VALUE + 
            " <metric id>\n      " + cmdSpace + 
            " [" + PARAM_FROM + " <date> [" + PARAM_TO + " <date>]" +            //" [" + PARAM_INTERVAL +" <time>]" +            "]\n\n" +
            "    If the " + PARAM_TO + " flag is not specified, the current " +
            "time will be used.\n\n" +
            "    Dates and times can be specified in a number of ways.  They "+
            "can be\n" +
            "    called out explicitly (such as \"3:00pm\", " +
            "\"12/24/02 9:00pm\", \"monday\",\n" +
            "    \"yesterday\", or \"now\") and can be given offsets (such " +
            "as \"now - 1 day\", \n" +
            "    \"march + 4 days\", etc.)\n\n" +
            /*
            "    If the optional time interval is used, it must take the " +
            "following form:\n" +
            "      #d, #h, #m, or #s (" +
            "where # is the # of days, hours, minutes, or seconds)\n\n" +
             */            "\n" +
            "    Examples:\n\n" +
            "    List all the metrics currently configured for the " +
            "server\n    called \"Jons Apache\":\n" +
            "      " + this.getCommandName() + " " +
      ClientShell_resource.convertParamToString(ClientShell_resource.PARAM_SERVER)+
            " \"Jons Apache\"\n\n" +
            "    List the values which have already been collected for metric"+
            " #7294:\n" +
            "      " + this.getCommandName() +" " + PARAM_VALUE + " 7294\n\n" +
            "    List the values which were collected from Mar 4, 2002 to" +
            " Jun 22, 2003:\n" +
            "      " + this.getCommandName() + " " + PARAM_VALUE + " 7294 " + 
            PARAM_FROM + " 03/04/02 " + PARAM_TO + " 06/22/03\n\n" +
            "    List the values which were collected from Mar 4, 2002 to " +
            "a week ago\n" +
            //"    at an interval of 2 days:\n" +
            "      " + this.getCommandName() + " " + 
        ClientShell_resource.convertParamToString(ClientShell_resource.PARAM_SERVER)+
            " \"My Server\" " + PARAM_VALUE + " Availability " + 
            PARAM_FROM + " 03/04/02\n" + 
            "                  " + PARAM_TO + " \"now - 1 week\" " + 
            //PARAM_INTERVAL + " 2d";
            "";
    }
}
