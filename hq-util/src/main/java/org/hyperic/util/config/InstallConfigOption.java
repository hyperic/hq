/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.util.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InstallConfigOption extends ConfigOption implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List _values = new ArrayList(); // Values the enum holds
    
    /**
     * This constructor allows you to create an InstallConfigOption
     * and behaves pretty much like the EnumConfigOption class, however
     * it deals with ConfigOptionDisplay objects rather than raw Strings.
     */
    public InstallConfigOption(String optName, String optDesc, ConfigOptionDisplay defValue, ConfigOptionDisplay[] installOptionValues)
    {
        this(optName, optDesc, defValue, installOptionValues, null);
    }
    
    public InstallConfigOption(String optName, String optDesc, ConfigOptionDisplay defValue, ConfigOptionDisplay[] installOptionValues, String confirm) {
        super(optName, optDesc, defValue.getName());
        
        for (int i = 0; i < installOptionValues.length; i++) {
            if (installOptionValues[i] != null && installOptionValues[i].getName().length() > 0) {
                _values.add(installOptionValues[i]);
            }
        }
        
        if (confirm != null) setConfirm(confirm);
    }

    public void addValue(ConfigOptionDisplay option){
        _values.add(option);
    }

    public List getValues(){
        return _values;
    }

    public void checkOptionIsValid(String value) throws InvalidOptionValueException {
        boolean valid = false;
        
        for (int x = 0; x < _values.size(); x++) {
            if (((ConfigOptionDisplay) _values.get(x)).getName().equals(value)) {
                valid = true;
                    
                break;
            }
        }
        
        if (!valid) throw invalidOption("must be one of options presented below. value: " + value);
    }

    public String getDefault() {
        String defVal = super.getDefault();
        
        //if no default was specified, use the first in the list
        if ((defVal == null) && (_values.size() > 0)) {
                defVal = ((ConfigOptionDisplay) _values.get(0)).getName();
        }
        
        return defVal;
    }

    protected Object clone() throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        return super.clone();
    }
}
