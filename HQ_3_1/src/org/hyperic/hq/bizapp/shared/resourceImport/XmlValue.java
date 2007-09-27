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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

abstract class XmlValue 
    implements java.io.Serializable
{
    private String[]  requiredAttrs;
    private HashSet   validAttrs;
    private HashMap   attrs;

    XmlValue(String[] requiredAttrs, String[] optionalAttrs){
        this.requiredAttrs = requiredAttrs;
        this.validAttrs    = new HashSet();
        this.attrs         = new HashMap();

        this.validAttrs.addAll(Arrays.asList(requiredAttrs));
        this.validAttrs.addAll(Arrays.asList(optionalAttrs));
    }

    String getValue(String key){
        return (String)this.attrs.get(key);
    }

    void setValue(String key, String value)
        throws XmlInvalidAttrException
    {
        if(!this.validAttrs.contains(key))
            throw new XmlInvalidAttrException(key);
        
        this.attrs.put(key, value);
    }

    void validate()
        throws XmlValidationException
    {
        for(int i=0; i<this.requiredAttrs.length; i++){
            if(this.attrs.get(this.requiredAttrs[i]) == null)
                throw new XmlValidationException("Required attribute, '" +
                                                    this.requiredAttrs[i] + 
                                                    "' missing");
        }
    }

    public String toString(){
        return this.attrs.toString();
    }
}
