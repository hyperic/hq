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

import java.io.Reader;
import java.io.PushbackReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.types.FilterSet;

/**
 * Property replacer for Strings and Readers.
 *
 * The default begin and end tokens are ${ and }.  These are configurable.
 *
 * By default the available filters include all system properties.  
 */

public class TokenReplacerFilterReader extends FilterReader 
{
    private static final String DEFAULT_TOKEN_BEGIN = "${";
    private static final String DEFAULT_TOKEN_END   = "}";
    private static final int    DEFAULT_BUFSIZ      = 8192;

    private FilterSet        filter;
    private PushbackReader   reader;
    private String           buffer; // internal buffer
    private int              index;  // index into our buffer

    // Public constructors

    public TokenReplacerFilterReader(Reader r) {
        super(r);

        this.reader = new PushbackReader(r, DEFAULT_BUFSIZ);
        this.buffer = null;
        this.index  = 0;

        this.filter = new FilterSet();
        this.filter.setBeginToken(DEFAULT_TOKEN_BEGIN);
        this.filter.setEndToken(DEFAULT_TOKEN_END);
    }

    public TokenReplacerFilterReader(String s) {
        this(new StringReader(s));
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
        this.filter.addFilter(name, value);
    }

    public void addFilters(Map filters)
    {
        Set keys = filters.keySet();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            String key = (String)i.next();
            this.filter.addFilter(key, filters.get(key).toString());
        }
    }

    // Overridden FilterReader methods

    public int read () throws IOException {
        synchronized (in) {
            if (!refreshBuffer()) {
                return -1;
            }
            return this.buffer.charAt(this.index++);
        }
    }

    public int read (char[] buf, int offset, int len) throws IOException {
        
        if (len > buf.length) {
            throw new IllegalArgumentException("buffer not large enough");
        }

        int i;
        synchronized (in) {
            for (i = 0; i < len; i++) {
                if (!refreshBuffer()) {
                    return (i == 0) ? -1 : i;
                }
                buf[offset+i] = this.buffer.charAt(this.index++);
            }
        }
        return i;
    }

    // Private helper methods

    /**
     * The buffer refresh is called every time a character is read from our
     * internal buffer.  If our index has hit the end of the buffer, we 
     * refresh the data, making sure that we dont split the character buffer
     * between token boundries.
     */
    private boolean refreshBuffer() throws IOException {

        if (this.buffer == null || this.index >= this.buffer.length()) {
            char[] tmp = new char[DEFAULT_BUFSIZ];
            int size;

            if ((size = this.reader.read(tmp, 0, DEFAULT_BUFSIZ)) == -1) {
                return false;
            }

            String tmpStr = new String(tmp, 0, size);

            // check to ensure we dont have a buffer that spans tags
            int lastBeginToken =  
                tmpStr.lastIndexOf(this.filter.getBeginToken());
            if (lastBeginToken != -1) {
                // have at least one, see if we also have the end tag

                int lastEndToken =
                    tmpStr.indexOf(this.filter.getEndToken(), lastBeginToken);
                if (lastEndToken == -1) {
                    String toPush = tmpStr.substring(lastBeginToken);
                    tmpStr = tmpStr.substring(0, lastBeginToken);
                    this.reader.unread(toPush.toCharArray());
                }
                
                // else, the buffer also contained the end tag
            }

            // reset
            this.buffer = this.filter.replaceTokens(tmpStr);
            this.index = 0;
        }

        return true;
    }
}
