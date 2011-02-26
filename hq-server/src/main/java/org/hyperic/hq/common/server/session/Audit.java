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

package org.hyperic.hq.common.server.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;

@Entity
@Table(name="EAM_AUDIT")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="KLAZZ",length=255)
public abstract class Audit implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="START_TIME",nullable=false)
    private long            startTime;
    
    @Column(name="END_TIME",nullable=false)
    private long            endTime;
    
    @Column(name="PURPOSE",nullable=false)
    private int             purpose;
    
    @Column(name="IMPORTANCE",nullable=false)
    private int             importance;
    
    @Column(name="NATURE",nullable=false)
    private int             nature;
    
    @Column(name="ORIGINAL",nullable=false)
    private boolean         original;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="SUBJECT_ID",nullable=false)
    @Index(name="SUBJECT_ID_IDX")
    private AuthzSubject    subject;
    
    @Column(name="MESSAGE",length=1000,nullable=false)
    private String          message;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="RESOURCE_ID",nullable=false)
    @Index(name="RESOURCE_ID_IDX")
    private Resource        resource;
    
    @Column(name="FIELD",length=100)
    private String          fieldName;
    
    @Column(name="OLD_VAL",length=1000)
    private String          oldFieldValue;
    
    @Column(name="NEW_VAL",length=1000)
    private String          newFieldValue;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="PARENT_ID")
    @Index(name="PARENT_ID_IDX")
    private Audit           parent;
    
    @OneToMany(mappedBy="parent",fetch=FetchType.LAZY,cascade=CascadeType.ALL,orphanRemoval=true)
    @OptimisticLock(excluded=true)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<Audit>      children = new ArrayList<Audit>();
    
    protected Audit() {}
    
    protected Audit(AuthzSubject subject, Resource r, AuditPurpose purpose,
                    AuditNature nature, AuditImportance importance, 
                    String message)    
    {
        this.purpose    = purpose.getCode();
        this.importance = importance.getCode();
        this.nature     = nature.getCode();
        this.subject    = subject;
        resource   = r;
        this.message    = message;
        original   = true;
    }
  
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long t) {
        startTime = t;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long t) {
        endTime = t;
    }
    
    public AuditPurpose getPurpose() {
        return AuditPurpose.findByCode(purpose);
    }
    
    protected int getPurposeEnum() {
        return purpose;
    }
    
    protected void setPurposeEnum(int p) {
        purpose = p;
    }
    
    public AuditImportance getImportance() {
        return AuditImportance.findByCode(importance);
    }
    
    protected int getNatureEnum() {
        return nature;
    }
    
    protected void setNatureEnum(int e) {
        nature = e;
    }
    
    public AuditNature getNature() {
        return AuditNature.findByCode(nature);
    }
    
    protected void setImportanceEnum(int p) {
        importance = p;
    }
    
    protected int getImportanceEnum() {
        return importance;
    }
    
    public boolean isOriginal() {
        return original;
    }
    
    protected void setOriginal(boolean o) {
        original = o;
    }

    public Resource getResource() {
        return resource;
    }
    
    protected void setResource(Resource r) {
        resource = r;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String f) {
        fieldName = f;
    }
    
    public String getOldFieldValue() {
        return oldFieldValue;
    }
    
    public void setOldFieldValue(String f) {
        oldFieldValue = f;
    }
    
    public String getNewFieldValue() {
        return newFieldValue;
    }
    
    public void setNewFieldValue(String v) {
        newFieldValue = v;
    }
    
    public AuthzSubject getSubject() {
        return subject;
    }
    
    protected void setSubject(AuthzSubject s) {
        subject = s;
    }
    
    public String getMessage() {
        return message;
    }
    
    protected void setMessage(String m) {
        message = m;
    }
    
    public Audit getParent() {
        return parent;
    }
    
    protected void setParent(Audit p) {
        parent = p;
    }
    
    protected Collection<Audit> getChildrenBag() {
        return children;
    }
    
    protected void setChildrenBag(Collection<Audit> c) {
        children = c;
    }
    
    public Collection<Audit> getChildren() {
        return Collections.unmodifiableCollection(children);
    }
    
    void addChild(Audit a) {
        children.add(a);
        a.setParent(this);
    }
    
    void removeChild(Audit a) {
        children.remove(a);
        a.setParent(null);
    }
    
    public String getHtmlMessage() {
        if (!isOriginal()) {
            return getMessage();
        } else {
            return formatHtmlMessage();
        }
    }
    
    protected String formatHtmlMessage() {
        return getMessage();
    }
    
    public String toString() {
        return "Audit[user=" + subject.getName() + ",purpose=" + purpose +
               ",time=" + startTime +
               ",resource=" + (resource != null ? resource.getName() : "N/A")+
               ",msg=" + message + "]";
    }
    
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if (!(obj.getClass().equals(getClass())) || !super.equals(obj)) {
            return false;
        }
        
        Audit o = (Audit)obj;
        return o.getImportance().equals(getImportance()) &&
               o.getPurpose().equals(getPurpose()) &&
               o.getMessage().equals(getMessage()) &&
               o.getStartTime() == getStartTime();
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + getImportance().hashCode();
        result = 37 * result + getPurpose().hashCode();
        result = 37 * result + getMessage().hashCode();
        result = 37 * result + System.identityHashCode(new Long(getStartTime()));

        return result;
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
    
    
}
