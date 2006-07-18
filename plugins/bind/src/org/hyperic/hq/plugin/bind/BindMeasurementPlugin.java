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

package org.hyperic.hq.plugin.bind;

import java.util.HashMap;
import java.util.Properties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;

import org.hyperic.util.JDK;

public class BindMeasurementPlugin
    extends SigarMeasurementPlugin
{
    static final String PROP_RNDC = "rndc";
    static final String PROP_NAMED_STATS = "named.stats";

    private static final HashMap queryInfo = new HashMap();

    public void getQueryInfo(Properties props)
        throws MetricNotFoundException
    {
        String rndc = props.getProperty(PROP_RNDC);
        String namedStats = props.getProperty(PROP_NAMED_STATS);
        
        File statsFile = new File(namedStats);

        // Test the stats file exists, and is writeable by the
        // agent process.
        if (!statsFile.exists()) {
            throw new MetricNotFoundException("Stats file " + namedStats +
                                              " does not exist.  Please " +
                                              "create this file and make " +
                                              "sure it is writeable by " +
                                              "the agent process");
        }
        if (!statsFile.canWrite()) {
            throw new MetricNotFoundException("Stats file " + namedStats +
                                              " is not writable by agent " +
                                              "process.");
        }

        // Before doing anything, truncate the current stats file.
        try {
            String mode = "rws";
            RandomAccessFile toTrunc = new RandomAccessFile(statsFile, mode);
            toTrunc.setLength(0);
            toTrunc.close();
        } catch (IOException e) {
            throw new MetricNotFoundException("IO Error reading stats file: " +
                                              e);
        }

        // Execute rndc to generate named.stats
        Process proc;
        try {
            String[] argv = new String[] { rndc, "stats" };

            proc = Runtime.getRuntime().exec(argv);

            // Check error condition.
            BufferedReader err = 
                new BufferedReader(new InputStreamReader(proc.
                                                         getErrorStream()));
            String line = err.readLine();
            if (line != null) {
                throw new MetricNotFoundException("Unable to exec process: " +
                                                  line);
            }

            try {
                err.close();
            } catch (IOException e) {}
        } catch (IOException e) {
            throw new MetricNotFoundException("Unable to exec process: " + e);
        }

        try {
            proc.waitFor();
        } catch (InterruptedException e) {
        }

        // Process the stats file.
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(statsFile));

            String line;
            Double val;
            while ((line = in.readLine()) != null) {

                if (line.startsWith("success")) {
                    val = new Double(line.substring(8 ,line.length()));
                    queryInfo.put("SuccessfulQueries", val);
                } else if (line.startsWith("referral")) {
                    val = new Double(line.substring(9, line.length()));
                    queryInfo.put("ReferralQueries", val);
                } else if (line.startsWith("nxrrset")) {
                    val = new Double(line.substring(8, line.length()));
                    queryInfo.put("NoRecordQueries", val);
                } else if (line.startsWith("nxdomain")) {
                    val = new Double(line.substring(9, line.length()));
                    queryInfo.put("NoDomainQueries", val);
                } else if (line.startsWith("recursion")) {
                    val = new Double(line.substring(10, line.length()));
                    queryInfo.put("RecursiveQueries", val);
                } else if (line.startsWith("failure")) {
                    val = new Double(line.substring(8, line.length()));
                    queryInfo.put("FailedQueries", val);
                }
            }
        } catch (IOException e) {
            throw new MetricNotFoundException("Unable to process rndc " +
                                              "output: " + e);
        } finally {
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

        // rndc statistic
        Double val = (Double)queryInfo.get(attr);
        if (val == null) {
            // not yet cached
            getQueryInfo(metric.getProperties());
            val = (Double)queryInfo.get(attr);
            if (val == null) {
                throw new MetricNotFoundException("No metric mapped to " +
                                                  " metric: " + attr);
            }
        }

        // remove the metric from the cache to force a refresh
        // next time around
        queryInfo.remove(attr);

        return new MetricValue(val.doubleValue(), System.currentTimeMillis());
    }
}
