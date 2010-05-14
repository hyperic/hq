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
