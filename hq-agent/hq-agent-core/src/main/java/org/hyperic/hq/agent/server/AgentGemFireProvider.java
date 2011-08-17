/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
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

package org.hyperic.hq.agent.server;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gemstone.gemfire.cache.DiskStore;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.query.Query;
import com.gemstone.gemfire.cache.query.QueryService;
import com.gemstone.gemfire.cache.query.SelectResults;

/**
 * This is an optional replacement for agent's default
 * persistent storage (AgentDListProvider) backed by 
 * gemfire local persistent and overflow cache. This provider
 * is handling internal caches in three different levels.
 * 
 * AgentStorageProvider 1. provides methods for provider itself
 * to act as a simple key/value storage. Region keyVal is created
 * for that. Original implementation also used simple disk lists
 * for data storage. These disk lists were used from spools as queues,
 * and from measurement schedule storage as simple list.
 * 
 * This provider 2. wraps underlying gemfire storage inside
 * RegionQueue to give simple queue functionality to spools. Since
 * queue doesn't need keys, original addToList(String listName, String value)
 * method can be used. Measurement schedule 3. is a more complex structure
 * and for efficient usage it needs a key based access. New method
 * addToList(String listName, Object value, Object key) is used to
 * insert items to underlying region. In this case we directly
 * manage gemfire region and we can request iterator over the whole
 * dataset or use query to limit items to iterate.
 */
public class AgentGemFireProvider implements AgentStorageProvider {
    
    private final static Log log = 
        LogFactory.getLog(AgentGemFireProvider.class);
    
    private Region<Object, Object> keyVal;
    private ClientCache c;
    private File writeDir;   // Dir to write stuff to
    
    private Map<String, RegionQueue<Object>> queues;
        
    public AgentGemFireProvider() {
        this.queues = new HashMap<String, RegionQueue<Object>>();
    }

    public String getDescription() {
        return "GemFire provider for local agent persistent storage.";
    }

    public void setValue(String key, String value) {        
        if(value == null){
            if(log.isDebugEnabled())
                log.debug("Removing '" + key + "' from storage");
            keyVal.remove(key);
        } else {
            if(log.isDebugEnabled())
                log.debug("Setting '" + key + "' to '" + value + "'");
            keyVal.put(key, value);
        }
    }

    public String getValue(String key) {
        return (String)keyVal.get(key);
    }

    public Set getKeys() {
        return keyVal.keySet();
    }

    public void flush() throws AgentStorageException {
        // no need to flush
    }

    public void init(String info) throws AgentStorageException {
        
        // Parse out configuration
        StringTokenizer st = new StringTokenizer(info, "|");
        if (st.countTokens() != 5) {
            throw new AgentStorageException(info + " is an invalid agent" +
                                            " storage provider configuration");
        }

        String dir      = st.nextToken();
        this.writeDir   = new File(dir);
        
        c = new ClientCacheFactory()
            .create();
        
        DiskStore ds = c.findDiskStore("keyval");
        if(ds == null) {
            ds = c.createDiskStoreFactory()
                .setDiskDirs(new File[]{writeDir}).create("keyval");
        }
        
        keyVal = c.createClientRegionFactory("LOCAL_PERSISTENT_OVERFLOW").setDiskStoreName("keyval").create("keyval");
        
    }

    public void dispose() {
        // no need to dispose
    }

    /**
     * Method to add items to queue backed regions.
     */
    public void addToList(String listName, String value)
            throws AgentStorageException {
        createList(listName, 0);
        RegionQueue<Object> queue = queues.get(listName);
        queue.add(value);
    }

    /**
     * Meant for measurement schedule cache
     * 
     * @param listName
     * @param value
     * @param key
     * @throws AgentStorageException
     */
    public void addToList(String listName, Object value, Object key)
    throws AgentStorageException {
        Region r = c.getRegion(listName);
        if(r == null) {
            r = createRegion(listName);
        }
        r.put(key, value);
    }

    /**
     * Not accessing queues.
     * 
     * @param listName
     * @param key
     * @return
     * @throws AgentStorageException
     */
    public Object getFromList(String listName, Object key) throws AgentStorageException {
        Region r = c.getRegion(listName);
        if(r == null)
            return null;
        return r.get(key);
    }

    /**
     * 
     * @param listName
     */
    public Iterator getListIterator(String listName) {
        return queues.get(listName).iterator();
    }

    /**
     * 
     * @param listName
     * @param query
     */
    public Iterator getListIterator(String listName, String query) {
        Region r = c.getRegion(listName);
        if(r != null) {
            QueryService queryService = c.getLocalQueryService();
            Query q = queryService.newQuery(query);
            try {
                SelectResults results = (SelectResults)q.execute();
                return new NonQueueSelectResultsIterator(results, r);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }

    public void deleteList(String listName) {
        Region r = c.getRegion(listName);
        if(r != null) {
            r.clear();
        }

    }

    /**
     * Create a gemfire reqion backed queue.
     *
     * This method should only be used from components which
     * needs to use data storage as a queue.
     * 
     * 
     */
    public void createList(String listName, int recSize)
            throws AgentStorageException {
        
        if(queues.get(listName) == null) {
            queues.put(listName, createRegionQueue(listName));
        }
    }

    public void addOverloadedInfo(String listName, String info) {
        // nothing to do
    }

    public boolean isKeyAndQueryProvider() {
        return true;
    }
    
    /**
     * 
     * @param listName
     * @return
     */
    private Region<Object,Object> createRegion(String listName) {
        Region<Object, Object> r = c.getRegion(listName);            
        if(r == null) {
            DiskStore ds = c.findDiskStore(listName);
            if(ds == null) {
                ds = c.createDiskStoreFactory()
                    .setDiskDirs(new File[]{writeDir}).create(listName);
            }
            r = c.createClientRegionFactory("LOCAL_PERSISTENT_OVERFLOW").setDiskStoreName(listName).create(listName);
        }
        return r;
    }
    
    /**
     * 
     * @param listName
     * @return
     */
    private RegionQueue createRegionQueue(String listName) {
        Region<Object, Object> r = createRegion(listName);
        RegionQueue rQ = new RegionQueue(Integer.MAX_VALUE, r, c.getLocalQueryService());
        return rQ;
    }

    public class NonQueueSelectResultsIterator implements Iterator {

        private Iterator iter;
        private Region.Entry current;
        private Region r;
        
        private NonQueueSelectResultsIterator(SelectResults results, Region r) {
            this.iter = results.iterator();
            this.r = r;
        }
        
        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
            current = (Region.Entry)iter.next();
            return current.getValue();
        }

        public void remove() {
            if(current != null)
                r.remove(current.getKey());
        }
        
    }

}
