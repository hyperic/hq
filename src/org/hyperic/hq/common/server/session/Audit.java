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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;

public abstract class Audit
    extends PersistedObject
{
    private String          _klazz;
    private long            _startTime;
    private long            _endTime;
    private int             _purpose;
    private int             _importance;
    private int             _nature;
    private boolean         _original;
    private AuthzSubject    _subject;
    private String          _message;
    private Resource        _resource;
    private String          _fieldName;
    private String          _oldFieldValue;
    private String          _newFieldValue;
    private Audit           _parent;
    private Collection      _children = new ArrayList();
    
    protected Audit() {}
    
    protected Audit(AuthzSubject subject, Resource r, AuditPurpose purpose,
                    AuditNature nature, AuditImportance importance, 
                    String message)    
    {
        _purpose    = purpose.getCode();
        _importance = importance.getCode();
        _nature     = nature.getCode();
        _subject    = subject;
        _resource   = r;
        _message    = message;
        _original   = true;
    }

    protected String getKlazz() {
        return _klazz;
    }
    
    protected void setKlazz(String k) {
        _klazz = k;
    }
    
    public long getStartTime() {
        return _startTime;
    }
    
    protected void setStartTime(long t) {
        _startTime = t;
    }
    
    public long getEndTime() {
        return _endTime;
    }
    
    protected void setEndTime(long t) {
        _endTime = t;
    }
    
    public AuditPurpose getPurpose() {
        return AuditPurpose.findByCode(_purpose);
    }
    
    protected int getPurposeEnum() {
        return _purpose;
    }
    
    protected void setPurposeEnum(int p) {
        _purpose = p;
    }
    
    public AuditImportance getImportance() {
        return AuditImportance.findByCode(_importance);
    }
    
    protected int getNatureEnum() {
        return _nature;
    }
    
    protected void setNatureEnum(int e) {
        _nature = e;
    }
    
    public AuditNature getNature() {
        return AuditNature.findByCode(_nature);
    }
    
    protected void setImportanceEnum(int p) {
        _importance = p;
    }
    
    protected int getImportanceEnum() {
        return _importance;
    }
    
    public boolean isOriginal() {
        return _original;
    }
    
    protected void setOriginal(boolean o) {
        _original = o;
    }

    public Resource getResource() {
        return _resource;
    }
    
    protected void setResource(Resource r) {
        _resource = r;
    }
    
    public String getFieldName() {
        return _fieldName;
    }
    
    protected void setFieldName(String f) {
        _fieldName = f;
    }
    
    public String getOldFieldValue() {
        return _oldFieldValue;
    }
    
    protected void setOldFieldValue(String f) {
        _oldFieldValue = f;
    }
    
    public String getNewFieldValue() {
        return _newFieldValue;
    }
    
    protected void setNewFieldValue(String v) {
        _newFieldValue = v;
    }
    
    public AuthzSubject getSubject() {
        return _subject;
    }
    
    protected void setSubject(AuthzSubject s) {
        _subject = s;
    }
    
    public String getMessage() {
        return _message;
    }
    
    protected void setMessage(String m) {
        _message = m;
    }
    
    public Audit getParent() {
        return _parent;
    }
    
    protected void setParent(Audit p) {
        _parent = p;
    }
    
    protected Collection getChildrenBag() {
        return _children;
    }
    
    protected void setChildrenBag(Collection c) {
        _children = c;
    }
    
    public Collection getChildren() {
        return Collections.unmodifiableCollection(_children);
    }
    
    void addChild(Audit a) {
        _children.add(a);
        a.setParent(this);
    }
    
    void removeChild(Audit a) {
        _children.remove(a);
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
        return "Audit[purpose=" + _purpose + ",time=" + _startTime + ",msg=" +
            _message + "]";
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof Audit) || !super.equals(obj)) {
            return false;
        }
        
        Audit o = (Audit)obj;
        return o.getImportance().equals(getImportance()) &&
               o.getPurpose().equals(getPurpose()) &&
               o.getMessage().equals(getMessage()) &&
               o.getStartTime() == getStartTime() &&
               o.getKlazz().equals(getKlazz());
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + getImportance().hashCode();
        result = 37 * result + getPurpose().hashCode();
        result = 37 * result + getMessage().hashCode();
        result = 37 * result + System.identityHashCode(new Long(getStartTime()));
        result = 37 * result + getKlazz().hashCode();

        return result;
    }
}
