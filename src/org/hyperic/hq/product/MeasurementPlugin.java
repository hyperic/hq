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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.pluginxml.PluginData;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.filter.TokenReplacer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Define and collect metrics.
 *
 */
public class MeasurementPlugin extends GenericPlugin {

    public static final String PROP_TEMPLATE_CONFIG = "template-config";
    public static final String TYPE_COLLECTOR = "collector"; //plugin type

    private static final String HELP_PLATFORM_ALL = "ALL";

    private Log log = LogFactory.getLog(this.getClass().getName());
    private Class collector = null;
    private static boolean inProxyRegister = false;

    private MeasurementPluginManager manager;

    private static final int PLATFORM_HELP_UNIX  = 1;
    private static final int PLATFORM_HELP_WIN32 = 2;

    //properties to assist with help text portability
    private static final String[][] PLATFORM_HELP_PROPS = {
        { "CMD.cp", "cp", "copy" },
        { "CMD.cpr", "cp -R", "xcopy /E" },
        { "CMD.prompt", "%", "C:\\&gt;" },
        { "CMD.env.set", "export", "set" },
        { "CMD.ext", "sh", "exe" },
        { "CMD.rm", "rm", "del" },
        { "FILE.sep", "/", "\\" },
        { "FILE.sep.esc", "/", "\\\\" }, //for .properties files
    };

    public MeasurementPlugin() {}

    private void registerProxies(PluginManager manager)
        throws PluginException {

        if (inProxyRegister) {
            return; //prevent recursion
        }

        //allow plugins to register proxy domains via plugin.xml
        if (getTypeInfo() == null) {
            return; //this is already a proxy
        }
        String domain = getTypeProperty("DOMAIN");
        if (domain == null) {
            return;
        }

        inProxyRegister = true;

        StringTokenizer tok =
            new StringTokenizer(domain, ",");
        while (tok.hasMoreTokens()) {
            String name = tok.nextToken();
            if (this.manager.isRegistered(name)) {
                continue;
            }
            getLog().info("Register " + getName() +
                          " proxy for domain: " + name);

            try {
                getManager().createPlugin(name, this);
            } catch (PluginException e) {
                inProxyRegister = false;
                throw e;
            }
        }

        inProxyRegister = false;
    }
    
    public void init(PluginManager manager)
        throws PluginException
    {
        super.init(manager);
        this.manager = (MeasurementPluginManager)manager;

        registerProxies(manager);
    }

    protected MeasurementPluginManager getManager() {
        return this.manager;
    }

    /**
     * Allow xml template properties to be added by a plugin.
     * The properties can be used as if the hq-plugin.xml
     * defined &lt;property name="..." value="..."/&gt;
     * for each entry in the Map returned.  Properties with the
     * same name in the xml file will override these.
     */
    protected Map getMeasurementProperties() {
        return null;
    }

    private void setInterval(MeasurementInfo metric) {
        // non-avail metric, avail set based on TypeInfo in
        // MeasurementPlugin
        switch (metric.getCollectionType()) {
          case MeasurementConstants.COLL_TYPE_DYNAMIC:
            metric.setInterval(MeasurementConstants.INTERVAL_DYNAMIC);
            break;
          case MeasurementConstants.COLL_TYPE_TRENDSUP:
          case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
            metric.setInterval(MeasurementConstants.INTERVAL_TRENDING);
            break;
          case MeasurementConstants.COLL_TYPE_STATIC:
            metric.setInterval(MeasurementConstants.INTERVAL_STATIC);
            break;
          default:
            // Shouldn't happen.. should maybe blow up on this.
            log.error("Unknown collection type: " + 
                      metric.getCollectionType());
        }
    }

    private void setAvailInterval(TypeInfo type, MeasurementInfo metric) {
        //set availability metric based on resource type
        switch (type.getType()) {
          case TypeInfo.TYPE_PLATFORM:
            metric.setInterval(MeasurementConstants.INTERVAL_AVAIL_PLAT);
            break;
          case TypeInfo.TYPE_SERVER:
            metric.setInterval(MeasurementConstants.INTERVAL_AVAIL_SVR);
            break;
          case TypeInfo.TYPE_SERVICE:
            metric.setInterval(MeasurementConstants.INTERVAL_AVAIL_SVC);
            break;
          default:
            //XXX: blow up?  should never happen
            log.error("Unable to set default metric interval: " +
                      "Unknown type: " + type);
        }
    }

