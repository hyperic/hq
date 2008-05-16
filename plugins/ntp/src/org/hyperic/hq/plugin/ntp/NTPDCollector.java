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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

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

    protected void init() throws PluginException {
        String ntpdc = getProperty(PROP_NTPDC, "");
        if (!new File(ntpdc).exists()) {
            throw new PluginException(PROP_NTPDC + "=" + ntpdc + " does not exist");
        }
    }

    public void collect() {
        String ntpdc = getProperty(PROP_NTPDC);
        int timeout = getTimeoutMillis();
        String argv[] = new String[ARGS.length + 3];
        argv[0] = ntpdc;
        argv[1] = "-c";
        argv[2] = "'timeout " + timeout + "'";
        System.arraycopy(ARGS, 0, argv, 3, ARGS.length);

        ByteArrayOutputStream output = 
            new ByteArrayOutputStream();
        //peer timeout is short, use longer timeout for the exec
        ExecuteWatchdog wdog =
            new ExecuteWatchdog(15 * 1000);
        Execute exec =
            new Execute(new PumpStreamHandler(output), wdog);

        exec.setCommandline(argv);

        try {
            int exitStatus = exec.execute();
            if (exitStatus != 0 || wdog.killedProcess()) {
                setErrorMessage(ntpdc+" command failed");
                return;
            }
        } catch (Exception e) {
            setErrorMessage("Unable to exec process: " + e);
            return;
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new StringReader(output.toString()));
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
                    if (exploded.length < 2) {
                        setErrorMessage(
                            "Unable to parse ntpdc output for jitter: ");
                        return;
                    }
                    setValue("Jitter", Double.parseDouble(exploded[1]));
                } else if (line.startsWith("system uptime:") ||
                           (line.startsWith("time since restart:"))) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    if (exploded.length < 1) {
                        setErrorMessage(
                            "Unable to parse ntpdc output for system uptime: ");
                        return;
                    }
                    setValue("Uptime",
                        Double.parseDouble(exploded[exploded.length - 1]));
                } else if (line.startsWith("time since reset:")) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    if (exploded.length < 4) {
                        setErrorMessage(
                            "Unable to parse ntpdc output for time since reset: ");
                        return;
                    }
                    setValue("TimeSinceReset", Double.parseDouble(exploded[3]));
                } else if (seenPeerHeader) {
                    String[] exploded = StringUtil.explodeQuoted(line);
                    if (exploded.length < 8) {
                        setErrorMessage(
                            "Unable to parse ntpdc output for seenPeerHeader: ");
                        return;
                    }

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
            setValue("PeerAverageDisplacement", peerDisp/peers);
            //XXX compat w/ old alias
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
