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

/*
 * GroupMonitoringConfigForm.java
 */

package org.hyperic.hq.ui.action.resource.group.monitor.config;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.action.resource.common.monitor.config.MonitoringConfigForm;
import org.hyperic.hq.ui.Constants;

/**
 * Form for setting the collection interval for metrics in
 * resource/group/monitoring/configuration areas of the application, and for
 * adding metrics to a group.
 * 
 */
public class GroupMonitoringConfigForm
    extends MonitoringConfigForm {

    /** Holds value of property availabilityThreshold. */
    private String availabilityThreshold;

    /** Holds value of property unavailabilityThreshold. */
    private String unavailabilityThreshold;

    /** Creates new MonitoringConfigForm */
    public GroupMonitoringConfigForm() {
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.availabilityThreshold = "100";
        this.unavailabilityThreshold = "0";
        ;
        super.reset(mapping, request);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("availability: ").append(availabilityThreshold);
        buf.append(" unvailabilityThreshold: ").append(unavailabilityThreshold);
        return super.toString() + buf.toString();
    }

    /**
     * Getter for property availabilityThreshold.
     * @return Value of property availabilityThreshold.
     * 
     */
    public String getAvailabilityThreshold() {
        return this.availabilityThreshold;
    }

    /**
     * Setter for property availabilityThreshold.
     * @param availabilityThreshold New value of property availabilityThreshold.
     * 
     */
    public void setAvailabilityThreshold(String availabilityThreshold) {
        this.availabilityThreshold = availabilityThreshold;
    }

    /**
     * Getter for property unavailabilityThreshold.
     * @return Value of property unavailabilityThreshold.
     * 
     */
    public String getUnavailabilityThreshold() {
        return this.unavailabilityThreshold;
    }

    /**
     * Setter for property unavailabilityThreshold.
     * @param unavailabilityThreshold New value of property
     *        unavailabilityThreshold.
     * 
     */
    public void setUnavailabilityThreshold(String unavailabilityThreshold) {
        this.unavailabilityThreshold = unavailabilityThreshold;
    }

}
