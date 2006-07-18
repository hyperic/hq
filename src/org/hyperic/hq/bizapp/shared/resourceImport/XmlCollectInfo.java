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

package org.hyperic.hq.bizapp.shared.resourceImport;

import org.hyperic.hq.measurement.MeasurementConstants;

public class XmlCollectInfo
    extends XmlValue
{
    private static final String ATTR_METRIC   = "metric";
    private static final String ATTR_INTERVAL = "interval";

    private static final String[] ATTRS_REQUIRED = {
        ATTR_METRIC,
    };

    private static final String[] ATTRS_OPTIONAL = {
        ATTR_INTERVAL,
    };

    XmlCollectInfo(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
        long interval;

        interval = MeasurementConstants.INTERVAL_DEFAULT_MILLIS / 1000;
        try {
            this.setValue(ATTR_INTERVAL, Long.toString(interval));
        } catch(XmlInvalidAttrException exc){
            throw new IllegalStateException("This should never occur");
        }
    }
    
    public static String[] getRequiredAttributes(){
        return ATTRS_REQUIRED;
    }

    public static String[] getOptionalAttributes(){
        return ATTRS_OPTIONAL;
    }

    public String getMetric(){
        return this.getValue(ATTR_METRIC);
    }

    public int getInterval(){
        return Integer.valueOf(this.getValue(ATTR_INTERVAL)).intValue();
    }

    void setValue(String key, String value)
        throws XmlInvalidAttrException
    {
        if(key.equals(ATTR_INTERVAL)){
            try {
                int iVal = Integer.parseInt(value);

                if(iVal <= 0){
                    throw new XmlInvalidAttrException("'" + key + "' must "+
                                                         "be a positive " +
                                                         "integer");
                }
            } catch(NumberFormatException exc){
                throw new XmlInvalidAttrException("'" + key + "' attribute"+
                                                     " must be an integer");
            }
        }
        super.setValue(key, value);
    }
}
