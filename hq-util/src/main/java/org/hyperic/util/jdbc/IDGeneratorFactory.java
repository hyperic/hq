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

package org.hyperic.util.jdbc;

import java.sql.SQLException;
import java.util.HashMap;

import org.hyperic.util.ConfigPropertyException;

/**
 * This factory is meant to be the central point of access for IDGenerator
 * objects used by an application. It will hold the single IDGenerator in a
 * private map and reuse it across repeated invocations of getNextId(). It is
 * particularly useful for Entity bean scenarios, where multiple instances of an
 * IDGenerator result in database row contention and SQL timeouts if more than
 * one thread attempts to get a new id from the sequence at the same time.
 */
public class IDGeneratorFactory {

    // the map stores a key with the sequence name, and a value of the 
    // IDGenerator itself
    private static HashMap generatorMap = new HashMap();
    private static int DEFAULT_INTERVAL = 50;
    
    /**
     * Get a new ID from the named sequence, accessible through the named
     * datasource string.
     * @param ctx - the logging context. Typically the name of the calling
     * class
     * @param sequenceName - the name of the sequence to retrieve the new id from
     * @param dsName - the JNDI name of the datasource
     */
    public static long getNextId(String ctx, 
                                 String sequenceName, 
                                 DBUtil dbUtil)
        throws ConfigPropertyException, 
               SequenceRetrievalException, 
               SQLException {
        IDGenerator theGenerator = getGenerator(ctx, sequenceName, dbUtil);                   
        return theGenerator.getNewID();
    }
    
    private static IDGenerator getGenerator(String ctx, String sequenceName, 
                                             DBUtil dbUtil) 
        throws ConfigPropertyException,
               SequenceRetrievalException,
               SQLException {
    	
    	IDGenerator result = null;
    	synchronized (generatorMap) {
    		result = (IDGenerator) generatorMap.get(sequenceName);
    	
    		if (result == null) {
                result = new IDGenerator(ctx, sequenceName,
                						DEFAULT_INTERVAL,  dbUtil);
                generatorMap.put(sequenceName, result); 
            }
        }
    	
        return result;                                                                    
    }
}
