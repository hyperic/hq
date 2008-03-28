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

package org.hyperic.hq.authz.server.session;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

public class AuthzSubjectField extends HypericEnum {
    private static final ResourceBundle BUNDLE = 
        ResourceBundle.getBundle("org.hyperic.hq.authz.Resources");
    
    public static final AuthzSubjectField FIRSTNAME = 
        new AuthzSubjectField(1, "firstName", "subject.field.firstName");
    public static final AuthzSubjectField LASTNAME  = 
        new AuthzSubjectField(2, "lastName", "subject.field.lastName");
    public static final AuthzSubjectField EMAIL     = 
        new AuthzSubjectField(3, "email", "subject.field.email");
    public static final AuthzSubjectField SMS       = 
        new AuthzSubjectField(4, "sms", "subject.field.sms");
    public static final AuthzSubjectField PHONE     = 
        new AuthzSubjectField(5, "phone", "subject.field.phone");
    public static final AuthzSubjectField DEPT      = 
        new AuthzSubjectField(6, "dept", "subject.field.dept");
    public static final AuthzSubjectField ACTIVE    = 
        new AuthzSubjectField(7, "active", "subject.field.active");
    public static final AuthzSubjectField HTML      = 
        new AuthzSubjectField(8, "html", "subject.field.html");

    private AuthzSubjectField(int code, String desc, String localeProp) {
        super(code, desc, localeProp, BUNDLE);
    }
}
