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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A simple storage provider which keeps the entire set of data in memory
 * during the invocation of the process.  
 */
public class AgentInMemProvider 
    implements AgentStorageProvider 
{
    private Log        logger;     // da logger
    private Hashtable  tableData;  // Regular key/value pairs
    private Hashtable  lists;      // Hash of list names onto string lists

    public AgentInMemProvider(){
        this.logger    = LogFactory.getLog(AgentInMemProvider.class);
        this.tableData = null;
        this.lists     = null;
    }
 
    /**
     * Get a description of this storage provider.
     *
     * @return A string describing the functionality of the object.
     */

    public String getDescription(){
        return "Agent in-memory provider.  Entire contents of data " +
            "kept in memory during runtime";
    }

    /**
     * Sets a value within the storage object.  
     *
     * @param key           Key of the value to set.
     * @param value         Value to set for 'key'.
     */

    public void setValue(String key, String value){
        if(value == null){
            this.logger.debug("Removing '"+key+"' from storage");
            this.tableData.remove(key);
        } else {
            this.logger.debug("Setting '"+key+"' to '"+value+"'");
            this.tableData.put(key, value);
        }
    }

    /**
     * Gets a value from the storage object.
     *
     * @param key  Key of the value to get.
     *
     * @return The value associated with the key for the subsystem.
     */

    public String getValue(String key){
        String res;

        this.logger.debug("Retreiving value for '" + key + "'");
        res = (String)this.tableData.get(key);
        this.logger.debug(key+"='"+res+"'");
        return res;
    }

    public Set getKeys () { return this.tableData.keySet(); }

    public void flush(){
    }

    public void init(String info) 
        throws AgentStorageException 
    {
        this.tableData = new Hashtable();
        this.lists     = new Hashtable();
    }

    public void dispose(){
    }

    /*** LIST FUNCTIONALITY ***/

    public void addToList(String listName, String value)
        throws AgentStorageException
    {
        Vector v;

        synchronized(this.lists){
            v = (Vector)this.lists.get(listName);

            if(v == null){
                v = new Vector();
                this.lists.put(listName, v);
            }
        }

        v.add(value);
    }

    private class SafeIterator
        implements Iterator
    {
        private Vector   realList;
        private Iterator fakeIterator;
        private Object   lastVal;

        private SafeIterator(Vector realList, Iterator fakeIterator){
            this.realList     = realList;
            this.fakeIterator = fakeIterator;
            this.lastVal      = null;
        }

        public boolean hasNext(){
            return this.fakeIterator.hasNext();
        }
        
        public Object next(){
            return (this.lastVal = this.fakeIterator.next());
        }

        public void remove(){
            this.realList.remove(this.lastVal);
            this.fakeIterator.remove();
        }
    }

    public Iterator getListIterator(String listName){
        Vector v;

        synchronized(this.lists){
            if((v = (Vector)this.lists.get(listName)) == null)
                return null;

            if(v.size() == 0){
                // This occurs if someone modified the list from under us
                this.lists.remove(listName);
                return null;
            }
        }

        /* We need to return a safe iterator -- this basically sucks
           with the backend we are using, since every remove operation on
           the iterator will yield a linear search through the table in 
           order to remove it.. Oh well */
        return new SafeIterator(v, ((Vector)v.clone()).iterator());
    }

    public void deleteList(String listName){
        synchronized(this.lists){
            this.lists.remove(listName);
        } 
    }

    public void createList(String listName, int recSize) {
        // no-op.
    }
}
