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

package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class Application extends AppdefResource
{
    private String engContact;
    private String opsContact;
    private String busContact;
    private ApplicationType applicationType;
    private Collection appServices;

    /**
     * default constructor
     */
    public Application()
    {
        super();
    }

    public String getEngContact()
    {
        return this.engContact;
    }

    public void setEngContact(String engContact)
    {
        this.engContact = engContact;
    }

    public String getOpsContact()
    {
        return this.opsContact;
    }

    public void setOpsContact(String opsContact)
    {
        this.opsContact = opsContact;
    }

    public String getBusContact()
    {
        return this.busContact;
    }

    public void setBusContact(String busContact)
    {
        this.busContact = busContact;
    }

    public ApplicationType getApplicationType()
    {
        return this.applicationType;
    }

    public void setApplicationType(ApplicationType applicationType)
    {
        this.applicationType = applicationType;
    }

    public Collection getAppServices()
    {
        return this.appServices;
    }

    public void setAppServices(Collection appServices)
    {
        this.appServices = appServices;
    }
}
