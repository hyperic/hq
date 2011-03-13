/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;
import org.hyperic.hq.alert.data.AlertActionLogRepository;
import org.hyperic.hq.alert.data.AlertConditionLogRepository;
import org.hyperic.hq.alert.data.ResourceAlertDefinitionRepository;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.common.EntityNotFoundException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertValue;

@Entity
@Table(name="EAM_ALERT")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Alert implements AlertInterface, Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="FIXED",nullable=false)
    private boolean         fixed;
    
    @Column(name="CTIME",nullable=false)
    @Index(name="ALERT_TIME_IDX")
    private long            ctime;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ALERT_DEFINITION_ID",nullable=false)
    @Index(name="ALERT_ALERTDEFINITION_IDX")
    private ResourceAlertDefinition alertDefinition;
    
    @OneToMany(mappedBy="alert",fetch=FetchType.LAZY,cascade=CascadeType.ALL)
    @OptimisticLock(excluded=true)
    @OrderBy("timeStamp, id")
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<AlertActionLog>     actionLog    = new ArrayList<AlertActionLog>();
    
    @OneToMany(mappedBy="alert",fetch=FetchType.LAZY,cascade=CascadeType.ALL)
    @OptimisticLock(excluded=true)
    @OrderBy("id")
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<AlertConditionLog>      conditionLog = new ArrayList<AlertConditionLog>();
    
    @Formula("select e.id from EAM_ESCALATION_STATE e where e.alert_id = id and e.alert_type = -559038737")
    private Long            stateId;
    
    @Formula("select e.acknowledged_by from EAM_ESCALATION_STATE e where e.alert_id = id and e.alert_type = -559038737")
    private Long            ackedBy;
    
    private transient Boolean  ackable = null;
    
    public Alert() {
    }

    protected Alert(AlertDefinition def, AlertValue val) {
        val.cleanConditionLog();
        val.cleanActionLog();

        // Now just set the entire value object
        setAlertValue(val);

        //TODO
        setAlertDefinition((ResourceAlertDefinition)def);
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

    protected AlertActionLog createActionLog(String detail, Action action,
                                             AuthzSubject fixer)
    {
        AlertActionLog res = new AlertActionLog(this, detail, action, fixer);

        actionLog.add(res);
        return res;
    }

    protected AlertConditionLog createConditionLog(String value,
                                                   AlertCondition cond)
    {
        AlertConditionLog res = new AlertConditionLog(this, value, cond);

        conditionLog.add(res);
        return res;
    }

    public PerformsEscalations getDefinition() {
        return getAlertDefinition();
    }

    public boolean isFixed() {
        return fixed;
    }

    protected void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public long getCtime() {
        return ctime;
    }

    protected void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getTimestamp() {
        return getCtime();
    }

    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(ResourceAlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    public AlertDefinitionInterface getAlertDefinitionInterface() {
        return getAlertDefinition();
    }

    public Collection<AlertActionLog> getActionLog() {
        return Collections.unmodifiableCollection(actionLog);
    }

    protected Collection<AlertActionLog> getActionLogBag() {
        return actionLog;
    }

    protected void setActionLogBag(Collection<AlertActionLog> actionLog) {
        this.actionLog = actionLog;
    }

    private void addActionLog(AlertActionLog aal) {
        actionLog.add(aal);
    }

    private void removeActionLog(AlertActionLog aal) {
        actionLog.remove(aal);
    }

   
    public Collection<AlertConditionLog> getConditionLog() {
        return Collections.unmodifiableCollection(conditionLog);
    }

    protected Collection<AlertConditionLog> getConditionLogBag() {
        return conditionLog;
    }

    protected void setConditionLogBag(Collection<AlertConditionLog> conditionLog) {
        this.conditionLog = conditionLog;
    }

    private void addConditionLog(AlertConditionLog acl) {
        conditionLog.add(acl);
    }

    private void removeConditionLog(AlertConditionLog acl) {
        conditionLog.remove(acl);
    }

    protected void setAckedBy(Long ackedBy) {
        this.ackedBy = ackedBy;
    }

    protected Long getAckedBy() {
        return ackedBy;
    }

    protected void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    protected Long getStateId() {
        return stateId;
    }

    public boolean isAckable() {
        // Performing the conditional check to maintain existing functionality
        return (ackable == null) ? 
               (getStateId() != null && getAckedBy() == null) :
               ackable.booleanValue();
    }

    public void setAckable(boolean ackable) {
        this.ackable = new Boolean(ackable);
    }
    
    /**
     * Need to have a way of invalidating the object so that we will not use
     * Alert POJOs out of the query caches and the object cache.  This is
     * necessary because we may be changing the escalation state, which serves
     * as the value for the SQL formula-based field acakble.  Without a
     * constraint relationship between the two objects, Hibernate will not know
     * that the Alert should be evicted.
     */
    protected void invalidate() {
        setVersion(new Long(getVersion() + 1));     // Invalidate caches
    }

    public AlertValue getAlertValue() {
        AlertValue alertVal = new AlertValue();

        alertVal.setId(getId());
        alertVal.setAlertDefId(getAlertDefinition().getId());
        alertVal.setCtime(getCtime());
        alertVal.setFixed(isFixed());

        alertVal.removeAllConditionLogs();

        for (AlertConditionLog l  :getConditionLog() ) {
            alertVal.addConditionLog(l.getAlertConditionLogValue());
        }
        alertVal.cleanConditionLog();

        alertVal.removeAllActionLogs();
        alertVal.removeAllEscalationLogs();
        for (AlertActionLog l : getActionLog() ) {
            alertVal.addActionLog(l);

            // No action or alert definition means escalation log
            if (l.getAction() == null) { //TODO need this? ||
                //l.getAction().getAlertDefinition() == null) {
                
                alertVal.addEscalationLog(l);
            }
        }
        alertVal.cleanActionLog();
        alertVal.cleanEscalationLog();

        return alertVal;
    }

    @SuppressWarnings("unchecked")
    protected void setAlertValue(AlertValue val) {
        ResourceAlertDefinitionRepository aDao = Bootstrap.getBean(ResourceAlertDefinitionRepository.class);
        ResourceAlertDefinition def = aDao.findById(val.getAlertDefId());
        if(def == null) {
            throw new EntityNotFoundException("Resource Alert Definition with ID: " + 
                val.getAlertDefId() + " was not found");
        }
        AlertActionLogRepository alDao = Bootstrap.getBean(AlertActionLogRepository.class);
        AlertConditionLogRepository aclDao = Bootstrap.getBean(AlertConditionLogRepository.class);

        setFixed(false);
        //TODO
        setAlertDefinition((ResourceAlertDefinition)def);
        setCtime(val.getCtime());

        for (Iterator<AlertConditionLogValue> i=val.getAddedConditionLogs().iterator(); i.hasNext(); ){
            AlertConditionLogValue lv = i.next();
            AlertConditionLog log = aclDao.findById(lv.getId());
            if(log == null) {
                throw new EntityNotFoundException("Alert Condition Log with ID: " + lv.getId() + 
                    " was not found");
            }
            addConditionLog(log);
        }

        for (Iterator<AlertConditionLogValue> i=val.getRemovedConditionLogs().iterator(); i.hasNext();){
            AlertConditionLogValue lv = i.next();
            AlertConditionLog log = aclDao.findById(lv.getId());
            if(log == null) {
                throw new EntityNotFoundException("Alert Condition Log with ID: " + lv.getId() + 
                    " was not found");
            }
            removeConditionLog(log);
        }

        for (Iterator<AlertActionLog> i=val.getAddedActionLogs().iterator(); i.hasNext(); ) {
            AlertActionLog lv = i.next();
            AlertActionLog existing = alDao.findById(lv.getId());
            if(existing == null) {
                throw new EntityNotFoundException("AlertActionLog with ID: " + lv.getId() + " was not found");
            }
            addActionLog(existing);
        }

        for (Iterator<AlertActionLog> i=val.getRemovedActionLogs().iterator(); i.hasNext(); ) {
            AlertActionLog lv = i.next();
            AlertActionLog existing = alDao.findById(lv.getId());
            if(existing == null) {
                throw new EntityNotFoundException("AlertActionLog with ID: " + lv.getId() + " was not found");
            }
            removeActionLog(existing);
        }
    }

    /**
     * Get a list of the fields which can be used to sort various queries
     * for alerts.
     */
    public static List<AlertSortField> getSortFields() {
        return AlertSortField.getAll(AlertSortField.class);
    }

    public String toString() {
        return "(id=" + getId() + ", alertdef=" + alertDefinition.getId() +
               ", createdtime=" + ctime +")";
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Alert)) {
            return false;
        }
        Integer objId = ((Alert)obj).getId();
  
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
