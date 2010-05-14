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

public class XmlGroupMemberValue
    extends XmlValue
{
    private static final String[] ATTRS_REQUIRED = {
        XmlResourceValue.ATTR_NAME,
    };

    private static final String[] ATTRS_OPTIONAL = {
        XmlResourceValue.ATTR_TYPE,
    };

    XmlGroupMemberValue(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
    }
    
    public static String[] getRequiredAttributes(){
        return ATTRS_REQUIRED;
    }

    public static String[] getOptionalAttributes(){
        return ATTRS_OPTIONAL;
    }

    public String getName(){
        return this.getValue(XmlResourceValue.ATTR_NAME);
    }

    public String getType(){
        String res;

        if((res = this.getValue(XmlResourceValue.ATTR_TYPE)) != null){
            return res;
        }

        return null;
    }

    void setValue(String key, String value)
        throws XmlInvalidAttrException
    {
        if(key.equalsIgnoreCase(XmlResourceValue.ATTR_TYPE)){
            if(!(value.equalsIgnoreCase("platform") ||
                 value.equalsIgnoreCase("server")   ||
                 value.equalsIgnoreCase("service")  ||
                 value.equalsIgnoreCase("group")    ||
                 value.equalsIgnoreCase("application")))
            {
                throw new XmlInvalidAttrException("Invalid value for <" +
                                                     GroupMemberTag.TAG_NAME +
                                                     "> attribute " + key +
                                                     ", '" + value +"'.  Must"+
                                                     " be platform, server,"+
                                                     " service, group, or " +
                                                     "application");
            }
        }

        super.setValue(key, value);
    }
}
