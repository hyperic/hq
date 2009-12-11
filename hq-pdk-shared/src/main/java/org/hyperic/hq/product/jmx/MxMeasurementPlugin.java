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

package org.hyperic.hq.product.jmx;

import java.util.StringTokenizer;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class MxMeasurementPlugin
    extends MeasurementPlugin {

    private double doubleValue(Object obj)
        throws PluginException {

        try {
            return Double.valueOf(obj.toString()).doubleValue();
        } catch (NumberFormatException e) {
            throw new PluginException("Cannot convert '" + obj +
                                      "' to double");
        }
    }
    
    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException 
    {
        double doubleVal;
        Object objectVal = MxUtil.getValue(metric);
        String stringVal = objectVal.toString();

        //check for value mappings in plugin.xml:
        //<property name"StateVal.Stopped"  value="0.0"/>
        //<property name="StateVal.Started" value="1.0"/>
        //<property name"State.3" value="1.0"/>
        String mappedVal =
            getTypeProperty(metric.getAttributeName() + "." +
                            stringVal);

        if (mappedVal != null) {
            doubleVal = doubleValue(mappedVal);
        }
        else if (objectVal instanceof Number) {
            doubleVal = ((Number)objectVal).doubleValue();
        }
        else if (objectVal instanceof Boolean) {
            doubleVal =
                ((Boolean)objectVal).booleanValue() ?
                Metric.AVAIL_UP : Metric.AVAIL_DOWN;
        }
        else {
            doubleVal = doubleValue(stringVal);
        }

        if (doubleVal == -1) {
            return new MetricValue(Double.NaN);
        }

        return new MetricValue(doubleVal);
    }

    public String translate(String template, ConfigResponse config) {
        //ugh.  template in the form of:
        //plugin name:jmx-domain:jmx-key=jmx-val,jmx-key2=*:Attribute:conn-key=conn-vall
        //we just want to expand "jmx-key2=*" -> "jmx-key2=%jmx-key2%"
        StringBuffer expanded = new StringBuffer();

        StringTokenizer tok = new StringTokenizer(template, ":");

        if (tok.countTokens() < 4) {
            //e.g. can happen if ${OBJECT_NAME} is not expanded
            String msg = "Malformed metric template: " + template;
            throw new IllegalArgumentException(msg);            
        }

        expanded.append(tok.nextToken()).append(':'); //plugin name
        
        expanded.append(tok.nextToken()).append(':'); //ObjectName domain
        
        expanded.append(MxUtil.expandObjectName(tok.nextToken())); //ObjectName key properties

        expanded.append(':').append(tok.nextToken()); //attribute name

        String connProps;
        if (tok.hasMoreTokens()) {
            //optional conn properties
            connProps = tok.nextToken();
        }
        else {
            //jmx.url=%jmx.url%,jmx.username=%jmx.username%,jmx.password=%jmx.password%
            connProps =
                MxUtil.PROP_JMX_URL + "=" + "%" + MxUtil.PROP_JMX_URL + "%" + "," +
                MxUtil.PROP_JMX_USERNAME + "=" + "%" + MxUtil.PROP_JMX_USERNAME + "%" + "," +
                MxUtil.PROP_JMX_PASSWORD + "=" + "%" + MxUtil.PROP_JMX_PASSWORD + "%";
        }

        expanded.append(':').append(connProps); //conn props

        while (tok.hasMoreTokens()) {
            expanded.append(':').append(tok.nextToken());
        }

        return super.translate(expanded.toString(), config);
    }
}
