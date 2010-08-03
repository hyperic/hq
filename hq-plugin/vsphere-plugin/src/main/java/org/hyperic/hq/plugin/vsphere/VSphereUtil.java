/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
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
import com.vmware.vim25.mo.ManagedObject;
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
    private final Map<String, ObjectCache<ManagedEntity>> entitiesByUuid =
       Collections.synchronizedMap(new HashMap());
   

    public VSphereUtil(URL url, String username, String password, boolean ignoreCert)
        throws RemoteException, MalformedURLException {
        super(url, username, password, ignoreCert);
        _url = url.toString();
    }

    public static VSphereUtil getInstance(Properties props)
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

    private ManagedEntity findByUuidCached(String uuid) {
      ObjectCache<ManagedEntity> rtn = entitiesByUuid.get(uuid);
      return (rtn == null || rtn.isExpired()) ? null : rtn.getEntity();
    }
    
    private long now() {
      return System.currentTimeMillis();
    }


    /** Find a managed entity by UUID. This may be less performant
     * than using find(type, name), but allows managed entities
     * with the same name to be uniquely found.
     */
    public ManagedEntity findByUuid(String type, String uuid)
        throws PluginException {
        ManagedEntity obj = findByUuidCached(uuid);
        final boolean debug = _log.isDebugEnabled();
        if (obj != null) {
            // HPD-681 / HPD-691 need to set the serverConnection field in order to avoid an
            // NPE when the connection associated with the object is closed
            // This whole method should be re-written once we start packaging the libs necessary
            // in order to call SearchIndex.findByUuid()
            try {
                Field field = ManagedObject.class.getDeclaredField("serverConnection");
                field.setAccessible(true);
                field.set(obj, getServerConnection());
                if (debug) _log.debug("uuid=" + uuid + " is cached");
                return obj;
            } catch (NoSuchFieldException e) {
                _log.debug(e,e);
            } catch (IllegalAccessException e) {
                _log.debug(e,e);
            }
        }
        if (debug) _log.debug("uuid=" + uuid + " is NOT CACHED");
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
                
                entitiesByUuid.put(entUuid, new ObjectCache<ManagedEntity>(entity, CACHE_TIMEOUT));
              
                if (uuid.equals(entUuid)) {
                    obj = entity;
                    break;
                  
                }
             
            }
        } catch (Exception e) {
            throw new PluginException(type + "/" + uuid + ": " + e, e);
        } finally {
            if (_log.isDebugEnabled()) {
                _log.debug("findByUuid: type=" + type + ", uuid=" + uuid + ", managedEntity=" + obj);
            }
        }
        
        if (obj == null) {
            throw new ManagedEntityNotFoundException(type + "/" + uuid + ": not found");
        }
        entitiesByUuid.put(uuid, new ObjectCache<ManagedEntity>(obj, CACHE_TIMEOUT));
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
    
    private String getUuid(ManagedEntity entity) {
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
