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

package org.hyperic.hq.agent.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hyperic.util.GenericValueMap;

/**
 * Provides a very simple/generic storage interface.  Subsystems within
 * the Agent may require local storage for configurations or for a 
 * temporary datastore.  This interface should be used by things such as
 * a property file writer, or a JDBC backend.  It is based on the Properties
 * object.
 */

public interface AgentStorageProvider extends GenericValueMap {

    /**
     * @param folderName
     * @param obj
     * @param createTime
     */
    void addObjectToFolder(String folderName, Object obj, long createTime, int maxElementsInFolder);

    /**
     * @param folderName
     * @param maxNumOfObjects
     * @return
     */
    <T> List<T> getObjectsFromFolder(String folderName, int maxNumOfObjects);

    /**
     * @param folderName
     * @param objects
     */
    void deleteObjectsFromFolder(String folderName, String... objects);

    /**
     * @param obj
     * @param objectName
     */
    void saveObject(Object obj, String objectName);

    /**
     * @param objectName
     * @return
     */
    <T> T getObject(String objectName);

    /**
     * @param objectName
     */
    void deleteObject(String objectName);
    
    /**
     * Get information about the storage provider.
     *
     * @return A short description about the provider.
     */

    public String getDescription();

    /**
     * Sets a value within the storage object.  The key may be any
     * String, but should probably be in a Java Properties stylee.
     * If 'value' is null, the key will be deleted from storage.
     *
     * @param key    Key for the attribute
     * @param value  Value of the key
     */

    public void setValue(String key, String value);
    
    /**
     * Gets a value from the storage object.
     * 
     * @param key   Key for which to retrieve the value
     *
     * @return The value previously specified via setValue, or null if the
     *          key does not exist.
     */

    public String getValue(String key);

    /**
     * @return A set of all keys in the storage provider.
     */
    public Set getKeys ();

    /**
     * Flush values to permanent storage.  Implementers of this interface may
     * cache properties internally -- this method gives them a chance to
     * store it to permanent storage before it gets lost.
     */

    public void flush() throws AgentStorageException;

    /**
     * Initialize the storage provider with simple bootstrap information.
     * This string is unique to the storage provider and may contain a
     * filename for further configuration, database DSN, etc.
     *
     * @param info parameters for the StorageProvider to store data and index files
     */

    public void init(String info) throws AgentStorageException;

    /**
     * Perform any cleanup that the storage provider requires.
     */

    public void dispose();

    /**
     * Add a value to a storage column.  If the column does not yet
     * exist, it will be created.
     *
     * @param listName Name of the column to add to
     * @param value    Value to add to the column
     */

    public void addToList(String listName, String value)
        throws AgentStorageException;

    /**
     * Get an iterator for a named list.  If there is no list currently
     * in storage, or the list contains 0 elements, null will be returned.
     *
     * @param listName name of the list to get an iterator for.
     */
    public Iterator<String> getListIterator(String listName);

    /**
     * Delete an entire list from storage.  This is basically a shortcut
     * for deleting all elements as returned by getListIterator()
     */
    public void deleteList(String listName);

    /**
     * Create a list in storage.
     *
     * This method is intended creating non standard lists.  Currently
     * only used in the DList provider.
     */
    public void createList(String listName, int recSize)
        throws AgentStorageException;

    public void addOverloadedInfo(String listName, String info);

	/**
	 * This method converts lists from the old record size to the current one -
	 * should be used when starting the first time after an upgrade. In version
	 * 4.6.5 the default record size was changed from 1024 to 4000 and when we
	 * will try to read the records with size 4000 we will get an exception because the
	 * records size is 1024, when that happens we should call this method in order
	 * to covert the records to the new size and then read again. This is a fix for Jira bug [HHQ-5387].
	 * @param listName - name of the list
	 * @throws IOException 
	 */
	public void convertListToCurrentRecordSize(String listName) throws IOException;
	
	/**
	 * Removes a record from the list
	 * @param listName - name of the list
	 * @param recNumber - index of the record to remove
	 * @throws AgentStorageException
	 */
	public void removeFromList(String listName, long recNumber) throws AgentStorageException;
}
