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

package org.hyperic.util.paramParser;

import org.hyperic.util.TimeUtil;

/**
 * Parses an interval of the form "#[d|h|m|s]"
 *
 * getValue() returns the milliseconds of the duration
 */
public class IntervalParser
    implements FormatParser
{
    private long value;

    public void parseValue(String arg)
        throws ParseException
    {
        String subStr;
        int val;
        char c;

        if(arg.length() < 2)
            throw new ParseException("Malformatted interval, '" + arg + "'");
        
        subStr = arg.substring(0, arg.length() - 1);
        try {
            val = Integer.parseInt(subStr);
        } catch(NumberFormatException exc){
            throw new ParseException("Malformatted interval, '" + arg + "'");
        }

        c = Character.toLowerCase(arg.charAt(arg.length() - 1));
        switch(c){
        case 'd':
            val *= 24;
        case 'h':
            val *= 60;
        case 'm':
            val *= 60;
        case 's':
            val *= 1000;
            break;
        default:
            throw new ParseException("Unknown interval type '" + c + "'");
        }

        this.value = val;
    }

    public long getValue(){
        return this.value;
    }
}
