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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.events.shared.AlertConditionLogValue;

@Entity
@Table(name = "EAM_ALERT_CONDITION_LOG")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AlertConditionLog implements Serializable {
    public static final int MAX_LOG_LENGTH = 250;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALERT_ID")
    @Index(name = "ALERT_COND_LOG_IDX")
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONDITION_ID")
    @Index(name = "ALERT_CONDITION_ID_IDX")
    private AlertCondition condition;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VALUE", length = 250)
    private String value;

    protected AlertConditionLog() {
    }

    protected AlertConditionLog(Alert alert, String value, AlertCondition condition) {
        if (value != null && value.length() >= MAX_LOG_LENGTH)
            value = value.substring(0, MAX_LOG_LENGTH);

        setValue(value);
        setAlert(alert);
        setCondition(condition);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AlertConditionLog)) {
            return false;
        }
        Integer objId = ((AlertConditionLog) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    protected Alert getAlert() {
        return alert;
    }

    public AlertConditionLogValue getAlertConditionLogValue() {

        AlertConditionLogValue valueObj = new AlertConditionLogValue();
        valueObj.setId(getId());
        valueObj.setValue(getValue() == null ? "" : getValue());
        if (getCondition() != null)
            valueObj.setCondition(getCondition().getAlertConditionValue());
        else
            valueObj.setCondition(null);

        return valueObj;
    }

    public AlertCondition getCondition() {
        return condition;
    }

    public Integer getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    protected void setAlert(Alert alert) {
        this.alert = alert;
    }

    protected void setCondition(AlertCondition condition) {
        this.condition = condition;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setValue(String value) {
        this.value = value;
    }

}
