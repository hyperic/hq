/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.gfee.detector;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.GFProductPlugin;
import org.hyperic.hq.plugin.gfee.GFVersionInfo;
import org.hyperic.hq.plugin.gfee.cache.MemberCache;
import org.hyperic.hq.plugin.gfee.cache.MemberInfo;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;
import org.hyperic.hq.plugin.gfee.util.GFMXUtils;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Class MemberDetector.
 * 
 */
public abstract class MemberDetector extends ServerDetector implements AutoServerDetector {

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(MemberDetector.class);

    /** Prefix for property key for globals */
    public static final String PROP_PREFIX = "GemFire.autodiscovery.";

    /** Prop string from includes */
    public static final String PROP_INCLUDES = "includeInstances";

    /** Prop string for excludes */
    public static final String PROP_EXCLUDES = "excludeInstances";

    public static final String[] DEFAULT_INCLUDES = {
        "GemFire Cache Server 6.6 Cache Performance",
        "GemFire Cache Server 6.6 Cache Server",
        "GemFire Cache Server 6.6 Disk Store",
        "GemFire Cache Server 6.6 Function",
        "GemFire Cache Server 6.6 Function Service",
        "GemFire Cache Server 6.6 Partitioned Region",
        "GemFire Cache Server 6.6 Region",
        "GemFire Cache Server 6.6 Resource Manager",
        "GemFire Cache Server 6.6 VM Stats",
        "GemFire Gateway Hub 6.6 Cache Performance",
        "GemFire Gateway Hub 6.6 Cache Server",
        "GemFire Gateway Hub 6.6 Disk Store",
        "GemFire Gateway Hub 6.6 Gateway Hub Statistics",
        "GemFire Gateway Hub 6.6 Gateway Statistics",
        "GemFire Gateway Hub 6.6 Partitioned Region",
        "GemFire Gateway Hub 6.6 Region",
        "GemFire Gateway Hub 6.6 VM Stats",
        "GemFire Gateway Hub 6.6 Pool Stats",
        "GemFire Cache Server 6.5 Cache Performance",
        "GemFire Cache Server 6.5 Cache Server",
        "GemFire Cache Server 6.5 Disk Store",
        "GemFire Cache Server 6.5 Function",
        "GemFire Cache Server 6.5 Function Service",
        "GemFire Cache Server 6.5 Partitioned Region",
        "GemFire Cache Server 6.5 Region",
        "GemFire Cache Server 6.5 Resource Manager",
        "GemFire Cache Server 6.5 VM Stats",
        "GemFire Gateway Hub 6.5 Cache Performance",
        "GemFire Gateway Hub 6.5 Cache Server",
        "GemFire Gateway Hub 6.5 Disk Store",
        "GemFire Gateway Hub 6.5 Gateway Hub Statistics",
        "GemFire Gateway Hub 6.5 Gateway Statistics",
        "GemFire Gateway Hub 6.5 Partitioned Region",
        "GemFire Gateway Hub 6.5 Region",
        "GemFire Gateway Hub 6.5 VM Stats",
        "GemFire Gateway Hub 6.5 Pool Stats",
        "GemFire Cache Server 6.0 Cache Performance",
        "GemFire Cache Server 6.0 Cache Server",
        "GemFire Cache Server 6.0 Disk Store",
        "GemFire Cache Server 6.0 Function",
        "GemFire Cache Server 6.0 Function Service",
        "GemFire Cache Server 6.0 Partitioned Region",
        "GemFire Cache Server 6.0 Region",
        "GemFire Cache Server 6.0 Resource Manager",
        "GemFire Cache Server 6.0 VM Stats",
        "GemFire Gateway Hub 6.0 Cache Performance",
        "GemFire Gateway Hub 6.0 Cache Server",
        "GemFire Gateway Hub 6.0 Disk Store",
        "GemFire Gateway Hub 6.0 Gateway Hub Statistics",
        "GemFire Gateway Hub 6.0 Gateway Statistics",
        "GemFire Gateway Hub 6.0 Partitioned Region",
        "GemFire Gateway Hub 6.0 Region",
        "GemFire Gateway Hub 6.0 VM Stats",
        "GemFire Gateway Hub 6.5 Pool Stats",
        "GemFire Application Peer 6.0 Distribution Statistics",
        "GemFire Application Peer 6.0 Function",
        "GemFire Application Peer 6.0 Function Service",
        "GemFire Application Peer 6.0 VM Stats",
        "GemFire Application Peer 6.5 Distribution Statistics",
        "GemFire Application Peer 6.5 Function",
        "GemFire Application Peer 6.5 Function Service",
        "GemFire Application Peer 6.5 VM Stats",
        "GemFire Application Peer 6.6 Distribution Statistics",
        "GemFire Application Peer 6.6 Function",
        "GemFire Application Peer 6.6 Function Service",
        "GemFire Application Peer 6.6 VM Stats"
    };

