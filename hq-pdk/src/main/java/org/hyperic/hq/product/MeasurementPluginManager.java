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

package org.hyperic.hq.product;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class MeasurementPluginManager extends PluginManager implements MeasurementValueGetter {

    private Map metricCache = Collections.synchronizedMap(new HashMap());
    private boolean debugRateMetrics;
    private LogTrackPluginManager ltpm;
    
    public MeasurementPluginManager() {
        super();
    }

    public MeasurementPluginManager(Properties props) {
        super(props);
        //only doing this because trace() still logs even
        //at debug level???
        this.debugRateMetrics = 
            "true".equals("debugRateMetrics");
    }

    private void registerProxy(String name, GenericPlugin plugin)
        throws PluginException {
        registerPlugin(name, plugin);
        setPluginInfo(name, new PluginInfo(name, plugin.getPluginVersion()));
    }
    
    public void init(PluginManager parent) throws PluginException {
        super.init(parent);
        this.ltpm =
            ((ProductPluginManager)parent).getLogTrackPluginManager();

        //registry "proxy" plugins.  these plugins handle metrics
        //based on the Metric domain name, rather than calling into
        //the plugin for resource type associated with the metric.
        registerProxy(ExecutableProcess.DOMAIN,
                      new ExecutableMeasurementPlugin());
        registerProxy(SNMPMeasurementPlugin.DOMAIN,
                      new SNMPMeasurementPlugin());
        registerProxy(SigarMeasurementPlugin.DOMAIN,
                      new SigarMeasurementPlugin());
        registerProxy(SigarMeasurementPlugin.PTQL_DOMAIN,
                      new SigarMeasurementPlugin());
        registerProxy(Win32MeasurementPlugin.DOMAIN,
                      new Win32MeasurementPlugin());
    }

    public String getName() {
        return ProductPlugin.TYPE_MEASUREMENT;
    }

    //from ProductProperties.getProperties()
    private boolean isPluginTypeSupported(String type) {
        return "true".equals(getProperty("plugin." + type + ".supported"));
    }
    
    public ConfigSchema getConfigSchema(String plugin,
                                        TypeInfo info,
                                        ConfigResponse config)
        throws PluginNotFoundException {

        ConfigSchema schema = super.getConfigSchema(plugin, info, config);

        //attach log/config track enable options to measurement config schema
        //if the given type supports it.
        ProductPluginManager ppm = (ProductPluginManager)getParent();

        if (isPluginTypeSupported(ProductPlugin.TYPE_LOG_TRACK)) {
            mergeConfigSchema(ppm.getLogTrackPluginManager(),
                              schema, info, config);
        }

        if (isPluginTypeSupported(ProductPlugin.TYPE_CONFIG_TRACK)) {
            mergeConfigSchema(ppm.getConfigTrackPluginManager(),
                              schema, info, config);
        }

        return schema;
    }

    public MetricValue getValue(String template)
        throws PluginException, PluginNotFoundException,
               MetricNotFoundException, MetricUnreachableException
    {
        //split plugin:template...
        int ix = template.indexOf(":");
        String plugin = template.substring(0, ix);
        String metric = template.substring(ix+1, template.length());
        return getValue(plugin, metric);
    }
    
    public MetricValue getValue(String name, String metric)
        throws PluginException, PluginNotFoundException,
               MetricNotFoundException, MetricUnreachableException
    {
        try {
            return getValue(name, Metric.parse(metric));
        } catch (PluginNotFoundException e) {
            if (e.getMessage() == null) {
                String msg =
                    getName() + " plugin name=" + name + " not found";
                throw new PluginNotFoundException(msg);
            }
            else {
                throw e;
            }
        }
    }

    private MetricValue getPluginValue(String name, Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        //dont use getPlugin() here to prevent
        //PluginNotFoundException where one may have a 
        //foo-plugin.xml that uses all proxy metrics
        //we don't have to require foo-plugin.xml to
        //be deployed on the agent
        MeasurementPlugin plugin =
            (MeasurementPlugin)this.plugins.get(name);

        String domain = metric.getDomainName();
        //check if there is a proxy registered for the domain
        MeasurementPlugin invoker = 
            (MeasurementPlugin)this.plugins.get(domain);

        if (invoker == null) {
            if (plugin == null) {
                //last chance, might be found as a service extension
                plugin = (MeasurementPlugin)getPlugin(name);
            }
            invoker = plugin; //no proxy
        }

        PluginLoader.setClassLoader(invoker);
        try {
            return invoker.getValue(metric);
        } finally {
            PluginLoader.resetClassLoader(invoker);    
        }
    }

    public MetricValue getValue(String name, Metric metric)
        throws PluginException, PluginNotFoundException,
               MetricNotFoundException, MetricUnreachableException
    {
        String metricString = metric.toString();

        try {
            int idx;

            if ((idx = metricString.indexOf(MeasurementInfo.RATE_KEY)) != -1) {
                // Rate based metric
                MetricValue oldVal, newVal, rateVal;
                double interval;

                // Re-write the Metric to not include the rate.
                String newDsn = metricString.substring(0, idx);
                String rate =
                    metricString.substring(idx + 
                                           MeasurementInfo.RATE_KEY.length() +
                                           1);

                idx = rate.indexOf(':');
                if (idx != -1) {
                    //connection properties were appended after the __RATE__
                    newDsn += rate.substring(idx);
                    rate = rate.substring(0, idx);
                }

                if (rate.equals(MeasurementInfo.NO_RATE)) {
                    // Don't collect rates for this metric.
                    throw new MetricNotFoundException("Rate calculation " +
                                                        "disabled for this " +
                                                        "metric");
                } else if (rate.equals(MeasurementInfo.SEC_RATE)) {
                    interval = 1000;
                } else if (rate.equals(MeasurementInfo.MIN_RATE)) {
                    interval = 1000 * 60;
                } else if (rate.equals(MeasurementInfo.HOUR_RATE)) {
                    interval = 1000 * 60 * 60;
                } else {
                    // Invalid rate interval
                    throw new MetricNotFoundException("Invalid rate of " +
                                                      rate + " for Metric=" +
                                                      newDsn);
                }

                newVal = getPluginValue(name, Metric.parse(newDsn));
                oldVal = (MetricValue)metricCache.get(metricString);

                if (oldVal == null) {
                    metricCache.put(metricString, newVal);
                    if (this.debugRateMetrics) {
                        String msg =
                            "First time collecting rate " +
                            "based metric '" + newDsn + "'";
                        log.trace(msg);
                    }
                    return new MetricValue(Double.NaN);
                }

                // XXX: Maybe we should wait for two metric collections to
                //      avoid spikes in the first rate calculation.

                metricCache.put(metricString, newVal);

                double oldValue, newValue;
                double oldTime, newTime;

                oldValue = oldVal.getValue();
                newValue = newVal.getValue();
                
                // Check if new value is < old value in case a counter
                // rolls over.  We ignore those measurements
                if (newValue < oldValue) {
                    if (this.debugRateMetrics) {
                        String msg =
                            "Rate based metric '" + newDsn +
                            "'counter rolled over";
                        log.trace(msg);
                    }
                    return new MetricValue(Double.NaN);
                }

                oldTime = oldVal.getTimestamp();
                newTime = newVal.getTimestamp();

                rateVal = new MetricValue((newValue - oldValue) /
                                          ((newTime - oldTime)/interval),
                                          System.currentTimeMillis());
                return rateVal;
            } else {
                // Else, normal metric
                return getPluginValue(name, metric);
            }
        } catch(NoClassDefFoundError e) {
            throw new PluginException(classNotFoundMessage(e), e);
        }
    }

    private String translate(MeasurementPlugin plugin,
                             String template,
                             ConfigResponse config) {
        PluginLoader.setClassLoader(plugin);

        try {
            return plugin.translate(template, config);
        } finally {
            PluginLoader.resetClassLoader(plugin);
        }        
    }

    public String translate(String template, ConfigResponse config)
        throws PluginNotFoundException 
    {
        int ix = template.indexOf(":");
        String type = template.substring(0, ix);
        
        MeasurementPlugin plugin;

        //if the domain has a proxy registered,
        //run the proxy plugin's translate method.
        String name = template.substring(type.length()+1);
        ix = name.indexOf(":");
        if (ix != -1) {
            name = name.substring(0, ix);
            plugin = (MeasurementPlugin)this.plugins.get(name);
            if (plugin != null) {
                template = translate(plugin, template, config);
            }
        }

        plugin = (MeasurementPlugin)getPlugin(type);

        template = translate(plugin, template, config);
        
        return template;
    }

    public MeasurementInfo[] getMeasurements(TypeInfo info)
        throws PluginNotFoundException {

        String name = info.getName();
        MeasurementPlugin plugin = (MeasurementPlugin)getPlugin(name);

        return plugin.getMeasurements(info);
    }

    public String getHelp(TypeInfo info, Map props)
        throws PluginNotFoundException {

        String name = info.getName();

        MeasurementPlugin plugin = (MeasurementPlugin)getPlugin(name);
        ConfigSchema schema =
            getConfigSchema(plugin.getName(), info,
                            new ConfigResponse());

        String help = plugin.getHelp(info, props);
        String trackHelp =
            TrackEventPluginManager.getGenericHelp(this.ltpm, schema, info);
        if (trackHelp != null) {
            trackHelp =
                plugin.replaceHelpProperties(trackHelp, info, props);
        }
        if (help == null) {
            return trackHelp;
        }
        else if (trackHelp != null) {
            return help + "<hr>" + trackHelp;
        }
        else {
            return help;
        }
    }
    
    public void reportEvent(Metric metric,
                            long time, int level,
                            String source, String message) {
        LogTrackPlugin plugin = null;
        if (metric.getId() != null) {
            plugin = ltpm.getLogTrackPlugin(metric.getId());
        }

        if (plugin != null) {
            if (message == null) {
                message = "No Message";
            }
            plugin.reportEvent(time, level, source, message);
        }
    }
}
