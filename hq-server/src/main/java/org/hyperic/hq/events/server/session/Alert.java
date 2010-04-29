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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertValue;

public class Alert
    extends PersistedObject
    implements AlertInterface
{
    private boolean         _fixed;
    private long            _ctime;
    private AlertDefinition _alertDefinition;
    private Collection      _actionLog    = new ArrayList();
    private Collection      _conditionLog = new ArrayList();
    private Long            _stateId;
    private Long            _ackedBy;
    private AlertValue      _alertVal;
    private Boolean         _ackable = null;
    
    public Alert() {
    }

    protected Alert(AlertDefinition def, AlertValue val) {
        val.cleanConditionLog();
        val.cleanActionLog();

        // Now just set the entire value object
        setAlertValue(val);

        setAlertDefinition(def);
    }

    protected AlertActionLog createActionLog(String detail, Action action,
                                             AuthzSubject fixer)
    {
        AlertActionLog res = new AlertActionLog(this, detail, action, fixer);

        _actionLog.add(res);
        return res;
    }

    protected AlertConditionLog createConditionLog(String value,
                                                   AlertCondition cond)
    {
        AlertConditionLog res = new AlertConditionLog(this, value, cond);

        _conditionLog.add(res);
        return res;
    }

    public PerformsEscalations getDefinition() {
        return getAlertDefinition();
    }

    public boolean isFixed() {
        return _fixed;
    }

    protected void setFixed(boolean fixed) {
        _fixed = fixed;
    }

    public long getCtime() {
        return _ctime;
    }

    protected void setCtime(long ctime) {
        _ctime = ctime;
    }

    public long getTimestamp() {
        return getCtime();
    }

    public AlertDefinition getAlertDefinition() {
        return _alertDefinition;
    }

    protected void setAlertDefinition(AlertDefinition alertDefinition) {
        _alertDefinition = alertDefinition;
    }

    public AlertDefinitionInterface getAlertDefinitionInterface() {
        return getAlertDefinition();
    }

    public Collection<AlertActionLog> getActionLog() {
        return Collections.unmodifiableCollection(_actionLog);
    }

    protected Collection getActionLogBag() {
        return _actionLog;
    }

    protected void setActionLogBag(Collection actionLog) {
        _actionLog = actionLog;
    }

    private void addActionLog(AlertActionLog aal) {
        _actionLog.add(aal);
    }

    private void removeActionLog(AlertActionLog aal) {
        _actionLog.remove(aal);
    }

    @SuppressWarnings("unchecked")
    public Collection<AlertConditionLog> getConditionLog() {
        return Collections.unmodifiableCollection(_conditionLog);
    }

    protected Collection getConditionLogBag() {
        return _conditionLog;
    }

    protected void setConditionLogBag(Collection conditionLog) {
        _conditionLog = conditionLog;
    }

    private void addConditionLog(AlertConditionLog acl) {
        _conditionLog.add(acl);
    }

    private void removeConditionLog(AlertConditionLog acl) {
        _conditionLog.remove(acl);
    }

    protected void setAckedBy(Long ackedBy) {
        _ackedBy = ackedBy;
    }

    protected Long getAckedBy() {
        return _ackedBy;
    }

    protected void setStateId(Long stateId) {
        _stateId = stateId;
    }

    protected Long getStateId() {
        return _stateId;
    }

    public boolean isAckable() {
        // Performing the conditional check to maintain existing functionality
        return (_ackable == null) ? 
               (getStateId() != null && getAckedBy() == null) :
               _ackable.booleanValue();
    }

    public void setAckable(boolean ackable) {
        this._ackable = new Boolean(ackable);
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
        set_version_(new Long(get_version_() + 1));     // Invalidate caches
    }

    public AlertValue getAlertValue() {
        if (_alertVal == null)
            _alertVal = new AlertValue();

        _alertVal.setId(getId());
        _alertVal.setAlertDefId(getAlertDefinition().getId());
        _alertVal.setCtime(getCtime());
        _alertVal.setFixed(isFixed());

        _alertVal.removeAllConditionLogs();

        for (Iterator i=getConditionLog().iterator(); i.hasNext(); ) {
            AlertConditionLog l = (AlertConditionLog)i.next();

            _alertVal.addConditionLog(l.getAlertConditionLogValue());
        }
        _alertVal.cleanConditionLog();

        _alertVal.removeAllActionLogs();
        _alertVal.removeAllEscalationLogs();
        for (Iterator i=getActionLog().iterator(); i.hasNext(); ) {
            AlertActionLog l = (AlertActionLog)i.next();
            _alertVal.addActionLog(l);

            // No action or alert definition means escalation log
            if (l.getAction() == null ||
                l.getAction().getAlertDefinition() == null) {
                _alertVal.addEscalationLog(l);
            }
        }
        _alertVal.cleanActionLog();
        _alertVal.cleanEscalationLog();

        return _alertVal;
    }

    protected void setAlertValue(AlertValue val) {
        AlertDefinitionDAO aDao = Bootstrap.getBean(AlertDefinitionDAO.class);
        AlertDefinition def = aDao.findById(val.getAlertDefId());
        AlertActionLogDAO alDao = Bootstrap.getBean(AlertActionLogDAO.class);
        AlertConditionLogDAO aclDao = Bootstrap.getBean(AlertConditionLogDAO.class);

        setFixed(false);
        setAlertDefinition(def);
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
            AlertActionLog lv = (AlertActionLog)i.next();

            addActionLog(alDao.findById(lv.getId()));
        }

        for (Iterator i=val.getRemovedActionLogs().iterator(); i.hasNext(); ) {
            AlertActionLog lv = (AlertActionLog)i.next();

            removeActionLog(alDao.findById(lv.getId()));
        }
    }

    /**
     * Get a list of the fields which can be used to sort various queries
     * for alerts.
     */
    public static List getSortFields() {
        return AlertSortField.getAll(AlertSortField.class);
    }

    public String toString() {
        return "(id=" + getId() + ", alertdef=" + _alertDefinition.getId() +
               ", createdtime=" + _ctime +")";
    }
}
