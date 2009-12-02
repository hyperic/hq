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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//we bury the Pattern.matcher reference here
//because CharSequence is 1.4 only.  this allows
//us to maintain 1.3 compat.
public class StringMatcher {

    private Pattern includes;
    private Pattern excludes;
    private List matches = new ArrayList();

    private Pattern compile(String pattern) {
        if (pattern == null) {
            return null;
        }
        int flags = 
            Pattern.CASE_INSENSITIVE |
            Pattern.DOTALL; //allow '.' to match newline
        return Pattern.compile(pattern, flags);        
    }
    
    public StringMatcher() {
    }

    public void setIncludes(String includes) {
        this.includes = compile(includes);
    }

    public void setExcludes(String excludes) {
        this.excludes = compile(excludes);
    }

    private boolean matches(Pattern pattern, String s,
                            boolean defaultMatch) {
        if (pattern == null) {
            return defaultMatch;
        }
        return pattern.matcher(s).find();
    }

    public List getLastMatches() {
        return this.matches;
    }

    public boolean matches(String s) {
        boolean includesMatch;

        if (this.includes == null) {
            includesMatch = true;
        }
        else {
            this.matches.clear();
            includesMatch = false;
            Matcher matcher = this.includes.matcher(s);

            while (matcher.find()) {
                includesMatch = true;
                int count = matcher.groupCount();
                //skip group(0):
                //"Group zero denotes the entire pattern by convention"
                for (int i=1; i<=count; i++) {
                    this.matches.add(matcher.group(i));
                }
            }

            if (this.matches.size() == 0) {
                this.matches.add(this.includes.pattern());
            }
        }

        return
            includesMatch &&
            !matches(this.excludes, s, false);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (this.includes != null) {
            buffer.append(this.includes.pattern());
            if (this.excludes != null) {
                buffer.append(" && ");
            }
        }
        if (this.excludes != null) {
            buffer.append('!').append(this.excludes.pattern());
        }
        return buffer.toString();
    }
}
