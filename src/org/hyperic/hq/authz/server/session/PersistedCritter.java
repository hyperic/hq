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

package org.hyperic.hq.authz.server.session;

import java.math.BigDecimal;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.critters.CritterDump;

public class PersistedCritter extends PersistedObject implements CritterDump {

    private int _listIndex;
    private ResourceGroup _group;
    private String _klazz;
    
    private String _stringProp;
    private Long _dateProp;
    private Resource _resourceProp;
    private BigDecimal _numericProp;
    private Integer _enumProp;

    protected PersistedCritter() {
    }
    
    public PersistedCritter(ResourceGroup group, CritterType critterType, int index) {
       _group = group;
       _listIndex = index;
       if (critterType == null)
           throw new IllegalArgumentException("The argument critterType may not be null");
       else
           _klazz = critterType.getClass().getName();
    }

    public int getListIndex() {
        return _listIndex;
    }

    protected void setListIndex(int index) {
        _listIndex = index;
    }

    public ResourceGroup getResourceGroup() {
        return _group;
    }

    protected void setResourceGroup(ResourceGroup group) {
        _group = group;
    }

    public String getKlazz() {
        return _klazz;
    }

    protected void setKlazz(String klazz) {
        _klazz = klazz;
    }
    
    public Long getDateProp() {
        return _dateProp;
    }
    public void setDateProp(Long dateProp) {
        _dateProp = dateProp;
    }
    
    public Integer getEnumProp() {
        return _enumProp;
    }
    
    public void setEnumProp(Integer enumProp) {
        _enumProp = enumProp;
        
    }
    public BigDecimal getNumericProp() {
        return _numericProp;
    }

    public void setNumericProp(BigDecimal numericProp) {
        _numericProp = numericProp;
    }
    
    public Resource getResourceProp() {
        return _resourceProp;
    }

    public void setResourceProp(Resource resourceProp) {
        _resourceProp = resourceProp;
    }
    
    public String getStringProp() {
        return _stringProp;
    }

    public void setStringProp(String stringProp) {
        _stringProp = stringProp;
    }
    
    /**
     * The resource group and the list index uniquely identify
     * a PersistedCritter.
     */
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PersistedCritter)) return false;

        final PersistedCritter critter = (PersistedCritter) other;
        if (!critter.getResourceGroup().equals(getResourceGroup())) return false;
        if (critter.getListIndex() != getListIndex()) return false;
     
        return true;
    }

    public int hashCode() {
        int result;
        result = getResourceGroup().hashCode();
        result = 29 * result + getListIndex();
        return result;
    }
    
}
