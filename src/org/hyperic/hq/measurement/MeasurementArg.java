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

import org.hyperic.hibernate.PersistedObject;

public class MeasurementArg extends PersistedObject
    implements java.io.Serializable {

    // Fields
    private Integer _cid;
    private Integer _placement;
    private Integer _ticks;
    private float _weight;
    private Integer _previous;
    private MeasurementTemplate _template;
    private MeasurementTemplate _templateArg;

    // Constructors
    public MeasurementArg() {
    }

    public MeasurementArg(Integer placement) {
        _placement = placement;
    }

    public MeasurementArg(Integer placement, Integer ticks,
                          float weight, Integer previous,
                          MeasurementTemplate template,
                          MeasurementTemplate templateArg) {
        _placement = placement;
        _ticks = ticks;
        _weight = weight;
        _previous = previous;
        _template = template;
        _templateArg = templateArg;
    }
   
    // Property accessors
    public Integer getCid() {
        return _cid;
    }

    public void setCid(Integer cid) {
        _cid = cid;
    }

    public Integer getPlacement() {
        return _placement;
    }
    
    public void setPlacement(Integer placement) {
        _placement = placement;
    }

    public Integer getTicks() {
        return _ticks;
    }
    
    public void setTicks(Integer ticks) {
        _ticks = ticks;
    }

    public float getWeight() {
        return _weight;
    }
    
    public void setWeight(float weight) {
        _weight = weight;
    }

    public Integer getPrevious() {
        return _previous;
    }
    
    public void setPrevious(Integer previous) {
        _previous = previous;
    }

    public MeasurementTemplate getTemplate() {
        return _template;
    }
    
    public void setTemplate(MeasurementTemplate template) {
        _template = template;
    }

    public MeasurementTemplate getTemplateArg() {
        return _templateArg;
    }
    
    public void setTemplateArg(MeasurementTemplate templateArg) {
        _templateArg = templateArg;
    }
}


