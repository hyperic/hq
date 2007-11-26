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
package org.hyperic.util.stats;

/**
 * The StatsCollector is a class used to calculate metrics based on a   
 * series of events.  Events consist of a value and a timestamp.
 * 
 * This class is synchronized.
 */
public class StatsCollector {
    private final Object LOCK = new Object();
    private long[]   _timestamps;
    private double[] _values;
    private int      _start;    // Head of ringbuffer -- points at oldest ent
    private int      _numEnts;  // # entries used in the arrays
    private int      _size;     // Size allocated for the arrays
   
    // Running stats
    private double   _total;    // Sum of everything in the values array
    
    /**
     * Create a new collector which is able to internally store 'size'
     * elements.
     */
    public StatsCollector(int size) {
        _timestamps = new long[size];
        _values     = new double[size];
        _start      = 0;
        _numEnts    = 0;
        _size       = size;
        _total      = 0;
    }

    /**
     * Add a value to the collection.  Old values will be evicted from the
     * collection if the addition would exceed the internal size.
     */
    public void add(double value, long timestamp) {
        synchronized (LOCK) {
            if (_numEnts == _size) {
                // Remove oldest entry at _values[_start]
                _total -= _values[_start];
                _values[_start]     = value;
                _timestamps[_start] = timestamp;
                _start++;
                if (_start == _size)
                    _start = 0;
            } else {
                int insertIdx = _start + _numEnts;
                _values[insertIdx]     = value;
                _timestamps[insertIdx] = timestamp;
                _numEnts++;
            }
            _total += value;
        }
    }

    /**
     * Get the sum of all values.
     */
    public double getTotal() {
        synchronized (LOCK) {
            return _total;
        }
    }

    /**
     * Get the timestamp of the oldest entry.
     */
    public long getOldestTime() {
        synchronized (LOCK) {
            return _timestamps[_start];
        }
    }
    
    /**
     * Get the timestamp of the newest entry.
     */
    public long getNewestTime() {
        synchronized (LOCK) {
            int idx = _start + (_numEnts - 1);
            if (idx >= _size)
                idx -= _size;
            return _timestamps[idx];
        }
    }

    /**
     * Get the value per timestamp increment.  I.e:
     * 
     *   getTotal() / (getNewestTime() - getOldestTime())
     */
    public double valPerTimestamp() {
        synchronized (LOCK) {
            if (_numEnts < 2)
                return Double.NaN;
            
            long runtime = getNewestTime() - getOldestTime();
            return getTotal() / runtime;
        }
    }
    
    /**
     * Get the # of elements in the collector
     */
    public int getSize() {
        synchronized (LOCK) {
            return _numEnts;
        }
    }
    
    public String dump() {
        synchronized (LOCK) {
            StringBuffer res = new StringBuffer();
            int idx = _start;
        
            res.append("Size=" + _size + " Start=" + _start + " numEnts=" + 
                       _numEnts + " valPerTimestamp=" + valPerTimestamp() + 
                       " oldest=" + getOldestTime() + 
                       " newest=" + getNewestTime() + "\n");
            for (int i=0; i<_numEnts; i++) {
                res.append("Value[" + idx + "]  val=" + _values[idx] + 
                           " ts=" + _timestamps[idx] + "\n");
                idx++;
                if (idx == _size)
                    idx = 0;
            }
            return res.toString();
        }
    }
    
    public static void main(String[] args) {
        StatsCollector x = new StatsCollector(2);
        
        x.add(100, 1);
        System.out.println(x.dump());
        x.add(200, 2);
        System.out.println(x.dump());
        x.add(300, 3);
        System.out.println(x.dump());
        x.add(400, 4);
        System.out.println(x.dump());
    }
}
