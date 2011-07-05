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
package org.hyperic.hq.plugin.gfee.mx;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.cache.MemberInfo;
import org.hyperic.hq.plugin.gfee.util.GFMXUtils;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Class AbstractGFJmxConnection is handling basic
 * jmx connection towards Gemfire jmx endpoint. We always
 * connect to Gemfire jmx agent which is responsible to
 * manage the whole Gemfire distributed system.
 * 
 * Some of the MBean are not visible unless certain operations
 * are not invoked. For example the AdminDistrubutedSystemMBean
 * becomes visible after we asks GemFireAgentMBean to connect
 * to the distributed system. The same logic goes through
 * every Gemfire MBeans respectively.
 * 
 */
public abstract class AbstractGFJmxConnection {

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(AbstractGFJmxConnection.class);

    /** The connection properties. */
    protected Properties props;

    /** The admin distributed system mbean. */
    private ObjectName adminDistributedSystemMBean;

    /**
     * Instantiates a new gemfire jmx connection.
     *
     * @param props the props
     */
    public AbstractGFJmxConnection(Properties props) {
        this.props = props;
    }

    /**
     * Instantiates a new gF jmx connection.
     *
     * @param config the config
     */
    public AbstractGFJmxConnection(ConfigResponse config) {
        this.props = config.toProperties();
    }

    /**
     * Instantiates a new gF jmx connection.
     *
     * @param url the url
     * @param user the user
     * @param password the password
     */
    public AbstractGFJmxConnection(String url, String user, String password) {
        Properties p = getProperties();
        if(url != null) p.put(MxUtil.PROP_JMX_URL, url);
        if(user != null) p.put(MxUtil.PROP_JMX_USERNAME, user);
        if(password != null) p.put(MxUtil.PROP_JMX_PASSWORD, password);
    }

    /**
     * Instantiates a new gF jmx connection.
     *
     * @param url the url
     */
    public AbstractGFJmxConnection(String url) {
        this(url, null, null);
    }

