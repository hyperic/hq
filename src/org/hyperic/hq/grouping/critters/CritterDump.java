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

package org.hyperic.hq.grouping.critters;

import java.math.BigDecimal;

import org.hyperic.hq.authz.server.session.Resource;

/**
 * This interface is used to isolate critters from the persistence layer.
 * 
 */
public interface CritterDump {

    /**
     * 
     * @return the string property for this critter
     */
    public String getStringProp();

    /**
     * Sets the string property for this critter
     * @param stringProp
     */
    public void setStringProp(String stringProp);

    /**
     * 
     * @return the date property for this critter
     */
    public Long getDateProp();

    /**
     * Sets the date property for this critter
     * @param dateProp
     */
    public void setDateProp(Long dateProp);

    /**
     * 
     * @return the Resource property for this critter
     */
    public Resource getResourceProp();

    /**
     * Sets the Resource property for this critter
     * @param resourceProp
     */
    public void setResourceProp(Resource resourceProp);

    /**
     * 
     * @return the numeric property for this critter
     */
    public BigDecimal getNumericProp();

    /**
     * Sets the numeric property for this critter
     * @param numericProp
     */
    public void setNumericProp(BigDecimal numericProp);

    /**
     * 
     * @return the enum property for this critter
     */
    public Integer getEnumProp();

    /**
     * Sets the enum property for this critter
     * @param enumProp
     */
    public void setEnumProp(Integer enumProp);

}
