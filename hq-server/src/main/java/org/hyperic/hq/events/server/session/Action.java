/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2009], Hyperic, Inc. 
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.NoOpAction;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.json.JSON;
import org.json.JSONException;
import org.json.JSONObject;

@Entity
@Table(name="EAM_ACTION")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Action implements JSON, Serializable {
    
    private transient final Log log = LogFactory.getLog(Action.class);

    public static final String JSON_NAME = "action";
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;

    @Column(name="CLASSNAME",nullable=false,length=200)
    private String className;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CONFIG",columnDefinition="BLOB")
    private byte[] config;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="PARENT_ID")
    @Index(name="ACTION_CHILD_IDX")
    private Action parent;
    
    @OneToMany(mappedBy="action",fetch=FetchType.LAZY,cascade=CascadeType.ALL)
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<AlertActionLog> logEntries = new ArrayList<AlertActionLog>();
    
    @OneToMany(mappedBy="parent",fetch=FetchType.LAZY,cascade=CascadeType.ALL)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<Action> children = new ArrayList<Action>();
    
    @Column(name="DELETED",nullable=false)
    private boolean deleted = false;

    static Action newInstance(JSONObject json)
        throws JSONException {
        String className = json.getString("className");
        Action action;
        if (className.endsWith("EmailAction")) {
            action = newEmailAction(json.getJSONObject("config"));
        } else if (className.endsWith("SyslogAction")) {
            action = newSyslogAction(json.getJSONObject("config"));
        } else if (className.endsWith("NoOpAction")) {
            action = newNoOpAction(json.getJSONObject("config"));
        } else {
            throw new JSONException("Unsupported Action class " + className);
        }
        return action;
    }

    static Action newEmailAction(JSONObject json) throws JSONException {
        EmailActionConfig config = new EmailActionConfig();
        config.setType(json.getInt(EmailActionConfig.CFG_TYPE));
        config.setNames(json.getString(EmailActionConfig.CFG_NAMES));
        config.setSms(json.getBoolean(EmailActionConfig.CFG_SMS));

        return createAction(config);
    }

    static Action newSyslogAction(String metaProject, String project,
                                  String version) {
        SyslogActionConfig sa = new SyslogActionConfig();
        sa.setMeta(metaProject);
        sa.setProduct(project);
        sa.setVersion(version);

        return createAction(sa);
    }

    static Action newSyslogAction(JSONObject json) throws JSONException {
        return newSyslogAction(
                               json.getString(SyslogActionConfig.CFG_META),
                               json.getString(SyslogActionConfig.CFG_PROD),
                               json.getString(SyslogActionConfig.CFG_VER));
    }

    static Action newNoOpAction() {
        NoOpAction na = new NoOpAction();
        return createAction(na);
    }

    static Action newNoOpAction(JSONObject json) throws JSONException {
        return newNoOpAction();
    }

    static Action createAction(ActionConfigInterface config) {
        Action act = new Action();
        act.setClassName(config.getImplementor());
        try {
            act.setConfig(config.getConfigResponse().encode());
        } catch (EncodingException e) {
            throw new IllegalArgumentException("Can't encode action config " +
                                               "response");
        }
        return act;
    }

    protected Action() {
    }

    @SuppressWarnings("unchecked")
    Action(String className, byte[] config, Action parent) {
        this.className = className;
        this.config = config;
        this.parent = parent;
        logEntries = Collections.EMPTY_LIST;
        children = Collections.EMPTY_LIST;
    }
    
    

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getClassName() {
        return className;
    }

    protected void setClassName(String className) {
        this.className = className;
    }

    public byte[] getConfig() {
        return ArrayUtil.clone(config);
    }

    protected void setConfig(byte[] config) {
        this.config = config;
    }

    public Action getParent() {
        return parent;
    }

    protected void setParent(Action parent) {
        this.parent = parent;
    }

    public Collection<Action> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    protected Collection<Action> getChildrenBag() {
        return children;
    }

    protected void setChildrenBag(Collection<Action> children) {
        this.children = children;
    }

    public Collection<AlertActionLog> getLogEntries() {
        return Collections.unmodifiableCollection(logEntries);
    }

    protected Collection<AlertActionLog> getLogEntriesBag() {
        return logEntries;
    }

    protected void setLogEntriesBag(Collection<AlertActionLog> logEntries) {
        this.logEntries = logEntries;
    }

    public boolean isDeleted() {
        return deleted;
    }

    protected void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public ActionValue getActionValue() {
        ActionValue valueObj = new ActionValue();
        valueObj.setId(getId());
        valueObj.setClassname(getClassName());
        valueObj.setConfig(getConfig());
        if (getParent() != null) {
            valueObj.setParentId(getParent().getId());
        } else {
            valueObj.setParentId(null);
        }

        return valueObj;
    }

    public JSONObject toJSON() {
        try {
            ConfigResponse conf = ConfigResponse.decode(getConfig());
            JSONObject json = new JSONObject()
                                              .put("className", getClassName())
                                              .put("config", conf.toProperties());
            if (getId() != null) {
                json.put("id", getId());
                json.put("_version_", getVersion());
            }
            return json;
        } catch (EncodingException e) {
            // can't happen
            throw new SystemException(e);
        } catch (JSONException e) {
            throw new SystemException(e);
        }
    }

    public String getJsonName() {
        return JSON_NAME;
    }

    public ActionInterface getInitializedAction() {
        String actionClassName = null;
        try {
            actionClassName = getClassName();
            Class<?> ac = Class.forName(actionClassName);
            ActionInterface action = (ActionInterface) ac.newInstance();

            action.init(ConfigResponse.decode(action.getConfigSchema(),
                                              getConfig()));

            return action;
        } catch (Exception e) {
            log.error("Error getting initialized action for " + actionClassName);
            throw new SystemException("Unable to get action", e);
        }
    }

    /**
     * Execute the action specified by the classname and config data.
     */
    public String executeAction(AlertInterface alert, ActionExecutionInfo info)
        throws ActionExecuteException {
        try {
            return getInitializedAction().execute(alert, info);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to execute action", e);
            }
            throw new ActionExecuteException("Unable to execute action: " +
                                             e.getMessage(), e);
        }
    }

    public String toString() {
        return "(id=" + getId() + ", class=" + className + ")";
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Action)) {
            return false;
        }
        Integer objId = ((Action)obj).getId();
  
        return getId() == objId ||
        (getId() != null && 
         objId != null && 
         getId().equals(objId));     
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }

}
