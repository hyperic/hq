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

public class XmlApplicationValue 
    extends XmlResourceValue
{
    private static final String ATTR_BUSINESSCONTACT = "businesscontact";
    private static final String ATTR_ENGCONTACT      = "engcontact";
    private static final String ATTR_OPSCONTACT      = "opscontact";

    private static final String[] ATTRS_REQUIRED = {
        XmlResourceValue.ATTR_NAME,
    };

    private static final String[] ATTRS_OPTIONAL = {
        XmlResourceValue.ATTR_LOCATION,
        XmlResourceValue.ATTR_DESCRIPTION,
        ATTR_BUSINESSCONTACT,
        ATTR_ENGCONTACT,
        ATTR_OPSCONTACT,
    };

    private ArrayList services;

    XmlApplicationValue(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
        this.services = new ArrayList();
    }
    
    public static String[] getRequiredAttributes(){
        return ATTRS_REQUIRED;
    }

    public static String[] getOptionalAttributes(){
        return ATTRS_OPTIONAL;
    }

    void addService(XmlApplicationServiceValue service){
        this.services.add(service);
    }

    public String getBusinessContact(){
        return this.getValue(ATTR_BUSINESSCONTACT);
    }

    public String getEngContact(){
        return this.getValue(ATTR_ENGCONTACT);
    }

    public String getOpsContact(){
        return this.getValue(ATTR_OPSCONTACT);
    }

    public List getServices(){
        return this.services;
    }
    
    void validate()
        throws XmlValidationException
    {
        HashSet h;

        super.validate();

        if(this.services.size() == 0){
            throw new XmlValidationException(this.getValue("name") + 
                                                " requires at least one " +
                                                "service");
        }
        
        h = new HashSet();
        for(Iterator i=this.services.iterator(); i.hasNext(); ){
            XmlApplicationServiceValue appSvc;

            appSvc = (XmlApplicationServiceValue)i.next();
            if(h.contains(appSvc.getName())){
                throw new XmlValidationException(this.getValue("name") +
                                                    " has service '" + 
                                                    appSvc.getName() + 
                                                    "' defined > 1 times");
            }

            appSvc.validate(this);
            h.add(appSvc.getName());
        }
    }

    public String toString(){
        return super.toString() + " " + this.services;
    }
}
