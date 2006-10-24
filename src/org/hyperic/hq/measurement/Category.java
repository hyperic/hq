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

package org.hyperic.hq.measurement;

import java.util.Collection;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.measurement.shared.CategoryValue;

public class Category extends PersistedObject
    implements java.io.Serializable {

    // Fields    
     private String _name;
     private Integer _cid;
     private Collection _measurementTemplates;

     // Constructors
    public Category() {
    }

    public Category(String name) {
        _name = name;
    }

    public Category(String name, Integer cid, Collection measurementTemplates) {
        _name = name;
        _cid = cid;
        _measurementTemplates = measurementTemplates;
    }
   
    // Property accessors
    public String getName() {
        return _name;
    }
    
    public void setName(String name) {
        _name = name;
    }
    public Integer getCid() {
        return _cid;
    }
    
    public void setCid(Integer cid) {
        _cid = cid;
    }

    public Collection getMeasurementTemplates() {
        return _measurementTemplates;
    }
    
    public void setMeasurementTemplates(Collection measurementTemplates) {
        _measurementTemplates = measurementTemplates;
    }

    /**
     * Legacy EJB DTO pattern
     * @deprecated Use (this) Category object instead
     */
    public CategoryValue getCategoryValue() {
        CategoryValue category = new CategoryValue();
        category.setId(getId());
        category.setName(getName());
        return category;
    }
}
