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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import org.hyperic.hq.ui.action.resource.ResourceForm;

/**
 * Form for removing notifications for alerts
 * 
 */
public class RemoveNotificationsForm
    extends ResourceForm {
    public static final String ROLES = "Roles";
    public static final String USERS = "Users";
    public static final String OTHERS = "Others";

    private Integer ad;
    private String aetid;
    private String notificationType;
    private Integer[] users;
    private Integer[] roles;
    private String[] emails;

    public RemoveNotificationsForm() {
    }

    public Integer getAd() {
        return this.ad;
    }

    public void setAd(Integer ad) {
        this.ad = ad;
    }

    public Integer[] getUsers() {
        return this.users;
    }

    public void setUsers(Integer[] users) {
        this.users = users;
    }

    public Integer[] getRoles() {
        return this.roles;
    }

    public void setRoles(Integer[] roles) {
        this.roles = roles;
    }

    public String[] getEmails() {
        return this.emails;
    }

    public void setEmails(String[] emails) {
        this.emails = emails;
    }

    public String getAetid() {
        return aetid;
    }

    public void setAetid(String aetid) {
        this.aetid = aetid;
    }
}

// EOF
