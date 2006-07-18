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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.shared.BaselineValue;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.StringUtil;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_metric_view 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_GROUP,
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    private static final String PARAM_VALUE = "-value";
    private static final String METRIC_FORMAT = 
        "Metric:               %s (id=%s)\n" +
        "Collection Interval:  %s seconds\n" +
        "Units:                %s\n" +
        "High Range:           %s\n" +
        "Low Range:            %s";
    private static final int[] METRIC_ATTRS = new int[] {
        ValueWrapper.ATTR_TEMPLATENAME,
        ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_INTERVAL,
        ValueWrapper.ATTR_TEMPLATEUNITS,
        ValueWrapper.ATTR_MAXEXPECTED,
        ValueWrapper.ATTR_MINEXPECTED };
    private static final String BASELINE_FORMAT = 
        "Baseline ID:          %s\n" +
        "Mean:                 %.3f\n" +
        "Min:                  %.3f\n" +
        "Max:                  %.3f\n" +
        "Compute Date:         %s\n";
    private static final int[] BASELINE_ATTRS = new int[] {
        ValueWrapper.ATTR_ID,
        ValueWrapper.ATTR_MEAN,
        ValueWrapper.ATTR_MINEXPECTED,
        ValueWrapper.ATTR_MAXEXPECTED,
        ValueWrapper.ATTR_COMPUTETIME};
    private static final int[] BASELINE_ATTRTYPES = new int[] {
        ValuePrinter.ATTRTYPE_STRING,
        ValuePrinter.ATTRTYPE_DOUBLE,
        ValuePrinter.ATTRTYPE_DOUBLE,
        ValuePrinter.ATTRTYPE_DOUBLE,
        ValuePrinter.ATTRTYPE_LONGDATE};

    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_metric_view(ClientShell shell){
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    private void printMetric(DerivedMeasurementValue dVal){
        ValuePrinter printer;

        printer = new ValuePrinter(this.getOutStream(), METRIC_FORMAT,
                                   METRIC_ATTRS);
        printer.printItem(new ValueWrapper(dVal));
        if(dVal.getBaseline() != null){
            printer = new ValuePrinter(this.getOutStream(), BASELINE_FORMAT,
                                       BASELINE_ATTRS, BASELINE_ATTRTYPES);
            printer.printItem(new ValueWrapper(dVal.getBaseline()));
        }
    }

    private void printMetricValue(AppdefResourceValue resource,
                                  DerivedMeasurementValue dVal, 
                                  MetricValue val)
    {
        final String METRICVALUE_FORMAT = 
            "Value:                 %s\n" +
            "Collection Time:       %s";
        PrintStream out = this.getOutStream();
        ValueWrapper wrapper;
        PrintfFormat fmt;
        Object[] fArgs;
        double rVal;

        out.println("Retrieved " + dVal.getTemplate().getName() + 
                    " from '" + resource.getName() + 
                    "' (" + resource.getEntityId() + ")");

        wrapper  = new ValueWrapper(val);
        rVal     = val.getValue();
        fmt      = new PrintfFormat(METRICVALUE_FORMAT);
        fArgs    = new Object[2];
        fArgs[0] = 
            UnitsConvert.convert(rVal, dVal.getTemplate().getUnits()); 
        fArgs[1] = wrapper.getLongDate(ValueWrapper.ATTR_TIME);

        out.println(fmt.sprintf(fArgs));
    }

    private void doViewMetricForResource(int appdefType, String resourceTag,
                                         String metricTag)
        throws ShellCommandExecException
    {
        this.doViewMetric(this.getMetricForResource(appdefType, resourceTag,
                                                    metricTag));
    }

    private void doViewMetric(DerivedMeasurementValue dVal)
        throws ShellCommandExecException
    {
        this.printMetric(dVal);
    }

    private void doViewMetricForID(int metricID)
        throws ShellCommandExecException
    {
        DerivedMeasurementValue dVal;
        BaselineValue bVal;

        try { 
            if((dVal = this.entityFetcher.getMetric(metricID)) == null){
                this.getErrStream().println("Could not find metric " + 
                                            metricID);
                return;
            }
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }

        this.printMetric(dVal);
    }

    private void doViewMetricValue(DerivedMeasurementValue dVal)
        throws ShellCommandExecException
    {
        AppdefResourceValue resource;
        MetricValue val;

        try {
            AppdefEntityID entId;
            int id, measId;

            measId   = dVal.getId().intValue();
            val      = this.entityFetcher.getLiveMetricValue(measId);
            id       = dVal.getAppdefType();
            entId    = new AppdefEntityID(id, dVal.getInstanceId().intValue());
            resource = this.entityFetcher.findResourceByID(entId);
        } catch(LiveMeasurementException exc){
            this.getErrStream().println("Unable to fetch live value: " +
                                        exc.getMessage());
            return;
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
        
        
        this.printMetricValue(resource, dVal, val);
    }

    private DerivedMeasurementValue getMetricForResource(int appdefType,
                                                         String resourceTag,
                                                         String metricTag)
        throws ShellCommandExecException
    {
        AppdefEntityID id;

        try {
            id = this.entityFetcher.getID(appdefType, resourceTag, false);
            return this.entityFetcher.getMetricByAliasAndID(id, metricTag);
        } catch(AppdefEntityNotFoundException e) {
            throw new ShellCommandExecException("Resource " + resourceTag +
                                                " not found");
        } catch(Exception exc){
            throw new ShellCommandExecException("Metric " + metricTag +
                                                " not found");
        }
    }
    private void doViewMetricValueForResource(int appdefType, 
                                              String resourceTag, 
                                              String metricTag)
        throws ShellCommandExecException
    {
        this.doViewMetricValue(this.getMetricForResource(appdefType, 
                                                         resourceTag,
                                                         metricTag));
    }

    private void doViewMetricValueForID(int metricID)
        throws ShellCommandExecException
    {
        DerivedMeasurementValue dVal;

        try {
            if((dVal = this.entityFetcher.getMetric(metricID)) == null){
                this.getErrStream().println("Could not find metric " + 
                                            metricID);
                return;
            }
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
        
        this.doViewMetricValue(dVal);
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        if(args.length == 0)
            throw new ShellCommandUsageException(this.getSyntaxEx());

        if(ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, args[0]) &&
           args.length >= 3)
        {
            String resourceType, resourceID;
            int appdefType;

            resourceType = args[0];
            resourceID   = args[1];
            if(ClientShell_resource.convertParamToInt(resourceType) ==
               ClientShell_resource.PARAM_GROUP)
            {
                throw new ShellCommandExecException("Not yet implemented");
            } 

            appdefType = ClientShell_resource.paramToEntityType(resourceType);

            if(args.length == 3 && !args[2].equals(PARAM_VALUE)){
                this.doViewMetricForResource(appdefType, resourceID, args[2]);
            } else if(args.length == 4 && args[2].equals(PARAM_VALUE)){
                this.doViewMetricValueForResource(appdefType, resourceID,
                                                  args[3]);
            } else {
                throw new ShellCommandUsageException(this.getSyntaxEx());
            }
        } else if(args.length == 1 ||
                  args.length == 2 && args[0].equals(PARAM_VALUE))
        {
            int metricID;

            try {
                metricID = Integer.parseInt(args.length == 1 ? args[0] :
                                                               args[1]);
            } catch(NumberFormatException exc){
                throw new ShellCommandUsageException(this.getSyntaxEx());
            }
            if(args.length == 1)
                this.doViewMetricForID(metricID);
            else
                this.doViewMetricValueForID(metricID);
        } else {
            throw new ShellCommandUsageException(this.getSyntaxEx());
        }
    }

    public String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort(){
        return "View metric details and retrieve live values";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".  To locate a metric's\n" +
            "    alias, use the 'metric list' command.\n\n" +
            "    To view a metric for a resource:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            ">\n      " + cmdSpace + " <resource> <metric alias>\n" +
            "    To get a live value for a resource's metric:\n" +
            "      " + this.getCommandName() + " <" +
            ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            ">\n      " + cmdSpace + " <resource> -value <metric alias>\n" +
            "    To view a metric by ID:\n" +
            "      " + this.getCommandName() + " <metric id>\n" +
            "    To get a live value for a metric by ID:\n" +
            "      " + this.getCommandName() + " -value <metric id>";
    }
}
