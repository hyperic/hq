/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.appdef.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.Cprop;
import org.hyperic.hq.appdef.server.session.CpropKey;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.product.TypeInfo;

/**
 * Local interface for CPropManager.
 */
public interface CPropManager {
	/**
	 * Get all the keys associated with an appdef resource type.
	 * 
	 * @param appdefType
	 *            One of AppdefEntityConstants.APPDEF_TYPE_*
	 * @param appdefTypeId
	 *            The ID of the appdef resource type
	 * @return a List of CPropKey objects
	 */
	public List<CpropKey> getKeys(int appdefType, int appdefTypeId);

	/**
	 * find appdef resource type
	 */
	public AppdefResourceType findResourceType(TypeInfo info);

	/**
	 * find Cprop by key to a resource type based on a TypeInfo object.
	 */
	public CpropKey findByKey(AppdefResourceType appdefType, String key);

	/**
	 * Add a key to a resource type based on a TypeInfo object.
	 * 
	 * @throw AppdefEntityNotFoundException if the appdef resource type that the
	 *        key references could not be found
	 * @throw CPropKeyExistsException if the key already exists
	 */
	public void addKey(AppdefResourceType appdefType, String key,
			String description);

	/**
	 * Add a key to a resource type. The key's 'appdefType' and 'appdefTypeId'
	 * fields are used to locate the resource -- if that resource does not
	 * exist, an AppdefEntityNotFoundException will be thrown.
	 * 
	 * @param key
	 *            Key to create
	 * @throw AppdefEntityNotFoundException if the appdef resource type that the
	 *        key references could not be found
	 * @throw CPropKeyExistsException if the key already exists
	 */
	public void addKey(CpropKey key) throws AppdefEntityNotFoundException,
			CPropKeyExistsException;

	/**
	 * Remove a key from a resource type.
	 * 
	 * @param appdefType
	 *            One of AppdefEntityConstants.APPDEF_TYPE_*
	 * @param appdefTypeId
	 *            The ID of the resource type
	 * @param key
	 *            Key to remove
	 * @throw CPropKeyNotFoundException if the CPropKey could not be found
	 */
	public void deleteKey(int appdefType, int appdefTypeId, String key)
			throws CPropKeyNotFoundException;

	/**
	 * Set (or delete) a custom property for a resource. If the property already
	 * exists, it will be overwritten.
	 * 
	 * @param aID
	 *            Appdef entity id to set the value for
	 * @param typeId
	 *            Resource type id
	 * @param key
	 *            Key to associate the value with
	 * @param val
	 *            Value to assicate with the key. If the value is null, then the
	 *            value will simply be removed.
	 * @throw CPropKeyNotFoundException if the key has not been created for the
	 *        resource's associated type
	 * @throw AppdefEntityNotFoundException if id for 'aVal' specifies a
	 *        resource which does not exist XXX: scottmf, we should move this
	 *        over to hql at some point rather than trying to manage the
	 *        transaction via jdbc within this container
	 */
	public void setValue(AppdefEntityID aID, int typeId, String key, String val)
			throws CPropKeyNotFoundException, AppdefEntityNotFoundException,
			PermissionException;

	/**
	 * Get a custom property for a resource.
	 * 
	 * @param aVal
	 *            Appdef entity to get the value for
	 * @param key
	 *            Key of the value to get
	 * @return The value associated with 'key' if found, else null
	 * @throw CPropKeyNotFoundException if the key for the associated resource
	 *        is not found
	 * @throw AppdefEntityNotFoundException if the passed entity is not found
	 */
	public String getValue(AppdefEntityValue aVal, String key)
			throws CPropKeyNotFoundException, AppdefEntityNotFoundException,
			PermissionException;

	/**
	 * Get a map which holds the keys & their associated values for an appdef
	 * entity.
	 * 
	 * @param aID
	 *            Appdef entity id to get the custom properties for
	 * @return The properties stored for a specific entity ID. An empty
	 *         Properties object will be returned if there are no custom
	 *         properties defined for the resource
	 */
	public Properties getEntries(AppdefEntityID aID)
			throws PermissionException, AppdefEntityNotFoundException;

	/**
	 * Get a map which holds the descriptions & their associated values for an
	 * appdef entity.
	 * 
	 * @param aID
	 *            Appdef entity id to get the custom properties for
	 * @return The properties stored for a specific entity ID
	 */
	public Properties getDescEntries(AppdefEntityID aID)
			throws PermissionException, AppdefEntityNotFoundException;

	/**
	 * Set custom properties for a resource. If the property already exists, it
	 * will be overwritten.
	 * 
	 * @param aID
	 *            Appdef entity id to set the value for
	 * @param typeId
	 *            Resource type id
	 * @param data
	 *            Encoded ConfigResponse
	 */
    public void setConfigResponse(AppdefEntityID aID, int typeId, byte[] data)
            throws PermissionException, AppdefEntityNotFoundException;


    /**
	 * Remove custom properties for a given resource.
	 */
	public void deleteValues(int appdefType, int id);

	/**
	 * Get all Cprops values with specified key name, irregardless of type
	 */
	public List<Cprop> getCPropValues(AppdefResourceTypeValue appdefType,
			String key, boolean asc);

    /**
     * @return {@link Map} of {@link String} to {@link AppdefResourceType}s
     */
    public Map<String, AppdefResourceType> findResourceType(Collection<TypeInfo> typeInfos);

    /**
     * @param keys - keys to search for in the propkey field, must be specified or not values will be returned
     * @return {@link Map} of {@link AppdefEntityID} to {@link Properties}
     */
    public Map<AppdefEntityID, Properties> getAllEntries(String ... keys);
}
