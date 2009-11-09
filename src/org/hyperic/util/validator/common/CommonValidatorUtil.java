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

package org.hyperic.util.validator.common;


import org.apache.commons.validator.Field;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.util.ValidatorUtils;
import org.apache.commons.validator.GenericValidator;
                                                         
/**                                                       
 * <p>Contains validation utility methods for different types of fields.
 * Methods "validateRequired" and "validateEmail" were "borrowed" from 
 * Jakarta's validation samples.</p>
 */                                                       
public class CommonValidatorUtil {

   /**
    * Checks if the field is required.
    *
    * @param 	value 		The value validation is being performed on.
    * @return	boolean		If the field isn't <code>null</code> and 
    *                           has a length greater than zero, 
    *                           <code>true</code> is returned.  
    *                           Otherwise <code>false</code>.
   */
   public static boolean validateRequired(Object bean, Field field) {
      String value = ValidatorUtils.getValueAsString(bean, field.getProperty());

      return !GenericValidator.isBlankOrNull(value);
   }

   /**
    * Checks if the field is an e-mail address.
    *
    * @param 	value 		The value validation is being performed on.
    * @return	boolean		If the field is an e-mail address
    *                           <code>true</code> is returned.  
    *                           Otherwise <code>false</code>.
   */
   public static boolean validateEmail(Object bean, Field field) {
      String value = ValidatorUtils.getValueAsString(bean, field.getProperty());

      return GenericValidator.isEmail(value);
   }

	/** Default minimum length for a username is 3 */
   public static final int 		USERNAME_MIN_LENGTH		= 3;

   /** Default maximum length for a username is 24 */
   public static final int 		USERNAME_MAX_LENGTH		= 24;
   /** Default character set for username is ^[A-Za-z0-9_-]$ */

   public static final String 	USERNAME_VALID_REGEXP	= "^[A-Za-z0-9-_]{"+
   														  USERNAME_MIN_LENGTH+","+
                                                          USERNAME_MAX_LENGTH+"}$";
   /**
    * Validates the principal (Username) field based on default regular
    * expression.
    *
    * @param 	bean 		The bean containing the field to validate.
    * @param    field       The Field property info from the mapping file.
    * @return	boolean		If the field contains valid data <code>true</code>
    *                       is returned, otherwise <code>false</code>.
   */
   // note to-do: accept non-default regexp (as property arg?)
   public static boolean validatePrincipal (Object bean, Field field) {
    boolean valid = false;
    // validate the length and characters.
    String userName =
        ValidatorUtils.getValueAsString(bean, field.getProperty());
    if (	(userName != null)											&&
            (GenericValidator.matchRegexp(userName,USERNAME_VALID_REGEXP)) )
        valid = true;
	return valid;
   }

   /** Default password minimum length is 3 */
    public static final int PASSWORD_MIN_LENGTH=3; // extern these

   /** Default password maximum length is 24 */
    public static final int PASSWORD_MAX_LENGTH=24;

   /**
    * Validates a password field which restricts the length
    * between PASSWORD_MIN_LENGTH and PASSWORD_MAX_LENGTH
    *
    * @param bean containing the fields to validate.
    * @param Field object containing the property resource info.
    * 
    */
   public static boolean validatePassword (Object bean, Field field) {
    boolean valid = false;
    // fetch the text to validate and enforce
    String pwd = ValidatorUtils.getValueAsString(bean, field.getProperty());
    if (	(pwd != null)											&&
        	(GenericValidator.minLength(pwd,PASSWORD_MIN_LENGTH))	&&
            (GenericValidator.maxLength(pwd,PASSWORD_MAX_LENGTH))   )
        valid = true;
	return valid;
   }

   /**
    * Validates a password verification field which requires that the
    * value exactly match the String value of the bean property
    * referenced by property argument 1 (probably "password").
    *
    * @param bean containing the fields to validate.
    * @param Field object containing the property resource info.
    * 
    */
   public static boolean validatePasswordVerification (Object bean, Field field){
    boolean valid = false;
    String pwdv = ValidatorUtils.getValueAsString(bean, field.getProperty());
    String pwd  = ValidatorUtils.getValueAsString(bean, field.getArg(1).getKey());
    // compare the pw verification field to the pw field.
    if (	(pwd != null && pwdv!=null)		&&
        	(pwdv.compareTo(pwd)==0)		)
        valid = true;
	return valid;
   }

  public final static String FIELD_TEST_NULL = "NULL";
  public final static String FIELD_TEST_NOTNULL = "NOTNULL";
  public final static String FIELD_TEST_EQUAL = "EQUAL";

  /**
   * Conditional validation method.
   * @param bean to be tested
   * @param bean's field to be tested.
   * @param current validator
   * @return true if condition satisfied, false otherwise.
   */
  public static boolean validateRequiredIf(Object bean,
      Field field,
      Validator validator) {
    Object form = validator.getParameterValue(Validator.BEAN_PARAM);
    String value = null;
    boolean required = false;
    if (isString(bean)) {
      value = (String) bean;
    } else {
      value = ValidatorUtils.getValueAsString(bean, field.getProperty());
    }
    int i = 0;
    String fieldJoin = "AND";
    if (!GenericValidator.isBlankOrNull(field.getVarValue("field-join"))) {
      fieldJoin = field.getVarValue("field-join");
    }
    if (fieldJoin.equalsIgnoreCase("AND")) {
      required = true;
    }
    while (!GenericValidator.isBlankOrNull(field.getVarValue("field[" + i + "]"))) {
      String dependProp = field.getVarValue("field[" + i + "]");
      String dependTest = field.getVarValue("field-test[" + i + "]");
      String dependTestValue = field.getVarValue("field-value[" + i + "]");
      String dependIndexed = field.getVarValue("field-indexed[" + i + "]");
      if (dependIndexed == null) dependIndexed="false";
      String dependVal = null;
      boolean this_required = false;
      if (field.isIndexed() && dependIndexed.equalsIgnoreCase("true")) {
        String key = field.getKey();
        if ((key.indexOf("[") > -1) &&
            (key.indexOf("]") > -1)) {
          String ind = key.substring(0, key.indexOf(".") + 1);
          dependProp = ind + dependProp;
        }
      }
      dependVal = ValidatorUtils.getValueAsString(form, dependProp);
      if (dependTest.equals(FIELD_TEST_NULL)) {
        if ((dependVal != null ) && (dependVal.length() > 0)) {
          this_required =  false;
        } else {
          this_required =  true;
        }
      }
      if (dependTest.equals(FIELD_TEST_NOTNULL)) {
        if ((dependVal != null ) && (dependVal.length() > 0)) {
          this_required =  true;
        } else {
          this_required =  false;
        }
      }
      if (dependTest.equals(FIELD_TEST_EQUAL)) {
        this_required =  dependTestValue.equalsIgnoreCase(dependVal);
      }
      if (fieldJoin.equalsIgnoreCase("AND")) {
        required = required && this_required;
      } else {
        required = required || this_required;
      }
      i++;
    }
    if (required) {
      if ((value != null) && (value.length() > 0)) {
        return true;
      } else {
        return false;
      }
    }
    return true;
  }
  private static Class stringClass = new String().getClass();

  private static boolean isString(Object o) {
    if (o == null) return true;
    return (stringClass.isInstance(o));
  }
      
}                                                         
