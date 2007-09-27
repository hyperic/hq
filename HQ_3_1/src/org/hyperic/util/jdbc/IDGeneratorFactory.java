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

import java.util.HashMap;
import org.hyperic.util.ConfigPropertyException;
import javax.naming.NamingException;
import java.sql.SQLException;

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
                                 String dsName)
        throws ConfigPropertyException, 
               NamingException, 
               SequenceRetrievalException, 
               SQLException {
        IDGenerator theGenerator = getGenerator(ctx, sequenceName, dsName);                   
        return theGenerator.getNewID();
    }
    
    private static IDGenerator getGenerator(String ctx, String sequenceName, 
                                            String dsName) 
        throws ConfigPropertyException,
               NamingException,
               SequenceRetrievalException,
               SQLException {
        if(!generatorMap.containsKey(sequenceName)) {
            synchronized(generatorMap) {
                createGenerator(ctx, sequenceName, dsName);
            }
        }
        return (IDGenerator)generatorMap.get(sequenceName);                                                                    
    }

    private static synchronized void createGenerator(String ctx, String seq, 
                                                     String dsName) 
        throws ConfigPropertyException,
               NamingException,
               SequenceRetrievalException,
               SQLException {
        if (!generatorMap.containsKey(seq)) {                          
            IDGenerator aGenerator = new IDGenerator(ctx,
                                                 seq,
                                                 DEFAULT_INTERVAL,
                                                 dsName);
            generatorMap.put(seq, aGenerator); 
        }
    }
}
