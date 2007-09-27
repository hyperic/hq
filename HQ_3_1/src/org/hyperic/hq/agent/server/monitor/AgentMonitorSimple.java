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

package org.hyperic.hq.agent.server.monitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

import org.hyperic.hq.agent.AgentMonitorValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class which implements the AgentMonitorInterface to make
 * monitoring simpler for subclasses.  
 *
 * Classes extending this class can have methods of the following
 * form:
 *
 * {String,double,String[],int[]} get*()
 *    throws AgentMonitorException
 *
 * where get* = getSomething
 *
 * Methods which do not match this signature exactly will not be
 * exposed.
 *
 * These methods are exposed through the monitoring interface.
 */
public abstract class AgentMonitorSimple 
    implements AgentMonitorInterface
{
    // Hash of key names onto methods
    private HashMap   methodMap;
    private String[]  methodKeys;
    private int[]     methodTypes;
    private Log       log;


    public AgentMonitorSimple(){
        this.log = LogFactory.getLog(this.getClass());
        this.methodMap = new HashMap();
        this.setupMethodMap();
    }

    /**
     * Go through all the methods that we define to find ones which
     * match the signature we're looking for
     */ 
    private void setupMethodMap(){
        Method[] methods;
        Class asClass = String[].class, aiClass = int[].class;
        int j;

        methods = this.getClass().getMethods();
        for(int i=0; i<methods.length; i++){
            Method m = methods[i];
            Class[] exceptions;
            Class retType;

            if(m.getName().startsWith("get") == false ||
               m.getParameterTypes().length != 0)
            {
                continue;
            }

            exceptions = m.getExceptionTypes();
            if(exceptions.length != 1 ||
               exceptions[0].equals(AgentMonitorException.class) == false)
            {
                continue;
            }
            
            retType = m.getReturnType();
            if(retType.equals(String.class) == false &&
               retType.equals(Double.TYPE) == false &&
               retType.equals(asClass) == false &&
               retType.equals(aiClass) == false)
            {
                continue;
            }
            
            this.methodMap.put(m.getName().substring(3), m);
        }

        this.methodKeys  = new String[this.methodMap.size()];
        this.methodTypes = new int[this.methodKeys.length];

        j = 0;
        for(Iterator i=this.methodMap.keySet().iterator(); i.hasNext(); j++){
            String key = (String)i.next();
            Method m = (Method)this.methodMap.get(key);
            Class retType = m.getReturnType();
            int type;

            this.methodKeys[j] = key;
            if(retType.equals(String.class))
                type = AgentMonitorValue.TYPE_STRING;
            else if(retType.equals(Double.TYPE))
                type = AgentMonitorValue.TYPE_DOUBLE;
            else if(retType.equals(asClass))
                type = AgentMonitorValue.TYPE_ASTRING;
            else if(retType.equals(aiClass))
                type = AgentMonitorValue.TYPE_AINT;
            else
                throw new IllegalStateException("Should never get here");

            this.methodTypes[j] = type;
        }
    }

    /**
     * Get a value of monitorKeys
     *
     * @param monitorKeys Keys that the monitor recognizes
     * @return A value for each monitorKey presented
     */
    public AgentMonitorValue[] getMonitorValues(String[] monitorKeys){
        Object[] noArgs = new Object[0];
        AgentMonitorValue[] res;

        res = new AgentMonitorValue[monitorKeys.length];
        for(int i=0; i<monitorKeys.length; i++){
            String key = monitorKeys[i];
            Method m = (Method)this.methodMap.get(key);
            AgentMonitorValue val;

            val = new AgentMonitorValue();
            if(m == null){
                val.setErrCode(AgentMonitorValue.ERR_BADKEY);
            } else {
                try {
                    Object invokeRes;
                    Class resClass;

                    invokeRes = m.invoke(this, noArgs);
                    resClass  = invokeRes.getClass();
                    if(resClass.equals(String.class)){
                        val.setValue((String)invokeRes);
                    } else if(resClass.equals(Double.TYPE) ||
                              resClass.equals(Double.class))
                    {
                        val.setValue(((Double)invokeRes).doubleValue());
                    } else if(resClass.equals(String[].class)){
                        val.setValue((String[])invokeRes);
                    } else if(resClass.equals(int[].class)){
                        val.setValue((int[])invokeRes);
                    } else {
                        throw new IllegalStateException("Unhandled res type: "+
                                                        resClass);
                    }
                } catch(IllegalAccessException exc){
                    this.log.error("Unable to access monitor method '" + key +
                                   "': " + exc.getMessage(), exc);
                    val.setErrCode(AgentMonitorValue.ERR_INTERNAL);
                } catch(IllegalArgumentException exc){
                    // Should never occur
                    this.log.error("Invalid arguments passed to monitor " +
                                   "method '" + key + "': " + exc.getMessage(),
                                   exc);
                    val.setErrCode(AgentMonitorValue.ERR_INTERNAL);
                } catch(InvocationTargetException exc){
                    Throwable tExc = exc.getTargetException();

                    if(tExc instanceof AgentMonitorIncalculableException){
                        val.setErrCode(AgentMonitorValue.ERR_INCALCULABLE);
                    } else if(tExc instanceof AgentMonitorInternalException){
                        val.setErrCode(AgentMonitorValue.ERR_INTERNAL);
                    } else {
                        val.setErrCode(AgentMonitorValue.ERR_INTERNAL);
                        this.log.error("Monitor method '" + key + "' threw " +
                                       "a runtime exception: " + 
                                       tExc.getMessage(), tExc);
                    }
                }
            }

            res[i] = val;
        }
        return res;
    }

    public String[] getMonitorKeys() 
        throws AgentMonitorException 
    {
        return this.methodKeys;
    }
    
    public int[] getMonitorTypes() 
        throws AgentMonitorException 
    {
        return this.methodTypes;
    }
}
