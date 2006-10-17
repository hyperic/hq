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
package org.hyperic.hq.events;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

public class RegisteredTrigger 
    extends PersistedObject
{
    private String                 _className;
    private byte[]                 _config;
    private long                   _frequency;
    private AlertDefinition        _alertDef;
    
    private RegisteredTriggerValue _valueObj;
    
    protected RegisteredTrigger() { // Needed for Hibernate
    }
    
    public RegisteredTrigger(RegisteredTriggerValue val) {
        setRegisteredTriggerValue(val);
    }
    
    public void setRegisteredTriggerValue(RegisteredTriggerValue val) {
        setClassname(val.getClassname());
        setConfig(val.getConfig());
        setFrequency(val.getFrequency());
    }
    
    public RegisteredTriggerValue getRegisteredTriggerValue() {
        if (_valueObj == null)
            _valueObj = new RegisteredTriggerValue();
        
        _valueObj.setId(getId());
        _valueObj.setClassname(getClassname());
        // XXX -- Config is mutable here.  The proper thing to do is clone it
        _valueObj.setConfig(getConfig());
        _valueObj.setFrequency(getFrequency());

        return _valueObj;
    }
    
    public String getClassname() {
        return _className;
    }

    public byte[] getConfig() {
        // XXX -- We're returning a mutable array.  The proper thing to do 
        //        is clone it
        return _config;
    }

    public long getFrequency() {
        return _frequency;
    }

    public void setClassname(String className) {
        _className = className;
    }

    public void setConfig(byte[] config) {
        // XXX -- Should be cloning the byte array here
        _config = config;
    }

    public void setFrequency(long frequency) {
        _frequency = frequency;
    }
    
    public AlertDefinition getAlertDef() {
        return _alertDef;
    }
    
    public void setAlertDef(AlertDefinition def) {
        _alertDef = def;
    }
}
