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

package org.hyperic.hq.ui.action.resource.platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.ui.action.resource.ResourceForm;

/**
 * A subclass of <code>ResourceForm</code> representing the
 * <em>New Platform</em> form.
 */
public class PlatformForm
    extends ResourceForm {

    // -------------------------------------instance variables

    private Integer cpuCount;
    private List cpuCounts;
    private String fqdn;
    private IpValue[] ips;
    private int numIps;
    private List agents = new ArrayList();
    private Integer agentId;
    private String agentIpPort;

    // -------------------------------------constructors

    public PlatformForm() {
        super();
        setDefaults();
    }

    // -------------------------------------public methods

    public Integer getCpuCount() {
        return this.cpuCount;
    }

    public void setCpuCount(Integer cpuCount) {
        this.cpuCount = cpuCount;
    }

    public List getCpuCounts() {
        if (this.cpuCounts == null) {
            this.cpuCounts = new ArrayList();

            String[] counts = new String[] { "1", "2", "4", "8", "16" };
            for (int i = 0; i < counts.length; i++) {
                this.cpuCounts.add(new LabelValueBean(counts[i], counts[i]));
            }
        }

        return this.cpuCounts;
    }

    public String getFqdn() {
        return this.fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public IpValue getIp(int index) {
        if (index >= ips.length || this.ips[index] == null) {
            setIp(index, new IpValue());
        }
        return this.ips[index];
    }

    public IpValue[] getIps() {
        return this.ips;
    }

    public void setIp(int index, IpValue ip) {
        if (index >= ips.length) {
            IpValue[] newIps = new IpValue[index + 1];
            System.arraycopy(ips, 0, newIps, 0, ips.length);
            newIps[index] = ip;
            this.ips = newIps;
        } else {
            ips[index] = ip;
        }
    }

    public void setIps(IpValue[] ips) {
        this.ips = ips;
        int numIps = ips != null ? ips.length : 0;
        setNumIps(numIps);
    }

    public int getNumIps() {
        return this.numIps;
    }

    public void setNumIps(int numIps) {
        this.numIps = numIps;
    }

    public Collection getAgents() {
        return agents;
    }

    public void setAgents(List agents) {
        this.agents = agents;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public String getAgentIpPort() {
        return this.agentIpPort;
    }

    public void setAgentIpPort(String agentIpPort) {
        this.agentIpPort = agentIpPort;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    private void setDefaults() {
        this.cpuCount = null;
        this.fqdn = null;
        this.ips = new IpValue[0];
        this.numIps = 0;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" cpuCount=");
        s.append(cpuCount);
        s.append(" fqdn=");
        s.append(fqdn);
        s.append(" ips=");
        if (ips != null) {
            s.append(Arrays.asList(ips));
        }
        s.append(" numIps=");
        s.append(numIps);

        return s.toString();
    }

    public void loadPlatformValue(PlatformValue platform) {
        loadResourceValue(platform);
        setResourceType(platform.getPlatformType().getId());
        setCpuCount(platform.getCpuCount());
        setFqdn(platform.getFqdn());
        // don't load ip addresses- let the action handle that itself,
        // so as to support adding and removing of ips from the form
        // jsp
    }

    public void updatePlatformValue(PlatformValue platform) {
        updateResourceValue(platform);
        platform.setCpuCount(getCpuCount());
        platform.setFqdn(getFqdn());

        // update ip addresses
        IpValue[] oldIps = platform.getIpValues();

        if (oldIps == null) {
            oldIps = new IpValue[0];
        }
        int numOldIps = oldIps.length;
        int numNewIps = getNumIps();

        // add all new ips
        HashMap oldIpsMap = new HashMap();
        for (int i = 0; i < numOldIps; i++) {
            oldIpsMap.put(oldIps[i].getId(), oldIps[i]);
        }

        for (int i = 0; i < numNewIps; i++) {
            IpValue newIp = getIp(i);
            if (newIp.getId() != null && newIp.getId().intValue() > 0) {
                IpValue oldIp = (IpValue) oldIpsMap.remove(newIp.getId());
                if (!newIp.getAddress().equals(oldIp.getAddress()) ||
                        !newIp.getNetmask().equals(oldIp.getNetmask()) ||
                        !newIp.getMACAddress().equals(oldIp.getMACAddress())) {
                    oldIp.setAddress(newIp.getAddress());
                    oldIp.setMACAddress(newIp.getMACAddress());
                    oldIp.setNetmask(newIp.getNetmask());
                    platform.updateIpValue(oldIp);
                }
            } else {
                // we're into the land of new ips- use the form one
                IpValue newAddedIp = getIp(i);
                IpValue dbNewIp = new IpValue();
                dbNewIp.setAddress(newAddedIp.getAddress());
                dbNewIp.setMACAddress(newAddedIp.getMACAddress());
                dbNewIp.setNetmask(newAddedIp.getNetmask());
                platform.updateIpValue(dbNewIp);
                platform.addIpValue(dbNewIp);
            }
        }

        // Remove the left-overs
        for (Iterator it = oldIpsMap.values().iterator(); it.hasNext();) {
            IpValue oldIp = (IpValue) it.next();
            platform.removeIpValue(oldIp);
        }
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        // don't validate if it's already happened in this action chain
        ActionErrors previous = (ActionErrors) request.getAttribute(Globals.ERROR_KEY);
        if (previous != null) {
            return null;
        }

        // don't validate if the user clicked any button other than 'ok'
        if (!isOkClicked() || getNumIps() == 0) {
            return null;
        }

        ActionErrors errors = super.validate(mapping, request);
        if (errors == null) {
            errors = new ActionErrors();
        }

        // manually validate ips since i can't figure out how to do it
        // in the config file
        IpValue[] ips = getIps();
        if (ips.length == 1) {
            String address = ips[0].getAddress();
            if (address.equals("127.0.0.1")) {
                ActionMessage err = new ActionMessage("resource.platform.inventory.error.IpAddressInvalid");
                errors.add("ip[0].address", err);
            }
        } else {
            for (int i = 1; i < ips.length; i++) {
                IpValue ip = ips[i];

                // address is required
                String address = ip.getAddress();
                if (address == null || "".equals(address)) {
                    ActionMessage err = new ActionMessage("resource.platform.inventory.error.IpAddressIsRequired");
                    errors.add("ip[" + i + "].address", err);
                }
            }
        }
        return errors.isEmpty() ? null : errors;
    }
}
