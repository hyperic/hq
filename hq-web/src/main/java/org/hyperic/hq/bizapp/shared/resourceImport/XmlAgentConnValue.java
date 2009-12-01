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

package org.hyperic.hq.bizapp.shared.resourceImport;

public class XmlAgentConnValue
    extends XmlValue
{
    private static final String ATTR_ADDR = "address";
    private static final String ATTR_PORT = "port";

    private static final String[] ATTRS_REQUIRED = {
        ATTR_ADDR,
    };

    private static final String[] ATTRS_OPTIONAL = {
        ATTR_PORT,
    };

    XmlAgentConnValue(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
    }
    
    public static String[] getRequiredAttributes(){
        return ATTRS_REQUIRED;
    }

    public static String[] getOptionalAttributes(){
        return ATTRS_OPTIONAL;
    }

    public String getAddress(){
        return this.getValue(ATTR_ADDR);
    }

    public Integer getPort(){
        String val = this.getValue(ATTR_PORT);

        return val == null ? null : Integer.valueOf(val);
    }

    void setValue(String key, String value)
        throws XmlInvalidAttrException
    {
        if(key.equals(ATTR_PORT)){
            try {
                Integer.parseInt(value);
            } catch(NumberFormatException exc){
                throw new XmlInvalidAttrException("'" + key + "' must be " +
                                                     "an integer");
            }
        }
        super.setValue(key, value);
    }

    public String toString(){
        return super.toString();
    }
}