    /**
     * Instantiates a new abstract gf jmx connection.
     */
    private AbstractGFJmxConnection() {
        // No instantiation without properties
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    protected Properties getProperties(){
        if(props == null)
            props = new Properties();
        return props;
    }

    /**
     * Connect JMX agent to system.
     * 
     * After GemFire agent has been started, it doesn't know
     * anything about the distributed system unless we specifically
     * tell it to connect to the system. This function does that
     * and only that.
     * 
     * We're using ObjectName 'GemFire:type=Agent' and
     * invoking 'connectToSystem' on it.
     *
     * @return ObjectName of the distributed system, null if connection
     *         failed or some other error happened.
     */
    protected ObjectName connectToSystem(){
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName gemFireAgentMBean = new ObjectName(GFMXConstants.AGENT_OBJ_NAME);
            adminDistributedSystemMBean = 
                (ObjectName) mServer.invoke(gemFireAgentMBean,
                        GFMXConstants.AGENT_OP_CONNECTTOSYSTEM,
                        GFMXUtils.EMPTY_ARGS,
                        GFMXUtils.EMPTY_DEF);
            if(log.isDebugEnabled())
                log.debug("Found distributed system: " + adminDistributedSystemMBean);
            return adminDistributedSystemMBean;
        } catch (Exception e) {
            log.debug("Error to connect jmx agent to system: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Manage cache servers.
     * 
     * To find and let jmx agent to manage cache servers we need to do
     * additional call to distributed system MBean. ObjectName is
     * something like this where id is retrieved from call to
     * connectToSystem:
     * GemFire:type=AdminDistributedSystem,id=localhost[10335] 
     *
     * @return List of ObjectNames which describe managed
     *         cache servers, null if error happened.
     */
    protected ObjectName[] manageCacheServers(){
        // TODO: cache result from connectToSystem
        connectToSystem();
        String dsName = GFMXUtils.getId(adminDistributedSystemMBean);
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = new ObjectName(GFMXConstants.ADS_OBJ_NAME);
            ObjectName[] systemMemberMBean = 
                (ObjectName[]) mServer.invoke(GFMXUtils.combine(bean, GFMXUtils.KEY_ID, dsName),
                        GFMXConstants.ADS_OP_MANAGECACHESERVERS,
                        GFMXUtils.EMPTY_ARGS,
                        GFMXUtils.EMPTY_DEF);
            return systemMemberMBean;
        } catch (Exception e) {
            log.debug("Error requesting cache servers: " + e.getMessage(), e);
        }
        return null;
    }

    protected ObjectName[] manageSystemMembersApplications(){
        // TODO: cache result from connectToSystem
        connectToSystem();
        String dsName = GFMXUtils.getId(adminDistributedSystemMBean);
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = new ObjectName(GFMXConstants.ADS_OBJ_NAME);
            ObjectName[] systemMemberMBean = 
                (ObjectName[]) mServer.invoke(GFMXUtils.combine(bean, GFMXUtils.KEY_ID, dsName),
                        GFMXConstants.ADS_OP_MANAGESYSTEMMEMBERAPPLICATIONS,
                        GFMXUtils.EMPTY_ARGS,
                        GFMXUtils.EMPTY_DEF);
            return systemMemberMBean;
        } catch (Exception e) {
            log.debug("Error requesting system member applications: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Manage and find statistics for system member.
     * 
     * System member has it's on ObjectName:
     * GemFire.CacheVm:id=127.0.0.1(25584)<v16>-20636/52014,type=CacheVm
     * 
     * Call to 'manageStats' method on this MBean subscribes agent to
     * system statistics and returns ObjectNames which contains supported
     * statistics. Format of the returned ObjectName is something similar
     * seen below:
     * GemFire.Statistic:source=127.0.0.1(25584)<v16>-20636/52014,
     *                   uid=1,
     *                   name=distributionStats
     *
     *
     * @param id The id of the system member 
     * @return List of statistics ObjectNames, null if error happened.
     */
    protected ObjectName[] manageCacheVMStats(String id){

        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = new ObjectName(GFMXConstants.CACHEVM_OBJ_NAME);
            ObjectName[] statMBean = 
                (ObjectName[]) mServer.invoke(GFMXUtils.combine(bean, GFMXUtils.KEY_ID, id),
                        GFMXConstants.CACHEVM_OP_MANAGESTATS,
                        GFMXUtils.EMPTY_ARGS,
                        GFMXUtils.EMPTY_DEF);
            return statMBean;
        } catch (Exception e) {
            log.debug("Error requesting statistics mbeans: " + e.getMessage(), e);
        }
        return null;
    }

    protected ObjectName[] manageApplicationStats(String id){

        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = new ObjectName(GFMXConstants.APPLICATION_OBJ_NAME);
            ObjectName[] statMBean = 
                (ObjectName[]) mServer.invoke(GFMXUtils.combine(bean, GFMXUtils.KEY_ID, id),
                        GFMXConstants.APPLICATION_OP_MANAGESTATS,
                        GFMXUtils.EMPTY_ARGS,
                        GFMXUtils.EMPTY_DEF);
            return statMBean;
        } catch (Exception e) {
            log.debug("Error requesting statistics mbeans: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Manage caches.
     *
     * @return the object name[]
     */
    protected ObjectName[] manageCaches() {
        ArrayList<ObjectName> caches = new ArrayList<ObjectName>();
        ObjectName[] cacheServers = manageCacheServers();
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);

            for (ObjectName objectName : cacheServers) {
                ObjectName cacheMBean =
                    (ObjectName) mServer.invoke(objectName,
                            GFMXConstants.CACHEVM_OP_MANAGECACHE,
                            GFMXUtils.EMPTY_ARGS,
                            GFMXUtils.EMPTY_DEF);
                caches.add(cacheMBean);
            }
            return caches.toArray(new ObjectName[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    /**
     * 
     * @param cacheVms
     * @return
     */
    protected ObjectName[] manageCaches(ObjectName[] cacheVms) {
        ArrayList<ObjectName> caches = new ArrayList<ObjectName>();
        //		ObjectName[] cacheServers = manageCacheServers();
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);

            for (ObjectName objectName : cacheVms) {
                ObjectName cacheMBean =
                    (ObjectName) mServer.invoke(objectName,
                            GFMXConstants.CACHEVM_OP_MANAGECACHE,
                            GFMXUtils.EMPTY_ARGS,
                            GFMXUtils.EMPTY_DEF);
                caches.add(cacheMBean);
            }
            return caches.toArray(new ObjectName[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    /**
     * 
     * @param cacheVm
     * @return
     */
    public ObjectName manageCache(ObjectName cacheVm) {
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);

            ObjectName cacheMBean =
                (ObjectName) mServer.invoke(cacheVm,
                        GFMXConstants.CACHEVM_OP_MANAGECACHE,
                        GFMXUtils.EMPTY_ARGS,
                        GFMXUtils.EMPTY_DEF);
            return cacheMBean;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }


    /**
     * 
     * @param cache
     * @return
     */
    public ObjectName[] manageRegions(ObjectName cache) {
        ArrayList<ObjectName> regions = new ArrayList<ObjectName>();
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);

            Set<String> rootRegions = (Set<String>) mServer.getAttribute(cache, "rootRegionNames");

            for (String s : rootRegions) {
                log.debug("Found root region: " + s);
                ObjectName region = 
                    (ObjectName) mServer.invoke(cache,
                            "manageRegion",
                            new Object[]{s},
                            new String[]{"java.lang.String"});
                regions.add(region);
                manageSubRegions(region, cache, regions);
            }
            return regions.toArray(new ObjectName[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 
     * @param region
     * @param cache
     * @param regions
     */
    public void manageSubRegions(ObjectName region, ObjectName cache, ArrayList<ObjectName> regions) {
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            Set<String> subRegions = (Set<String>) mServer.getAttribute(region, "subregionFullPaths");

            for (String s : subRegions) {
                log.debug("Found sub region: " + s);
                ObjectName subRegion = 
                    (ObjectName) mServer.invoke(cache,
                            "manageRegion",
                            new Object[]{s},
                            new String[]{"java.lang.String"});
                regions.add(subRegion);
                manageSubRegions(subRegion, cache, regions);				
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Gets the stats.
     *
     * This function is accessing MBean e.g. using below ObjectName:
     * GemFire.Statistic:source=127.0.0.1(25584)<v16>-20636/52014,
     *                   uid=1,
     *                   name=distributionStats
     *                   
     * Statistics can be found from MBean attributes. We're only
     * interested with attributes which represent itself as a
     * java.lang.Number. 
     *                   
     * @param id the id of system member
     * @param name the name of statistics
     * @return the stats
     */
    protected Map<String, Double>getStats(MemberInfo member, String name) {

        // if member was restarted, we need to manage stats
        // before those are visible through jmx
        if(member.isStatisticsDirty()) {
            
            ObjectName[] statObjects = null;
            if(member.getMemberType() == MemberInfo.MEMBER_TYPE_CACHEVM) {
                statObjects = manageCacheVMStats(member.getGfid());                
            } else if(member.getMemberType() == MemberInfo.MEMBER_TYPE_APPLICATION) {
                statObjects = manageApplicationStats(member.getGfid());
            }
            
            if(statObjects != null)
                member.setStatisticsDirty(false);
            else
                return null;            
        }

        // TODO: make this function pretty
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = new ObjectName(GFMXConstants.STATS_OBJ_NAME+"source="+member.getGfid()+",name="+name+",*");


            Set<ObjectInstance> res = mServer.queryMBeans(bean, null);
            if(res.size() == 0)
                return null;
            ObjectInstance ins = (ObjectInstance)res.toArray()[0];
            MBeanInfo info = mServer.getMBeanInfo(ins.getObjectName());

            refreshStatistics(ins.getObjectName());

            MBeanAttributeInfo[] atts = info.getAttributes();

            ArrayList<String> l = new ArrayList<String>();

            for (int i = 0; i < atts.length; i++) {
                MBeanAttributeInfo mBeanAttributeInfo = atts[i];
                l.add(mBeanAttributeInfo.getName());
            }

            String[] nameArray = l.toArray(new String[0]);
            AttributeList attlist = mServer.getAttributes(ins.getObjectName(), nameArray);

            Map<String, Double> map = new Hashtable<String, Double>();

            for(int i = 0; i < attlist.size(); i++) {
                Attribute o = (Attribute)attlist.get(i);
                if(o.getValue() instanceof Number) {
                    map.put(o.getName(), ((Number)o.getValue()).doubleValue());                 
                }
            }
            return map;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    protected Map<String, Double>getStats(MemberInfo member, String name, String[] keys) {

        Map<String, Double> map = new Hashtable<String, Double>();

        if(member.isStatisticsDirty()) {
            
            ObjectName[] statObjects = null;
            if(member.getMemberType() == MemberInfo.MEMBER_TYPE_CACHEVM) {
                statObjects = manageCacheVMStats(member.getGfid());                
            } else if(member.getMemberType() == MemberInfo.MEMBER_TYPE_APPLICATION) {
                statObjects = manageApplicationStats(member.getGfid());
            }
            
            if(statObjects != null) {
                member.setStatisticsDirty(false);                
            } else {
                if(log.isDebugEnabled())
                    log.debug("Can't manage stats MBean, returning empty map.");
                return map;                            
            }
        }

        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = new ObjectName(GFMXConstants.STATS_OBJ_NAME+"source="+member.getGfid()+",name="+name+",*");

            Set<ObjectInstance> res = mServer.queryMBeans(bean, null);
            if(res.size() == 0) {
                if(log.isDebugEnabled())
                    log.debug("Can't query stats MBean, returning empty map. Bean is: " + bean != null ? bean.getCanonicalName() : "null");
                return map;
            }
            ObjectInstance ins = (ObjectInstance)res.toArray()[0];

            // need to call refresh() on mbean to get updated values
            refreshStatistics(ins.getObjectName());

            AttributeList attlist = mServer.getAttributes(ins.getObjectName(), keys);

            for(int i = 0; i < attlist.size(); i++) {
                Attribute o = (Attribute)attlist.get(i);
                if(o.getValue() instanceof Number) {
                    map.put(o.getName(), ((Number)o.getValue()).doubleValue());
                }
            }
            return map;

        } catch (Exception e) {
            log.info("Unable to collect stats." + e.getMessage());
            log.debug(e.getMessage(), e);
        }

        return null;
    }

    
    protected boolean refreshStatistics(ObjectName statMBean){
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
                mServer.invoke(statMBean,
                        GFMXConstants.STATS_OP_REFRESH,
                        GFMXUtils.EMPTY_ARGS,
                        GFMXUtils.EMPTY_DEF);
            return true;
        } catch (Exception e) {
            log.debug("Error updating stats for mbean:" + statMBean, e);
        }
        return false;
    }


    protected ObjectName getCacheServerObject(String gfid) {
        ObjectName bean = null;
        try {
            bean = new ObjectName(GFMXConstants.CACHEVM_OBJ_NAME + ",id=" + gfid);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return bean;
    }

    protected ObjectName getApplicationObject(String gfid) {
        ObjectName bean = null;
        try {
            bean = new ObjectName(GFMXConstants.APPLICATION_OBJ_NAME + ",id=" + gfid);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return bean;
    }

    protected String getAgentVersionString() {
        String version = "";
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName gemFireAgentMBean = new ObjectName(GFMXConstants.AGENT_OBJ_NAME);
            version = (String)mServer.getAttribute(gemFireAgentMBean, "version");
        } catch (MalformedURLException mue) {
            log.debug("Can't use given jmx url", mue);
        } catch (Exception e) {
            log.debug("General error talking to jmx agent", e);
        }
        return version;
    }

}
