/*
 * 'IfTableCollector.java' NOTE: This copyright does *not* cover user programs
 * that use HQ program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development Kit or
 * the Hyperic Client Development Kit - this is merely considered normal use of
 * the program, and does *not* fall under the heading of "derived work".
 * Copyright (C) [2004, 2005, 2006, 2007, 2008, 2009], Hyperic, Inc. This file
 * is part of HQ. HQ is free software; you can redistribute it and/or modify it
 * under the terms version 2 of the GNU General Public License as published by
 * the Free Software Foundation. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.netdevice;

import java.util.HashMap;
import java.util.List;

import org.hyperic.hq.product.PluginException;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;

public class IfTableCollector
    extends SNMPCollector
{
    private boolean _isVersion1;
    private String _columnName;

    private static HashMap counter64 = new HashMap();

    //
    // Conditionally use Counter64 versions of IF-MIB metrics.
    // These metrics are not supported when using SNMPv1.
    // Not all devices support the 64 bit versions, regardless.
    //
    static {
        counter64.put("ifInOctets", "ifHCInOctets");
        counter64.put("ifOutOctets", "ifHCOutOctets");
        counter64.put("ifInUcastPkts", "ifHCInUcastPkts");
        counter64.put("ifOutUcastPkts", "ifHCOutUcastPkts");
        counter64.put("ifInNUcastPkts", "ifInMulticastPkts");
        counter64.put("ifOutNUcastPkts", "ifOutMulticastPkts");
    }

    protected String getColumnName() {
        return _columnName;
    }

    protected void init(SNMPSession session) throws PluginException {
        _isVersion1 = "v1".equals(_props.getProperty(SNMPClient.PROP_VERSION));

        _columnName = super.getColumnName();

        if (_columnName == null) {
            throw new PluginException(PROP_COLUMN + " not defined: " + getProperties() + " (stale template?)");
        }

        if (_isVersion1) {
            return;
        }

        String name = (String) counter64.get(_columnName);

        if (name != null) {
            List list64 = null;

            try {
                list64 = session.getBulk(name);
            } catch (SNMPException e) {
            }

            if (isEmpty(list64, name)) {
                getLog().debug(getInfo() + " does not support Counter64: " + name);
            } else {
                getLog().debug("Switching to 64 bit counter: " + _columnName + "->" + name + ": " + getInfo());

                _columnName = name;
            }
        }

        setSource(_columnName + "@" + getInfo());
    }

    protected boolean isTotalCounter(String name) {
        return name.endsWith("Octets");
    }

    public void collect() {
        collectIndexedColumn();
    }
}
