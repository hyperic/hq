package org.hyperic.hq.events.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.json.JSON;
import org.json.JSONObject;
import org.json.JSONException;

public class EscalationState extends PersistedObject
    implements Cloneable, JSON
{
    public final static int ALERT_TYPE_CLASSIC = 0;
    public final static int ALERT_TYPE_GROUP   = 1;
    
    public static EscalationState newInstance(Escalation e, Integer aid) {
        return new EscalationState(e, aid.intValue());
    }

    /**
     * The current escalation leven in the chain.  I.e.,
     * current escalation level == actions[currentLevel].
     */
    private int _currentLevel;

    /**
     * If true, then wait for
     * max(pauseWaitTime, EscalationAction.waitTime)
     * else wait for
     * EscalationAction.waitTime
     * before escalating up the chain.
     */
    private boolean _pauseEscalation;

    /**
     * >0 next scheduled run time.
     * =0 not scheduled.
     */
    private long _scheduleRunTime;

    /**
     * meaningful if pauseEscalation == true.
     * (pauseWaitTime <= maxWaitTime)
     */
    private long _pauseWaitTime;

    /**
     * If fixed, then stop escalation chain. (terminal condition)
     */
    private boolean _fixed;

    /**
     * "updateBy" has taken ownership of this issue at the
     * current escalation level.
     */
    private boolean _acknowledge;

    /**
     * Escalation is in progress.
     */
    private boolean _active;
    private String _updateBy;
    private long _creationTime;

    /**
     * timestamp is updated on state change.
     */
    private long _modifiedTime;
    private Escalation _escalation;
    private int _alertDefinitionId;
    private int _alertId;
    private int _alertType;

    protected EscalationState(){
    }

    protected EscalationState(Escalation e, int aid) {
        _escalation = e;
        _alertDefinitionId = aid;
    }

    /**
     * *         The current escalation leven in the chain.  I.e.,
     * current escalation level == actions[currentLevel].
     */
    public int getCurrentLevel() {
        return _currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        _currentLevel = currentLevel;
    }

    /**
     * *         If true, then wait for
     * max(pauseWaitTime, EscalationAction.waitTime)
     * else wait for
     * EscalationAction.waitTime
     * before escalating up the chain.
     */
    public boolean isPauseEscalation() {
        return _pauseEscalation;
    }

    public void setPauseEscalation(boolean pauseEscalation) {
        _pauseEscalation = pauseEscalation;
    }

    public long getScheduleRunTime() {
        return _scheduleRunTime;
    }

    public void setScheduleRunTime(long scheduleRunTime) {
        _scheduleRunTime = scheduleRunTime;
    }

    /**
     * *         meaningful if pauseEscalation == true.
     * (pauseWaitTime <= maxWaitTime)
     */
    public long getPauseWaitTime() {
        return _pauseWaitTime;
    }

    public void setPauseWaitTime(long pauseWaitTime) {
        _pauseWaitTime = pauseWaitTime;
    }

    /**
     * *         If fixed, then stop escalation chain. (terminal condition)
     */
    public boolean isFixed() {
        return _fixed;
    }

    public void setFixed(boolean fixed) {
        _fixed = fixed;
    }

    /**
     * *         "updateBy" has taken ownership of this issue at the
     * current escalation level.
     */
    public boolean isAcknowledge() {
        return _acknowledge;
    }

    public void setAcknowledge(boolean acknowledge) {
        _acknowledge = acknowledge;
    }

    /**
     * *         Escalation is in progress.
     */
    public boolean isActive() {
        return _active;
    }

    public void setActive(boolean active) {
        _active = active;
    }

    public String getUpdateBy() {
        return _updateBy;
    }

    public void setUpdateBy(String updateBy) {
        _updateBy = updateBy;
    }

    public long getCreationTime() {
        return _creationTime;
    }

    public void setCreationTime(long creationTime) {
        _creationTime = creationTime;
    }

    /**
     * *         timestamp is updated on state change.
     */
    public long getModifiedTime() {
        return _modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        _modifiedTime = modifiedTime;
    }

    public Escalation getEscalation() {
        return _escalation;
    }

    public void setEscalation(Escalation escalation) {
        _escalation = escalation;
    }

    public int getAlertDefinitionId() {
        return _alertDefinitionId;
    }

    public void setAlertDefinitionId(int alertDefinitionId) {
        _alertDefinitionId = alertDefinitionId;
    }

    public int getAlertId() {
        return _alertId;
    }

    public void setAlertId(int alertId) {
        _alertId = alertId;
    }

    public int getAlertType() {
        return _alertType;
    }

    public void setAlertType(int alertType) {
        _alertType = alertType;
    }

    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("id", getId())
                .put("currentLevel", _currentLevel)
                .put("creationTime", _creationTime)
                .put("modifiedTime", _modifiedTime)
                .put("pauseWaitTime", _pauseWaitTime)
                .put("scheduleRunTime", _scheduleRunTime)
                .put("acknowledge", _acknowledge)
                .put("fixed", _fixed)
                .put("active", _active)
                .put("pauseEscalation", _pauseEscalation)
                .put("alertDefinitionId", _alertDefinitionId)
                .put("alertId", _alertId)
                .put("escalationId", _escalation.getId())
                .put("updateBy", _updateBy)
                .put("alertType", _alertType);
        } catch(JSONException e) {
            throw new SystemException(e);
        }
    }

    public String getJsonName() {
        return "escalationState";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof EscalationState)) {
            return false;
        }
        EscalationState o = (EscalationState)obj;

        return _currentLevel == o.getCurrentLevel() &&
               _creationTime == o.getCreationTime() &&
               _modifiedTime == o.getModifiedTime() &&
               _pauseWaitTime == o.getPauseWaitTime() &&
               _scheduleRunTime == o.getScheduleRunTime() &&
               _acknowledge == o.isAcknowledge() &&
               _alertType == o.getAlertType() &&
               _fixed == o.isFixed() &&
               _active == o.isActive() &&
               _pauseEscalation == o.isPauseEscalation() &&
               _alertDefinitionId == o.getAlertDefinitionId() &&
               _alertId == o.getAlertId() &&
               (_escalation == o.getEscalation() ||
                (_escalation != null && o.getEscalation() != null &&
                 _updateBy.equals(o.getEscalation()) ) ) &&
               (_updateBy == o.getUpdateBy() ||
                (_updateBy != null && o.getUpdateBy() != null &&
                 _updateBy.equals(o.getUpdateBy()) ) );
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + (_acknowledge ? 0 : 1);
        result = 37*result + (_fixed ? 0 : 1);
        result = 37*result + (_pauseEscalation ? 0 : 1);
        result = 37*result + (_active ? 0 : 1);
        result = 37*result + _currentLevel;
        result = 37*result + _alertDefinitionId;
        result = 37*result + _alertId;
        result = 37*result + _alertType;
        result = 37*result + (int)(_creationTime ^ (_creationTime >>> 32));
        result = 37*result + (int)(_modifiedTime ^ (_modifiedTime >>> 32));
        result = 37*result + (int)(_pauseWaitTime ^ (_pauseWaitTime >>> 32));
        result = 37*result + (int)(_scheduleRunTime ^ (_scheduleRunTime >>> 32));
        result = 37*result + (_updateBy != null ? _updateBy.hashCode() : 0);
        result = 37*result + (_escalation != null ? _escalation.hashCode() : 0);

        return result;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch(CloneNotSupportedException e) {
            // Can't happen
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    public String toString() {
        return new StringBuffer()
            .append("(id=" + getId() + 
                    ", escalationId=" + getEscalation().getId() +
                    ", alertDefId=" + _alertDefinitionId +
                    ", alertId=" + _alertId +
                    ", currentLevel=" + _currentLevel +
                    ", fixed=" + _fixed +
                    ", active"+ _active +
                    ", acknowledge=" + _acknowledge +
                    ", pause=" + _pauseEscalation +
                    ", scheduleRunTime=" + _scheduleRunTime +
                    ", pauseWaitTIme=" + _pauseWaitTime +
                    ", modified=" + _modifiedTime)
            .append((_updateBy != null ? (", updateBy="+_updateBy) : ""))
            .append(")")
            .toString();
    }
}
