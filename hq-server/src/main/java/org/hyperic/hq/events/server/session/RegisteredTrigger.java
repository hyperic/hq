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
package org.hyperic.hq.events.server.session;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.ArrayUtil;

@Entity
@Table(name="EAM_REGISTERED_TRIGGER")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class RegisteredTrigger implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="CLASSNAME",nullable=false,length=200)
    private String           className;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CONFIG",columnDefinition="BLOB")
    private byte[]           config;
    
    @Column(name="FREQUENCY",nullable=false)
    private long             frequency;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ALERT_DEFINITION_ID")
    @Index(name="ALERT_DEF_TRIGGER_IDX")
    private ResourceAlertDefinition  alertDef;
    
    
    protected RegisteredTrigger() { // Needed for Hibernate
    }
    
    public RegisteredTrigger(RegisteredTriggerValue val) {
        setRegisteredTriggerValue(val);
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

    protected void setRegisteredTriggerValue(RegisteredTriggerValue val) {
        setClassname(val.getClassname());
        setConfig(ArrayUtil.clone(val.getConfig()));
        setFrequency(val.getFrequency());
    }
    
    /** Get the old style value object
     * @return RegisteredTriggerValue object
     * @deprecated
     */
    public RegisteredTriggerValue getRegisteredTriggerValue() {
        
        RegisteredTriggerValue valueObj = new RegisteredTriggerValue();
        
        valueObj.setId(getId());
        valueObj.setClassname(getClassname());
        // XXX -- Config is mutable here.  The proper thing to do is clone it
        valueObj.setConfig(ArrayUtil.clone(getConfig()));
        valueObj.setFrequency(getFrequency());

        return valueObj;
    }
    
    public String getClassname() {
        return className;
    }

    public byte[] getConfig() {
        return ArrayUtil.clone(config);
    }

    public long getFrequency() {
        return frequency;
    }

    protected void setClassname(String className) {
        this.className = className;
    }

    protected void setConfig(byte[] config) {
        this.config = config;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    public ResourceAlertDefinition getAlertDefinition() {
        return alertDef;
    }
    
    protected void setAlertDefinition(ResourceAlertDefinition def) {
        alertDef = def;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof RegisteredTrigger)) {
            return false;
        }
        Integer objId = ((RegisteredTrigger)obj).getId();
  
        return getId() == objId ||
        (getId() != null && 
         objId != null && 
         getId().equals(objId));     
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }
}
