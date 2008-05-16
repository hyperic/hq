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

package org.hyperic.hq.plugin.zimbra;

import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.ProductPlugin;

import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileTail;
import org.hyperic.sigar.FileWatcherThread;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class ZimbraCollector extends Collector
{
    private String myLastLine = null;
    private String myStatsFile;
    private FileTail myWatcher;
    private BufferedReader myReader;
    private Map myAliasMap = new HashMap();
    private static Log myLog = LogFactory.getLog(ZimbraCollector.class.getName());

    protected void init()
    {
        try
        {
            //read 1st line of filename and create a List or String[] of the field names
            myStatsFile = getProperty(ProductPlugin.PROP_INSTALLPATH)+"/log/zimbrastats.csv";
            myReader = new BufferedReader(new FileReader(myStatsFile));
            String line = myReader.readLine();
            if (line == null)
                return;
            String[] metrics = line.split(",");
            for (int i=0; i<metrics.length; i++)
                myAliasMap.put(new Integer(i), metrics[i]);

            myWatcher = getWatcher();

            FileWatcherThread.getInstance().add(myWatcher);
            FileWatcherThread.getInstance().doStart();
            myWatcher.add(myStatsFile); //the .csv file
        }
        catch (SigarException e) {
            myLog.error(e.getMessage(), e);
        }
        catch (FileNotFoundException e) {
            myLog.error(e.getMessage(), e);
        }
        catch (IOException e) {
            myLog.error(e.getMessage(), e);
        }
        finally
        {
            try { if (myReader != null) myReader.close(); } catch (IOException e) { }
        }
    }

    private FileTail getWatcher()
    {
        return new FileTail(new Sigar())
        {
            public void tail(FileInfo info, Reader reader)
            {
                String line;
                BufferedReader buffer = new BufferedReader(reader);
                try
                {
                    myLastLine = null;
                    while ((line = buffer.readLine()) != null)
                        myLastLine = line;
                }
                catch (IOException e) {
                    myLog.error(e.getMessage(), e);
                }
            }
        };
    }

    public void collect()
    {
        try
        {
            if (myLastLine == null)
                return;
            String[] metrics = myLastLine.split(",");
            // start at column 1 to ignore the timestamp
            for (int i=1; i<metrics.length; i++)
            {
                String alias = null;
                if (null == metrics[i] ||
                    metrics[i].matches("^\\s*$") ||
                    null == (alias = (String)myAliasMap.get(new Integer(i)))) {
                    continue;
                }
                myLog.debug(alias+", "+metrics[i]);
                setValue(alias, metrics[i]);
            }
        }
        finally {
            myLastLine = null;
        }
    }
}
