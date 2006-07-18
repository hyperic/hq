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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Validator {
    /**
     * Traverse throught he list of GhettoValue objects, validating
     * each one.
     */
    private static void validateObjects(BatchImportData data)
        throws BatchImportException
    {
        HashSet h;
        List platforms = data.getPlatforms();
        List apps      = data.getApplications();
        List groups    = data.getGroups();

        h = new HashSet();
        for(Iterator i=platforms.iterator(); i.hasNext(); ){
            XmlPlatformValue plat = (XmlPlatformValue)i.next();

            try {
                plat.validate();
            } catch(XmlValidationException exc){
                throw new BatchImportException(exc.getMessage());
            }

            if(h.contains(plat.getName())){
                throw new BatchImportException("Platform '" + plat.getName() +
                                               "' defined > 1 times");
            }
            h.add(plat.getName());
        }

        h = new HashSet();
        for(Iterator i=apps.iterator(); i.hasNext(); ){
            XmlApplicationValue app = (XmlApplicationValue)i.next();

            try {
                app.validate();
            } catch(XmlValidationException exc){
                throw new BatchImportException(exc.getMessage());
            }

            if(h.contains(app.getName())){
                throw new BatchImportException("Application '" + app.getName()+
                                               "' defined > 1 times");
            }
            h.add(app.getName());
        }

        h = new HashSet();
        for(Iterator i=groups.iterator(); i.hasNext(); ){
            XmlGroupValue group = (XmlGroupValue)i.next();

            try {
                group.validate();
            } catch(XmlValidationException exc){
                throw new BatchImportException(exc.getMessage());
            } 
            if(h.contains(group.getName())){
                throw new BatchImportException("Group '" + group.getName()+
                                               "' defined > 1 times");
            }
            h.add(group.getName());
        }
    }

    public static void validate(BatchImportData data)
        throws BatchImportException
    {
        validateObjects(data);
    }
}
