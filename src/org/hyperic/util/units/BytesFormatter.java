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

package org.hyperic.util.units;

import java.text.ParseException;

public class BytesFormatter 
    extends BinaryFormatter
{
    protected String getTagName(){
        return "B";
    }
    
    protected UnitNumber parseTag(double number, String tag, int tagIdx,
                                  ParseSpecifics specifics)
        throws ParseException
    {
        int scale;

        if(tag.equalsIgnoreCase("b") ||
           tag.equalsIgnoreCase("bytes"))
        {
            scale = UnitsConstants.SCALE_NONE;
        } else if(tag.equalsIgnoreCase("k") ||
                  tag.equalsIgnoreCase("kb"))
        {
            scale = UnitsConstants.SCALE_KILO;
        } else if(tag.equalsIgnoreCase("m") ||
                  tag.equalsIgnoreCase("mb"))
        {
            scale = UnitsConstants.SCALE_MEGA;
        } else if(tag.equalsIgnoreCase("g") ||
                  tag.equalsIgnoreCase("gb"))
        {
            scale = UnitsConstants.SCALE_GIGA;
        } else if(tag.equalsIgnoreCase("t") ||
                  tag.equalsIgnoreCase("tb"))
        {
            scale = UnitsConstants.SCALE_TERA;
        } else if(tag.equalsIgnoreCase("p") ||
                  tag.equalsIgnoreCase("pb"))
        {
            scale = UnitsConstants.SCALE_PETA;
        } else {
            throw new ParseException("Unknown byte type '" + tag + "'", 
                                     tagIdx);
        }

        return new UnitNumber(number, UnitsConstants.UNIT_BYTES, scale);
    }
}
