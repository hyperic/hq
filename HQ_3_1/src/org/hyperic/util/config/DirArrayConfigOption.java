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

package org.hyperic.util.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.util.StringUtil;

public class DirArrayConfigOption extends ArrayConfigOption
    implements Serializable {

    // for lack of a better delimiter, this works for now, 
    // only crazy people would name files with a pipe char in them.
    public static final char DELIM = '|';
    public static final String DELIM_STR = "|";

    public DirArrayConfigOption(String optName, String optDesc, 
                                String defValue) {
        super(optName, optDesc, defValue, DELIM);
    }

    public void checkOptionIsValid(String value) 
        throws InvalidOptionValueException {

    }

    public static List toList(String value) {
        if (value == null) {
            return new ArrayList();
        }
        return StringUtil.explode(value, DELIM_STR);
    }

    public static String[] toArray(String value) {
        return (String[])toList(value).toArray(new String[0]);
    }
}
