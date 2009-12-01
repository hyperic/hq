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

import javax.servlet.http.HttpServletRequest;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.apache.struts.action.ActionMapping;

/**
 * An extension of <code>BaseValidatorForm</code> representing the
 * <em>Add Role Users</em> form.
 */
public class AddUsersForm extends AddNotificationsForm  {

    //-------------------------------------instance variables

    private Integer[] availableUsers;
    private Integer[] pendingUsers;
    private Integer psa;
    private Integer psp;
    private Integer ad;

    //-------------------------------------constructors

    public AddUsersForm() {
        super();
    }

    //-------------------------------------public methods


    public Integer[] getAvailableUser() {
        return this.availableUsers;
    }
    
    public Integer[] getAvailableUsers() {
        return getAvailableUser();
    }

    public void setAvailableUser(Integer[] availableUsers) {
        this.availableUsers = availableUsers;
    }

    public void setAvailableUsers(Integer[] availableUsers) {
        setAvailableUser(availableUsers);
    }

    public Integer[] getPendingUser() {
	return this.pendingUsers;
    }

    public Integer[] getPendingUsers() {
	return getPendingUser();
    }

    public void setPendingUser(Integer[] pendingUsers) {
        this.pendingUsers = pendingUsers;
    }

    public void setPendingUsers(Integer[] pendingUsers) {
        setPendingUser(pendingUsers);
    }

    public Integer getPsa() {
        return this.psa;
    }

    public void setPsa(Integer ps) {
        this.psa = ps;
    }

    public Integer getPsp() {
        return this.psp;
    }

    public void setPsp(Integer ps) {
        this.psp = ps;
    }

    public Integer getAd() {
	return this.ad;
    }

    public void setAd(Integer ad) {
	this.ad = ad;
    }

    public void reset(ActionMapping mapping,
                      HttpServletRequest request) {
        this.availableUsers = new Integer[0];
        this.pendingUsers = new Integer[0];
        this.psa = null;
        this.psp = null;
        this.ad = null;
	super.reset(mapping, request);
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("ad=" + ad + " ");
        s.append("psa=" + psa + " ");
        s.append("psp=" + psp + " ");

        s.append("availableUsers={");
        listToString(s, availableUsers);
        s.append("} ");

        s.append("pendingUsers={");
        listToString(s, pendingUsers);
        s.append("}");

        return s.toString();
    }

    private void listToString(StringBuffer s, Integer[] l) {
        if (l != null) {
            for (int i=0; i<l.length; i++) {
                s.append(l[i]);
                if (i<l.length-1) {
                    s.append(", ");
                }
            }
        }
    }
}
