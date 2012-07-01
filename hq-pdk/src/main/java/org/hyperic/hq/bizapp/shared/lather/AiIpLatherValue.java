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

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.lather.LatherValue;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.hq.appdef.shared.AIIpValue;

public class AiIpLatherValue extends LatherValue {

    private static final String PROP_CTIME = "CTime";
    private static final String PROP_MACADDRESS = "MACAddress";
    private static final String PROP_MTIME = "MTime";
    private static final String PROP_ADDRESS = "address";
    private static final String PROP_DIFF = "diff";
    private static final String PROP_ID = "id";
    private static final String PROP_IGNORED = "ignored";
    private static final String PROP_NETMASK = "netmask";
    private static final String PROP_QUEUESTATUS = "queueStatus";

    public AiIpLatherValue() {
        super();
    }

    public AiIpLatherValue(AIIpValue v) {
        this();

        if (v.cTimeHasBeenSet()) {
            this.setDoubleValue(PROP_CTIME, (double) v.getCTime().longValue());
        }

        String macAddress = v.getMACAddress();
        if (v.mACAddressHasBeenSet() && macAddress != null) {
            this.setStringValue(PROP_MACADDRESS, macAddress);
        }

        if (v.mTimeHasBeenSet()) {
            this.setDoubleValue(PROP_MTIME, (double) v.getMTime().longValue());
        }

        String address = v.getAddress();
        if (v.addressHasBeenSet() && address != null) {
            this.setStringValue(PROP_ADDRESS, address);
        }

        if (v.diffHasBeenSet()) {
            this.setDoubleValue(PROP_DIFF, (double) v.getDiff());
        }

        if (v.idHasBeenSet()) {
            this.setIntValue(PROP_ID, v.getId());
        }

        if (v.ignoredHasBeenSet()) {
            this.setIntValue(PROP_IGNORED, v.getIgnored() ? 1 : 0);
        }

        String netmask = v.getNetmask();
        if (v.netmaskHasBeenSet() && netmask != null) {
            this.setStringValue(PROP_NETMASK, netmask);
        }

        if (v.queueStatusHasBeenSet()) {
            this.setIntValue(PROP_QUEUESTATUS, v.getQueueStatus());
        }
    }

    public AIIpValue getAIIpValue() {
        AIIpValue r = new AIIpValue();

        try {
            r.setCTime((long) this.getDoubleValue(PROP_CTIME));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setMACAddress(this.getStringValue(PROP_MACADDRESS));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setMTime((long) this.getDoubleValue(PROP_MTIME));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setAddress(this.getStringValue(PROP_ADDRESS));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setDiff((long) this.getDoubleValue(PROP_DIFF));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setId(this.getIntValue(PROP_ID));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setIgnored(this.getIntValue(PROP_IGNORED) == 1);
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setNetmask(this.getStringValue(PROP_NETMASK));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setQueueStatus(this.getIntValue(PROP_QUEUESTATUS));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        return r;
    }

    public void validate()
            throws LatherRemoteException {
    }
}
