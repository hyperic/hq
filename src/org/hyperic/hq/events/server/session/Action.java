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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.Json;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.ConfigResponse;
import org.json.JSONObject;
import org.json.JSONException;

public class Action  
    extends PersistedObject
    implements Json
{
    private String          _className;
    private byte[]          _config;
    private Action          _parent;
    private AlertDefinition _alertDef;
    private Collection      _logEntries = new ArrayList();
    private Collection      _children = new ArrayList();

    private ActionValue     _valueObj;

    public static Action newInstance(JSONObject json) throws JSONException
    {
        String className = json.getString("className");
        Action action;
        if (className.endsWith("EmailAction")) {
            action = newEmailAction(json.getJSONObject("config"));
        } else if (className.endsWith("SyslogAction")) {
            action = newSyslogAction(json.getJSONObject("config"));
        } else {
            throw new JSONException("unsupport Action class " + className);
        }
        int id = json.optInt("id");
        if (id > 0) {
            action.setId(new Integer(id));
        }
        return action;
    }

    public static Action newEmailAction(JSONObject json) throws JSONException
    {
        EmailActionConfig config = new EmailActionConfig();
        config.setType(json.getInt(EmailActionConfig.CFG_TYPE));
        config.setNames(json.getString(EmailActionConfig.CFG_NAMES));
        return createAction(config);
    }

    public static Action newEmailAction(int type, Set notifs) {
        EmailActionConfig config = new EmailActionConfig();
        config.setType(type);
        config.setNames(StringUtil.iteratorToString(notifs.iterator(), ",", ""));

        return createAction(config);
    }

    public static Action newSyslogAction(String metaProject, String project,
                                         String version)
    {
        SyslogActionConfig sa = new SyslogActionConfig();
        sa.setMeta(metaProject);
        sa.setProduct(project);
        sa.setVersion(version);
        
        return createAction(sa);
    }

    public static Action newSyslogAction(JSONObject json) throws JSONException
    {
        return newSyslogAction(
            json.getString(SyslogActionConfig.CFG_META),
            json.getString(SyslogActionConfig.CFG_PROD),
            json.getString(SyslogActionConfig.CFG_VER)
        );
    }

    private static Action createAction(ActionConfigInterface config)
    {
        Action act = new Action();
        act.setClassName(config.getImplementor());
        try {
            act.setConfig(config.getConfigResponse().encode());
        } catch (EncodingException e) {
            throw new IllegalArgumentException("Can't encode email config " +
                                               "response");
        }
        return act;
    }

    protected Action() {
    }

    public Action(AlertDefinition def, ActionValue val, Action parent) {
        _className  = val.getClassname();
        _config     = ArrayUtil.clone(val.getConfig());
        _parent     = parent;
        _alertDef   = def;
        _logEntries = Collections.EMPTY_LIST;
        _children   = Collections.EMPTY_LIST;
    }
    
    public String getClassName() {
        return _className;
    }
    
    protected void setClassName(String className) {
        _className = className;
    }
    
    public byte[] getConfig() {
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

    public Collection getChildren() {
        return Collections.unmodifiableCollection(_children);
    }

    protected Collection getChildrenBag() {
        return _children;
    }
    
    protected void setChildrenBag(Collection children) {
        _children = children;
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

    public JSONObject toJSON() throws JSONException
    {
        try {
            ConfigResponse conf = ConfigResponse.decode(getConfig());
            JSONObject json = new JSONObject()
                    .put("className", getClassName())
                    .put("config", conf.toProperties());
            if (getId() != null) {
                json.put("id", getId());
            }
            return json;
        } catch (EncodingException e) {
            // can't happen
            throw new SystemException(e);
        }
    }
  
    public String toString() {
        return "(id=" + getId() + ", class=" + _className + ")";
    }
}
