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

package org.hyperic.hq.ui.action.admin.user;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * A subclass of <code>EditForm</code> representing the <em>New User</em> form.
 * This has the additional properties for password.
 */
public class NewForm
    extends EditForm {

    // -------------------------------------instance variables

    /** Holds value of property newPassword. */
    private String newPassword;

    /** Holds value of property confirmPassword. */
    private String confirmPassword;

    // -------------------------------------constructors

    public NewForm() {
    }

    // -------------------------------------public methods

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.newPassword = null;
        this.confirmPassword = null;
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (errors == null) {
            errors = new ActionErrors();
        }

        if (errors.isEmpty()) {
            return null;
        }

        return errors;
    }

    /**
     * Getter for property newPassword.
     * @return Value of property newPassword.
     * 
     */
    public String getNewPassword() {
        return this.newPassword;
    }

    /**
     * Setter for property newPassword.
     * @param newPassword New value of property newPassword.
     * 
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    /**
     * Getter for property confirmPassword.
     * @return Value of property confirmPassword.
     * 
     */
    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    /**
     * Setter for property confirmPassword.
     * @param confirmPassword New value of property confirmPassword.
     * 
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

}