    public static final String[] DEFAULT_EXCLUDES = {
        "GemFire Cache Server 6.6 Cache Client Notifier",
        "GemFire Cache Server 6.6 Disk Directory",
        "GemFire Cache Server 6.6 Disk Region",
        "GemFire Cache Server 6.6 Distributed Lock",
        "GemFire Cache Server 6.6 Distribution Statistics",
        "GemFire Cache Server 6.6 Statistics Sampler",
        "GemFire Gateway Hub 6.6 Cache Client Notifier",
        "GemFire Gateway Hub 6.6 Disk Directory",
        "GemFire Gateway Hub 6.6 Disk Region",
        "GemFire Gateway Hub 6.6 Distributed Lock",
        "GemFire Gateway Hub 6.6 Distribution Statistics",
        "GemFire Gateway Hub 6.6 Function",
        "GemFire Gateway Hub 6.6 Function Service",
        "GemFire Gateway Hub 6.6 Resource Manager",
        "GemFire Cache Server 6.5 Cache Client Notifier",
        "GemFire Cache Server 6.5 Disk Directory",
        "GemFire Cache Server 6.5 Disk Region",
        "GemFire Cache Server 6.5 Distributed Lock",
        "GemFire Cache Server 6.5 Distribution Statistics",
        "GemFire Cache Server 6.5 Statistics Sampler",
        "GemFire Gateway Hub 6.5 Cache Client Notifier",
        "GemFire Gateway Hub 6.5 Disk Directory",
        "GemFire Gateway Hub 6.5 Disk Region",
        "GemFire Gateway Hub 6.5 Distributed Lock",
        "GemFire Gateway Hub 6.5 Distribution Statistics",
        "GemFire Gateway Hub 6.5 Function",
        "GemFire Gateway Hub 6.5 Function Service",
        "GemFire Gateway Hub 6.5 Resource Manager",
        "GemFire Cache Server 6.0 Cache Client Notifier",
        "GemFire Cache Server 6.0 Disk Directory",
        "GemFire Cache Server 6.0 Disk Region",
        "GemFire Cache Server 6.0 Distributed Lock",
        "GemFire Cache Server 6.0 Distribution Statistics",
        "GemFire Cache Server 6.0 Statistics Sampler",
        "GemFire Gateway Hub 6.0 Cache Client Notifier",
        "GemFire Gateway Hub 6.0 Disk Directory",
        "GemFire Gateway Hub 6.0 Disk Region",
        "GemFire Gateway Hub 6.0 Distributed Lock",
        "GemFire Gateway Hub 6.0 Distribution Statistics",
        "GemFire Gateway Hub 6.0 Function",
        "GemFire Gateway Hub 6.0 Function Service",
        "GemFire Gateway Hub 6.0 Resource Manager"
    };

    /** Includes */
    private String[] includes = null;
    
    /** Excludes */
    private String[] excludes = null;

