/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.CPropChangeEvent;
import org.hyperic.hq.appdef.shared.CPropKeyExistsException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("restriction")
@Service
@Transactional
public class CPropManagerImpl implements CPropManager {

    private static Log log = LogFactory.getLog(CPropManagerImpl.class.getName());

    private Messenger sender;
    private CpropDAO cPropDAO;
    private CpropKeyDAO cPropKeyDAO;
    private ApplicationTypeDAO applicationTypeDAO;
    private PlatformTypeDAO platformTypeDAO;
    private ServerTypeDAO serverTypeDAO;
    private ServiceTypeDAO serviceTypeDAO;
    
    @Autowired
    public CPropManagerImpl(Messenger sender, CpropDAO cPropDAO, CpropKeyDAO cPropKeyDAO,
                            ApplicationTypeDAO applicationTypeDAO, PlatformTypeDAO platformTypeDAO,
                            ServerTypeDAO serverTypeDAO, ServiceTypeDAO serviceTypeDAO) {
        this.sender = sender;
        this.cPropDAO = cPropDAO;
        this.cPropKeyDAO = cPropKeyDAO;
        this.applicationTypeDAO = applicationTypeDAO;
        this.platformTypeDAO = platformTypeDAO;
        this.serverTypeDAO = serverTypeDAO;
        this.serviceTypeDAO = serviceTypeDAO;
    }

//    @PostConstruct
//    public void afterPropertiesSet() throws Exception {
//        this.vmMgr = (VCManager) Bootstrap.getBean("VMManagerImpl");
//    }


    /**
     * Get all the keys associated with an appdef resource type.
     * 
     * @param appdefType One of AppdefEntityConstants.APPDEF_TYPE_*
     * @param appdefTypeId The ID of the appdef resource type
     * 
     * @return a List of CPropKeyValue objects
     */
    @Transactional(readOnly = true)
    public List<CpropKey> getKeys(int appdefType, int appdefTypeId) {
        return cPropKeyDAO.findByAppdefType(appdefType, appdefTypeId);
    }

