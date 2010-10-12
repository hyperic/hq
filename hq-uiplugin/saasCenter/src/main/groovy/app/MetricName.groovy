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

class MetricName implements Comparable {
    private final String _protoName 
    private final String _metricName
    
    MetricName(String protoName, String metricName) {
        _protoName  = protoName
        _metricName = metricName
    }
    
    public int hashCode() {
        return _protoName.hashCode() + _metricName.hashCode()
    }
    
    public boolean equals(Object rhs) {
        if (!(rhs instanceof MetricName)) {
            return false
        } else if (_protoName.equals(rhs.protoName) &&
                   _metricName.equals(rhs.metricName)) {
            return true
        }
        return false
    }
    
    public String getMetricName() {
        return _metricName
    }
    
    public String getProtoName() {
        return _protoName
    }
    
    public String toString() {
        return "${_protoName} ${_metricName}"
    }
    
    public int compareTo(Object rhs) throws ClassCastException {
        if (!(rhs instanceof MetricName)) {
            throw new ClassCastException()
        }
        def tmp
        if (0 != (tmp = _metricName.compareTo(rhs.metricName))) {
            return tmp
        }
        return _protoName.compareTo(rhs.protoName)
    }
}
