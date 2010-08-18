/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.plugin.zimbra.five;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileTail;
import org.hyperic.sigar.FileWatcherThread;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class ZimbraCollector extends Collector {

    private static final String STATSFILE = "statsfile";
    private String lastLine = null;
    private File myStatsFile;
    private FileTail myWatcher;
    private Map myAliasMap = new HashMap();
    private static Log log = LogFactory.getLog(ZimbraCollector.class);
    public static final String[] percentage_list = {"cpu.csv"};
    private boolean percentage = false;
    private Map acumulate = null;

    protected void init() {
        BufferedReader buffer = null;
        log.debug("init() prop=" + getProperties());
        log.debug("init() PConfig=" + getPlugin().getConfig());
        try {
            // read 1st line of filename and create a List or String[] of the field names
            assert (getProperties().getProperty(STATSFILE) != null);

            String file = getProperties().getProperty(STATSFILE);
            if (file == null) {
                log.error("propertie '" + STATSFILE + "' not found");
                return;
            }

            myStatsFile = new File(file);
            if (!myStatsFile.exists()) {
                log.error("File '" + myStatsFile + "' no found");
                return;
            }

            buffer = new BufferedReader(new FileReader(myStatsFile));
            String line = buffer.readLine();
            if (line == null) {
                return;
            }
            String[] metrics = line.split(",");
            log.debug("metrics= " + Arrays.asList(metrics));
            for (int i = 0; i < metrics.length; i++) {
                myAliasMap.put(new Integer(i), metrics[i].trim().replaceAll(":", "_"));
            }

            // read complete file to found last line
            //buffer.skip(myStatsFile.length() - 1000);
            while ((line = buffer.readLine()) != null) {
                lastLine = line;
            }

            // datas are percentage?
            percentage = Arrays.asList(percentage_list).contains(myStatsFile.getName());
            log.debug(myStatsFile.getName() + " --> percentage=" + percentage);

            // prepare acumulate metrics
            Properties acc_metrics = new Properties();
            log.debug("Using acumulate metrics file =>" + this.getClass().getClassLoader().getResource("/etc/metrics.prop"));
            acc_metrics.load(this.getClass().getClassLoader().getResourceAsStream("/etc/metrics.prop"));
            if (acc_metrics.getProperty(myStatsFile.getName()) != null) {
                acumulate = new HashMap();
                metrics = acc_metrics.getProperty(myStatsFile.getName()).split(",");
                log.debug("acumulate metrics ('" + myStatsFile.getName() + "') = " + Arrays.asList(metrics));
                for (int n = 0; n < metrics.length; n++) {
                    String metric = metrics[n];
                    acumulate.put(metric, new Integer(0));
                }
            }

            myWatcher = new Tail(new Sigar());

            FileWatcherThread.getInstance().add(myWatcher);
            FileWatcherThread.getInstance().doStart();
            myWatcher.add(myStatsFile); // the .csv file
        } catch (SigarException e) {
            log.error(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (buffer != null) {
                    buffer.close();
                }
            } catch (IOException e) {
            }
        }
    }
    // xxx
    NumberFormat nf = NumberFormat.getInstance(Locale.US);

    public void collect() {
        if (log.isDebugEnabled()) {
            log.debug("collect() (" + getProperties() + ")");
        }
        if (log.isTraceEnabled()) {
            log.trace("[collect] ("+getProperties().getProperty("statsfile")+") myLastLine=" + lastLine);
        }
        try {
            if (lastLine == null) {
                return;
            }
            String[] metrics = lastLine.split(",");
            // start at column 1 to ignore the timestamp
            for (int i = 1; i < metrics.length; i++) {
                // remove some spaces
                metrics[i] = metrics[i].trim();
                String alias = null;
                if (null == metrics[i] || metrics[i].matches("^\\s*$") || null == (alias = (String) myAliasMap.get(new Integer(i)))) {
                    continue;
                }
                try {
                    Number v = nf.parse(metrics[i]);
                    if (percentage) {
                        v = new Double(v.doubleValue() / 100d);
                    }
                    if (acumulate != null) {
                        Number va = (Number) acumulate.get(alias);
                        if (va != null) {
                            Integer vf = new Integer(v.intValue() + va.intValue());
                            acumulate.put(alias, vf);
                            //if (log.isTraceEnabled())
                            //	log.trace("read(v)='" + v + "' - prev(va)='"+va+"' - final(vf)='"+vf+"'");
                            v = vf;
                        }
                    }
                    setValue(alias, v.toString());
                    /*if (log.isTraceEnabled()) {
                        log.trace(alias + "= '" + metrics[i] + "' --> '" + v.toString() + "'");
                    }*/
                } catch (ParseException e) {
                    if (log.isTraceEnabled()) {
                        log.trace(alias + "=" + metrics[i] + "-->" + e.getMessage());
                    }
                    setValue(alias, metrics[i]);
                }
            }
        } finally {
            lastLine = null;
        }
    }

    private class Tail extends FileTail {

        private Log _log = LogFactory.getLog(Tail.class);

        public Tail(Sigar sigar) {
            super(sigar);
            _log.debug("<init>");
        }

        public void tail(FileInfo info, Reader reader) {
            _log.debug("tail(" + info + "," + reader + ")");
            String line;
            BufferedReader buffer = new BufferedReader(reader);
            try {
                lastLine = null;
                while ((line = buffer.readLine()) != null) {
                    lastLine = line;
                }
            } catch (IOException e) {
                _log.error(e.getMessage(), e);
            }
        }
    }
}
