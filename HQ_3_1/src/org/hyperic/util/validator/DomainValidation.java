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

package org.hyperic.util.validator;

import org.hyperic.util.DNS_TLD;

public class DomainValidation {

    public static boolean isValidDomainName (String domain) {
        boolean onDot = true;
        for (int i=0; i<domain.length(); i++) {
            char c = domain.charAt(i);
            if (c == '.') {
                // no double dots or starting/ending with dot
                if (onDot
                    || i == 0
                    || i == domain.length() - 1) return false;
                onDot = true;
                continue;
            }
            onDot = false;
            if (Character.isLetterOrDigit(c)) continue;
            if (c == '-') {
                // no starting or ending with hyphen
                if (i == 0 || i == domain.length() - 1) return false;
                else continue;
            }
            return false;
        }
        
        int lastDotIndex = domain.lastIndexOf('.');

        if (lastDotIndex == -1) return false;

        // Get text after the last dot
        String suffix = domain.substring(lastDotIndex + 1);
        
        // Make sure suffix is one of the TLDs
        if (!DNS_TLD.isTLD(suffix)) return false;

        return true;
    }
}
