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

/**
 * A class that provides multi-valued config options.
 */
public abstract class ArrayConfigOption extends ConfigOption
    implements Serializable {

    private char delim;

    /**
     * Create a new ArrayConfigOption
     * @param optName The name of the option.
     * @param optDesc The description of the option.
     * @param defValue The default value of the option.
     * @param delim The character to use to delimit each entry
     * in the array when rendering or interpreting it as a single String
     */
    public ArrayConfigOption(String optName, String optDesc, 
                             String defValue, char delim){
        super(optName, optDesc, defValue);
        this.delim = delim;
    }

    /**********************
     * Option properties
     **********************/
    public void setDelim(char delim){
        this.delim = delim;
    }

    public char getDelim(){
        return this.delim;
    }
}