    public MeasurementInfo[] getMeasurements(TypeInfo info) {
        if (this.data == null) {
            log.debug(getName() + " has no PluginData");
            return null;
        }

        List xmlMetrics = this.data.getMetrics(info.getName());
        
        if (xmlMetrics == null) {
            return null;
        }

        //append template-config if any
        String tmplConfig =
            this.data.getProperty(PROP_TEMPLATE_CONFIG);

        final String availCat = MeasurementConstants.CAT_AVAILABILITY;

        List metrics = new ArrayList(xmlMetrics.size());

        for (int i=0; i<xmlMetrics.size(); i++) {
            MeasurementInfo metric = (MeasurementInfo)xmlMetrics.get(i);
            //clone due to modifications made below and in generateRateMetric
            metric = (MeasurementInfo)metric.clone();
            metrics.add(metric);

            if (metric.getInterval() == -1) {
                if (metric.getCategory().equals(availCat)) {
                    setAvailInterval(info, metric);
                }
                else {
                    setInterval(metric);
                }
            }

            String template = metric.getTemplate();
            if (tmplConfig != null) {
                String ptqlDomain = SigarMeasurementPlugin.PTQL_DOMAIN;
                //XXX only case atm is mysql plugin defines template-config
                //but also has sigar.ptql metrics, which break if this is appended
                if (!template.startsWith(ptqlDomain)) {
                    metric.setTemplate(template + ":" + tmplConfig);
                    template = metric.getTemplate();
                }
            }

            //templates require the plugin name prefix
            String prefix = info.getName() + ":";
            if (!template.startsWith(prefix)) {
                metric.setTemplate(prefix + template);
            }
            
            //NOTE: this currently needs to happen here,
            //after the template-config has been appended.
            if ((metric = generateRateMetric(metric)) != null) {
                metrics.add(metric);
            }
        }

        MeasurementInfo[] infos = new MeasurementInfo[metrics.size()];
        metrics.toArray(infos);

        return infos;
    }

    private static final String[][] NO_PLATFORM_HELP_PROPS = 
        new String[0][0];

    protected String[][] getPlatformHelpProperties() {
        return NO_PLATFORM_HELP_PROPS;
    }

    String replaceHelpProperties(String help,
                                 TypeInfo info,
                                 Map props) {

        String platform = getHelpPlatform(info);

        TokenReplacer replacer = new TokenReplacer();

        if (info.getVersion() != null) {
            replacer.addFilter("product.version",
                               info.getVersion());
        }

        int ix;
        if (PlatformDetector.isWin32(platform)) {
            ix = PLATFORM_HELP_WIN32;
        }
        else {
            ix = PLATFORM_HELP_UNIX;
        }

        for (int i=0; i<PLATFORM_HELP_PROPS.length; i++) {
            replacer.addFilter(PLATFORM_HELP_PROPS[i][0],
                               PLATFORM_HELP_PROPS[i][ix]);
        }

        String[][] pluginProps = getPlatformHelpProperties();

        for (int i=0; i<pluginProps.length; i++) {
            replacer.addFilter(pluginProps[i][0],
                               pluginProps[i][ix]);
        }

        replacer.addFilters(props);

        // Add all <property> tags from hq-plugin.xml
        replacer.addFilters(getProperties());
        replacer.addFilters(getTypeProperties());

        return replacer.replaceTokens(help);
    }

    /**
     * If the MeasurementInfo collection type is "trendsup" and
     * rate attribute is not set to "none" then create a new metric:
     * - name changed to original name plus the rate.
     * - alias changed to original name plus the rate.
     * - collection type is changed to "dynamic"
     * - turn off designate for the original metric.
     * - append __RATE__=$rate to the measurement template.
     */
    private static MeasurementInfo generateRateMetric(MeasurementInfo info) {

        if (info.getCollectionType() !=
            MeasurementConstants.COLL_TYPE_TRENDSUP)
        {
            return null;
        }

        if (info.getRate().equals(MeasurementInfo.NO_RATE)) {
            return null;
        }

        MeasurementInfo rate = (MeasurementInfo)info.clone();
        if (info.isIndicator()) {
            info.setIndicator(false); //cant have more than one
        }
        if (info.isDefaultOn()) {
            info.setDefaultOn(false); //turn off raw counter by default
        }

        info = null; //just making sure we don't use this again.

        if ("".equals(rate.getRate())) {
            rate.setRate(MeasurementInfo.DEFAULT_RATE);
        }

        rate.setCollectionType(MeasurementConstants.COLL_TYPE_DYNAMIC);

        rate.setName(rate.getName() + " " + rate.getReadableRate());

        rate.setAlias(rate.getAlias() + rate.getRate());

        rate.setTemplate(rate.getTemplate() + 
                         MeasurementInfo.RATE_KEY + "=" +
                         rate.getRate());
        return rate;
    }
    
