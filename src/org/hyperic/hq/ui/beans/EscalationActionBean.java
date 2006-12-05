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

package org.hyperic.hq.ui.beans;

/**
 * Bean to capture user input in escalation configuration
 * 
 */
public class EscalationActionBean {
    private String _actionType;
    private String _emailType;
    private String _configuration;
    private String _syslogMeta;
    private String _syslogProduct;
    private String _syslogVersion;
    private String _snmpOid;
    private String _snmpAddress;
    private long   _waitTime;

    public String getActionType() {
        return _actionType;
    }

    public void setActionType(String actionType) {
        _actionType = actionType;
    }

    public String getConfiguration() {
        return _configuration;
    }

    public void setConfiguration(String configuration) {
        _configuration = configuration;
    }

    public String getEmailType() {
        return _emailType;
    }

    public void setEmailType(String emailType) {
        _emailType = emailType;
    }

    public String getSnmpAddress() {
        return _snmpAddress;
    }

    public void setSnmpAddress(String snmpAddress) {
        _snmpAddress = snmpAddress;
    }

    public String getSnmpOid() {
        return _snmpOid;
    }

    public void setSnmpOid(String snmpOid) {
        _snmpOid = snmpOid;
    }

    public String getSyslogMeta() {
        return _syslogMeta;
    }

    public void setSyslogMeta(String syslogMeta) {
        _syslogMeta = syslogMeta;
    }

    public String getSyslogProduct() {
        return _syslogProduct;
    }

    public void setSyslogProduct(String syslogProduct) {
        _syslogProduct = syslogProduct;
    }

    public String getSyslogVersion() {
        return _syslogVersion;
    }

    public void setSyslogVersion(String syslogVersion) {
        _syslogVersion = syslogVersion;
    }

    public long getWaitTime() {
        return _waitTime;
    }

    public void setWaitTime(long waitTime) {
        _waitTime = waitTime;
    }
}
