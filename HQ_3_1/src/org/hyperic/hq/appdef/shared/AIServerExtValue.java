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

package org.hyperic.hq.appdef.shared;

import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;

import org.hyperic.util.config.ConfigResponse;

/**
 * Extends AIServerValue by allowing it to hold an
 * array of AIServiceValue objects.  Also provides 
 * a "placeholder" flag, used to indicate to the server-side
 * of runtime-AI that the object exists solely to carry
 * services underneath it, and that its other properties should
 * not be used to update the corresponding appdef server.
 */
public class AIServerExtValue  extends AIServerValue {

    private AIServiceValue[] _aiservices;
    private boolean _placeholder;
    private boolean autoEnable;
    private int metricConnectHashCode;

    public AIServerExtValue () {
        super();
        _placeholder = false;
        autoEnable = true;
        metricConnectHashCode = 0;
    }

    public AIServiceValue[] getAIServiceValues () {
        return _aiservices;
    }

    public void setAIServiceValues (AIServiceValue[] aiservices) {
        _aiservices = aiservices;
    }

    public void addAIServiceValue (AIServiceValue aiservice) {
        AIServiceValue[] newservice = {aiservice};
        _aiservices =
            (AIServiceValue[]) ArrayUtil.combine(_aiservices, newservice);
    }

    public boolean getPlaceholder () { return _placeholder; }
    public void setPlaceholder (boolean ph) { _placeholder = ph; }

    /**
     * When true tells the HQ server that this server resource
     * is ready to have metrics and runtime auto-inventory enabled.
     * This is in addition to have the metric ConfigResponse set
     * and allows plugins to turn off AutoEnable when there is more
     * than 1 server resourcs with the same metric configuration.
     */
    public boolean getAutoEnable() {
        return this.autoEnable;
    }

    public void setAutoEnable(boolean autoEnable) {
        this.autoEnable = autoEnable;
    }

    /**
     * The hashCode of metric configuration values used to connect to
     * this server for monitoring.  When set makes it possible to determine
     * if two servers are being reported with the same config, in
     * which case the server will only auto enable metrics and runtime AI
     * for the first server reported.
     */
    public int getMetricConnectHashCode() {
        return this.metricConnectHashCode;
    }

    public void setMetricConnectHashCode(int metricConnectHashCode) {
        this.metricConnectHashCode = metricConnectHashCode;
    }

    //note that hashCode of entire ConfigResponse is not useful.
    //pieces of it will be different such as the server name.
    //we just need the pieces that are used to connect to the server,
    //generally this will be a url or hostname/port in two properties.
    public void addMetricConnectHashCode(ConfigResponse config,
                                         String[] keys) {

        for (int i=0; i<keys.length; i++) {
            addMetricConnectHashCode(config, keys[i]);
        }
    }

    public void addMetricConnectHashCode(ConfigResponse config,
                                         String key) {
        if (config == null) {
            return;
        }
        String value = config.getValue(key);
        if (value == null) {
            return;
        }

        addMetricConnectHashCode(value);
    }

    public void addMetricConnectHashCode(Object object) {
        this.metricConnectHashCode += object.hashCode();
    }

    public String toString () {
        if ( _aiservices == null ) {
            return "[AIServerExtValue: " + super.toString()
                + " ExtServices=NULL]";
        } else {
            return "[AIServerExtValue: " + super.toString()
                + " ExtServices=" + StringUtil.arrayToString(_aiservices) + "]";
        }
    }
}
