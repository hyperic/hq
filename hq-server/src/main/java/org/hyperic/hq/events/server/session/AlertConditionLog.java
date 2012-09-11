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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.shared.AlertConditionLogValue;

public class AlertConditionLog  
    extends PersistedObject
{
    public static final int MAX_LOG_LENGTH = 250;
    
    private String         _value;
    private Alert          _alert;
    private AlertCondition _condition;

    private AlertConditionLogValue _valueObj;
    
    protected AlertConditionLog() {
    }

    protected AlertConditionLog(Alert alert, String value, 
                                AlertCondition condition)
    {
        if (value != null && value.length() >= MAX_LOG_LENGTH)
            value = value.substring(0, MAX_LOG_LENGTH);
        
        setValue(value);
        setAlert(alert);
        setCondition(condition);
    }

    public String getValue() {
        return _value;
    }
    
    protected void setValue(String value) {
        if(value != null) value = value.replace('\0', '\n') ; 
        _value = value;
    }

    protected Alert getAlert() {
        return _alert;
    }
    
    protected void setAlert(Alert alert) {
        _alert = alert;
    }

    public AlertCondition getCondition() {
        return _condition;
    }
    
    protected void setCondition(AlertCondition condition) {
        _condition = condition;
    }

    public AlertConditionLogValue getAlertConditionLogValue() {
        if (_valueObj == null) {
            _valueObj = new AlertConditionLogValue();
        }

        _valueObj.setId(getId());
        _valueObj.setValue(getValue() == null ? "" : getValue());
        if (getCondition() != null)
            _valueObj.setCondition(getCondition().getAlertConditionValue());
        else
            _valueObj.setCondition(null);

        return _valueObj;
    }
}
