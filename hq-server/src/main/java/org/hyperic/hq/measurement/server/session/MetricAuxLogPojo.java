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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.measurement.galerts.MetricAuxLog;
import org.hyperic.hq.measurement.galerts.MetricAuxLogProvider;

@Entity
@Table(name="EAM_METRIC_AUX_LOGS")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class MetricAuxLogPojo implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AUX_LOG_ID",nullable=false)
    @Index(name="AUX_LOG_ID_IDX")
    private GalertAuxLog       auxLog;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="METRIC_ID",nullable=false)
    @Index(name="AUX_LOG_METRIC_ID_IDX")
    private Measurement        metric;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="DEF_ID",nullable=false)
    @Index(name="METRIC_AUX_LOG_IDX")
    private GalertDef          def;
    
    protected MetricAuxLogPojo() {
    }

    MetricAuxLogPojo(GalertAuxLog log, MetricAuxLog logInfo, GalertDef def) {
        auxLog = log;
        metric = logInfo.getMetric();
        this.def    = def;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public GalertAuxLog getAuxLog() {
        return auxLog;
    }
    
    protected void setAuxLog(GalertAuxLog log) {
        auxLog = log;
    }
    
    public Measurement getMetric() {
        return metric;
    }
    
    protected void setMetric(Measurement metric) {
        this.metric = metric;
    }

    public GalertDef getAlertDef() {
        return def;
    }
    
    protected void setAlertDef(GalertDef def) {
        this.def = def;
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
