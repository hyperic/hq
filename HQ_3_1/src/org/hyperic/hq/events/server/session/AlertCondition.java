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

import java.util.Collection;
import java.util.Collections;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.shared.AlertConditionValue;

public class AlertCondition  
    extends PersistedObject
{
    private int                 _type;
    private boolean             _required;
    private int                 _measurementId;
    private String              _name;
    private String              _comparator;
    private double              _threshold;
    private String              _optionStatus;
    private AlertDefinition     _alertDefinition;
    private RegisteredTrigger   _trigger;
    private Collection          _logEntries;

    private AlertConditionValue _value;

    protected AlertCondition() {
    }

    AlertCondition(AlertDefinition def, AlertConditionValue val,
                   RegisteredTrigger trigger) 
    {
        _type            = val.getType();
        _required        = val.getRequired();
        _measurementId   = val.getMeasurementId();
        _name            = val.getName();
        _comparator      = val.getComparator();
        _threshold       = val.getThreshold();
        _optionStatus    = val.getOption();
        _alertDefinition = def;
        _trigger         = trigger;
        _logEntries      = Collections.EMPTY_LIST; 
    }
    
    public int getType() {
        return _type;
    }
    
    protected void setType(int type) {
        _type = type;
    }

    public boolean isRequired() {
        return _required;
    }
    
    protected void setRequired(boolean required) {
        _required = required;
    }

    public int getMeasurementId() {
        return _measurementId;
    }
    
    protected void setMeasurementId(int measurementId) {
        _measurementId = measurementId;
    }

    public String getName() {
        return _name;
    }
    
    protected void setName(String name) {
        _name = name;
    }
    
    public String getComparator() {
        return _comparator;
    }
    
    protected void setComparator(String comparator) {
        _comparator = comparator;
    }
    
    public double getThreshold() {
        return _threshold;
    }
    
    protected void setThreshold(double threshold) {
        _threshold = threshold;
    }
    
    public String getOptionStatus() {
        return _optionStatus;
    }
    
    protected void setOptionStatus(String optionStatus) {
        _optionStatus = optionStatus;
    }
    
    public AlertDefinition getAlertDefinition() {
        return _alertDefinition;
    }
    
    protected void setAlertDefinition(AlertDefinition alertDefinition) {
        _alertDefinition = alertDefinition;
    }
    
    public RegisteredTrigger getTrigger() {
        return _trigger;
    }
    
    protected void setTrigger(RegisteredTrigger trigger) {
        _trigger = trigger;
    }
    
    public Collection getLogEntries() {
        return Collections.unmodifiableCollection(_logEntries);
    }
    
    protected Collection getLogEntriesBag() {
        return _logEntries;
    }

    protected void setLogEntriesBag(Collection logEntries) {
        _logEntries = logEntries;
    }
    
    public AlertConditionValue getAlertConditionValue() {
        if (_value == null) {
            _value = new AlertConditionValue();
        }

        _value.setId(getId());
        _value.setType(getType());
        _value.setRequired(isRequired());
        _value.setMeasurementId(getMeasurementId());
        _value.setName(getName() == null ? "" : getName());
        _value.setComparator(getComparator() == null ? "" : getComparator());
        _value.setThreshold(getThreshold());
        _value.setOption(getOptionStatus() == null ? "" : getOptionStatus());
        if (getTrigger() != null) {
            _value.setTriggerId(getTrigger().getId());
        }
        return _value;
    }

    public void setAlertConditionValue(AlertConditionValue val) {
        TriggerDAO tDAO = DAOFactory.getDAOFactory().getTriggerDAO();

        setType(val.getType());
        setRequired(val.getRequired());
        setMeasurementId(val.getMeasurementId());
        setName(val.getName());
        setComparator(val.getComparator());
        setThreshold(val.getThreshold());
        setOptionStatus(val.getOption());
        setTrigger(tDAO.findById(val.getTriggerId()));
    }
}
