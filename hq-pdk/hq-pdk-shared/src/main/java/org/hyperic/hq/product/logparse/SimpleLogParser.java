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

package org.hyperic.hq.product.logparse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.StringTokenizer;

import org.hyperic.hq.product.RtStat;

/* This is a very simple log parser that uses a StringTokenizer instead
 * of a regular expression to parse the log file.  This should greatly
 * improve the performance.  It does require that the log uses the format:
 *
 *     URI Date_in_milliseconds time_taken status_code
 *
 * This is the format that all of our native plugins use.
 */
public class SimpleLogParser extends BaseLogParser
{
    static SimpleLogParser instance = null;
    double tm;

    public SimpleLogParser() {
        super();
    }

    public static SimpleLogParser getInstance() {
        if (instance == null) {
            instance = new SimpleLogParser();
        }

        return instance;
    }

    public void initConfig(double timeMultiplier, String regex)
    {
        tm = timeMultiplier;
    }

    public RtStat parseLine(String current)
    {
        if (current.equals("-") || current.trim().length() == 0) {
            /* This is a special case for the log parser.  Basically, it means
             * that we have an entry in the log file that doesn't contain
             * valid data.  This can happen in the End-user case with Apache,
             * requests other than /CAM_blank.gif will get a - as the only
             * entry.  Or if somebody added a blank line.
             */
            return null;
        }
        try {
            StringTokenizer st = new StringTokenizer(current);
            
            String url = st.nextToken();
            long msec = Long.parseLong(st.nextToken());
            double time_taken = Double.parseDouble(st.nextToken()) * tm;
            Integer status = Integer.valueOf(st.nextToken());
            String ip = null;
            if (st.hasMoreTokens()) {
                ip = st.nextToken();
                
                // Make sure we form the IP correctly
                String[] strips = ip.split("^\\d+\\.\\d+\\.\\d+\\.\\d+");
                for (int i = 0; i < strips.length; i++) {
                    if (strips[i].length() > 0) {
                        int index = ip.indexOf(strips[i]);
                        ip = ip.substring(0, index);
                        break;
                    }
                }
            }
            
            RtStat rs = new RtStat(id, svcType, ip);
            rs.recompute(url, new Date(msec), time_taken, status);
            return rs;
        } catch (Exception e) {
            /* Yes, we shouldn't catch all exceptions here, but this allows
             * us to be very lenient in parsing errors.  If the log file has
             * an error, we just want to ignore it and move on with the next
             * entry.
             */
            log.debug("Problem parsing line: " + current);
            return null;
        }
    }

    public void postFileParse(File f)
        throws IOException
    {
        /* After the SimpleLogParser has completely parsed the file, truncate
         * it.  This is safe because we own any file that can be parsed by
         * this log parser.
         */
        try {
            String mode = "rws";
            RandomAccessFile toTrunc = new RandomAccessFile(f, mode);
            toTrunc.setLength(0);
            toTrunc.close();
        } catch (SecurityException e) {
            /* User doesn't have permission to change the length, so
             * ignore this exception.
             */
        } catch (FileNotFoundException e) {
            /* Can't happen.  We have just parsed this file.
             * Could be a permission error.  Log it.
             */
            log.error("Encountered file error truncating log: " + e);
        }
        
    }
}
