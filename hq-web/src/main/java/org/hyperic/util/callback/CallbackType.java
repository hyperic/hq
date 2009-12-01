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

package org.hyperic.util.callback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

public abstract class CallbackType {
    /**
     * RETURN_LAST is a callback type which invokes all listeners (or until
     * one throws an exception) and returns the last value returned by
     * the invoked method.  If no listeners are registered, a primitive
     * value will be returned from the method.
     */
    public static final CallbackType RETURN_LAST = new CallbackType() {
        public Object callListeners(Method meth, Object[] methArgs,
                                    Set listeners)
            throws Throwable
        {
            Object last = null;
            for (Iterator i=listeners.iterator(); i.hasNext(); ) {
                Object listener = (Object)i.next();
            
                try {
                    last = meth.invoke(listener, methArgs);
                } catch(InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
            
            if (last == null) {
                // Nobody listening ...
                return PrimitiveUtil.getBasicValue(meth.getReturnType());
            }

            return last;
        }
    };
    
    /**
     * This method is called by {@link CallbackDispatcher} to invoke listeners
     * which are setup to be called back.  
     * 
     * @param meth       Method to invoke
     * @param methArgs   Arguments to the method
     * @param listeners  Set of {@link Object} listeners which support
     *                   an invocation of 'meth'.
     */
    public abstract Object callListeners(Method meth, Object[] methArgs,
                                         Set listeners)
        throws Throwable;
}