    private AppdefResourceType findResourceType(int appdefType, int appdefTypeId)
        throws AppdefEntityNotFoundException {
        Integer id = new Integer(appdefTypeId);

        if (appdefType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            return platformTypeDAO.findById(id);
        } else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            try {
                return serverTypeDAO.findById(id);
            } catch (ObjectNotFoundException exc) {
                throw new ServerNotFoundException("Server type id=" + appdefTypeId + " not found");
            }
        } else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            try {
                return serviceTypeDAO.findById(id);
            } catch (ObjectNotFoundException exc) {
                throw new ServiceNotFoundException("Service type id=" + appdefTypeId + " not found");
            }
        } else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION) {
            return applicationTypeDAO.findById(id);
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type:" + " " + appdefType);
        }
    }

    /**
     * find appdef resource type
     */
    @Transactional(readOnly = true)
    public AppdefResourceType findResourceType(TypeInfo info) {
        int type = info.getType();

        if (type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            return platformTypeDAO.findByName(info.getName());
        } else if (type == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            return serverTypeDAO.findByName(info.getName());
        } else if (type == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            return serviceTypeDAO.findByName(info.getName());
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type: " + info);
        }
    }

    /**
     * @return {@link Map} of {@link String} to {@link AppdefResourceType}s
     */
    @Transactional(readOnly = true)
    public Map<String, AppdefResourceType> findResourceType(Collection<TypeInfo> typeInfos) {
        List<String> platformTypeInfos = new ArrayList<String>();
        List<String> serverTypeInfos = new ArrayList<String>();
        List<String> serviceTypeInfos = new ArrayList<String>();
        for (final TypeInfo info : typeInfos) {
            int type = info.getType();
            if (type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                platformTypeInfos.add(info.getName());
            } else if (type == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
                serverTypeInfos.add(info.getName());
            } else if (type == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
                serviceTypeInfos.add(info.getName());
            } else {
                throw new IllegalArgumentException("Unrecognized appdef type: " + info);
            }
        }
        List<AppdefResourceType> resTypes = new ArrayList<AppdefResourceType>(typeInfos.size());
        Map<String, AppdefResourceType> rtn = new HashMap<String, AppdefResourceType>(typeInfos
            .size());
        if (platformTypeInfos.size() > 0) {
            resTypes.addAll(platformTypeDAO.findByName(platformTypeInfos));
        }
        if (serverTypeInfos.size() > 0) {
            resTypes.addAll(serverTypeDAO.findByName(serverTypeInfos));
        }
        if (serviceTypeInfos.size() > 0) {
            resTypes.addAll(serviceTypeDAO.findByName(serviceTypeInfos));
        }
        for (AppdefResourceType type : resTypes) {
            rtn.put(type.getName(), type);
        }
        return rtn;
    }

    /**
     * find Cprop by key to a resource type based on a TypeInfo object.
     */
    @Transactional(readOnly = true)
    public CpropKey findByKey(AppdefResourceType appdefType, String key) {
        int type = appdefType.getAppdefType();
        int instanceId = appdefType.getId().intValue();

        return cPropKeyDAO.findByKey(type, instanceId, key);
    }

    /**
     * Add a key to a resource type based on a TypeInfo object.
     * 
     * @throw AppdefEntityNotFoundException if the appdef resource type that the
     *        key references could not be found
     * @throw CPropKeyExistsException if the key already exists
     */
    public void addKey(AppdefResourceType appdefType, String key, String description) {
        int type = appdefType.getAppdefType();
        int instanceId = appdefType.getId().intValue();

        cPropKeyDAO.create(type, instanceId, key, description);
    }

    /**
     * Add a key to a resource type. The key's 'appdefType' and 'appdefTypeId'
     * fields are used to locate the resource -- if that resource does not
     * exist, an AppdefEntityNotFoundException will be thrown.
     * 
     * @param key Key to create
     * @throw AppdefEntityNotFoundException if the appdef resource type that the
     *        key references could not be found
     * @throw CPropKeyExistsException if the key already exists
     */
    public void addKey(CpropKey key) throws AppdefEntityNotFoundException, CPropKeyExistsException {
        // Insure that the resource type exists
        AppdefResourceType recValue = findResourceType(key.getAppdefType(), key.getAppdefTypeId());
        CpropKey cpKey = cPropKeyDAO.findByKey(key.getAppdefType(), key.getAppdefTypeId(), key
            .getKey());

        if (cpKey != null) {
            throw new CPropKeyExistsException("Key, '" +
                                              key.getKey() +
                                              "', " +
                                              "already exists for " +
                                              AppdefEntityConstants.typeToString(recValue
                                                  .getAppdefType()) + " type, '" +
                                              recValue.getName() + "'");
        }

        cPropKeyDAO.create(key.getAppdefType(), key.getAppdefTypeId(), key.getKey(), key
            .getDescription());
    }

    /**
     * Remove a key from a resource type.
     * 
     * @param appdefType One of AppdefEntityConstants.APPDEF_TYPE_*
     * @param appdefTypeId The ID of the resource type
     * @param key Key to remove
     * 
     * @throw CPropKeyNotFoundException if the CPropKey could not be found
     */
    public void deleteKey(int appdefType, int appdefTypeId, String key)
        throws CPropKeyNotFoundException {
        CpropKey cpKey = cPropKeyDAO.findByKey(appdefType, appdefTypeId, key);

        if (cpKey == null) {
            throw new CPropKeyNotFoundException("Key, '" + key + "', does not" + " exist for " +
                                                AppdefEntityConstants.typeToString(appdefType) +
                                                " " + appdefTypeId);
        }

        // cascade on delete to remove Cprop as well
        cPropKeyDAO.remove(cpKey);
    }

    /**
     * Set (or delete) a custom property for a resource. If the property already
     * exists, it will be overwritten.
     * 
     * @param aID Appdef entity id to set the value for
     * @param typeId Resource type id
     * @param key Key to associate the value with
     * @param val Value to assicate with the key. If the value is null, then the
     *        value will simply be removed.
     * 
     * @throw CPropKeyNotFoundException if the key has not been created for the
     *        resource's associated type
     * @throw AppdefEntityNotFoundException if id for 'aVal' specifies a
     *        resource which does not exist
     */
    public void setValue(AppdefEntityID aID, int typeId, String key, String val)
        throws CPropKeyNotFoundException, AppdefEntityNotFoundException, PermissionException {
        String oldval;
        try {
           oldval = cPropDAO.setValue(aID, typeId, key, val);
        }catch(Exception e) {
            log.error("Unable to update CPropKey values: " + e.getMessage(), e);
            throw new SystemException(e);
        }
        if((val == null && oldval == null) || (val != null && val.equals(oldval))) {
            //We didn't change anything
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Entity " + aID.getAppdefKey() + " " + key + " changed from " + oldval +
                      " to " + val);
        }
        // Send cprop value changed event
        CPropChangeEvent event = new CPropChangeEvent(aID, key, oldval, val);
        sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }

    /**
     * Get a custom property for a resource.
     * 
     * @param aVal Appdef entity to get the value for
     * @param key Key of the value to get
     * 
     * @return The value associated with 'key' if found, else null
     * 
     * @throw CPropKeyNotFoundException if the key for the associated resource
     *        is not found
     * @throw AppdefEntityNotFoundException if the passed entity is not found
     */
    @Transactional(readOnly = true)
    public String getValue(AppdefEntityValue aVal, String key) throws CPropKeyNotFoundException,
        AppdefEntityNotFoundException, PermissionException {
        try {
            return cPropDAO.getValue(aVal, key);
        }catch(Exception e) {
            log.error("Unable to get CPropKey values: " + e.getMessage(), e);
            throw new SystemException(e);
        }
    }

    /**
     * Get a map which holds the keys & their associated values for an appdef
     * entity.
     * 
     * @param aID Appdef entity id to get the custom properties for
     * 
     * @return The properties stored for a specific entity ID. An empty
     *         Properties object will be returned if there are no custom
     *         properties defined for the resource
     */
    @Transactional(readOnly = true)
    public Properties getEntries(AppdefEntityID aID) throws PermissionException,
        AppdefEntityNotFoundException {
        return cPropDAO.getEntries(aID, "propkey");
    }

    @Transactional(readOnly = true)
    public Map<AppdefEntityID, Properties> getAllEntries(String ... keys) {
        return cPropDAO.getAllEntries(keys);
    }

    /**
     * Get a map which holds the descriptions & their associated values for an
     * appdef entity.
     * 
     * @param aID Appdef entity id to get the custom properties for
     * 
     * @return The properties stored for a specific entity ID
     */
    @Transactional(readOnly = true)
    public Properties getDescEntries(AppdefEntityID aID) throws PermissionException,
        AppdefEntityNotFoundException {
        return cPropDAO.getEntries(aID, "description");
    }

    /**
     * Set custom properties for a resource. If the property already exists, it
     * will be overwritten.
     * 
     * @param aID Appdef entity id to set the value for
     * @param typeId Resource type id
     * @param data Encoded ConfigResponse
     */
    public void setConfigResponse(AppdefEntityID aID, int typeId, byte[] data)
            throws PermissionException, AppdefEntityNotFoundException {

        if (data == null) {
            return;
        }

        ConfigResponse cprops;

        try {
            cprops = ConfigResponse.decode(data);
        } catch (EncodingException e) {
            throw new SystemException(e);
        }

       
        if (log.isDebugEnabled()) {
            log.debug("cprops=" + cprops);
            log.debug("aID=" + aID.toString() + ", typeId=" + typeId);
        }

        for (Iterator<String> it = cprops.getKeys().iterator(); it.hasNext();) {
            String key = it.next();
            String val = cprops.getValue(key);

            try {
                setValue(aID, typeId, key, val);
            } catch (CPropKeyNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
    
    }
    

    /**
     * Remove custom properties for a given resource.
     */
    public void deleteValues(int appdefType, int id) {
        try {
            cPropDAO.deleteValues(appdefType, id);
        }catch(Exception e) {
            log.error("Unable to delete CProp values: " + e.getMessage(), e);
            throw new SystemException(e);
        }
    }

    /**
     * Get all Cprops values with specified key name, regardless of type
     */
    @Transactional(readOnly = true)
    public List<Cprop> getCPropValues(AppdefResourceTypeValue appdefType, String key, boolean asc) {
        int type = appdefType.getAppdefType();
        int instanceId = appdefType.getId().intValue();

        CpropKey pkey = cPropKeyDAO.findByKey(type, instanceId, key);

        return cPropDAO.findByKeyName(pkey, asc);
    }
}
