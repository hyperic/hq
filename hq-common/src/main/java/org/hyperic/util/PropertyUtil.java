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

package org.hyperic.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class PropertyUtil {

    /**
     * Expand variable references in property values.
     *
     * I.e. if you have a props file:
     * 
     * a=foo
     * b=bar
     * c=${a} ${b}
     *
     * The value for 'c' will be 'foo bar'
     *
     * @param props  Properties to replace
     *
     */
    public static void expandVariables(Map props) {
        ArrayList vars = new ArrayList();

        for(Iterator i=props.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();
            String value;
            int idx;

            value  = (String)ent.getValue();
            idx    = value.indexOf("${");

            if(idx == -1)
                continue;

            vars.clear();
            while(idx != -1){
                int endIdx = value.indexOf("}", idx);

                if(endIdx == -1)
                    break;

                endIdx++;
                vars.add(value.substring(idx, endIdx));
                idx = value.indexOf("${", endIdx);
            }
            
            for(Iterator j=vars.iterator(); j.hasNext(); ){
                String replace = (String)j.next();
                String replaceVar, lookupVal;

                replaceVar = replace.substring(2, replace.length() - 1);
                lookupVal  = (String)props.get(replaceVar);
                if(lookupVal == null)
                    continue;

                value = StringUtil.replace(value, replace, 
                                           lookupVal);
            }
            props.put(ent.getKey(), value);
        }
    }

    /**
     * Strip a prefix from the keys in a properties object.
     *
     * Mainly used for backwards compatibility of net.covalent
     * property keys.
     */
    public static void stripKeys(Properties props, String prefix)
    {
        Properties stripped = new Properties();

        for(Iterator i = props.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();
            String key = (String)ent.getKey();
            
            if (key.startsWith(prefix)) {
                stripped.setProperty(key.substring(prefix.length()),
                                     (String)ent.getValue());
                // Remove, will be re-merged later
                i.remove();
            }
        }
        
        props.putAll(stripped);
    }

    /**
     * Load properties from a file.
     */
    public static Properties loadProperties (String file) throws IOException {
        FileInputStream fi = null;
        Properties props = new Properties();
        try {
            fi = new FileInputStream(file);
            props.load(fi);
        } finally {
            if (fi != null) fi.close();
        }
        return props;
    }

    /**
     * Store properties to a file
     */
    public static void storeProperties(String file, Properties props, 
                                       String header)
        throws IOException
    {
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(file);
            props.store(os, header);
        } finally {
            if (os != null) os.close();
        }
    }
}
