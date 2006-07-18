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

package org.hyperic.hq.plugin.iis;

import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.RtPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.util.StringUtil;

import org.hyperic.hq.product.logparse.BaseLogParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.novadeck.jxla.LogParse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

public class IisRtPlugin extends RtPlugin {

    private static final String FIELDS_FMT = "#Fields: ";

    // A cache of log formats to remove the need to scan the file
    // every time we parse the file for response time.
    private Hashtable logFormats = new Hashtable();

    // Static lookup table for specifiying the JXLA parser format
    private static Hashtable tokens;

    static {
        tokens = new Hashtable();
        
        tokens.put("date",            "y-m-d");
        tokens.put("time",            "h");
        tokens.put("c-ip",            "$remote_ip");
        tokens.put("cs-username",     "*");
        tokens.put("s-sitename",      "*");
        tokens.put("s-computername",  "*");
        tokens.put("s-ip",            "*");
        tokens.put("s-port",          "*");
        tokens.put("cs-method",       "*");
        tokens.put("cs-uri-stem",     "$uri");
        tokens.put("cs-uri-query",    "$query");
        tokens.put("sc-status",       "$status");
        tokens.put("sc-substatus",    "*");
        tokens.put("sc-win32-status", "*");
        tokens.put("sc-bytes",        "$size");
        tokens.put("cs-bytes",        "*");
        tokens.put("time-taken",      "$time_taken");
        tokens.put("cs-version",      "*");
        tokens.put("cs-host",         "$remote_host");
        tokens.put("cs(User-Agent)",  "*");
        tokens.put("cs(Cookie)",      "*");
        tokens.put("cs(Referer)",     "*");
    }

    private static final Log log = LogFactory.getLog("IisRtPlugin");

    public BaseLogParser getParser() {
        return new LogParse();
    }

    public String getLogFormat(ConfigResponse config) {
        return "";
    }

    /**
     * Generate the JXLA format from the IIS log format.
     */ 
    public String convertFormat(String fmt) {
        String jxlaFmt = new String();
        
        if (fmt == null) return "";
        
        StringTokenizer st = new StringTokenizer(fmt);
        
        while (st.hasMoreTokens()) {
            String temp = (String)tokens.get(st.nextToken());
            if (temp == null) {
                jxlaFmt += "*";
            } else {
                jxlaFmt += temp;
            }
            if (st.hasMoreTokens()) {
                jxlaFmt += " ";
            }
        }
        
        return jxlaFmt;
    }

    /**
     * Scan the IIS logs looking for the #Fields: component
     *
     * The response time logging requires that the 'Time Taken'
     * field is being logged.  If we do not find this, log an
     * error and skip the log.
     *
     * Getting the log format is a one time operation.  If we
     * cannot get it the first time from the file, we will not
     * try again.  (Only again on agent restart when the cache
     * is cleared).  This will reduce thrashing induced by 
     * re-reading log files over and over, only to fail each time.
     *
     * @return The JXLA format for parsing this IIS log.
     *
     **/
    private String getLogFormat(String fname)
    {
        BufferedReader logf = null;
        String format;

        // First look for the format in the cache
        format = (String)this.logFormats.get(fname);
        if (format != null)
            return format;

        // No match on the cache.. Read the file for the format.
        try {
            logf = new BufferedReader(new FileReader(fname));
            String line;

            while ((line = logf.readLine()) != null) {
                if (line.startsWith(FIELDS_FMT)) {
                    // A match.  See if the time-taken field is being logged.
                    if (line.indexOf("time-taken") != -1) {
                        format = line.substring(FIELDS_FMT.length()).trim();
                        String jxlaFmt = convertFormat(format);

                        this.logFormats.put(fname, jxlaFmt);
                        return jxlaFmt;
                    }
                }
            }

            // Finished the loop, but didn't find anything.
            log.error("Unable to determine log file format for file: " +
                      fname + ".  No valid " + FIELDS_FMT + " token " +
                      "found");

        } catch (IOException e) {
            // We're unable to scan the file looking for the format.
            // Return an empty format, the parser will do the error
            // handling for not being able to open the file.
            log.error("Unable to determine log format for file: " +
                      fname + ": " + e.getMessage());
        } finally {
            if (logf != null) {
                try {
                    logf.close();
                } catch (IOException ingore) {}
            }
        }

        format = "";
        this.logFormats.put(fname, format);
        return format;
    }

