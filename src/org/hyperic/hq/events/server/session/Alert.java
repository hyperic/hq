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
import java.util.Iterator;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertValue;

public class Alert 
    extends PersistedObject
{
    private long            _ctime;
    private AlertDefinition _alertDefinition;
    private Collection      _actions;
    private Collection      _conditions;
    private Collection      _userAlerts;

    private AlertValue      _alertVal;

    protected Alert() {
    }

    protected Alert(AlertDefinition def, AlertValue val) {
        val.cleanConditionLog();
        val.cleanActionLog();
        
        // Now just set the entire value object
        setAlertValue(val);

        setAlertDef(def);
    }
    
    protected UserAlert createUserAlert(Integer userId) {
        UserAlert ua = new UserAlert(this, userId);
        
        _userAlerts.add(ua);
        return ua;
    }
    
    protected AlertActionLog createActionLog(String detail, Action action) {
        AlertActionLog res = new AlertActionLog(this, detail, action);
        
        _actions.add(res);
        return res;
    }
    
    protected AlertConditionLog createConditionLog(String value,
                                                   AlertCondition cond)
    {
        AlertConditionLog res = new AlertConditionLog(this, value, cond);

        _conditions.add(res);
        return res;
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
    
    private void addActionLog(AlertActionLog aal) {
        _actions.add(aal);
    }
    
    private void removeActionLog(AlertActionLog aal) {
        _actions.remove(aal);
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
    
    private void addConditionLog(AlertConditionLog acl) {
        _conditions.add(acl);
    }
    
    private void removeConditionLog(AlertConditionLog acl) {
        _conditions.remove(acl);
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
    

    public org.hyperic.hq.events.shared.AlertValue getAlertValue() {
        if (_alertVal == null) 
            _alertVal = new AlertValue();

        _alertVal.setId(getId());
        _alertVal.setAlertDefId(getAlertDef().getId());
        _alertVal.setCtime(getCtime());

        _alertVal.removeAllConditionLogs();
      
        for (Iterator i=getConditions().iterator(); i.hasNext(); ) {
            AlertConditionLog l = (AlertConditionLog)i.next();
          
            _alertVal.addConditionLog(l.getAlertConditionLogValue());
        }
        _alertVal.cleanConditionLog();

        _alertVal.removeAllActionLogs();
        for (Iterator i=getActions().iterator(); i.hasNext(); ) {
            AlertActionLog l = (AlertActionLog)i.next();
          
            _alertVal.addActionLog(l.getAlertActionLogValue());
        }
        _alertVal.cleanActionLog();
      
        return _alertVal;
    }

    protected void setAlertValue(AlertValue val) {
        DAOFactory daoFactory = DAOFactory.getDAOFactory();
        AlertDefinitionDAO aDao = daoFactory.getAlertDefDAO();
        AlertDefinition def = aDao.findById(val.getAlertDefId());
        AlertActionLogDAO alDao = daoFactory.getAlertActionLogDAO();
        AlertConditionLogDAO aclDao = daoFactory.getAlertConditionLogDAO();
        
        setAlertDef(def);
        setCtime(val.getCtime());

        for (Iterator i=val.getAddedConditionLogs().iterator(); i.hasNext(); ){
            AlertConditionLogValue lv = (AlertConditionLogValue)i.next();

            addConditionLog(aclDao.findById(lv.getId()));
        }
        
        for (Iterator i=val.getRemovedConditionLogs().iterator(); i.hasNext();){
            AlertConditionLogValue lv = (AlertConditionLogValue)i.next();

            removeConditionLog(aclDao.findById(lv.getId()));
        }

        for (Iterator i=val.getAddedActionLogs().iterator(); i.hasNext(); ) {
            AlertActionLogValue lv = (AlertActionLogValue)i.next();
            
            addActionLog(alDao.findById(lv.getId()));
        }
        
        for (Iterator i=val.getRemovedActionLogs().iterator(); i.hasNext(); ) {
            AlertActionLogValue lv = (AlertActionLogValue)i.next();
            
            removeActionLog(alDao.findById(lv.getId()));
        }
    }
}
