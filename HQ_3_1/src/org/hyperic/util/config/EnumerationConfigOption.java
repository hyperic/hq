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

public class EnumerationConfigOption extends ConfigOption
    implements Serializable {
    private ArrayList _values = new ArrayList(); // Values the enum holds

    /**
     * This constructor allows you to create an EnumConfigOption
     * and supply the valid enum values at construction time.
     */
    public EnumerationConfigOption(String optName, String optDesc, 
                                   String defValue, String[] enumValues)
    {
        super(optName, optDesc, defValue);
        for (int i = 0; i < enumValues.length; i++) {
            if (enumValues[i] != null && enumValues[i].length() > 0) {
                _values.add(enumValues[i]);
            }
        }
    }
    
    public EnumerationConfigOption(String optName, String optDesc,
                                   String defValue, String[] enumValues,
                                   String confirm) {
        this(optName, optDesc, defValue, enumValues);
        setConfirm(confirm);
    }

    public void checkOptionIsValid(String value) 
        throws InvalidOptionValueException {

        if (!_values.contains(value)) {
            throw invalidOption("must be one of: " + _values);
        }
    } 

    public String getDefault() {
        String defVal = super.getDefault();
        //if no default was specified, use the first in the list
        if ((defVal == null) &&
            (_values.size() != 0))
        {
            defVal = (String)_values.get(0);
        }
        return defVal;
    }

    /**********************
     * Option properties
     **********************/

    public void addValue(String option){
        _values.add(option);
    }

    public List getValues(){
        return _values;
    }
}
