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

package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;

public class GalertAuxLog
    extends PersistedObject 
{ 
    private long         _timestamp;
    private GalertLog    _alert;
    private int          _auxType;
    private String       _description;
    private GalertAuxLog _parent;
    private Collection   _children;
    private GalertDef    _def;
    
    protected GalertAuxLog() {}
    
    GalertAuxLog(GalertLog alert, AlertAuxLog log, GalertAuxLog parent) {
        _timestamp   = log.getTimestamp();
        _alert       = alert;
        if (log.getProvider() == null)
            _auxType = 0;
        else
            _auxType = log.getProvider().getCode();
        _description = log.getDescription();
        _parent      = parent;
        _children    = new ArrayList();
        
        if (_parent != null) {
            _parent.getChildrenBag().add(this);
        }
        _def = alert.getAlertDef();
    }
    
    public long getTimestamp() {
        return _timestamp;
    }
    
    protected void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }
    
    public GalertLog getAlert() {
        return _alert;
    }
    
    protected void setAlert(GalertLog alert) {
        _alert = alert;
    }
    
    public AlertAuxLogProvider getProvider() {
        return AlertAuxLogProvider.findByCode(getAuxType());
    }
    
    protected int getAuxType() {
        return _auxType;
    }
    
    protected void setAuxType(int auxType) {
        _auxType = auxType;
    }
    
    public String getDescription() {
        return _description;
    }
    
    protected void setDescription(String description) {
        _description = description;
    }
    
    public GalertAuxLog getParent() {
        return _parent;
    }
    
    protected void setParent(GalertAuxLog parent) {
        _parent = parent;
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
    
    public GalertDef getAlertDef() {
        return _def;
    }
    
    protected void setAlertDef(GalertDef def) {
        _def = def;
    }
    
    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + (int)getTimestamp();
        hash = hash * 31 + getAlert().hashCode();
        hash = hash * 31 + getDescription().hashCode();
        hash = hash * 31 + (getParent() == null ? 0 : getParent().hashCode());
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof GalertLog == false)
            return false;
        
        GalertAuxLog oe = (GalertAuxLog)o;

        return oe.getTimestamp() == getTimestamp() &&
               oe.getAlert().equals(getAlert()) &&
               oe.getDescription().equals(getDescription()) &&
               ((oe.getParent() == getParent()) ||
                 getParent() != null && getParent().equals(oe.getParent()));
    }
}
