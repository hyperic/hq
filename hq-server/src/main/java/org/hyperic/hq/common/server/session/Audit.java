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
import org.hibernate.annotations.Parameter;
import org.hyperic.hq.auth.domain.AuthzSubject;

@Entity
@Table(name = "EAM_AUDIT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "KLAZZ", length = 255)
public abstract class Audit implements Serializable {
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OptimisticLock(excluded = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<Audit> children = new ArrayList<Audit>();

    @Column(name = "END_TIME", nullable = false)
    private long endTime;

    @Column(name = "FIELD", length = 100)
    private String fieldName;

    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_AUDIT_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "IMPORTANCE", nullable = false)
    private int importance;

    @Column(name = "MESSAGE", length = 1000, nullable = false)
    private String message;

    @Column(name = "NATURE", nullable = false)
    private int nature;

    @Column(name = "NEW_VAL", length = 1000)
    private String newFieldValue;

    @Column(name = "OLD_VAL", length = 1000)
    private String oldFieldValue;

    @Column(name = "ORIGINAL", nullable = false)
    private boolean original;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    @Index(name = "PARENT_ID_IDX")
    private Audit parent;

    @Column(name = "PURPOSE", nullable = false)
    private int purpose;

    @Column(name = "RESOURCE_ID", nullable = false)
    @Index(name = "RESOURCE_ID_IDX")
    private Integer resource;

    @Column(name = "START_TIME", nullable = false)
    private long startTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUBJECT_ID", nullable = false)
    @Index(name = "SUBJECT_ID_IDX")
    private AuthzSubject subject;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected Audit() {
    }

    protected Audit(AuthzSubject subject, Integer r, AuditPurpose purpose, AuditNature nature,
                    AuditImportance importance, String message) {
        this.purpose = purpose.getCode();
        this.importance = importance.getCode();
        this.nature = nature.getCode();
        this.subject = subject;
        resource = r;
        this.message = message;
        original = true;
    }

    void addChild(Audit a) {
        children.add(a);
        a.setParent(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Audit other = (Audit) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (importance != other.importance)
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (purpose != other.purpose)
            return false;
        if (startTime != other.startTime)
            return false;
        return true;
    }

    protected String formatHtmlMessage() {
        return getMessage();
    }

    public Collection<Audit> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    protected Collection<Audit> getChildrenBag() {
        return children;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getHtmlMessage() {
        if (!isOriginal()) {
            return getMessage();
        } else {
            return formatHtmlMessage();
        }
    }

    public Integer getId() {
        return id;
    }

    public AuditImportance getImportance() {
        return AuditImportance.findByCode(importance);
    }

    protected int getImportanceEnum() {
        return importance;
    }

    public String getMessage() {
        return message;
    }

    public AuditNature getNature() {
        return AuditNature.findByCode(nature);
    }

    protected int getNatureEnum() {
        return nature;
    }

    public String getNewFieldValue() {
        return newFieldValue;
    }

    public String getOldFieldValue() {
        return oldFieldValue;
    }

    public Audit getParent() {
        return parent;
    }

    public AuditPurpose getPurpose() {
        return AuditPurpose.findByCode(purpose);
    }

    public Integer getResource() {
        return resource;
    }

    public long getStartTime() {
        return startTime;
    }

    public AuthzSubject getSubject() {
        return subject;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + importance;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + purpose;
        result = prime * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    public boolean isOriginal() {
        return original;
    }

    void removeChild(Audit a) {
        children.remove(a);
        a.setParent(null);
    }

    protected void setChildrenBag(Collection<Audit> c) {
        children = c;
    }

    public void setEndTime(long t) {
        endTime = t;
    }

    public void setFieldName(String f) {
        fieldName = f;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setImportanceEnum(int p) {
        importance = p;
    }

    protected void setMessage(String m) {
        message = m;
    }

    protected void setNatureEnum(int e) {
        nature = e;
    }

    public void setNewFieldValue(String v) {
        newFieldValue = v;
    }

    public void setOldFieldValue(String f) {
        oldFieldValue = f;
    }

    protected void setOriginal(boolean o) {
        original = o;
    }

    protected void setParent(Audit p) {
        parent = p;
    }

    protected void setPurpose(int p) {
        purpose = p;
    }

    protected void setResource(Integer r) {
        resource = r;
    }

    public void setStartTime(long t) {
        startTime = t;
    }

    protected void setSubject(AuthzSubject s) {
        subject = s;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String toString() {
        return "Audit[user=" + subject.getName() + ",purpose=" + purpose + ",time=" + startTime +
               ",resource=" + resource + ",msg=" + message +
               "]";
    }

}
