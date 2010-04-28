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

package org.hyperic.hq.plugin.hqagent;

import org.hyperic.hq.agent.AgentMonitorValue;
import org.hyperic.hq.agent.server.AgentDaemon;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class AgentMeasurementPlugin
    extends SigarMeasurementPlugin
{
    private static final String AVAIL_TMPL =
        "camAgent:availability";

    public AgentMeasurementPlugin(){
        this.setName(AgentProductPlugin.NAME);
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config){
        return new ConfigSchema();
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException
    {
        AgentDaemon agent;

        if((agent = AgentDaemon.getMainInstance()) == null){
            throw new PluginException("Agent.getValue() called "+
                                      "when plugin not running " +
                                      "from within the Agent");
        }

        if(metric.toString().equals(AVAIL_TMPL)){
            return new MetricValue(Metric.AVAIL_UP);
        }

        if(metric.getDomainName().equals("camAgent")){
            return this.getAgentValue(agent, metric);
        } else if(metric.getDomainName().equals("sigar")){
            return this.getSigarValue(metric);
        } else {
            throw new MetricInvalidException("Invalid JMX domain '" + 
                                             metric.getDomainName() + "'");
        }
    }
    
    private MetricValue getSigarValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException
    {
        return super.getValue(metric);
    }
    
    private MetricValue getAgentValue(AgentDaemon agent, Metric metric)
        throws PluginException
    {
        AgentMonitorValue mVal;
        String monitorName, monitorVal;

        monitorName = metric.getObjectProperty("Monitor");
        monitorVal  = metric.getAttributeName();

        if(monitorName == null || monitorVal == null){
            throw new MetricInvalidException("Metric invalid -- no Monitor " +
                                             "key, or no attribute: " + metric);
        }

        mVal = agent.getMonitorValues(monitorName, 
                                      new String[] {monitorVal})[0];

        if(mVal.isErr()){
            switch(mVal.getErrCode()){
            case AgentMonitorValue.ERR_INCALCULABLE:
                getLog().debug("Unable to calculate " + metric);
                return new MetricValue(Double.NaN);
            case AgentMonitorValue.ERR_BADKEY:
                throw new MetricInvalidException("Agent monitor '" + 
                                                 monitorName + "' does not have "+
                                                 "a " + monitorVal + " value");
            case AgentMonitorValue.ERR_BADMONITOR:
                throw new MetricInvalidException("Agent monitor '" +
                                                 monitorName + "' unknown");
            case AgentMonitorValue.ERR_INTERNAL:
            default:
                throw new PluginException("Internal error fetching"+
                                          " '" + monitorName + ":"+
                                          monitorVal + "'");
            }
        } else {
            if(mVal.getType() != AgentMonitorValue.TYPE_DOUBLE){
                throw new MetricInvalidException("Agent Metric '" + metric + "' " +
                                              "does not return a double");
            }
            
            return new MetricValue(mVal.getDoubleValue());
        }
    }

    public String translate(String template, ConfigResponse config){
        return template;
    }
}
