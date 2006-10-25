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

import org.hyperic.hibernate.PersistedObject;

public class Alert 
    extends PersistedObject
{
    private long            _ctime;
    private AlertDefinition _alertDefinition;
    private Collection      _actions;
    private Collection      _conditions;
    private Collection      _userAlerts;

    protected Alert() {
    }

    public long getCtime() {
        return _ctime;
    }
    
    protected void setCtime(long ctime) {
        _ctime = ctime;
    }
    
    public AlertDefinition getAlertDef() {
        return _alertDefinition;
    }
    
    protected void setAlertDef(AlertDefinition alertDefinition) {
        _alertDefinition = alertDefinition;
    }

    public Collection getActions() {
        return Collections.unmodifiableCollection(_actions);
    }
    
    protected Collection getActionsBag() {
        return _actions;
    }
    
    protected void setActionsBag(Collection actions) {
        _actions = actions;
    }
    
    public Collection getConditions() {
        return Collections.unmodifiableCollection(_conditions);
    }
    
    protected Collection getConditionsBag() {
        return _conditions;
    }
    
    protected void setConditionsBag(Collection conditions) {
        _conditions = conditions;
    }
    
    public Collection getUserAlerts() {
        return Collections.unmodifiableCollection(_userAlerts);
    }
    
    protected Collection getUserAlertsBag() {
        return _userAlerts;
    }
    
    protected void setUserAlertsBag(Collection userAlerts) {
        _userAlerts = userAlerts;
    }
}
