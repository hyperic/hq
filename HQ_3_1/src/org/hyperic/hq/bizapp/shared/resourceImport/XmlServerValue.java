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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class XmlServerValue
    extends XmlResourceValue
{
    private static final String ATTR_INSTALLPATH = "installpath";
    private static final String ATTR_AUTOINVENTORY_IDENTIFIER =
        "autoinventoryidentifier";

    private static final String[] ATTRS_REQUIRED = {
        ATTR_INSTALLPATH,
        XmlResourceValue.ATTR_NAME,
        XmlResourceValue.ATTR_TYPE,
    };

    private static final String[] ATTRS_OPTIONAL = {
        ATTR_AUTOINVENTORY_IDENTIFIER,
        XmlResourceValue.ATTR_DESCRIPTION,
        XmlResourceValue.ATTR_LOCATION,
    };

    private ArrayList services;

    XmlServerValue(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
        this.services = new ArrayList();
    }
    
    public static String[] getRequiredAttributes(){
        return ATTRS_REQUIRED;
    }

    public static String[] getOptionalAttributes(){
        return ATTRS_OPTIONAL;
    }

    void addService(XmlServiceValue val){
        this.services.add(val);
    }

    public String getInstallPath(){
        return this.getValue(ATTR_INSTALLPATH);
    }

    public String getAutoinventoryIdentifier() {
        return this.getValue(ATTR_AUTOINVENTORY_IDENTIFIER);
    }

    public List getServices(){
        return this.services;
    }

    void validate()
        throws XmlValidationException
    {
        HashSet h;

        super.validate();
        h = new HashSet();
        for(Iterator i=this.services.iterator(); i.hasNext(); ){
            XmlServiceValue sVal = (XmlServiceValue)i.next();
            String name = sVal.getName();

            h.add(name);
            sVal.validate();
        }
    }

    public String toString(){
        return super.toString() + " " + this.services;
    }
}
