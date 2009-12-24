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

import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * A form for editing passwords.
 */
public class EditPasswordForm
    extends BaseValidatorForm {

    // -------------------------------------instance variables

    /** Holds value of property newPassword. */
    private String newPassword;

    /** Holds value of property confirmPassword. */
    private String confirmPassword;

    /** Holds value of property currentPassword. */
    private String currentPassword;

    /** Holds value of property id. */
    private Integer id;

    // -------------------------------------constructors

    public EditPasswordForm() {
    }

    // -------------------------------------public methods

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.newPassword = null;
        this.confirmPassword = null;
        this.currentPassword = null;
        this.id = null;
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

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("id= " + id + " ");
        s.append("newPassword=" + newPassword + " ");
        s.append("confirmPassword=" + confirmPassword + " ");
        s.append("currentPassword=" + currentPassword + " ");

        return super.toString() + s.toString();
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

    /**
     * Getter for property currentPassword.
     * @return Value of property currentPassword.
     * 
     */
    public String getCurrentPassword() {
        return this.currentPassword;
    }

    /**
     * Setter for property currentPassword.
     * @param currentPassword New value of property currentPassword.
     * 
     */
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    /**
     * Getter for property id.
     * @return Value of property id.
     * 
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for property id.
     * @param id New value of property id.
     * 
     */
    public void setId(Integer id) {
        this.id = id;
    }

}
