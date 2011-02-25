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

package org.hyperic.hq.appdef.server.session;

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
import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;

@Entity
@Table(name="EAM_RESOURCE_AUX_LOGS")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ResourceAuxLogPojo implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL")
    @Version
    private Long version;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AUX_LOG_ID",nullable=false)
    @Index(name="METRIC_AUX_LOG_ID_IDX")
    private GalertAuxLog  auxLog;
    
    @Column(name="APPDEF_TYPE",nullable=false)
    private int           appdefType;
    
    @Column(name="APPDEF_ID",nullable=false)
    private int           appdefId;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="DEF_ID",nullable=false)
    @Index(name="RSRC_AUX_LOG_IDX")
    private GalertDef     def;
    
    protected ResourceAuxLogPojo() {
    }

    ResourceAuxLogPojo(GalertAuxLog log, ResourceAuxLog logInfo, GalertDef def) 
    { 
        auxLog     = log;
        appdefType = logInfo.getEntity().getType();
        appdefId   = logInfo.getEntity().getID();
        this.def        = def;
    }
   
    public GalertAuxLog getAuxLog() {
        return auxLog;
    }
    
    protected void setAuxLog(GalertAuxLog log) {
        auxLog = log;
    }
    
    protected int getAppdefType() {
        return appdefType;
    }
    
    protected void setAppdefType(int appdefType) {
        this.appdefType = appdefType;
    }
    
    protected int getAppdefId() {
        return appdefId;
    }
    
    protected void setAppdefId(int appdefId) {
        this.appdefId = appdefId;
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

    public AppdefEntityID getEntityId() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
    }
    
    public GalertDef getAlertDef() {
        return def;
    }
    
    protected void setAlertDef(GalertDef def) {
        this.def = def;
    }
    
    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + getAuxLog().hashCode();
        hash = hash * 31 + getEntityId().hashCode();
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof ResourceAuxLogPojo == false)
            return false;
        
        ResourceAuxLogPojo oe = (ResourceAuxLogPojo)o;

        return oe.getAuxLog().equals(getAuxLog()) &&
               oe.getEntityId().equals(getEntityId());
    }
}