/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.gfee.metric;

import java.util.ArrayList;
import java.util.StringTokenizer;


public abstract class CustomMetric {

    private String[] metrics;
    private String type;
    private String alias;

    public CustomMetric(){}
    
    public CustomMetric(String[] metrics, String type) {
        super();
        this.metrics = metrics;
        this.type = type;
        StringBuilder b = new StringBuilder();
        b.append("custom_");
        b.append(type);
        for (String metric : metrics) {
            b.append("_");
            b.append(metric);
        }
        this.alias = b.toString();
    }

    public CustomMetric(String alias) {
        StringTokenizer token = new StringTokenizer(alias, "_");
        token.nextToken();
        this.type = token.nextToken();
        ArrayList<String> list = new ArrayList<String>();
        while(token.hasMoreElements()) {
            list.add(token.nextToken());
        }
        this.metrics = list.toArray(new String[0]);
        this.alias = alias;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String[] getMetrics() {
        return metrics;
    }

    public void setMetrics(String[] metrics) {
        this.metrics = metrics;
    }

    public abstract Double calculate(Double[] values);
    
    public static CustomMetric buildByAlias(String alias) {
        String[] fields = alias.split("_");
        if(fields[1].equals(RatioMetric.TYPE))
            return new RatioMetric(alias);
        else
            return null;
    }
}
