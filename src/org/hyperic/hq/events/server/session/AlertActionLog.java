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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hibernate.dao.ActionDAO;
import org.hyperic.hq.events.shared.AlertActionLogValue;

public class AlertActionLog  
    extends PersistedObject
{
    private String _detail;
    private Alert  _alert;
    private Action _action;

    private AlertActionLogValue _valueObj;

    
    protected AlertActionLog() {
    }
   
    protected AlertActionLog(Alert alert, String detail, Action action) {
        _detail = detail;
        _alert  = alert;
        _action = action;
    }
    
    protected String getDetail() {
        return _detail;
    }
    
    protected void setDetail(String detail) {
        _detail = detail;
    }
    
    protected Alert getAlert() {
        return _alert;
    }
    
    protected void setAlert(Alert alert) {
        _alert = alert;
    }
    
    protected Action getAction() {
        return _action;
    }
    
    protected void setAction(Action action) {
        _action = action;
    }
    
    public AlertActionLogValue getAlertActionLogValue() {
        if (_valueObj == null) {
            _valueObj = new AlertActionLogValue();
        }

        _valueObj.setId(getId());
        _valueObj.setDetail(getDetail() == null ? "" : getDetail());
        _valueObj.setActionId(getAction().getId());
        return _valueObj;
    }

    protected void setAlertActionLogValue(AlertActionLogValue val) {
        ActionDAO aDao = DAOFactory.getDAOFactory().getActionDAO();
        Action action;

        action = aDao.findById(val.getActionId());
        setDetail(val.getDetail());
        setAction(action);
    }
}
