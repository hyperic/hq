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

package org.hyperic.hq.application;

import java.lang.reflect.Method;

public class MethodStats {
    private Object  LOCK = new Object();
    private String  _prettyName;
    private String  _className;
    private String  _methName;
    private long    _numCalls;
    private long    _minTime;
    private long    _maxTime;
    private long    _totalTime;
    private long    _numFailures;
    
    MethodStats(Class c, Method meth) {
        _prettyName  = makePrettyName(c, meth);
        _className   = c.getName();
        _methName    = meth.getName();
        _numCalls    = 0;
        _minTime     = Long.MAX_VALUE;
        _maxTime     = Long.MIN_VALUE;
        _totalTime   = 0;
        _numFailures = 0;
    }
    
    private String makePrettyName(Class c, Method meth) {
        StringBuffer buf = new StringBuffer();
        
        buf.append(c.getName())
           .append("#")
           .append(meth.getName())
           .append("(");
           
        Class[] params = meth.getParameterTypes();
        for (int i=0; i<params.length; i++){
            buf.append(params[i].getName());
            if (i != (params.length - 1))
                buf.append(", ");
        }
        buf.append(")");
        return buf.toString();
    }
    
    public String getClassName() {
        return _className;
    }
    
    public String getMethodName() {
        return _methName;
    }

    public String getName() {
        return _prettyName;
    }
    
    public long getCalls() {
        synchronized (LOCK) {
            return _numCalls;
        }
    }
    
    public long getMin() {
        synchronized (LOCK) {
            return _minTime;
        }
    }
    
    public long getMax() {
        synchronized (LOCK) {
            return _maxTime;
        }
    }
    
    public long getTotal() {
        synchronized (LOCK) {
            return _totalTime;
        }
    }
    
    public long getFailures() {
        synchronized (LOCK) {
            return _numFailures;
        }
    }
    
    public double getAverage() {
        synchronized (LOCK) {
            return (double)_totalTime / (double)_numCalls;
        }
    }

    /**
     * Update with the time it took for a single invocation.
     */
    void update(long invokeTotal, boolean failed) {
        synchronized (LOCK) {
            _numCalls++;
            if (invokeTotal < _minTime)
                _minTime = invokeTotal;
            if (_maxTime < invokeTotal)
                _maxTime = invokeTotal;
            _totalTime += invokeTotal;
            if (failed)
                _numFailures++;
        }
    }
    
    public String toString() {
        return getName() + " num=" + getCalls() + 
            " min=" + getMin() + " max=" + getMax() + 
            " tot=" + getTotal();
    }
}
