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

public class XmlApplicationServiceValue
    extends XmlValue
{
    private static final String[] ATTRS_REQUIRED = {
        XmlResourceValue.ATTR_NAME,
    };

    private static final String[] ATTRS_OPTIONAL = {
    };

    private List depServices;

    XmlApplicationServiceValue(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
        this.depServices = new ArrayList();
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

    public void addDependency(String depServiceName){
        this.depServices.add(depServiceName);
    }

    public List getDependencies(){
        return this.depServices;
    }

    void setValue(String key, String value)
        throws XmlInvalidAttrException
    {
        super.setValue(key, value);
    }

    void validate(XmlApplicationValue appValue)
        throws XmlValidationException
    {
        HashSet depNames;

        super.validate();

        depNames = new HashSet();
        for(Iterator i=this.depServices.iterator(); i.hasNext(); ){
            String svc = (String)i.next();
            boolean found;

            if(depNames.contains(svc)){
                throw new XmlValidationException("Service dependency '" +
                           svc + "' declared more than one time " +
                           "for Application Service '" + this.getName() + "'" +
                           " in Application '" + appValue.getName() + "'");
            }
            depNames.add(svc);

            // Make sure that the Application defines the service that we
            // are depending on
            found = false;
            for(Iterator j=appValue.getServices().iterator(); j.hasNext(); ){
                XmlApplicationServiceValue srchSvc;

                srchSvc = (XmlApplicationServiceValue)j.next();
                if(srchSvc == this)
                    continue;
                
                if(srchSvc.getName().equals(svc)){
                    found = true;
                    break;
                }
            }

            if(found == true)
                continue;

            throw new XmlValidationException("Service '" + this.getName() +
                          "' in Application '" + appValue.getName() +
                          "' depends on service '" + svc + "' which could " +
                          "not be found within the same Application");
        }
    }

    public String toString(){
        return super.toString() + " DEPSON=" + this.depServices;
    }
}
