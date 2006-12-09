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

package org.hyperic.hq.plugin.memcached;

import java.io.IOException;
import java.util.Map;

import org.hyperic.hq.plugin.netservices.NetServicesCollector;
import org.hyperic.hq.plugin.netservices.SocketWrapper;

public class MemcachedStats
    extends NetServicesCollector {

    private static final String STATS_CMD = "stats" + SocketWrapper.CRLF;
    private static final String END = "END";
    private static final String STAT = "STAT";
    private static final String GET_HITS = "get_hits";
    private static final String GET_CMD = "cmd_get";
    private static final String HIT_RATIO = "hit_ratio";

    public void collect() {
        SocketWrapper socket = null;

        try {
            startTime();

            socket = getSocketWrapper();

            setAvailability(true);

            socket.writeLine(STATS_CMD);

            String line;

            while ((line = socket.readLine()) != null) {
                if (line.startsWith(END)) {
                    break;
                }
                if (!line.startsWith(STAT)) {
                    continue;
                }
                line = line.substring(STAT.length()).trim();
                String key,val;
                int ix = line.indexOf(' ');
                if (ix == -1) {
                    continue;
                }

                key = line.substring(0, ix).trim();
                val = line.substring(ix+1).trim();

                setValue(key, val);
            }

            endTime();

            Map values = getResult().getValues();
            String get_hits = (String)values.get(GET_HITS);
            String get_cmd = (String)values.get(GET_CMD);

            if ((get_hits != null) && (get_cmd != null)) {
                double hits = Double.parseDouble(get_hits);
                double gets = Double.parseDouble(get_cmd);
                double hit_ratio = 0;
                if (hits != 0) {
                    hit_ratio = hits / gets;
                }
                setValue(HIT_RATIO, hit_ratio * 100);
            }
        } catch (IOException e) {
            setAvailability(false);
            if (getMessage() == null) {
                setErrorMessage(e.getMessage());
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        netstat();        
    }
}
