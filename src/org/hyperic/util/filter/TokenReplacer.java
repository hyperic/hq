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

package org.hyperic.util.filter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.types.FilterSet;

/**
 * Property replacer for Strings.
 *
 * The default begin and end tokens are ${ and }.  These are configurable.
 *
 * By default the available filters include all system properties.  
 */

public class TokenReplacer
{
    private static final String DEFAULT_TOKEN_BEGIN = "${";
    private static final String DEFAULT_TOKEN_END   = "}";

    private FilterSet        filter;
    private Map filters = new HashMap();

    // Public constructors
    public TokenReplacer() {
        clear();
    }

    public TokenReplacer(Map filters) {
        this();
        addFilters(filters);
    }

    public void clear() {
        this.filters.clear();
        this.filter = new FilterSet();
        this.filter.setBeginToken(DEFAULT_TOKEN_BEGIN);
        this.filter.setEndToken(DEFAULT_TOKEN_END);
    }

    // Configuration

    public void setBeginToken(String token)
    {
        this.filter.setBeginToken(token);
    }

    public void setEndToken(String token)
    {
        this.filter.setEndToken(token);
    }

    public void addFilter(String name, String value)
    {
        this.filters.put(name, value);
        this.filter.addFilter(name, value);
    }

    public void addFilters(Map filters)
    {
        this.filters.putAll(filters);
        Set keys = filters.keySet();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            String key = (String)i.next();
            this.filter.addFilter(key, filters.get(key).toString());
        }
    }

    public Map getFilters()
    {
        return this.filters;
    }

    public String getFilter(String name)
    {
        return (String)this.filters.get(name);
    }

    public String replaceTokens(String input)
    {
        return this.filter.replaceTokens(input);
    }

    public Properties replaceProperties(Properties source) {
        return replaceProperties(source, source);
    }

    public Properties replaceProperties(Properties source, Map filters) {
        addFilters(filters);

        Properties props = new Properties();

        Enumeration en = source.propertyNames();

        while (en.hasMoreElements()) {
            String name = (String)en.nextElement();
            String value = source.getProperty(name);

            props.setProperty(replaceTokens(name),
                              replaceTokens(value));
        }

        return props;
    }

    public static String replace(String input, Map filters) {
        TokenReplacer replacer = new TokenReplacer(filters);
        return replacer.replaceTokens(input);
    }
}
 
