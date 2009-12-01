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

package org.hyperic.hq.product;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hyperic.hq.product.logparse.BaseLogParser;
import org.hyperic.hq.product.RtStat;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.io.GlobFilenameFilter;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Util;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public abstract class RtPlugin extends GenericPlugin {

    protected BaseLogParser lp             = null;
    private Log log;

    double timeMultiplier                  = 1;

    public static String CONFIG_SVCID     = "svcID";
    public static String CONFIG_TRANSFORM = "transforms";
    public static String CONFIG_DONTLOG   = "dontlog";
    
    public static String CONFIG_INTERVAL = "interval";
    public static String CONFIG_LOGDIR   = "logDir";
    public static String CONFIG_LOGMASK  = "logMask";

    public static String CONFIG_EUINTERVAL = "eu_interval";
    public static String CONFIG_EULOGDIR   = "eu_logDir";
    public static String CONFIG_EULOGMASK  = "eu_logMask";

    public static final String PARAM_LOG_DIR    = "responseTimeLogDir";
    public static final String DEFAULT_INTERVAL = "60";
    public static final String LOGFILE_SUFFIX   = "_HQResponseTime.log";

    /* A list of constants to define the tier which generated this measurement.
     * These must be in essentially descending order.  Since End-user will 
     * always take longer than web server, which will always take longer than 
     * app server, that is the order we will use.  As more RT types are added,
     * this may have to be revisited.
     *
     * @see getSvcType
     */
    public static final int UNKNOWN   = 0;
    public static final int ENDUSER   = 1;
    public static final int WEBSERVER = 2;
    public static final int APPSERVER = 3;
    public static final int STARTRTTYPE = 1;
    public static final int ENDRTTYPE = 4; //APPSERVER+1
    
    abstract public String convertFormat(String fmt);
    abstract public int getSvcType();

    public RtPlugin()
    {
        log = LogFactory.getLog(RtPlugin.class);
    }
    
    public abstract BaseLogParser getParser();
    
    public boolean supportsEndUser() {
        return false;
    }
    
    public String getLogFormat(ConfigResponse config) {
        return "";
    }
    
    public String getEULogFormat(ConfigResponse config) {
        return "";
    }
    
    public void dontLog(Long stat)
    {
        lp.DontLog(stat);
    }

    public void dontLog(String url)
    {
        lp.DontLog(url);
    }

    private void getTransforms(String transforms, ArrayList patterns,
                               ArrayList subs)
    {
        String[] regexs = (String[])StringUtil.explode(transforms, " ").
                                                       toArray(new String[0]);
        for (int i = 0; i < regexs.length; i++) {
            int ind = regexs[i].indexOf('|', 1);
            String pattern = regexs[i].substring(1, ind);
            String sub = regexs[i].substring(ind + 1, regexs[i].length() - 1);
            
            patterns.add(pattern);
            subs.add(sub);
        }
    }
    
    /* There is an obvious performance enhancement here.  We should be 
     * compiling the regex when we configure the RT collection.  That way,
     * we only need to use it here instead of recompiling it every time.
     */
    private void transformUrl(RtStat rs, String transforms)
    {
        if (transforms == null || transforms.equals("")) {
            return;
        }
    
        ArrayList patterns = new ArrayList();
        ArrayList subs = new ArrayList();
        getTransforms(transforms, patterns, subs);

        PatternMatcher matcher = new Perl5Matcher();
        Pattern pattern = null;
        PatternCompiler compiler = new Perl5Compiler();
        StringBuffer sb = new StringBuffer();

        for (Iterator p = patterns.iterator(), s = subs.iterator(); 
             (p.hasNext() && s.hasNext()); ) {
            String curpat = (String)p.next();
            String cursub = (String)s.next();
            int numsubs;
            
            try {
                // Make sure the StringBuffer is empty each time we try to 
                // substitute.
                sb.delete(0, sb.length());
            } catch (StringIndexOutOfBoundsException e) { 
            }

            Perl5Substitution sub = new Perl5Substitution(cursub);
            try {
                pattern = compiler.compile(curpat);
            } catch (MalformedPatternException e) {
                log.error("Poorly formed pattern " + curpat.toString());
            }
            
            numsubs = Util.substitute(sb, matcher, pattern, sub, rs.getUrl(),
                                      Util.SUBSTITUTE_ALL);

            if (numsubs != 0) {
                // We only let the first regex match.
                break;
            }
        }
        rs.setUrl(sb.toString());
    }

    public void setTimeMultiplier(double t)
    {
        timeMultiplier = t;
    }
    
    public double getTimeMultiplier()
    {
        return timeMultiplier;
    }

    // A small internal class to keep the filename and old length together.
    // This is used by generateFileList and getTimes to ensure that we don't
    // reparse old data.
    protected class ParsedFile {
        public String fname;
        public long oldLen;

        public ParsedFile(String f, long len) {
            fname = f;
            oldLen = len;
        }

        public ParsedFile() { }
    }

    /* This is a helper function.  It generates the list of files that
     * should be parsed and it trims the alreadParsedFiles properties
     * table so that it remains small.
     *
     * 1.  Add all files from the alreadyParsedArray to the toRemove list.
     * Files that weren't actually removed will be taken off the toRemove
     * list as we loop through all possible files.
     * 2.  Get a list of all files in a directory that match the mask.
     * 3.  Loop through the files in the directory:
     *    1.  Remove from the toRemove list
     *    2.  If old_len == 0 or old_len < cur_lem then add to toParse list
     * 4.  Loop through toRemove array, removing all entries from
     * alreadyParsedFiles properties table.
     *
     * This could be two functions, but we are looping through the list
     * of masked files, and I don't want to do that twice if I can help it.
     */
    protected ParsedFile[] generateFileList(Properties alreadyParsedFiles,
                                            String logdir, String logmask)
        throws IOException
    {
        FilenameFilter filter = new GlobFilenameFilter(logmask.trim());
        ArrayList removedFiles = new ArrayList();
        Enumeration en = alreadyParsedFiles.keys();

        while (en.hasMoreElements()) {
            String file = (String)en.nextElement();
            File temp = new File(file);

            if (filter.accept(temp.getParentFile(), temp.getName())) {
                removedFiles.add(file);
            }
        }

        File directory = new File(logdir);
        if (!directory.canRead()) {
            this.log.error("logDir ("+logdir+") is not readable by the agent!");
        }
        File[] flist = directory.listFiles(filter);
        ArrayList toParse = new ArrayList();

        if (flist == null || flist.length == 0) {
            this.log.warn("No valid response time log files found.  "
                          + "logDir='"+logdir+"', logMask='"+logmask+"'");
            return (ParsedFile[])toParse.toArray(new ParsedFile[0]);
        }

        for (int i = 0; i < flist.length; i++) {
            Long len = new Long(flist[i].length());
            String canonPath = flist[i].getCanonicalPath();
            String value = alreadyParsedFiles.getProperty(canonPath);
                                                 
            Long oldlen = (value == null) ? new Long(0) : Long.valueOf(value);

            // This file exists, remove it from the list of files
            // that have been removed.
            removedFiles.remove(canonPath);

            if (oldlen.compareTo(len) != 0) {
                this.log.debug("Adding " + canonPath + " to parse list " +
                               "(offset=" + oldlen + ")");
                toParse.add(new ParsedFile(canonPath, oldlen.longValue()));
            }
        }

        // Remove the files that were removed since the last time we parsed
        // the logs.  The way this 'removed files' thing is implemented is
        // soo lame.
        Iterator it = removedFiles.iterator();
        while (it.hasNext()) {
            String toRemove = (String) it.next();
            this.log.debug("Removing " + toRemove + " from parse list");
            this.log.debug(toRemove);
            alreadyParsedFiles.remove(toRemove);
        }

        return (ParsedFile[])toParse.toArray(new ParsedFile[0]);
    }

    // loop through the RtStat elements that we just received
    // and add them to the ones that we already know about.
    protected void combineUrls(Hashtable foundNew, Hashtable foundOld, 
                               String transforms)
    {
        Enumeration en = foundNew.elements();
        
        while (en.hasMoreElements()) {
            RtStat rs = (RtStat) en.nextElement();
            
            transformUrl(rs, transforms);
            RtStat saved = (RtStat) foundOld.get(rs.getIpUrlKey());
            
            rs.recompute(saved);
            foundOld.put(rs.getIpUrlKey(), rs);
        }
    }    
    
    public Collection getTimes(Integer svcID, Properties alreadyParsedFiles,
                               String logdir, String logmask, String logfmt,
                               int svcType, String transforms, ArrayList noLog,
                               boolean collectIPs)
        throws IOException
    {
        Hashtable urls = new Hashtable();
        lp = getParser();

        lp.setTimeMultiplier(this.getTimeMultiplier());
        lp.urlDontLog(noLog);
        ParsedFile[] flist = generateFileList(alreadyParsedFiles,
                                              logdir, logmask);
        for (int i = 0; i < flist.length; i++) {
            long flen[] = new long[1];
            ParsedFile f = flist[i];

            this.log.debug("Parsing log: " + f.fname);

            Hashtable rv =  lp.parseLog(f.fname, convertFormat(logfmt),
                                        f.oldLen, svcID, svcType, flen, 
                                        collectIPs);
            this.log.debug("Done parsing log, " + rv.keySet().size() +
                           " elements in table");
            alreadyParsedFiles.put(f.fname,
                                   Long.toString(flen[0]));
            combineUrls(rv, urls, transforms);
        }

        this.log.debug("Returning parsed data " + urls.values().size() + 
                       " entries");
        return urls.values();
    }

    /**
     * Get a properly initialized ConfigResponse for ResponseTime feature
     */
    public static ConfigResponse getConfig(String prefix, 
                                           String dir) 
    {
        ConfigResponse config = new ConfigResponse();
        String file = prefix + LOGFILE_SUFFIX;

        if (dir != null) {
            config.setValue(CONFIG_LOGMASK, file);
            config.setValue(CONFIG_LOGDIR, dir);
            config.setValue(CONFIG_INTERVAL,
                            DEFAULT_INTERVAL);
        }
    
        return config;
    }

    /**
     * Get the path to the response time log dir webapp filter param 
     * 
     * @param is - an input stream of a webapps web.xml where this
     * filter is defined
     * @return the path to the rtLogDir or null if it was not found
     */
    public static String getWebAppLogDir(InputStream is) {
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        List filters = doc.getRootElement().getChildren("filter");
        for (int i=0; i< filters.size(); i++) {
            Element filter = (Element)filters.get(i);
            String filterName = filter.getChildText("filter-name");

            if (!"JMXFilter".equals(filterName)) {
                continue;
            }
                
            List params = filter.getChildren("init-param");
            for (int j=0; j<params.size(); j++) {
                Element param = (Element)params.get(j);
                String name = param.getChildText("param-name");
                String value = param.getChildText("param-value");
                if ("responseTimeLogDir".equals(name)) {
                    return value;
                }
            }
        }

        return null;
    }
}
