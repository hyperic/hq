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

package org.hyperic.hq.measurement.server.express;

import instantj.expression.Expression;
import org.hyperic.util.collection.FIFOIntMap;

/**
* ExpressionCache realizes a First in, First out data store for InstantJ
* Expression objects. A FIFO data structure ensures the cache will not grow
* beyond a configurable size.
*/

public class ExpressionCache {

    private FIFOIntMap cache;

    /**
     * @link   aggregationByValue 
     */
    private ExpCacheEntry lnkExpCacheEntry;

    public ExpressionCache (int size) {
        init(size);
    }

    // initialize the datastructure.
    private void init (int size) {
        cache = new FIFOIntMap(size);
    }

    /** Lookup and return Expression matching key or null if not found.
     * @param measurementId key
     * @return instance of Expression.
     * */
    public Expression get (Integer key) {
        Expression retVal = null;
        if (exists(key)) {
            retVal = ((ExpCacheEntry) cache.get(key)).getExpression();
        }
        return retVal;
    }

    /** Adds new Expressions to the cache.
     * @param measurementId key
     * @param Expression
     * */
    public void put (Integer key, Expression expression)
        throws	ExpressionCacheException {

		try {
			ExpCacheEntry entry = new ExpCacheEntry(key,expression);

			synchronized (this) {
				cache.put(key,entry);
			}
        }
        catch (Exception ex) {
            throw new ExpressionCacheException(ex.getMessage());
        }
    }

    /** Removes an expression from the cache.
     *  @param key/id of measurement expression to remove.
     *  @return success flag
     * */
    public boolean remove (Integer key) throws ExpressionCacheException {
        try {
            if (! exists(key))
                return false;
            synchronized (this) {
                cache.remove(key);
            }
        } catch (Exception ex) {
            throw new ExpressionCacheException(ex.getMessage());
        }
        return true;
    }

    /** Verify existance of key
     * @param measurementId key
     * @return instance of Expression.
     * */
    public boolean exists (Integer key) {
        return cache.containsKey(key);
    }
}
