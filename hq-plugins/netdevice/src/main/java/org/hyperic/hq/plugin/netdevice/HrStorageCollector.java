/*
 * 'HrStorageCollector.java' NOTE: This copyright does *not* cover user programs
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

import java.util.Map;

import org.hyperic.snmp.SNMPSession;
import org.hyperic.snmp.SNMPValue;

public class HrStorageCollector
    extends SNMPCollector
{
    private static final String SIZE = "hrStorageSize";
    private static final String USED = "hrStorageUsed";
    private static final String UNITS = "hrStorageAllocationUnits";

    public static class HrUnitsConverter implements ColumnValueConverter {
        private Map _units;

        public HrUnitsConverter(Map units) {
            _units = units;
        }

        //
        // Convert value to bytes...
        // See HOST-RESOURCES-MIB::hrStorageAllocationUnits
        //
        public double convert(String index, SNMPValue value) throws Exception {
            double val = value.toLong();

            SNMPValue unit = (SNMPValue) _units.get(index);

            if (unit != null) {
                val *= unit.toLong();
            }

            return val;
        }
    }

    public void collect() {
        String columnName = getColumnName();

        if (columnName.equals(SIZE) || columnName.equals(USED)) {
            try {
                SNMPSession session = getSession();

                Map units = getIndexedColumn(session, UNITS, false);

                collectIndexedColumn(new HrUnitsConverter(units));
            } catch (Exception e) {
                return;
            }
        } else {
            collectIndexedColumn();
        }
    }
}
