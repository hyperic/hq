/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2008], Hyperic, Inc. 
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

package org.hyperic.hq.appdef.server.session;

import java.util.Collection;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class ApplicationType extends AppdefResourceType
{
    private Collection serviceTypes;
    private Collection applications;

    /**
     * default constructor
     */
    public ApplicationType()
    {
        super();
    }

    public ApplicationType(Integer id)
    {
        super(id);
    }

    public int getAuthzType() {
        return AuthzConstants.authzApplicationProto.intValue();
    }

    // Property accessors
    public Collection getServiceTypes()
    {
        return this.serviceTypes;
    }

    public void setServiceTypes(Collection serviceTypes)
    {
        this.serviceTypes = serviceTypes;
    }

    public Collection getApplications()
    {
        return this.applications;
    }

    public void setApplications(Collection applications)
    {
        this.applications = applications;
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof ApplicationType) && super.equals(obj);
    }

    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
    }

    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
        return new AppdefResourceTypeValue() {
            public int getAppdefType() {
                return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
            }

            public Long getCTime() {
                return new Long(ApplicationType.this.getCreationTime());
            }

            public String getDescription() {
                return ApplicationType.this.getDescription();
            }

            public Integer getId() {
                return ApplicationType.this.getId();
            }

            public Long getMTime() {
                return new Long(ApplicationType.this.getModifiedTime());
            }

            public String getName() {
                return ApplicationType.this.getName();
            }

            public void setDescription(String desc) {}

            public void setId(Integer id) {}

            public void setName(String name) {}
        };
    }
}