    protected String getPluginXMLHelp(TypeInfo info, String name, Map props) {
        if (this.data == null) {
            log.debug(getName() + " has no PluginData");
            return null;
        }

        String help = this.data.getHelp(name);

        if (help != null) {
            if (help.length() == 0) {
                //make sure UI doesnt anchor to help that does not exist
                return null;
            }
            return replaceHelpProperties(help, info, props);
        }

        return help;
    }

    private String getHelpPlatform(TypeInfo info) {
        String[] platforms;
        String platform = HELP_PLATFORM_ALL;

        if (info.getType() == TypeInfo.TYPE_PLATFORM) {
            platforms = new String[0];
        }
        else {
            platforms = info.getPlatformTypes();
        }
        if (platforms.length == 1) {
            platform = platforms[0];
        }

        return platform;
    }

    private String[] getHelpPlatformNames(TypeInfo info, String name) {
        String platform = getHelpPlatform(info);
        String[] names;

        if (platform.equals(HELP_PLATFORM_ALL)) {
            names = new String[] { name };
        }
        else {
            //look for platform specific help first, e.g. Win32
            names = new String[] {
                TypeBuilder.composePlatformTypeName(name, platform),
                name
            };
        }

        return names;
    }

    private String[] getHelpPlatformNames(TypeInfo info) {
        return getHelpPlatformNames(info, info.getName());
    }

    public String getHelp(TypeInfo info, Map props) {
        String[] names = getHelpPlatformNames(info);

        for (int i=0; i<names.length; i++) {
            String help = getPluginXMLHelp(info, names[i], props);

            if (help != null) {
                return help;
            }
        }

        return null;
    }

    /**
     * This method is called when the plugin is asked for a 
     * metric value.  The Metric is a translated value as returned
     * by the getMeasurements() routine, and then run through the
     * translate() method.
     * 
     * @param metric Value returned from translate(), representing a
     *            specific metric to retrieve
     *
     * @return The value of the Metric and timestamp of collection time
     *
     * @throws MetricInvalidException The plugin is unable to use the metric,
     * generally a developer bug where the template is malformed.
     * I.e. JMX MalformedObjectNameException
     *
     * @throws MetricNotFoundException The monitored resource does not know
     * about the requested Metric.  I.e. JMX AttributeNotFoundException
     *                                   
     * @throws MetricUnreachableException The monitored resource is unreachable.
     * I.e. ConnectException
     *
     * @throws PluginException Thrown when an internal plugin error occurs
     */
    public MetricValue getValue(Metric metric)
        throws PluginException, MetricNotFoundException,
               MetricUnreachableException {

        //if we reach this point, this resource type
        //must have a <plugin type="collector" ...> defined.
        return Collector.getValue(this, metric);
    }

    public Collector getNewCollector() {
        if (this.collector == null) {
            if (this.data != null) {
                String name =
                    this.data.getPlugin(TYPE_COLLECTOR,
                                        getTypeInfo().getName());

                if (name == null) {
                    String msg =
                        "No measurement plugin or collector defined for: " +
                        getTypeInfo().getName();
                    throw new MetricInvalidException(msg);                    
                }

                this.collector =
                    ProductPlugin.getPluginClass(this.getClass().getClassLoader(),
                                                 this.data,
                                                 name,
                                                 getTypeInfo().getName());
                if (this.collector == null) {
                    String msg =    
                        "Class '" + name +
                        "' NotFound using ClassLoader=" +
                        this.data.getClassLoader(); 
                    throw new MetricInvalidException(msg);
                }
            }
        }

        try {
            return (Collector)this.collector.newInstance();
        } catch (Exception e) {
            throw new MetricInvalidException(e.getMessage());
        }
    }

    public Properties getCollectorProperties(Metric metric) {
        return metric.getObjectProperties();
    }

    /**
     * Translate a measurement as returned from getMeasurements() into a 
     * value which can be passed into the plugin's getValue() routine.
     * 
     * @param template Measurement template from one of the plugins
     *                 measurements returned from getMeasurements()
     * @param config   Configuration used to perform translation on the 
     *                 template
     * 
     * @throws PluginException When an internal plugin error occurs
     * @throws MetricInvalidException        When the template passed cannot
     *                                    be mapped to a template returned
     *                                    via getMeasurements()
     */
    public String translate(String template, ConfigResponse config){
        TokenReplacer replacer = new TokenReplacer();

        Map props = getMeasurementProperties();
        if (props != null) {
            replacer.addFilters(props);
        }

        replacer.addFilters(PluginData.getGlobalProperties());
        template = replacer.replaceTokens(template);
        
        return Metric.translate(template, config);
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        if (this.data != null) {
            ConfigSchema schema =
                this.data.getConfigSchema(info,
                                          ProductPlugin.CFGTYPE_IDX_MEASUREMENT);
            if (schema != null) {
                return schema;
            }
        }
        return super.getConfigSchema(info, config);
    }
}
