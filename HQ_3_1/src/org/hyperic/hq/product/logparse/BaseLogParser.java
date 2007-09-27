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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.hyperic.hq.product.RtPlugin;
import org.hyperic.hq.product.RtStat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

public abstract class BaseLogParser implements LogParseInterface {

    protected ArrayList StatusDontLog;
    protected ArrayList URLDontLog;
    protected double timeMultiplier = 1;
    protected Log log;
    protected Integer id;
    protected int svcType;

    abstract public void initConfig(double timeMultiplier, String regex);
                           
    public BaseLogParser() {
        StatusDontLog = new ArrayList();
        URLDontLog = new ArrayList();

        log = LogFactory.getLog(BaseLogParser.class);
    }

    public Hashtable parseLog (String fname, String re, Integer ID, 
                               boolean collectIPs)
        throws IOException
    {
        return parseLog(fname, re, ID, RtPlugin.UNKNOWN, collectIPs);
    }

    public Hashtable parseLog (String fname, String re, Integer ID, 
                               int svcType, boolean collectIPs)
        throws IOException
    {
        return parseLog(fname, re, 0, ID, collectIPs);
    }

    public Hashtable parseLog (String fname, String re, long len, Integer ID, 
                               boolean collectIPs)
        throws IOException
    {
        long parsedlen[] = new long[1];
        return parseLog(new File(fname), re, len, ID, RtPlugin.UNKNOWN,
                        parsedlen, collectIPs);
    }

    public Hashtable parseLog (String fname, String re, long len, Integer ID,
                               int svcType, long parsedlen[], boolean collectIPs)
        throws IOException
    {
        return parseLog(new File(fname), re, len, ID, svcType, parsedlen, 
                        collectIPs);
    }

    /**
     * @return If collecting IPs, this is a Hashtable
     * of "ip:url" -> RtStat summary objects.  If not collecting IPs, then
     * the keys are just the urls, without the "ip:"
     */
    public Hashtable parseLog (File f, String re, long len, Integer ID, 
                               int svcType, long parsedlen[], 
                               boolean collectIPs) 
        throws IOException
    {
        GlobCompiler gc = new GlobCompiler();
        Perl5Matcher pm = new Perl5Matcher();
        Hashtable urls = new Hashtable();
        
        BufferedReader in = new BufferedReader(new FileReader(f));
        
        in.skip(len);
        
        String currentLine;
        this.id = ID;
        this.svcType = svcType;

        initConfig(timeMultiplier, re);
        
        while ( (currentLine = in.readLine ()) != null)
        {
            RtStat curr = parseLine(currentLine);

            if (curr == null) {
                continue;
            }
            if (!collectIPs) curr.resetIp();
                
            boolean logit = true;
            for (Iterator it = URLDontLog.iterator(); it.hasNext(); ) {
                String current = (String)it.next();
                try {
                    Pattern pa = gc.compile(current);
                    if (pm.matches(curr.getUrl(), pa)) {
                        logit = false;
                        break;
                    }
                } catch (MalformedPatternException e) {
                    this.log.error("Invalid regular expression: " +
                                   current);
                    continue;
                }
            }

            /* We know that there will only be a single status in the
             * curr.status hashtable, because we have only parsed a single
             * line.  So, find that status so that we can determine if we
             * should keep this or not.
             */
            Enumeration e = curr.getStatus().keys();
            Integer stat;
            if (e.hasMoreElements()) {
                stat = (Integer)e.nextElement();
            }
            else {
                stat = new Integer(200);
            }

            String ipUrlKey = curr.getIpUrlKey();
            if (!StatusDontLog.contains(stat) && logit) {
                
                // Determine if we have already found this URL
                RtStat found = (RtStat) urls.get(ipUrlKey);
                
                if (found == null) {
                    found = curr;
                }
                else {
                    found.recompute(curr);
                    
                }
                urls.put(ipUrlKey, found);
            }
        }

        in.close ();

        postFileParse(f);
        parsedlen[0] = f.length();
        return urls;
    }
    
    public void setTimeMultiplier(double mult)
    {
        timeMultiplier = mult;
    }

    public double getTimeMultiplier()
    {
        return timeMultiplier;
    }

    public void DontLog(Long stat)
    {
        StatusDontLog.add(stat);
    }

    public void DontLog(String url)
    {
        URLDontLog.add(url);
    }

    public void urlDontLog(ArrayList urls)
    {
        URLDontLog = urls;
    }

    public void postFileParse(File f)
        throws IOException
    {
    }
}
