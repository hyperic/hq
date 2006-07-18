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

package org.hyperic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Utility code taken from other parts of our source tree
 */
public class HQInitUtil {

    /** from StringUtil */
    public static String replace ( String source, String find, String replace ) {

        // Handle edge cases
        if ( source  == null || 
             find    == null || 
             replace == null ) return source;

        String retVal = null;
        int sourceLen = source.length();
        int findLen = find.length();

        try {
            if (source != null && 
                sourceLen > 0 && 
                find != null && 
                findLen > 0 && 
                replace != null ) {
                
                int idx, fromIndex;
                
                for (retVal = "", fromIndex = 0;
                     (idx = source.indexOf(find, fromIndex)) != -1;
                     fromIndex = idx + findLen) {
                    retVal += source.substring(fromIndex, idx);
                    retVal += replace;
                }
                retVal += source.substring(fromIndex);
            }
        } catch (Exception e) {
            // XXX This should never happen... but we should do something
            // here just in case -- maybe log this?
            retVal = null;
        }
        return retVal;
    }

    public static void copyStream(InputStream is, OutputStream os, 
                                  Properties symbols) 
        throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        String nl = System.getProperty("line.separator");
        while (line != null) {
            line += nl;
            if (line.indexOf("@@@") != -1) {
                line = substitute(line, symbols);
            }
            os.write(line.getBytes());
            line = br.readLine();
        }
    }

    public static String substitute (String src, Properties map) {
        Enumeration pNames = map.propertyNames();
        String pName;
        String sym;
        String val;
        while (pNames.hasMoreElements()) {
            pName = (String) pNames.nextElement();
            sym = "@@@" + pName + "@@@";
            if (src.indexOf(sym) != -1) {
                val = map.getProperty(pName);
                src = replace(src, sym, val);
            }
        }
        return src;
    }
    
    /** Borrowed from FileUtil */
    public static void copyStream(InputStream is, OutputStream os) 
        throws IOException {

        byte[] buf = new byte[2048];
        int bytesRead = 0;
        while (true) {
            bytesRead = is.read(buf);
            if (bytesRead == -1) break;
            os.write(buf, 0, bytesRead);
        }
    }
}
