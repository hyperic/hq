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

package org.hyperic.hq.autoinventory;

import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.product.RuntimeResourceReport;

/**
 * A CompositeRuntimeResourceReport is sent from an agent to the CAM server
 * to report resources detected at runtime through monitoring
 * capabilities.  Resources detected in a runtime scan are automatically
 * added to the appdef model, and do not require explicit approval the
 * way that platforms and servers detected through regular autoinventory 
 * scans do.
 * <br><br>
 * A CompositeRuntimeResourceReport is essentially a Map.  The keys in the map
 * are Integers representing the serverIds of the servers that performed
 * the runtime scan.  The values are RuntimeResourceReport objects representing
 * the platforms, servers, and services that were detected in the scan
 * for that server.
 * <br><br>
 */
public class CompositeRuntimeResourceReport {

    private RuntimeResourceReport[] _serverReports =
        new RuntimeResourceReport[0];

    public RuntimeResourceReport[] getServerReports () { 
        return _serverReports; 
    }

    /**
     * This method will be used by AXIS in SOAP deserialization.
     * Generally you do not want to call this method directly, instead use
     * the addServerReport method.
     */
    public void setServerReports (RuntimeResourceReport[] serverReports) {
        _serverReports = serverReports;
    }

    /**
     * Add a RuntimeResourceReport for the specified server.  Any existing
     * report for the server within this composite report will be overwritten.
     */
    public void addServerReport (RuntimeResourceReport report) {
        if (report == null) {
            return;
        }
        RuntimeResourceReport newReportArray[] = { report };
        _serverReports = (RuntimeResourceReport[]) 
            ArrayUtil.combine(_serverReports, newReportArray);
    }

    public String toString () {
        return "[CompositeRRR " + StringUtil.arrayToString(_serverReports) + "]";
    }

    public boolean isSameReport (CompositeRuntimeResourceReport other) {
        if (other == null) {
            return false;
        }
        RuntimeResourceReport[] rrr1, rrr2;
        rrr1 = getServerReports();
        rrr2 = other.getServerReports();
        // System.err.println("\n\nCISR: DIFFING:\nthis="+this+"\nother="+other+"\n");
        if (rrr1.length != rrr2.length) {
            // System.err.println("CISR: lengths differ.");
            return false;
        }
        boolean foundMatchingRRR;
        for (int i=0; i<rrr1.length; i++) {
            foundMatchingRRR = false;
            for (int j=0; j<rrr2.length; j++) {
                if (rrr1[i].isSameReport(rrr2[j])) {
                    foundMatchingRRR = true;
                    break;
                }
            }
            if (!foundMatchingRRR) {
                // System.err.println("CISR: no matching report for:"+rrr1[i]);
                return false;
            }
        }
        return true;
    }

    public String simpleSummary () {
        String rstr = "[CompositeRRR ";
        for ( int i=0; i<_serverReports.length; i++ ) {
            rstr += "\n\tReport #" + i + " from " + "reporting server=" + _serverReports[i].getServerId() + ": ";
            AIPlatformValue[] platforms = _serverReports[i].getAIPlatforms();
            for ( int j=0; j<platforms.length; j++ ) {
                rstr += "\n\t\tPlatform #" + j
                    + " ID=" + platforms[j].getId()
                    + " FQDN=" + platforms[j].getFqdn();
                AIServerValue[] servers
                    = ((AIPlatformValue) platforms[j]).getAIServerValues();
                if ((servers == null) || (servers.length == 0)) {
                    continue;
                }
                for (int k=0; k<servers.length; k++) {
                    rstr += "\n\t\t\tServer #" + k 
                        + " ID=" + servers[k].getId()
                        + " Name=" + servers[k].getName()
                        + " Stype=" + servers[k].getServerTypeName();
                    if (!(servers[k] instanceof AIServerExtValue)) {
                        continue;
                    }
                    AIServiceValue[] services
                        = ((AIServerExtValue) servers[k]).getAIServiceValues();
                    rstr += " serviceCount=" +
                    ((services != null) ? services.length : -1);
                    // for (int m=0; m<services.length; m++) {}
                }
            }
        }
        return rstr + "\n]";
    }

    public String fullSummary() {
        final StringBuilder rstr = new StringBuilder(1024);
        rstr.append("[CompositeRRR ");
        for ( int i=0; i<_serverReports.length; i++ ) {
            rstr.append("\n\tReport #").append(i).append(" from reporting server=")
                .append(_serverReports[i].getServerId()).append(": ");
            final AIPlatformValue[] platforms = _serverReports[i].getAIPlatforms();
            if (platforms == null) {
                continue;
            }
            for (int j=0; j<platforms.length; j++ ) {
                final AIPlatformValue platform = platforms[j];
                final Integer platformId = platform.getId();
                final String platformFqdn = platform.getFqdn();
                final String platformName = platform.getName();
                final String platformType = platform.getPlatformTypeName();
                rstr.append("\n\t\tPlatform #").append(j)
                    .append(" ID=").append(platformId)
                    .append(" FQDN=").append(platformFqdn)
                    .append(" Name=").append(platformName)
                    .append(" Type=").append(platformType);
                final AIServerValue[] servers = platforms[j].getAIServerValues();
                rstr.append(" serverCount=").append((servers != null) ? servers.length : -1);
                if (servers == null) {
                    continue;
                }
                for (int k=0; k<servers.length; k++) {
                    final AIServerValue server = servers[k];
                    final Integer serverId = server.getId();
                    final String serverName = server.getName();
                    final String serverType = server.getServerTypeName();
                    rstr.append("\n\t\t\tServer #").append(k)
                        .append(" ID=").append(serverId)
                        .append(" Name=").append(serverName)
                        .append(" Stype=").append(serverType);
                    if (!(servers[k] instanceof AIServerExtValue)) {
                        continue;
                    }
                    final AIServiceValue[] services = ((AIServerExtValue) servers[k]).getAIServiceValues();
                    rstr.append(" serviceCount=").append((services != null) ? services.length : -1);
                    if (services == null) {
                        continue;
                    }
                    for (int m=0; m<services.length; m++) {
                        final AIServiceValue service = services[m];
                        final Integer serviceId = service.getId();
                        final String serviceName = service.getName();
                        final String serviceType = service.getServiceTypeName();
                        rstr.append("\n\t\t\t\tService #").append(m)
                            .append(" ID=").append(serviceId)
                            .append(" Name=").append(serviceName)
                            .append(" Type=").append(serviceType);
                    }//loop services
                }//loop servers
            }//loop platforms
        }//loop serverReports
        rstr.append("\n]");
        return rstr.toString();
    }

}
