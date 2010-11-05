/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], VMWare, Inc.
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

package org.hyperic.hq.plugin.vsphere;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.HostHardwareSummary;
import com.vmware.vim25.HostListSummary;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VSphereUtil extends ServiceInstance {

    private static final long CACHE_TIMEOUT = 300000;
    static final String HOST_SYSTEM = "HostSystem";
    static final String POOL = "ResourcePool";
    static final String VM = "VirtualMachine";

    static final String PROP_URL = VSphereCollector.PROP_URL;
    static final String PROP_HOSTNAME = VSphereCollector.PROP_HOSTNAME;
    static final String PROP_USERNAME = VSphereCollector.PROP_USERNAME;
    static final String PROP_PASSWORD = VSphereCollector.PROP_PASSWORD;
    

    private static final Log _log =
        LogFactory.getLog(VSphereUtil.class.getName());
    private InventoryNavigator _nav;
    private String _url;
    private Map<String, ObjectCache<Map<String, ManagedEntity>>> entityCache =
        new HashMap<String, ObjectCache<Map<String, ManagedEntity>>>();
   
    public VSphereUtil(URL url, String username, String password, boolean ignoreCert)
        throws RemoteException, MalformedURLException {
        super(url, username, password, ignoreCert);
        _url = url.toString();
    }

    static VSphereUtil getInstance(Properties props)
        throws PluginException {

        String url = getURL(props);
        if (url == null) {
            throw new PluginException(VSphereCollector.PROP_URL +
                                      " not configured");
        }

        String username = props.getProperty(VSphereCollector.PROP_USERNAME);
        String password = props.getProperty(VSphereCollector.PROP_PASSWORD); 

        try {
            return new VSphereUtil(new URL(url), username, password, true);
        } catch (Exception e) {
            throw new PluginException("ServiceInstance(" + url + ", " +
                                      username + "): " + e, e);
        }

    }

    public static VSphereUtil getInstance(ConfigResponse config)
        throws PluginException {
    
        return getInstance(config.toProperties());
    }

    public boolean isSessionValid() {
        try {
            //make sure session is still valid. XXX better way?
            Calendar clock = getServerClock();
            if (clock == null) {
                _log.debug(_url + " session invalid, clock=NULL");
                return false;
            }
            else {
                _log.debug(_url + " session valid, clock=" +
                           new Date(clock.getTimeInMillis()));
                return true;
            }
        } catch (Exception e) {
            _log.debug(_url + " session invalid, clock=" + e, e);
            return false;
        }
    }

    public boolean isESX() {
        return !"gsx".equals(getAboutInfo().getProductLineId());
    }

    public InventoryNavigator getNavigator() {
        if (_nav == null) {
            _nav = new InventoryNavigator(getRootFolder());
        }
        return _nav;
    }

    /**
     * Find a managed entity by UUID.  This method caches the entired vm inventory every 5 minutes.
     * If a uuid is not found in the inventory during the cached period an Exception is thrown.
     * @throws {@link PluginException} general case exception is thrown while grabbing all the
     * entities.
     * @throws {@link ManagedEntityNotFoundException} if the ManagedEntity is not found.
     */
    public ManagedEntity findByUuid(String type, String uuid) throws PluginException {
        Map<String, ManagedEntity> cached =
            (entityCache.get(type) == null || entityCache.get(type).isExpired()) ?
                null : entityCache.get(type).getEntity();
        if (cached != null) {
            return cached.get(uuid);
        }
        ManagedEntity obj = null;
        Exception ex = null;
        cached = new HashMap<String, ManagedEntity>();
        try {
            ManagedEntity[] entities = find(type);
            for (int i=0; entities!=null && i<entities.length; i++) {
                ManagedEntity entity = entities[i];
                if (entity == null) {
                    continue;
                }
                String entUuid = getUuid(entity);
                if (entUuid == null) {
                    continue;
                }
                if (uuid.equals(entUuid)) {
                    obj = entity;
                }
                cached.put(entUuid, entity);
            }
        } catch (Exception e) {
            ex = e;
        } finally {
            if (_log.isDebugEnabled()) {
                _log.debug("findByUuid: type=" + type + ", uuid=" + uuid + ", managedEntity=" + obj);
            }
        }
        // does not matter if obj is null, want to cache that as well
        cached.put(uuid, obj);
        entityCache.put(type, new ObjectCache<Map<String, ManagedEntity>>(cached, CACHE_TIMEOUT));
        if (ex != null) {
            throw new PluginException(type + "/" + uuid + ": " + ex, ex);
        } else if (obj == null) {
            throw new ManagedEntityNotFoundException(type + "/" + uuid + ": not found");
        }
        return obj;
    }

	public ManagedEntity findByMOR(String type, String value) throws PluginException {
		ManagedEntity[] managedEntities = find(type);
		ManagedEntity obj = null;
		for(ManagedEntity managedEntity: managedEntities) {
			if(managedEntity.getMOR().getVal().equals(value)) {
				obj = managedEntity;
				break;
			}
		}
	  	if (obj == null) {
            throw new ManagedEntityNotFoundException(type + "/" + value + ": not found");
        }
        return obj;
	}
    
    public ManagedEntity find(String type, String name)
        throws PluginException {

        ManagedEntity obj = null;
        try {
            // the vijava api will return the first instance of the
            // entity type with the given name
            obj = getNavigator().searchManagedEntity(type, name);
        } catch (Exception e) {
            throw new PluginException(type + "/" + name + ": " + e, e);
        }
        if (obj == null) {
            throw new ManagedEntityNotFoundException(type + "/" + name + ": not found");
        }
        return obj;
    }

    public ManagedEntity getByTypeAndName(String type, String name) throws PluginException {
        ManagedEntity rtn;
        try {
            rtn = getNavigator().searchManagedEntity(type, name);
        } catch (Exception e) {
            throw new PluginException(type + ": " + e, e);
        }
        if (rtn == null) {
            throw new PluginException("name=" + name + ",type=" + type + ": not found");
        }
        return rtn;
    }

    public ManagedEntity[] find(String type) throws PluginException {
        ManagedEntity[] obj;
        try {
            obj = getNavigator().searchManagedEntities(type);
        } catch (Exception e) {
            throw new PluginException(type + ": " + e, e);
        }
        if (obj == null) {
            throw new PluginException(type + ": not found");
        }
        return obj;
    }

    public HostSystem getHost(String host) throws PluginException {
        return (HostSystem)find(HOST_SYSTEM, host);
    }
    
    static String getUuid(ManagedEntity entity) {
        String uuid = null;
        if (entity instanceof HostSystem) {
            HostSystem host = (HostSystem) entity;
            HostListSummary summary = host.getSummary();
            if (summary == null) {
                return null;
            }
            HostHardwareSummary hardware = summary.getHardware();
            if (hardware == null) {
                return null;
            }
            uuid = hardware.getUuid();
        } else if (entity instanceof VirtualMachine) {
            VirtualMachine vm = (VirtualMachine) entity;
            VirtualMachineConfigInfo config = vm.getConfig();
            if (config == null) {
                return null;
            }
            uuid = config.getUuid();
        }
        return uuid;
    }

    public static String getURL(Properties props) {
        return props.getProperty(VSphereCollector.PROP_URL);
    }

    public static void dispose(VSphereUtil vim) {
        if (vim != null) {
            vim.getServerConnection().logout();
        }
    }
}
