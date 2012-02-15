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

package org.hyperic.hq.product.pluginxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;

import org.hyperic.util.filter.TokenReplacer;
import org.hyperic.util.xmlparser.XmlParser;
import org.hyperic.util.xmlparser.XmlParseException;

public class PluginParser {
    private static final Log log = LogFactory.getLog("PluginParser");
    
    private TokenReplacer replacer;
    
    private boolean collectMetrics=true;
    private boolean collectHelp=true;
    
    public void collectMetrics(boolean collect) {
        this.collectMetrics = collect;
    }
    
    public void collectHelp(boolean collect) {
        this.collectHelp = collect;
    }

    public void parse(InputStream in, PluginData data) 
        throws PluginException {

        parse(in, data, new PluginData.PluginResolver(data));
    }

    public void parse(InputStream in, PluginData data,
                      EntityResolver resolver) 
        throws PluginException
    {
        this.replacer = new TokenReplacer();
        this.replacer.addFilters(PluginData.getGlobalProperties());
        this.replacer.addFilters(System.getProperties());

        data.parser = this;
        data.scratch = new HashMap();
        ProductTag tag = new ProductTag(data);
        tag.collectMetrics = this.collectMetrics;
        tag.collectHelp = this.collectHelp;
        
        try {
            XmlParser.parse(in, tag, resolver);
        } catch(XmlParseException e) {
            throw new PluginException(e);
        }
        
        //remove help text w/ lowercase keys
        //which should only be used for piecing together help,
        //which we are done with after parsing.
        String[] keys = new String[data.help.size()];
        data.help.keySet().toArray(keys);
        for (int i=0; i<keys.length; i++) {
            String key = keys[i];
            //dont want to remove "iPlanet ..."
            if (Character.isLowerCase(key.charAt(0)) &&
                Character.isLowerCase(key.charAt(1)))
            {
                data.help.remove(key);
            }
        }
        
        this.replacer = null;
        data.parser = null; //allow gc of this
        data.scratch.clear();
        data.scratch = null;
    }

    String applyFilters(String s) {
        String orig;
        //support nested tokens.
        do {
            orig = s;
            s = this.replacer.replaceTokens(s);
        } while (!s.equals(orig));
        return s;
    }

    void addFilter(String key, String value) {
        this.replacer.addFilter(key, value);
    }

    String getFilter(String key) {
        return this.replacer.getFilter(key);
    }

    void addFilters(Map props) {
        for (Iterator it = props.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();
            addFilter(key, value);
        }
    }
    
    public static void dumpFormat(PrintStream out) {
        XmlParser.dump(new ProductTag(null), out);
    }

    public static void dumpFormatWiki(PrintStream out) {
        XmlParser.dumpWiki(new ProductTag(null), out);
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            dumpFormat(System.out);
            return;
        }
        
        //register shared ConfigSchemas
        new ProductPluginManager().init();

        int i;
        List files = new ArrayList();
        PluginParser parser = new PluginParser();
        PluginData data = new PluginData();
        
        for (i=0; i<args.length; i++) {
            if (args[i].charAt(0) != '-') {
                files.add(args[i]);
                continue;
            }
            if (args[i].equals("-nometrics")) {
                parser.collectMetrics(false);
            }
            else if (args[i].equals("-nohelp")) {
                parser.collectHelp(false);
            }
            else if (args[i].equals("-wiki")) {
                dumpFormatWiki(System.out);
                return;
            }
        }
        
        for (i=0; i<files.size(); i++) {
            String name = (String)files.get(i);
            FileInputStream is = null;
            
            try {
                System.out.println(name);
                is = new FileInputStream(new File(name));
                parser.parse(is, data, null);
            } finally {
                is.close();
            }
        }

        data.dumpXML();
    }
}
