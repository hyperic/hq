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
//XXX move this class to sigar
package org.hyperic.hq.plugin.system;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarException;

public class NetConnectionData {

    private NetConnection _conn;
    private boolean _isNumericHosts;
    private boolean _isNumericPorts;
    private String _proto;
    private String _localPort;
    private String _remotePort;
    private int _maxLocalAddrLen = -1;
    private int _maxRemoteAddrLen = -1;
    private long _processPid = -1;
    private String _processName;

    public NetConnectionData() {
    }

    public NetConnectionData(SigarProxy sigar,
                             NetConnection conn,
                             boolean isNumericHosts,
                             boolean isNumericPorts) {
        _conn = conn;
        _isNumericHosts = isNumericHosts;
        _isNumericPorts = isNumericPorts;
        _proto = conn.getTypeString();
        _localPort =
            getFormattedPort(sigar, conn.getType(), conn.getLocalPort()); 
        _remotePort =
            getFormattedPort(sigar, conn.getType(), conn.getRemotePort()); 
    }

    public void setMaxLocalAddrLen(int len) {
        _maxLocalAddrLen = len;
    }

    public void setMaxRemoteAddrLen(int len) {
        _maxRemoteAddrLen = len;
    }

    public String getProtocol() {
        return _proto;
    }

    public String getLocalPort() {
        return _localPort;
    }

    public String getRemotePort() {
        return _remotePort;
    }

    private String getFormattedPort(SigarProxy sigar, int proto, long port) {
        if (port == 0) {
            return "*";
        }
        if (!_isNumericPorts) {
            String service =
                sigar.getNetServicesName(proto, port);
            if (service != null) {
                return service;
            }
        }

        return String.valueOf(port);
    }

    public String getFormattedAddress(String ip,
                                      String port,
                                      int max) {

        String address;

        if (NetFlags.isAnyAddress(ip)) {
            address = "*";
        }
        else if (_isNumericHosts) {
            address = ip;
        }
        else {
            try {
                address = InetAddress.getByName(ip).getHostName();
            } catch (UnknownHostException e) {
                address = ip;
            }
        }

        if (max != -1) {
            max -= port.length() + 1;
            if (address.length() > max) {
                address = address.substring(0, max);
            }
        }

        return address + ":" + port; 
    }

    public String getFormattedLocalAddress() {
        return getFormattedAddress(_conn.getLocalAddress(),
                                   _localPort,
                                   _maxLocalAddrLen);
    }
    
    public String getFormattedRemoteAddress() {
        return getFormattedAddress(_conn.getRemoteAddress(),
                                   _remotePort,
                                   _maxRemoteAddrLen);
    }

    public String getFormattedState() {
        if (_conn.getType() == NetFlags.CONN_UDP) {
            return "";
        }
        else {
            return _conn.getStateString();
        }
    }

    public long getProcessPid() {
        return _processPid;
    }

    public String getProcessName() {
        return _processName;
    }

    public String getFormattedProcessName() {
        if (_processPid == -1) {
            return "";
        }
        return _processPid + "/" + _processName;
    }

    public void lookupProcessInfo(SigarProxy sigar) {
        if (_conn.getState() != NetFlags.TCP_LISTEN) {
            return;
        }
        try {
            long pid =
                sigar.getProcPort(_conn.getType(),
                                  _conn.getLocalPort());
            if (pid != 0) { //XXX another bug
                _processPid = pid;
                _processName =
                    sigar.getProcState(pid).getName();
            }
        } catch (SigarException e) {
        }
    }
}
