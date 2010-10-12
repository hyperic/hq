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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarException;

public class NetstatData {
    public static final String LABEL_PROTO = "Proto";
    public static final String LABEL_LADDR = "Local Address";
    public static final String LABEL_RADDR = "Foreign Address";
    public static final String LABEL_STATE = "State";

    private boolean _isNumericHosts = true;
    private boolean _isNumericPorts = false;
    private boolean _wantPid = false;
    private int _flags = 
        NetFlags.CONN_CLIENT | NetFlags.CONN_PROTOCOLS;
    private List _connections;

    public NetstatData() {}

    public void populate(SigarProxy sigar) throws SigarException {
        _connections = new ArrayList();
        NetConnection[] connections =
            sigar.getNetConnectionList(_flags);

        for (int i=0; i<connections.length; i++) {
            NetConnectionData data =
                new NetConnectionData(sigar,
                                      connections[i],
                                      _isNumericHosts,
                                      _isNumericPorts);

            if (_wantPid) {
                data.lookupProcessInfo(sigar);
            }

            _connections.add(data);
        }
    }

    public List getConnections() {
        return _connections;
    }

    public void setIsNumeric(boolean isNumeric) {
        _isNumericHosts = _isNumericPorts = isNumeric;
    }

    public void setIsNumericHosts(boolean isNumeric) {
        _isNumericHosts = isNumeric;
    }

    public void setIsNumericPorts(boolean isNumeric) {
        _isNumericPorts = isNumeric;
    }

    public boolean wantPid() {
        return _wantPid;
    }

    public void setWantPid(boolean wantPid) {
        _wantPid = wantPid;
    }

    public int getFlags() {
        return _flags;
    }

    public void setFlags(int flags) {
        _flags = flags;
    }

    public void setFlags(String flags) {
        setFlags(new String[] { flags });
    }

    public void setFlags(String[] args) {
        int proto_flags = 0;

        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            int j = 0;

            while (j<arg.length()) {
                switch (arg.charAt(j++)) {
                  case '-':
                    continue;
                  case 'l':
                    _flags &= ~NetFlags.CONN_CLIENT;
                    _flags |= NetFlags.CONN_SERVER;
                    break;
                  case 'a':
                    _flags |= NetFlags.CONN_SERVER | NetFlags.CONN_CLIENT;
                    break;
                  case 'n':
                    setIsNumeric(true);
                    break;
                  case 'p':
                    _wantPid = true;
                    break;
                  case 't':
                    proto_flags |= NetFlags.CONN_TCP;
                    break;
                  case 'u':
                    proto_flags |= NetFlags.CONN_UDP;
                    break;
                  case 'w':
                    proto_flags |= NetFlags.CONN_RAW;
                    break;
                  case 'x':
                    proto_flags |= NetFlags.CONN_UNIX;
                    break;
                  default:
                }
            }
        }

        if (proto_flags != 0) {
            _flags &= ~NetFlags.CONN_PROTOCOLS;
            _flags |= proto_flags;
        }
    }

    public void print(PrintStream out) {
        final String header =
            LABEL_PROTO + "\t" +
            LABEL_LADDR + "\t" +
            LABEL_RADDR + "\t" +
            LABEL_STATE;
        out.println(header);

        List connections = getConnections();
        for (int i=0; i<connections.size(); i++) {
            NetConnectionData data =
                (NetConnectionData)connections.get(i);

            String conn =
                data.getProtocol() + "\t" +
                data.getFormattedLocalAddress() + "\t" +
                data.getFormattedRemoteAddress() + "\t" +
                data.getFormattedState() + "\t" +
                data.getFormattedProcessName();

            out.println(conn);
        }
    }

    public static void main(String[] args) throws Exception {
        Sigar sigar = new Sigar();
        NetstatData data = new NetstatData();
        if (args.length != 0) {
            data.setFlags(args);
        }
        data.populate(sigar);
        data.print(System.out);
        sigar.close();
    }
}
