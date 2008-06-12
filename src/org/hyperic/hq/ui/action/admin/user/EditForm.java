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

import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * A subclass of <code>EditForm</code> representing the
 * <em>EditUserProperties</em> form.
 */
public class EditForm extends BaseValidatorForm  {

    //-------------------------------------instance variables
    private Integer id;
    
    /** Holds value of property lastName. */
    private String lastName;
    
    /** Holds value of property firstName. */
    private String firstName;
    
    /** Holds value of property department. */
    private String department;
    
    /** Holds value of property name. */
    private String name;
    
    /** Holds value of property emailAddress. */
    private String emailAddress;
    
    private boolean htmlEmail;
    
    private String smsAddress;
    
    /** Holds value of property phoneNumber. */
    private String phoneNumber;
    
    /** Holds value of property enableLogin. */
    private String enableLogin;
    
    //-------------------------------------constructors

    public EditForm() {
    }

    //-------------------------------------public methods

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /** Getter for property lastName.
     * @return Value of property lastName.
     *
     */
    public String getLastName() {
        return this.lastName;
    }
    
    /** Setter for property lastName.
     * @param lastName New value of property lastName.
     *
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    /** Getter for property firstName.
     * @return Value of property firstName.
     *
     */
    public String getFirstName() {
        return this.firstName;
    }
    
    /** Setter for property firstName.
     * @param firstName New value of property firstName.
     *
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    /** Getter for property department.
     * @return Value of property department.
     *
     */
    public String getDepartment() {
        return this.department;
    }
    
    /** Setter for property department.
     * @param department New value of property department.
     *
     */
    public void setDepartment(String department) {
        this.department = department;
    }
    
    /** Getter for property userName.
     * @return Value of property userName.
     *
     */
    public String getName() {
        return this.name;
    }
    
    /** Setter for property userName.
     * @param userName New value of property userName.
     *
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /** Getter for property email.
     * @return Value of property email.
     *
     */
    public String getEmailAddress() {
        return this.emailAddress;
    }
    
    /** Setter for property email.
     * @param email New value of property email.
     *
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress.trim();
    }
    
    public boolean isHtmlEmail() {
        return htmlEmail;
    }

    public void setHtmlEmail(boolean htmlEmail) {
        this.htmlEmail = htmlEmail;
    }

    /** Getter for property phoneNumber.
     * @return Value of property phoneNumber.
     *
     */
    public String getPhoneNumber() {
        return this.phoneNumber;
    }
    
    /** Setter for property phoneNumber.
     * @param phoneNumber New value of property phoneNumber.
     *
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

     /** Getter for property enableLogin.
     * @return Value of property enableLogin.
     *
     */
    public String getEnableLogin() {
        return this.enableLogin;
    }
    
    /** Setter for property enableLogin.
     * @param confirmPassword New value of property enableLogin.
     *
     */
    public void setEnableLogin(String enableLogin) {
        this.enableLogin = enableLogin;
    }
    
    public String getSmsAddress() {
        return this.smsAddress;
    }
    
    public void setSmsAddress(String add) {
        this.smsAddress = add;
    }
    
    //-------- form methods-------------------------

    // for validation, please see web/WEB-INF/validation/validation.xml

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("id=" + id + " ");
        s.append("name=" + name + " firstName=" + firstName+ " ");
        s.append("lastName=" + lastName + " emailAddress=" + emailAddress+ " ");        
        s.append("phoneNumber=" + phoneNumber + " ");
        s.append("department=" + department + " " );
        s.append("enableLogin=" + enableLogin + " ");
        s.append("smsAddress=" + smsAddress +  " ");
        return s.toString();
    }
    
}
