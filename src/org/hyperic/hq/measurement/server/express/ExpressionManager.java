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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;

import instantj.compile.CompilationFailedException;
import instantj.reflect.IllegalPropertyException;
import instantj.expression.Expression;
import instantj.expression.EvaluationFailedException;

/**
 * ExpressionManager provides the realization of an expression cache manager. It
 * builds an expression cache into its evaluate method and also provides expression
 * deserialization capability.
 */
public class ExpressionManager {
    private static ExpressionManager expressionManager = null;
    private ExpressionCache 	expressionCache   = null;
    private final String 		logCtx            = "org.hyperic.hq.measurement.express.ExpressionManager";
    private final Log           log               = LogFactory.getLog(logCtx);

    private ExpressionManager() {
        init();
    }

    /** Return the single instance of ExpressionManager
     * */
    public static ExpressionManager getInstance() {
        if (expressionManager == null)
            expressionManager = new ExpressionManager();
    	return expressionManager;
    }

    private void init () {
        // initalize an empty cache. We may want instead have the Cache start
        // empty and grow to max size where max size is some function of total
        // measurements OR total measurments who's interval is less than x.
        expressionCache = new ExpressionCache(MeasurementConstants.EXPRESSION_CACHE_SIZE);
    }

    /** Verify that the inMemory expression cache contains the expression
     *  identified by the key.
     *  @param Integer
     *  @return true if cached
     */
    public boolean isCached(Integer key) {
       	return expressionCache.exists(key);
    }

    /**
     * Evaluate the expression identified by id. Will attempt to use an
     * inMemory cache version. If not available, will try to reconstitute
     * one from the serialized data bytes.  Lastly, it will compile its own
     * version.
     * */
    public Double evaluate (Integer id, String body, Map types,
        					String[] imports) {
        return evaluate (id,body,types,imports,null);
    }
    public Double evaluate (Integer id, String body, Map values,
        					String[] imports, byte[] expressionData) {
        Expression expression = null;
        Double retVal = null;

        // First check the inmemory cache.
        if (expressionCache.exists(id)) {
            try {
            	expression = (Expression) expressionCache.get(id);
            	retVal = (Double) expression.getInstance(values).evaluate();
            }
            catch (Exception e){
                if (log.isErrorEnabled())
                	log.error("ExpressionManager.evalute() - searching cache",e);
            }
        }
        else if (expressionData != null) {
            try {
                if (log.isDebugEnabled())
					log.debug("ExpressionManager.evalute() - attempting "+
                    "to deserialize");
                expression = ExpressionUtil.deSerialize(expressionData);
                retVal = (Double) expression.getInstance(values).evaluate();
                expressionCache.put(id,expression);
            }
            catch (Exception e){
                if (log.isErrorEnabled())
                	log.error("Exception during deserialization",e);
            }

        }
        // Lastly, if we haven't been successful, then use brute force and
        // compile it again.
        if (retVal == null) {
			// build a map of properties mapping name to type
			HashMap types = new HashMap();
			for (Iterator i = values.keySet().iterator(); i.hasNext(); ) {
				Object type = i.next();
				types.put (type, ExpressionUtil.calcClassFromName(
						values.get(type).getClass().getName() ));
			}
		   try {

				if (log.isDebugEnabled())
					log.debug ("template="+body+"  types="+types.toString()+
                    	" values="+values.toString());

				expression = new Expression(body, types, arrayToCollection(imports));
				// Put expression (back) in Map to ensure it's the "last in"
				// Protect from concurrent modification.
				expressionCache.put(id, expression);
	
				retVal = (Double) expression.getInstance(values).evaluate();
			}
			catch (CompilationFailedException e) {
                if (log.isErrorEnabled())
                	log.error("Exception compiling expression.",e);
			}
			catch (ExpressionCacheException e) {
                if (log.isErrorEnabled())
                	log.error("Exception caching expression.",e);
			}
			catch (IllegalPropertyException e) {
                if (log.isErrorEnabled())
                	log.error("Exception with property value of expression.",e);
			}
			catch (EvaluationFailedException e){
                if (log.isErrorEnabled())
                	log.error("Exception evaluating expression.",e);
			}
        }
        return retVal;
    }
    /** Serializes the expression identified by id and returns the bytes.*/
    public byte[] getExpressionBytes (Integer id) {
        byte[] retVal = null;
        try {
        	Expression expression = expressionCache.get(id);
        	retVal = ExpressionUtil.serializeExpression(expression);
        } catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Exception during serialization of expression.",e);
        }
        return retVal;
    }

    private Collection arrayToCollection (String[] sa) {
        Collection c = new ArrayList(sa.length);
        for (int i=0;i<sa.length;i++)
            c.add(sa[i]);
        return c;
    }



}
