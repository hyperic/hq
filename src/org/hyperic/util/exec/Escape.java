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

package org.hyperic.util.exec;

public class Escape {
    private static char[] enlargeArray(char[] in){
        char[] res;

        res = new char[in.length * 2];
        System.arraycopy(in, 0, res, 0, in.length);
        return res;
    }

    /**
     * Escape a string by quoting the magical elements 
     * (such as whitespace, quotes, slashes, etc.)
     */
    public static String escape(String in){
        char[] inChars, outChars, resChars;
        int numOut;

        inChars  = new char[in.length()];
        outChars = new char[inChars.length];
        in.getChars(0, inChars.length, inChars, 0);
        numOut = 0;

        for(int i=0; i<inChars.length; i++){
            if(outChars.length - numOut < 5){
                outChars = enlargeArray(outChars);
            }
            
            if(Character.isWhitespace(inChars[i]) ||
               inChars[i] == '\\' ||
               inChars[i] == '\'' ||
               inChars[i] == '\"' ||
               inChars[i] == '&'  ||
               inChars[i] == ';')
            {
                outChars[numOut++] = '\\';
                outChars[numOut++] = inChars[i];
            } else {
                outChars[numOut++] = inChars[i];
            }
        }

        return new String(outChars, 0, numOut);
    }

    public static void main(String[] args){
        System.out.println(Escape.escape("foo bar"));
        System.out.println(Escape.escape("\\\"foo' bar\""));
    }
}
