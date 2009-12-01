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

package org.hyperic.hq.plugin.coldfusion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.ProductPlugin;

import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileTail;
import org.hyperic.sigar.FileWatcherThread;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class ColdfusionCollector extends Collector
{
    private List myLines = new ArrayList();
    private String myStatsFile;
    private FileTail myWatcher;
    private BufferedReader myReader;
    private static Log myLog = LogFactory.getLog(ColdfusionCollector.class);
    private static final Pattern LINE_PATTERN = 
        Pattern.compile("\\w+=\\d+\\.?(\\d+)?");
    private static final Pattern NAMEVAL_PATTERN = 
        Pattern.compile("^\\w+=\\d+\\.?(\\d+)?$");

    protected void init()
    {
        try
        {
            Properties props = getProperties();
            //read 1st line of filename and create a List or String[] 
            //of the field names
            myStatsFile = props.getProperty(ProductPlugin.PROP_INSTALLPATH) + "/" +
                          props.getProperty("logfile");

            myWatcher = getWatcher();

            FileWatcherThread.getInstance().add(myWatcher);
            FileWatcherThread.getInstance().doStart();
            myWatcher.add(myStatsFile);
        }
        catch (SigarException e) {
            myLog.error(e.getMessage(), e);
        }
        finally
        {
            try {
                if (myReader != null) myReader.close();
            }
            catch (IOException e) {
                myLog.error(e.getMessage(), e);
            }
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
                    synchronized(myLines) {
                        while ((line = buffer.readLine()) != null)
                            myLines.add(line);
                    }
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
            synchronized(myLines)
            {
                for (Iterator i=myLines.iterator(); i.hasNext(); )
                {
                    String line = (String)i.next();
                    if (!LINE_PATTERN.matcher(line).find())
                        continue;

                    String[] tokens = line.split("\\s+");
                    for (int j=0; j<tokens.length; j++)
                    {
                        if (!NAMEVAL_PATTERN.matcher(tokens[j]).find())
                            continue;
                        String[] tmp = tokens[j].split("=");
                        myLog.debug("alias -> "+tmp[0]+", value -> "+tmp[1]);
                        setValue(tmp[0], tmp[1]);
                    }
                }
            }
        }
        finally
        {
            synchronized(myLines) {
                myLines.clear();
            }
        }
    }
}
