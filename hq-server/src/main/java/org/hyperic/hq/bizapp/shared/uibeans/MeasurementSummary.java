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

package org.hyperic.hq.bizapp.shared.uibeans;

import java.util.ArrayList;
import java.util.List;

/**
 * This holds the number of metrics of this type configured (the template type)
 * for the resource under discussion; the count of those currently
 * collecting data, the ones that aren't and the total.
 * 
 * When displaying availability status on an aggregate of resources, each
 * resource will counted in one of these buckets
 * <ol>
 * <li>availUp<br>
 * Resources that are configured for metric collection and for which we are 
 * currently collecting data
 * <li>availDown<br>
 * Resources that are configured for metric collection and for which we are 
 * <i>not</i> currently collecting data i.e. the resource is down, as far as we
 * can tell
 * <li>availUnknown<br>
 * This is the number of resources for which metric collection is configured
 * but whose availability status is unknown
 *  </ol> 
 */
public class MeasurementSummary extends MetricConfigSummary
    implements java.io.Serializable {
    private Integer availUp;
    private Integer availDown;
    private Integer availUnknown;

    /** 
     * Default Constructor
     */
    public MeasurementSummary() {
    }

    /** 
     * Constructor with init values
     */
    public MeasurementSummary(Integer up, Integer down, Integer unknown) {
        this.availUp = up;
        this.availDown = down;
        this.availUnknown = unknown;
    }

    /**
     * @return Integer
     */
    public Integer getAvailUp() {
        return availUp;
    }

    /**
     * @param availUp The availUp to set
     */
    public void setAvailUp(Integer currentConfigured) {
        this.availUp = currentConfigured;
    }

    /**
     * @return Integer
     */
    public Integer getAvailDown() {
        return availDown;
    }

    /**
     * @param availDown The availDown to set
     */
    public void setAvailDown(Integer unavailConfigured) {
        this.availDown = unavailConfigured;
    }

    /**
     * @return Integer
     */
    public Integer getAvailUnknown() {
        return availUnknown;
    }

    /**
     * Sets the availUnknown.
     * @param availUnknown The availUnknown to set
     */
    public void setAvailUnknown(Integer totalConfigured) {
        this.availUnknown = totalConfigured;
    }
    
    /**
     * Returns a three element list with up, down and unknown
     * 
     * @return List
     */
    public List asList() {
        List returnList = new ArrayList();
        returnList.add(availUp);
        returnList.add(availDown);
        returnList.add(availUnknown);
        return returnList;    
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(MeasurementSummary.class.getName());
        sb.append("(availUp=").append(availUp);
        sb.append(",availDown=").append(availDown);
        sb.append(",availUnknown=").append(availUnknown);
        sb.append(")");
        return sb.toString();
    }

}
