/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.StringUtil;

public class NTPDCollector extends Collector {

    static final String PROP_NTPDC = "ntpdc";

    private static final String[] ARGS = {
        "-c", "sysinfo",
        "-c", "sysstats",
        "-c", "peers"
    };

    //ThreadPool opt-in
    public boolean isPoolable() {
        return true;
    }

    //override old default timeout of 1 second.
    public int getTimeout() {
        int timeout = super.getTimeout();
        if (timeout == 1) {
            timeout = getDefaultTimeout();
        }
        return timeout;
    }

    protected void init() throws PluginException {
        String ntpdc = getProperty(PROP_NTPDC, "");
        if (!new File(ntpdc).exists()) {
            throw new PluginException(PROP_NTPDC + "=" + ntpdc + " does not exist");
        }
    }

    public void collect() {
        String ntpdc = getProperty(PROP_NTPDC);
        int timeout = getTimeout();

        Process proc;
        try {
            String argv[] = new String[ARGS.length + 3];
            argv[0] = ntpdc;
            argv[1] = "-c";
            argv[2] = "'timeout " + timeout + "000'";
            System.arraycopy(ARGS, 0, argv, 3, ARGS.length);

            proc = Runtime.getRuntime().exec(argv);
        } catch (IOException e) {
            setErrorMessage("Unable to exec process: " + e);
            return;
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
            while ((line = in.readLine()) != null) {
                if (line.startsWith("========")) {
                    // Peer information comes last
                    seenPeerHeader = true;
                    continue;
                }

                if (line.startsWith("jitter:")) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    setValue("Jitter", Double.parseDouble(exploded[1]));
                } else if (line.startsWith("system uptime:") ||
                           (line.startsWith("time since restart:"))) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    setValue("Uptime", Double.parseDouble(exploded[exploded.length - 1]));
                } else if (line.startsWith("time since reset:")) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    setValue("TimeSinceReset", Double.parseDouble(exploded[3]));
                } else if (seenPeerHeader) {
                    String[] exploded = StringUtil.explodeQuoted(line);

                    peers++;
                    
                    double delay = Double.parseDouble(exploded[5]);
                    double offset = Double.parseDouble(exploded[6]);
                    double disp = Double.parseDouble(exploded[7]);

                    peerDelay = peerDelay + Math.abs(delay);
                    peerOffset = peerOffset + Math.abs(offset);
                    peerDisp = peerDisp + Math.abs(disp);
                }
            }
            
            setValue("Peers", peers);
            setValue("PeerAverageDelay", peerDelay/peers);
            setValue("PeerAverageOffset", peerOffset/peers);
            setValue("PeerAverageDisp", peerDisp/peers);
        } catch (IllegalArgumentException e) {
            setErrorMessage("Unable to parse ntpdc output: " + e);
        } catch (IOException e) {
            setErrorMessage("Unable to process ntpdc output: " + e);
        } finally {
            // XXX: wait for process to exit?
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }
    }
}