    /**
     * End user response time not yet supported for IIS.
     */
    public String getEULogFormat(ConfigResponse config) {
        return "";
    }

    /**
     * End user response time not yet supported for IIS.
     */
    public boolean supportsEndUser() {
        return false;
    }

    /**
     * Generate a default config schema for IIS RT
     */
    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse config)
    {
        int type = info.getType();

        // Only services support response time
        if (type != TypeInfo.TYPE_SERVICE) {
            return new ConfigSchema();
        }

        SchemaBuilder schema = new SchemaBuilder(config);

        String serverRoot = config.getValue(ProductPlugin.PROP_INSTALLPATH);

        // Add required fields
        schema.add(CONFIG_LOGDIR, "Full path to log directory",
                   serverRoot + "\\LogFiles");
        schema.add(CONFIG_LOGMASK, 
                   "The filenames of your log files with wildcards",
                   "*.log");
        schema.add(CONFIG_INTERVAL,
                   "Interval between parsing log files (seconds)",
                   60);

        // Add optional fields for URL transforms and URL's to not log
        schema.addRegex(CONFIG_TRANSFORM,
                        "Regular expressions to apply to all URLS, " +
                        "space separated", null).setOptional(true);

        schema.addStringArray(CONFIG_DONTLOG,
                              "Regular expressions specifying which " +
                              "URLs not to log, space separated",
                              null).setOptional(true);

        return schema.getSchema();
    }

    public int getSvcType() {
        return WEBSERVER;
    }

    /**
     * Main method for parsing the log
     *
     * Much of this is duplicated from the BaseRTPlugin, mainly due
     * to the file format being specified in the log file itself.  This
     * needs to be abstracted.
     *
     */
    public Collection getTimes(Integer svcID, Properties alreadyParsedFiles,
                               String logdir, String logmask, String logfmt,
                               int svcType, String transforms, ArrayList noLog,
                               boolean collectIPs)
        throws IOException
    {
        Hashtable urls = new Hashtable();

        // Setup the parser
        lp = getParser();
        lp.setTimeMultiplier(this.getTimeMultiplier());
        lp.urlDontLog(noLog);

        // Get the list of logs to parse
        ParsedFile[] flist = generateFileList(alreadyParsedFiles,
                                              logdir, logmask);

        // For each log, parse out the response time info
        for (int i = 0; i < flist.length; i++) {

            long flen[] = new long[1];

            ParsedFile f = flist[i];

            logfmt = getLogFormat(f.fname);         
            if (logfmt == "") {
                // If we cannot determine the log format, don't bother
                // passing the file through the parser.
                log.debug("Not parsing " + f.fname + ": No log format");
                continue;
            }
   
            long start = System.currentTimeMillis();
            log.debug("Parsing log: " + f.fname);

            Hashtable rv = lp.parseLog(f.fname, logfmt,
                                       f.oldLen, svcID, svcType, flen, 
                                       collectIPs);
            
            if (log.isDebugEnabled()) {
                long elapsed = System.currentTimeMillis() - start;
                log.debug("Done parsing log, " + rv.keySet().size() +
                          " elements (" + 
                          StringUtil.formatDuration(elapsed, 0, true) +
                          ")");
            }

            alreadyParsedFiles.put(f.fname, Long.toString(flen[0]));
            combineUrls(rv, urls, transforms);
        }
        
        log.debug("Returning parsed data " + urls.values().size() + " entries");

        return urls.values();
    }
}
