/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.measurement.ext;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;

public class MeasurementEvent extends AbstractEvent
    implements Serializable, ResourceEventInterface {

    private static final long serialVersionUID = -4805198063892667418L;
    private AppdefEntityID _resource;
    private MetricValue    _value;
    private String         _units;

    public MeasurementEvent(Integer mid, MetricValue value) {
        super.setInstanceId(mid);
        super.setTimestamp(value.getTimestamp());
        _value = value;
    }

    public AppdefEntityID getResource(){
        return _resource;
    }

    public MetricValue getValue() {
        return _value;
    }

    public void setValue(MetricValue value) {
        _value = value;
    }

    public String getUnits() {
        return _units;
    }

    public void setResource(AppdefEntityID resource) {
        this._resource = resource;
    }

    public void setUnits(String units) {
        this._units = units;
    }

    public String toString() {
        int unit = UnitsConstants.UNIT_NONE;
        if(getUnits() != null) {
            unit  = UnitsConvert.getUnitForUnit(getUnits());
        }
        int scale = UnitsConstants.SCALE_NONE;
        if(getUnits() != null) {
            scale = UnitsConvert.getScaleForUnit(getUnits());
        }
        UnitNumber un = new UnitNumber(_value.getValue(), unit, scale);
        FormattedNumber fn = UnitsFormat.format(un);
        return fn.toString();
    }
}
