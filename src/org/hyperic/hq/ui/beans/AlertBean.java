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

package org.hyperic.hq.ui.beans;

import java.io.Serializable;

/**
 * Bean to hold alert values from AlertValue and AlertDefinitionValue.
 *
 */
public class AlertBean implements Serializable {
    // alert fields
    private Integer id;
    private long ctime;

    // alert def fields
    private Integer alertDefId;
    private String name;
    private int priority;
    private String conditionName;
    private String comparator;
    private String threshold;
    private String value;
    private boolean multiCondition;

    // resource fields
    private Integer rid;
    private Integer type;

    public AlertBean() { }

    public AlertBean(Integer id, long ctime,
                     Integer alertDefId, String name, int priority,
                     Integer rid, Integer type) {
        this.id = id;
        this.ctime = ctime;
        this.alertDefId = alertDefId;
        this.name = name;
        this.priority = priority;
        this.rid = rid;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime=ctime;
    }

    public Integer getAlertDefId() {
        return alertDefId;
    }

    public void setAlertDefId(Integer alertDefId) {
        this.alertDefId = alertDefId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority=priority;
    }

    public String getConditionName() {
        return comparator;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getComparator() {
        return comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isMultiCondition() {
        return multiCondition;
    }

    public void setMultiCondition(boolean multiCondition) {
        this.multiCondition = multiCondition;
    }

    public Integer getRid() {
        return rid;
    }

    public void setRid(Integer rid) {
        this.rid=rid;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type=type;
    }

    /**
     * Return the formatted alert condition.
     */
    public String getConditionFmt() {
        if (multiCondition) {
            return conditionName;
        } else {
            return conditionName.trim() + " " +
                comparator.trim() + " " +
                threshold;
        }
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("{id=")
            .append( getId() )
            .append(" ctime=")
            .append( getCtime() )
            .append(" alertDefId=")
            .append( getAlertDefId() )
            .append(" name=")
            .append( getName() )
            .append(" priority=")
            .append( getPriority() )
            .append(" conditionFmt=")
            .append( getConditionFmt() )
            .append(" value=")
            .append( getValue() )
            .append(" rid=")
            .append( getRid() )
            .append(" type=")
            .append( getType() )
            .append('}');

        return str.toString();
    }
}

// EOF
