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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.GFVersionInfo;
import org.hyperic.hq.plugin.gfee.cache.MemberInfo;
import org.hyperic.hq.plugin.gfee.util.GFMXUtils;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;


/**
 * The Class GFJmxConnection.
 */
public class GFJmxConnection extends AbstractGFJmxConnection {

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(GFJmxConnection.class);

    /**
     * Instantiates a new gF jmx connection.
     *
     * @param config the config
     */
    public GFJmxConnection(ConfigResponse config) {
        super(config);
    }


    /**
     * 
     * @param props
     */
    public GFJmxConnection(Properties props) {
        super(props);
    }


    /**
     * Gets the cache servers.
     *
     * @return the cache servers
     */
    public String[] getCacheServers(){
        ArrayList<String> list = new ArrayList<String>();
        ObjectName[] mServers = manageCacheServers();
        for (ObjectName objectName : mServers) {
            list.add(GFMXUtils.getId(objectName));
        }
        return list.toArray(new String[0]);
    }

    /**
     * Gets the cache vm attributes.
     *
     * @return the cache vm attributes
     */
    public Map<String, String[]> getCacheVmsAttributes(){

        Map<String, String[]> map = new Hashtable<String, String[]>();

        ObjectName[] mServers = manageCacheServers();
        for (ObjectName objectName : mServers) {
            try {
                MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
                String cwd = (String) mServer.getAttribute(objectName, "workingDirectory");
                String name = (String) mServer.getAttribute(objectName, "name");
                String host = (String) mServer.getAttribute(objectName, "host");
                String[] atts = {cwd,host,name};
                map.put(GFMXUtils.getId(objectName),atts);

                if(log.isDebugEnabled()) {
                    log.debug("id: " + objectName.getCanonicalName());
                    log.debug("cwd: " + cwd);
                    log.debug("name: " + name);
                    log.debug("host: " + host);					
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    public Map<String, Object> getCacheVmAttributes(String gfid, String[] keys) throws InstanceNotFoundException{

        Map<String, Object> map = new Hashtable<String, Object>();

        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = getCacheServerObject(gfid);
            AttributeList attlist = mServer.getAttributes(bean, keys);

            for(int i = 0; i < attlist.size(); i++) {
                Attribute o = (Attribute)attlist.get(i);
                map.put(keys[i], o.getValue());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        return map;
    }

    public Map<String, Object> getApplicationAttributes(String gfid, String[] keys) throws InstanceNotFoundException{

        Map<String, Object> map = new Hashtable<String, Object>();

        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName bean = getApplicationObject(gfid);
            AttributeList attlist = mServer.getAttributes(bean, keys);

            for(int i = 0; i < attlist.size(); i++) {
                Attribute o = (Attribute)attlist.get(i);
                map.put(keys[i], o.getValue());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        return map;
    }


    public MemberInfo[] getCacheVmMembers(){

        List<MemberInfo> members = new ArrayList<MemberInfo>();

        ObjectName[] mServers = manageCacheServers();
        for (ObjectName objectName : mServers) {
            try {
                MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
                String cwd = (String) mServer.getAttribute(objectName, "workingDirectory");
                String name = (String) mServer.getAttribute(objectName, "name");
                String host = (String) mServer.getAttribute(objectName, "host");
                String[] atts = {cwd,host,name};

                members.add(new MemberInfo(GFMXUtils.getId(objectName), name, host, cwd, MemberInfo.MEMBER_TYPE_CACHEVM));

                if(log.isDebugEnabled()) {
                    log.debug("id: " + objectName.getCanonicalName());
                    log.debug("cwd: " + cwd);
                    log.debug("name: " + name);
                    log.debug("host: " + host);					
                }

            } catch (Exception e) {
                log.info("Unable to request cache members.", e);
            }
        }

        return members.toArray(new MemberInfo[0]);
    }

    public MemberInfo[] getSystemMemberApplications(){

        List<MemberInfo> members = new ArrayList<MemberInfo>();

        ObjectName[] mServers = manageSystemMembersApplications();
        for (ObjectName objectName : mServers) {
            try {
                MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
                //				String cwd = (String) mServer.getAttribute(objectName, "workingDirectory");
                String name = (String) mServer.getAttribute(objectName, "name");
                String host = (String) mServer.getAttribute(objectName, "host");
                //				String[] atts = {cwd,host,name};

                members.add(new MemberInfo(GFMXUtils.getId(objectName), name, host, "", MemberInfo.MEMBER_TYPE_APPLICATION));

                if(log.isDebugEnabled()) {
                    log.debug("id: " + objectName.getCanonicalName());
                    //					log.debug("cwd: " + cwd);
                    log.debug("name: " + name);
                    log.debug("host: " + host);					
                }

            } catch (Exception e) {
                log.info("Unable to request cache members.", e);
            }
        }

        return members.toArray(new MemberInfo[0]);
    }

    /**
     * 
     * @return
     */
    public Map<ObjectName, String[]> getCacheVmAttributesAsObjectName(){

        Map<ObjectName, String[]> map = new Hashtable<ObjectName, String[]>();

        ObjectName[] mServers = manageCacheServers();
        for (ObjectName objectName : mServers) {
            try {
                MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
                String cwd = (String) mServer.getAttribute(objectName, "workingDirectory");
                String name = (String) mServer.getAttribute(objectName, "name");
                String host = (String) mServer.getAttribute(objectName, "host");
                String[] atts = {cwd,host,name};
                map.put(objectName,atts);

                if(log.isDebugEnabled()) {
                    log.debug("id: " + objectName.getCanonicalName());
                    log.debug("cwd: " + cwd);
                    log.debug("name: " + name);
                    log.debug("host: " + host);					
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return map;
    }
    
    public int getMemberRoles(String gfid) {
        int mask = 0;
        
        boolean isPeer = false;
        // if we find it under GemFire.Member it's app peer all right
        MBeanServerConnection mServer;
        try {
            mServer = MxUtil.getMBeanServer(props);
            ObjectName peerBean = getApplicationObject(gfid);
            String id = (String) mServer.getAttribute(peerBean, "id");
            isPeer = true;
        } catch (Exception e) {
        }        
        
        if(isPeer) mask = mask | GFMXConstants.MEMBER_ROLE_APPLICATIONPEER;

        // since we need to check stats to see if member is a hub,
        // check cs and hub role from there
        ObjectName[] stats = getStatObjects(gfid);
        for (ObjectName objectName : stats) {
            try {
                mServer = MxUtil.getMBeanServer(props);
                String type = (String) mServer.getAttribute(objectName, "type");
                if(type.equals("CacheServerStats")) mask = mask | GFMXConstants.MEMBER_ROLE_CACHESERVER;
                if(type.endsWith("GatewayHubStatistics")) mask = mask | GFMXConstants.MEMBER_ROLE_GATEWAYHUB;
            } catch (Exception e) {
            }            
        }
        return mask;
    }

    /**
     * Returns available statistics names.
     * 
     * Based of Gemfire DS member id queries all available statistics and
     * contructs a list of names found from MBeans.
     * 
     * If member distribution statistics objectName is
     * GemFire.Statistic:source=localhost(16792)<v2>-1726/53276,uid=1,name=distributionStats
     * "distributionStats" is added to the array.
     * 
     * @param gfid GF DS unique member id - e.g. 127.0.0.1(25584)<v16>-20636/52014 
     * @return Array of statistic names
     */
    public String[] getStatNames(String gfid) {
        ArrayList<String> list = new ArrayList<String>();

        ObjectName[] mStats = manageCacheVMStats(gfid);

        for (ObjectName objectName : mStats) {
            list.add(objectName.getKeyProperty("name"));
        }
        return list.toArray(new String[0]);
    }

    /**
     * Returns all known object names from member statistics.
     * 
     * @param gfid GF DS unique member id - e.g. 127.0.0.1(25584)<v16>-20636/52014 
     * @return Array of statistics object names
     */
    public ObjectName[] getStatObjects(String gfid) {
        ObjectName[] stats;
        stats = manageCacheVMStats(gfid);
        if(stats == null)
            stats = manageApplicationStats(gfid);
        if(stats == null)
            stats = new ObjectName[0];
        return stats;
    }

    public Object[][] getStatObjectsWithType(String gfid) {
        ObjectName[] mStats = manageCacheVMStats(gfid);
        if(mStats == null)
            mStats = manageApplicationStats(gfid);

        Object[][] ret = new Object[mStats.length][2];

        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);

            for (int i = 0; i < mStats.length; i++) {
                ObjectName objectName = mStats[i];
                String type = (String) mServer.getAttribute(objectName, "type");
                ret[i][0] = objectName;
                ret[i][1] = type;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * Returns values from member statistics object. 
     * 
     * @param gfid GF DS unique member id - e.g. 127.0.0.1(25584)<v16>-20636/52014 
     * @param name Name of the statistic object.
     * @return Map where key is stat name and value actual metric
     */
    public Map<String, Double> getStatValues(MemberInfo member, String name) {
        return getStats(member, name);
    }

    public Map<String, Double> getStatValues(MemberInfo member, String name, String[] keys) {
        return getStats(member, name, keys);
    }

    public GFVersionInfo getVersionInfoFromAgent() {
        String vString = getAgentVersionString();
        if(vString == null || vString.length() < 3)
            return null;
        return GFVersionInfo.parse(getAgentVersionString());
    }

    public boolean isDistributionAlive() {
        return connectToSystem() != null ? true : false;
    }
    
    public MBeanServerConnection addAlertNotificationListener(NotificationListener listener) {
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName obj = connectToSystem();
            mServer.addNotificationListener(obj, listener, null, null);
            return mServer;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (InstanceNotFoundException e) {
        }
        return null;
    }

    public void removeAlertNotificationListener(NotificationListener listener) {
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName obj = connectToSystem();
            mServer.removeNotificationListener(obj, listener);
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (InstanceNotFoundException e) {
        } catch (ListenerNotFoundException e) {
        }
    }

}
