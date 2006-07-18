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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * An extension of <code>BaseValidatorForm</code> representing the
 * <em>Add Others </em> form.
 */
public class AddOthersForm extends AddNotificationsForm  {

    private Log log = LogFactory.getLog( AddOthersForm.class.getName() );

    //-------------------------------------instance variables

    private String emailAddresses;
    private Integer ad;

    //-------------------------------------constructors

    public AddOthersForm() {
        super();
    }

    //-------------------------------------public methods


    public String getEmailAddresses() {
        return this.emailAddresses;
    }

    public void setEmailAddresses(String emailAddresses) {
        this.emailAddresses = emailAddresses;
    }


    public Integer getAd() {
        return this.ad;
    }

    public void setAd(Integer ad) {
        this.ad=ad;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("ad=" + ad + " ");
        s.append("emailAddresses={");
        s.append(emailAddresses);
        s.append("} ");

        return s.toString();
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.emailAddresses  =  "";
        super.reset(mapping, request);
    }

    public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        if (!shouldValidate(mapping, request))
            return null;
        
        log.debug("validating email addresses: " + emailAddresses);
        ActionErrors errs = super.validate(mapping, request);
        if (null == errs &&
            (emailAddresses == null || emailAddresses.length() == 0)) {
            // A special case, BaseValidatorForm will return null if Ok is
            // clicked and input is null, indicating a PrepareForm
            // action is occurring. This is tricky. However, it also
            // returns null if no validations failed. This is really
            // lame, in my opinion.
            return null;
        } else {
            errs = new ActionErrors();
        }

        try {
            InternetAddress[] addresses = InternetAddress.parse(emailAddresses);
        } catch (AddressException e) {
            if (e.getRef() == null) {
                ActionMessage err = new ActionMessage(
                        "alert.config.error.InvalidEmailAddresses",
                        e.getMessage());
                errs.add("emailAddresses", err);
            } else {
                ActionMessage err = new ActionMessage(
                        "alert.config.error.InvalidEmailAddress",
                        e.getRef(), e.getMessage());
                errs.add("emailAddresses", err);
            }
        }

        return errs;
    }
}

// EOF
