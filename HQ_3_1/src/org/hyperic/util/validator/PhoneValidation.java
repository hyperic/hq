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

public class PhoneValidation {

    public static void validatePhone (String country, String phone) 
        throws InvalidPhoneException {

        // Are we in the US?  
        // NOTE: we should really check to see if we are in a NANP region
        boolean isUSA = GeoValidation.isUSA(country);

        // Really dumb but better than nothing
        phone = phone.trim();
        int digits = 0;
        boolean onDash = true;
        boolean onDot = true;
        boolean inParen = false;
        boolean onPlus = false;
        boolean sawPlus = false;
        for (int i=0; i<phone.length(); i++) {
            char c = phone.charAt(i);

            if (c == '-') {
                // no double dashes, no ending with dash
                if (onDash || i==phone.length()-1) throw bail(phone);
                onDash = true;
                continue;
            }
            onDash = false;

            if (c == '.') {
                // no double dots, no ending with dot
                if (onDot || i==phone.length()-1) throw bail(phone);
                onDot = true;
                continue;
            }
            onDot = false;

            if (Character.isLetter(c)) break;
            if (Character.isDigit(c)) {
                if (onPlus && isUSA && c != '1') throw bail(phone);
                onPlus = false;
                digits++;
                continue;
            }
            if (Character.isWhitespace(c)) continue;
            if (c == '+') {
                // plus must be first char
                if (i==0) {
                    onPlus = true;
                    sawPlus = true;
                    continue;
                }
                throw bail(phone);
            }
            if (c == '*' || c == '#' || c == '~') {
                // i guess it's OK as long as it's not the first or last char
                if (i == 0 || i==phone.length()-1) throw bail(phone);
                continue;
            }
            if (c == '(') {
                // no double-open parens, no ending with paren
                if (inParen || i==phone.length()-1) throw bail(phone);
                inParen = true;
                continue;
            }
            if (c == ')') {
                if (inParen) {
                    inParen = false;
                    continue;
                } else throw bail(phone);
            }

            // Unrecognized char, everything else should have been handled:
            // letters, numbers, whitespace, and these specials: * # ( ) . -
            break;
        }

        // For the US, we need at least 10 digits
        if (isUSA) {
            int compareDigits = sawPlus ? 11 : 10;
            if (digits < compareDigits) throw bailUS(phone);
            return;
        }
        
        // Other places, assume we need at least 6 digits
        if (digits < 6) throw bail(phone);
    }

    private static InvalidPhoneException bail (String p) {
        return new InvalidPhoneException(p);
    }

    private static InvalidPhoneException bailUS (String p) {
        return new InvalidUSPhoneException(p);
    }
}
