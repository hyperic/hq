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

import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;

public class DerivedMeasurement extends Measurement
    implements java.io.Serializable {

    // Fields    
    private boolean _enabled = true;
    private long _interval;
    private String _formula;
    private Baseline _baseline;

    // Constructors
    public DerivedMeasurement() {
    }

    public DerivedMeasurement(Integer instanceId, MeasurementTemplate template,
                              long interval) {
        super(instanceId, template);
        _interval = interval;
    }

    // Property accessors
    public boolean isEnabled() {
        return _enabled;
    }
    
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public long getInterval() {
        return _interval;
    }
    
    public void setInterval(long interval) {
        _interval = interval;
    }

    public String getFormula() {
        return _formula;
    }
    
    public void setFormula(String formula) {
        _formula = formula;
    }

    public void setBaseline(Baseline b) {
        if (b != null) {
            b.setDerivedMeasurement(this);
        }
        _baseline = b;
    }

    public Baseline getBaseline() {
        return _baseline;
    }

    public int getAppdefType() {
        return getTemplate().getMonitorableType().getAppdefType();
    }


    /**
     * Legacy EJB DTO pattern
     * @deprecated Use (this) DerivedMeasurement object instead
     */
    public DerivedMeasurementValue getDerivedMeasurementValue() {
        Baseline b = getBaseline();

        DerivedMeasurementValue val = new DerivedMeasurementValue();
        val.setId(getId());
        val.setEnabled(isEnabled());
        val.setInterval(getInterval());
        val.setFormula(getFormula());
        val.setAppdefType(getAppdefType());
        val.setInstanceId(getInstanceId());
        val.setMtime(getMtime());
        val.setTemplate(getTemplate().getMeasurementTemplateValue());
        if (b != null) {
            val.setBaseline(b.getBaselineValue());
        }

        return val;
    }
}