    /**
     * Handling setting from init method.
     * 
     * @see org.hyperic.hq.product.ServerDetector#init(org.hyperic.hq.product.PluginManager)
     */
    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        initSettings(manager);
    }
    
    /**
     * Init extra settings.
     * 
     * @param manager Plugin Manager
     */
    private void initSettings(PluginManager manager) {
        includes = getSetting(manager.getProperties(), PROP_PREFIX + PROP_INCLUDES);
        excludes = getSetting(manager.getProperties(), PROP_PREFIX + PROP_EXCLUDES);

        // do not use exclude defaults if user
        // only defined includes.
        if(excludes.length == 0 && includes.length != 0)
            excludes = new String[0];
        else if(excludes.length == 0 && includes.length == 0)
            excludes = DEFAULT_EXCLUDES;
        
        if(includes.length == 0)
            includes = DEFAULT_INCLUDES;
        
        if(includes.length == 1 && includes[0].equals("all"))
            includes = excludes = new String[0];
    }

    
    /* (non-Javadoc)
     * @see org.hyperic.hq.product.AutoServerDetector#getServerResources(org.hyperic.util.config.ConfigResponse)
     */
    public List<ServerResource> getServerResources(ConfigResponse config) throws PluginException {

        if(log.isDebugEnabled()) {
            log.debug("Detecting server resources for: " + getTypeInfo().getFormattedName() + " " + getTypeInfo().getVersion());
            log.debug("Config used for detection: " + config);    
        }

        List<ServerResource> servers = new ArrayList<ServerResource>();

        // if jmx url doesn't exist, just bail out with empty server set
        if(config.getValue(GFMXConstants.CONF_JMX_URL) == null)
            return servers;

        GFJmxConnection gf = new GFJmxConnection(config);

        GFVersionInfo gfVersionInfo = gf.getVersionInfoFromAgent();
        if(gfVersionInfo == null)
            return servers;
        if(!gfVersionInfo.isGFVersion(getTypeInfo().getVersion()))
            return servers;

        GFProductPlugin master = (GFProductPlugin)getProductPlugin();
        MemberCache memberCache = master.getMemberCache(config.getValue(GFMXConstants.CONF_JMX_URL));
        memberCache.refreshCacheVMs(config);
        memberCache.refreshPeers(config);

        Set<MemberInfo> members = memberCache.getMembers();
        for (MemberInfo memberInfo : members) {

            int mask = gf.getMemberRoles(memberInfo.getGfid());

            if(hasCorrectRoles(mask)) {
                
                ServerResource server = createServerResource(memberInfo.getWorkingDirectory());
                server.setName(getTypeInfo().getName() + " " + memberInfo.getName());
                server.setIdentifier(memberInfo.getWorkingDirectory()+memberInfo.getHost()+memberInfo.getName());
                ConfigResponse c = new ConfigResponse();
                c.setValue(GFMXConstants.ATTR_HOST, memberInfo.getHost());
                c.setValue(GFMXConstants.ATTR_NAME, memberInfo.getName());
                c.setValue(GFMXConstants.ATTR_PWD, memberInfo.getWorkingDirectory());
                setMeasurementConfig(server, new ConfigResponse());
                setProductConfig(server, c);
                servers.add(server);
            }

        }

        return servers;
    }
    
    protected abstract boolean hasCorrectRoles(int mask);
    
    /* (non-Javadoc)
     * @see org.hyperic.hq.product.ServerDetector#discoverServices(org.hyperic.util.config.ConfigResponse)
     */
    @Override
    protected List<ServiceResource> discoverServices(ConfigResponse config)
    throws PluginException {
        List<ServiceResource> services = new ArrayList<ServiceResource>();

        GFJmxConnection gf = new GFJmxConnection(config);

        GFProductPlugin master = (GFProductPlugin)getProductPlugin();
        
        MemberCache memberCache = master.getMemberCache(config.getValue(GFMXConstants.CONF_JMX_URL));

        String workingDirectory = config.getValue(GFMXConstants.ATTR_PWD);
        String host = config.getValue(GFMXConstants.ATTR_HOST);
        String name = config.getValue(GFMXConstants.ATTR_NAME);

        Object[][] statObjectsWithType = gf.getStatObjectsWithType(memberCache.getGfid(workingDirectory, host, name));

        StatType[] filtered = filterSupportedStats(statObjectsWithType, config);

        for (StatType statType : filtered) {
            services.add(createService(config, statType, name));
        }
        
        // last add VM Stats
        // we need to iterate through stats, find correct names for gc's and memory pools.
        services.add(createVMStatsService(name, statObjectsWithType));

        return filterIncludesExcludes(services);
    }
    //serviceTypeName
    protected List<ServiceResource> filterIncludesExcludes(List<ServiceResource> services) {
        
        List<ServiceResource> filtered = new ArrayList<ServiceResource>();
        for (ServiceResource serviceResource : services) {
            String serviceTypeName = serviceResource.getType();
            boolean add = false;
            if(includes.length == 0 || ArrayUtil.exists(includes, serviceTypeName))
                add = true;
            if(add && ArrayUtil.exists(excludes, serviceTypeName))
                add = false;
            if(add)
                filtered.add(serviceResource);
        }
        return filtered;
    }

    /**
     * Filter supported stats.
     * 
     * Implementor is responsible to filter supported statistics aka only include
     * stats from jmx which are used.
     *
     * @param statObjects the stat objects
     * @param config the config
     * @return Statistic types
     */
    protected abstract StatType[] filterSupportedStats(Object[][] statObjects, ConfigResponse config);

    /**
     * Creates the service.
     *
     * @param config the config
     * @param typename the typename
     * @param name the name
     * @return the service resource
     */
    private ServiceResource createService(ConfigResponse config, StatType statType, String name){
        ServiceResource service = createServiceResource(statType.type);

        service.setName(name + " " + statType.type + (statType.postfix != null ? " " + statType.postfix : ""));
        ConfigResponse c = new ConfigResponse();
        c.setValue(GFMXConstants.CONF_STATNAME, statType.objectName.getKeyProperty(GFMXConstants.ATTR_NAME));

        setMeasurementConfig(service, new ConfigResponse());
        setProductConfig(service, c);

        return service;
    }

    private ServiceResource createVMStatsService(String name, Object[][] statObjectsWithType){
        ServiceResource service = createServiceResource("VM Stats");

        service.setName(name + " VM Stats");
        
        ConfigResponse c = new ConfigResponse();

        for (int i = 0; i < statObjectsWithType.length; i++) {
            ObjectName o = (ObjectName)statObjectsWithType[i][0];
            String oName = o.getKeyProperty("name");
            String olName = oName.toLowerCase();
            String type = (String)statObjectsWithType[i][1];
            if(type.equals("VMGCStats")) {
                if(olName.contains("sweep"))
                    c.setValue("gcexpensive", oName);
                else
                    c.setValue("gccheap", oName);
            } else if(type.equals("VMMemoryPoolStats")) {
                if(olName.contains("eden"))
                    c.setValue("youngeden", oName);
                else if(olName.contains("survivor"))
                    c.setValue("youngsurvivor", oName);
                else if(olName.contains("old") || olName.contains("tenured"))
                    c.setValue("oldtenured", oName);
                else if(olName.contains("perm gen"))
                    c.setValue("oldpermanent", oName);
                else if(olName.contains("cache"))
                    c.setValue("codecache", oName);
            }
        }
        c.setValue("vmstats", "vmStats");
        c.setValue("vmheapmemorystats", "vmHeapMemoryStats");
        c.setValue("vmnonheapmemorystats", "vmNonHeapMemoryStats");
        
        setMeasurementConfig(service, new ConfigResponse());
        setProductConfig(service, c);

        return service;
    }

    /**
     * The Class StatType.
     * 
     * Helper object.
     */
    class StatType {
        String type;
        ObjectName objectName;
        String postfix;
        public StatType(String type, ObjectName objectName, String postfix) {
            this.type = type;
            this.objectName = objectName;
            this.postfix = postfix;
        }
        public StatType(String type, ObjectName objectName) {
            this(type, objectName, null);
        }
    }

    /**
     * Helper function to copy settings.
     * 
     * @param props
     * @param array
     * @param key
     */
    private String[] getSetting(Properties props, String key) {
        Set<String> values = new HashSet<String>();
        
        Enumeration<?> e = props.keys();
        while(e.hasMoreElements()) {
            String k = (String)e.nextElement();
            if(k.startsWith(key))
                values.add((String)props.getProperty(k));
        }
        
        if(values.size() > 0)
            return (String[]) values.toArray(new String[values.size()]);
        else
            return new String[0];
    }

    public String[] getIncludes() {
        return includes;
    }

    public String[] getExcludes() {
        return excludes;
    }

    
}
