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

package org.hyperic.hq.plugin.ntp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Properties;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;

import org.hyperic.util.StringUtil;

public class NTPMeasurementPlugin
    extends SigarMeasurementPlugin
{
    static final String PROP_NTPDC = "ntpdc";
    static final String PROP_TIMEOUT = "timeout";

    private static final String[] ARGS = {
        "-c", "sysinfo",
        "-c", "sysstats",
        "-c", "peers"
    };

    private static HashMap procInfo = new HashMap();

    public void getProcInfo(Properties props)
        throws MetricNotFoundException
    {
        String ntpdc = props.getProperty(PROP_NTPDC);
        String timeout = props.getProperty(PROP_TIMEOUT);

        // Set timeout to 1 second in case we have an old
        // metric template that does not sepecify the timeout
        // value.
        if (timeout == null) {
            timeout = "1";
        }

        Process proc;
        try {
            String argv[] = new String[ARGS.length + 3];
            argv[0] = ntpdc;
            argv[1] = "-c";
            argv[2] = "'timeout " + timeout + "000'";
            System.arraycopy(ARGS, 0, argv, 3, ARGS.length);

            proc = Runtime.getRuntime().exec(argv);
        } catch (IOException e) {
            throw new MetricNotFoundException("Unable to exec process: " + e);
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(proc.
                                                          getInputStream()));
            boolean seenPeerHeader = false;
            double peers      = 0;
            double peerDelay  = 0;
            double peerOffset = 0;
            double peerDisp   = 0;

            String line;
            Double val;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("========")) {
                    // Peer information comes last
                    seenPeerHeader = true;
                    continue;
                }

                if (line.startsWith("jitter:")) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    val = new Double(exploded[1]);
                    procInfo.put("Jitter", val);
                } else if (line.startsWith("system uptime:") ||
                           (line.startsWith("time since restart:"))) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    val = new Double(exploded[exploded.length - 1]);
                    procInfo.put("Uptime", val);
                } else if (line.startsWith("time since reset:")) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    val = new Double(exploded[3]);
                    procInfo.put("TimeSinceReset", val); 
                } else if (seenPeerHeader) {
                    String[] exploded = StringUtil.explodeQuoted(line);

                    peers++;
                    
                    double delay = new Double(exploded[5]).doubleValue();
                    double offset = new Double(exploded[6]).doubleValue();
                    double disp = new Double(exploded[7]).doubleValue();

                    peerDelay = peerDelay + Math.abs(delay);
                    peerOffset = peerOffset + Math.abs(offset);
                    peerDisp = peerDisp + Math.abs(disp);
                }
            }
            
            Double numPeers = new Double(peers);
            procInfo.put("Peers", numPeers);
            procInfo.put("PeerAverageDelay", new Double(peerDelay/peers));
            procInfo.put("PeerAverageOffset", new Double(peerOffset/peers));
            procInfo.put("PeerAverageDisp", new Double(peerDisp/peers));
        } catch (IllegalArgumentException e) {
            throw new MetricNotFoundException("Unable to parse ntpdc " +
                                              "output: " + e);
        } catch (IOException e) {
            throw new MetricNotFoundException("Unable to process ntpdc " +
                                              "output: " + e);
        } finally {
            // XXX: wait for process to exit?
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException
    {
        String domain = metric.getDomainName();
        String attr = metric.getAttributeName();

        if (domain.equals("sigar.ptql")) {
            return super.getValue(metric);
        }

        // ntpdc statistic

        Double val = (Double)procInfo.get(attr);
        if (val == null) {
            // not yet cached
            getProcInfo(metric.getProperties());
            val = (Double)procInfo.get(attr);
            if (val == null) {
                throw new MetricNotFoundException("No metric mapped to " +
                                                  " metric: " + attr);
            }
        }

        // remove the metric from the cache to force a refresh
        // next time around
        procInfo.remove(attr);

        return new MetricValue(val.doubleValue(), System.currentTimeMillis());
    }
}
