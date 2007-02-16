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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class XmlResourceValue 
    extends XmlValue
{
    static final String ATTR_NAME        = "name";
    static final String ATTR_TYPE        = "type";
    static final String ATTR_DESCRIPTION = "description";
    static final String ATTR_LOCATION    = "location";

    private Map                    configs;
    private List                   metricsToCollect;
    private XmlCustomPropsValue customProps;

    public XmlResourceValue(String[] required, String[] optional){
        super(required, optional);

        this.configs          = new HashMap();
        this.metricsToCollect = new ArrayList();
        this.customProps      = new XmlCustomPropsValue();
    }

    public String getName(){
        return this.getValue(ATTR_NAME);
    }

    public String getType(){
        return this.getValue(ATTR_TYPE);
    }

    public String getDescription(){
        return this.getValue(ATTR_DESCRIPTION);
    }

    public String getLocation(){
        return this.getValue(ATTR_LOCATION);
    }

    void setCustomProps(XmlCustomPropsValue props){
        this.customProps = props;
    }

    public XmlCustomPropsValue getCustomProps(){
        return this.customProps;
    }

    void addConfig(XmlConfigInfo cfg){
        this.configs.put(cfg.getType(), cfg);
    }

    public XmlConfigInfo getConfig(String type){
        return (XmlConfigInfo)this.configs.get(type);
    }

    void addMetricCollect(XmlCollectInfo newCollect){
        this.metricsToCollect.add(newCollect);
    }

    public List getMetricsToCollect(){
        return this.metricsToCollect;
    }

    public String toString(){
        return super.toString() + " CONFIGS=" + this.configs + " METRICS=" + 
            this.metricsToCollect + " CUSTOMPROPS=" + this.customProps;
    }
}
