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

package org.hyperic.hq.measurement.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.measurement.galerts.MetricAuxLog;
import org.hyperic.hq.measurement.galerts.MetricAuxLogProvider;

public class MetricAuxLogPojo
    extends PersistedObject
{
    private GalertAuxLog       _auxLog;
    private Measurement        _metric;
    private GalertDef          _def;
    
    protected MetricAuxLogPojo() {
    }

    MetricAuxLogPojo(GalertAuxLog log, MetricAuxLog logInfo, GalertDef def) {
        _auxLog = log;
        _metric = logInfo.getMetric();
        _def    = def;
    }

    public GalertAuxLog getAuxLog() {
        return _auxLog;
    }
    
    protected void setAuxLog(GalertAuxLog log) {
        _auxLog = log;
    }
    
    public Measurement getMetric() {
        return _metric;
    }
    
    protected void setMetric(Measurement metric) {
        _metric = metric;
    }

    public GalertDef getAlertDef() {
        return _def;
    }
    
    protected void setAlertDef(GalertDef def) {
        _def = def;
    }
    
    public AlertAuxLogProvider getProvider() {
        return MetricAuxLogProvider.INSTANCE;
    }

    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + getAuxLog().hashCode();
        hash = hash * 31 + getMetric().hashCode();
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof MetricAuxLogPojo == false)
            return false;
        
        MetricAuxLogPojo oe = (MetricAuxLogPojo)o;

        return oe.getAuxLog().equals(getAuxLog()) &&
               oe.getMetric().equals(getMetric());
    }
}
