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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.CPropChangeEvent;
import org.hyperic.hq.appdef.shared.CPropKeyExistsException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CPropManagerImpl implements CPropManager {

    private static Log log = LogFactory.getLog(CPropManagerImpl.class.getName());

    private Messenger sender;
    
    private ResourceManager resourceManager;
    
    private ResourceTypeDao resourceTypeDao;
   

    @Autowired
    public CPropManagerImpl(Messenger sender, ResourceManager resourceManager, ResourceTypeDao resourceTypeDao) {
        this.sender = sender;
        this.resourceManager = resourceManager;
        this.resourceTypeDao = resourceTypeDao;
    }

    /**
     * Get all the keys associated with an appdef resource type.
     * 
     * @param appdefType One of AppdefEntityConstants.APPDEF_TYPE_*
     * @param appdefTypeId The ID of the appdef resource type
     * 
     * @return a List of CPropKeyValue objects
     */
    @Transactional(readOnly = true)
    public List<PropertyType> getKeys(int appdefType, int appdefTypeId) {
        return new ArrayList<PropertyType>(resourceManager.findResourceTypeById(appdefTypeId).getPropertyTypes());
    }

    /**
     * find Cprop by key to a resource type based on a TypeInfo object.
     */
    @Transactional(readOnly = true)
    public PropertyType findByKey(ResourceType appdefType, String key) {
        return appdefType.getPropertyType(key);
    }

    /**
     * Add a key to a resource type based on a TypeInfo object.
     * 
     * @throw AppdefEntityNotFoundException if the appdef resource type that the
     *        key references could not be found
     * @throw CPropKeyExistsException if the key already exists
     */
    public void addKey(ResourceType appdefType, String key, String description,Class<?> type) {
        PropertyType propertyType = new PropertyType(key,type);
        propertyType.setDescription(description);
        appdefType.addPropertyType(propertyType);
        resourceTypeDao.merge(appdefType);
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
    public void addKey(PropertyType key) throws AppdefEntityNotFoundException, CPropKeyExistsException {
//TODO
//        CpropKey cpKey = PropertyType.findByKey(key.getAppdefType(), key.getAppdefTypeId(), key
//          //  .getKey());
//
//        if (cpKey != null) {
//            throw new CPropKeyExistsException("Key, '" +
//                                              key.getKey() +
//                                              "', " +
//                                              "already exists for " +
//                                              AppdefEntityConstants.typeToString(recValue
//                                                  .getAppdefType()) + " type, '" +
//                                              recValue.getName() + "'");
//        }
//
//        cPropKeyDAO.create(key.getAppdefType(), key.getAppdefTypeId(), key.getKey(), key
//            .getDescription());
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
        ResourceType resourceType = resourceManager.findResourceTypeById(appdefTypeId);
        PropertyType cpKey  = resourceType.getPropertyType(key);
     

        if (cpKey == null) {
            throw new CPropKeyNotFoundException("Key, '" + key + "', does not" + " exist for " +
                                                AppdefEntityConstants.typeToString(appdefType) +
                                                " " + appdefTypeId);
        }

        // cascade on delete to remove Cprop as well
        //TODO remove property type?
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
           oldval = (String) resourceManager.findResourceById(aID.getId()).setProperty(key, val);
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
        sender.publishMessage(MessagePublisher.EVENTS_TOPIC, event);
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
            return (String)resourceManager.findResourceById(aVal.getID().getId()).getProperty(key);            
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
        Resource resource = resourceManager.findResourceById(aID.getId());
        //TODO assuming all prop values go to toString
        Properties properties = new Properties();
        Map<String,Object> propValues = resource.getProperties();
        for(Map.Entry<String, Object> propValue:propValues.entrySet()) {
            PropertyType type = resource.getType().getPropertyType(propValue.getKey());
            //TODO is this the place to filter hidden?
            if(!(type.isHidden())) {
                properties.setProperty(propValue.getKey(), propValue.getValue().toString());
            }
        }
        return properties;
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
        //TODO assuming all prop values go to toString, should we just disallow objects?
        Resource resource = resourceManager.findResourceById(aID.getId());
        Properties properties = new Properties();
        Map<String,Object> propValues = resource.getProperties();
        for(Map.Entry<String, Object> propValue:propValues.entrySet()) {
            PropertyType type = resource.getType().getPropertyType(propValue.getKey());
            //TODO is this the place to filter hidden?
            if(!(type.isHidden())) {
                properties.setProperty(type.getDescription(), propValue.getValue().toString());
            }
        }
        return properties;
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
            resourceManager.findResourceById(id).removeProperties();
        }catch(Exception e) {
            log.error("Unable to delete CProp values: " + e.getMessage(), e);
            throw new SystemException(e);
        }
    }

    /**
     * Get all Cprops values with specified key name, regardless of type
     */
    @Transactional(readOnly = true)
    public List<String> getCPropValues(AppdefResourceTypeValue appdefType, String key, boolean asc) {
        int instanceId = appdefType.getId().intValue();
        ResourceType resourceType = resourceManager.findResourceTypeById(instanceId);
        //TODO this can't possibly be what you'd expect from this method
        List<String> values = new ArrayList<String>();
        for(Resource resource: resourceType.getResources()) {
            values.add((String)resource.getProperty(key));
        }
        return values;
    }
}
