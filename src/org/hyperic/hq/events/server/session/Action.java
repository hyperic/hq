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
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.util.ArrayUtil;

public class Action  
    extends PersistedObject
{
    private String          _className;
    private byte[]          _config;
    private Action          _parent;
    private AlertDefinition _alertDef;
    private Collection      _logEntries;

    private ActionValue     _valueObj;
    
    protected Action() {
    }

    public Action(AlertDefinition def, ActionValue val, Action parent) {
        _className  = val.getClassname();
        _config     = ArrayUtil.clone(val.getConfig());
        _parent     = parent;
        _alertDef   = def;
        _logEntries = Collections.EMPTY_LIST;
    }
    
    public String getClassName() {
        return _className;
    }
    
    protected void setClassName(String className) {
        _className = className;
    }
    
    protected byte[] getConfig() {
        return ArrayUtil.clone(_config);
    }
    
    protected void setConfig(byte[] config) {
        _config = config;
    }
    
    public Action getParent() {
        return _parent;
    }
    
    protected void setParent(Action parent) {
        _parent = parent;
    }

    public AlertDefinition getAlertDefinition() {
        return _alertDef;
    }
    
    protected void setAlertDefinition(AlertDefinition alertDefinition) {
        _alertDef = alertDefinition;
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
    
    public ActionValue getActionValue() {
        if (_valueObj == null)
            _valueObj = new ActionValue();
            
        _valueObj.setId(getId());
        _valueObj.setClassname(getClassName());
        _valueObj.setConfig(getConfig());
        if (getParent() != null) {
            _valueObj.setParentId(getParent().getId());
        } else {
            _valueObj.setParentId(null);
        }

        return _valueObj;
    }

    public void setActionValue(ActionValue val) {
        setClassName(val.getClassname());
        setConfig(val.getConfig());
        
        if (val.getParentId() == null) {
            setParent(null);
        } else {
            ActionDAO aDao = DAOFactory.getDAOFactory().getActionDAO();

            setParent(aDao.findById(val.getId()));
        }
    }
}
