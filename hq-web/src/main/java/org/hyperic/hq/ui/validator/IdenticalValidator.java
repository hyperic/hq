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

/*
 * PasswordValidtor.java
 */

package org.hyperic.hq.ui.validator;


import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.Field;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.util.ValidatorUtils;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.Resources;

/**
 * This class is for use in the commons validation package to
 * validate two identical fields.
 *
 */
public class IdenticalValidator extends BaseValidator {
    
    /**
     * Validates if two fields are equal in terms of String's equal() 
     * function.
     *
     * Example of use:
     *<code>
     * <field property="password" depends="identical">
     *  <arg0 key="password.displayName"/>
     *  <arg1 key="passwordConfirm.displayName"/>
     *  <var>
     *   <var-name>secondProperty</var-name>
     *   <var-value>password2</var-value>
     *  </var>
     * </field>
     *
     *</code>
     *
     * @return Returns true if the fields property and property2 are
     *  identical.
     */
    public boolean validate(Object bean, 
                            ValidatorAction va, 
                            Field field, 
                            ActionMessages msgs, 
                            HttpServletRequest request) {                                
        String value =
            ValidatorUtils.getValueAsString(bean, field.getProperty());
        String sProperty2 = field.getVarValue("secondProperty");
        String value2 = ValidatorUtils.getValueAsString(bean, sProperty2);
        
        if (GenericValidator.isBlankOrNull(value)) {
            if (GenericValidator.isBlankOrNull(value2)) {
                return true;
            }
            return false;
        }
        if (!value.equals(value2)) {
            ActionMessage msg = Resources.getActionMessage(request, va, field);
            msgs.add(field.getKey(), msg);
            return false;
        }
        return true;       
    }

}
    

