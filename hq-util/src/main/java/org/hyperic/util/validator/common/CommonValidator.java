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

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Iterator;
import java.text.MessageFormat;

import org.apache.commons.validator.Field;
import org.apache.commons.validator.Form;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.ValidatorResults;
import org.apache.commons.validator.ValidatorResources;
import org.apache.commons.validator.ValidatorResult;
import org.apache.commons.validator.ValidatorException;
import org.apache.commons.validator.ValidatorAction;
import org.xml.sax.SAXException;

/**                                                       
 * Encapsulates most of specific jakarta commons validator logic and
 * provides access to validation results.
 * <p>NOTE: Very heavily commented so as to persuade others to use and
 * extend. The commons validation logic is a little bulky and awkward
 * so consider this as a good place to put any further wrapper methods.</p>
 */
public class CommonValidator {

    private ResourceBundle _properties = null; // message strings
    private ValidatorResults _validatorResults = null; // results of validation
    private String _resourceName = null; // current xml resource.
    private ValidatorResources _validatorResources = null; // commons-v object

    /**
     * Simple constructor with property resourcebundle containing localized
     * messages
     **/
    public CommonValidator(ResourceBundle properties) {
        _properties = properties;
    }

    private void setResourceName (String rName) {
        _resourceName = rName;
    }

    private void setValidatorResults(ValidatorResults results) {
        _validatorResults = results;
    }

    public ValidatorResults getValidatorResults () {
        return _validatorResults;
    }

    public String getResourceName () {
        return _resourceName;
    }

    /**
     * Simple getter for current Validator Resources. null until validate
     * is first run
     **/
    public ValidatorResources getValidatorResources () {
        return _validatorResources;
    }

    /**
     * Gets and/or initializes validatorResources before returning it. Init-
     * ialization is expensive so only perform it if necessary.
     * Note: (1) CALLER's responsibility to close InputStream.
     * (2) XML Resources always looked for in same package as Bean.
     **/
    private void setValidatorResources (String resourceName, Object bean,
                                        InputStream in)
    throws IOException, SAXException
    {
		if (getResourceName()==null ||
            !getResourceName().equals(resourceName)) {
			// remember this resource
			setResourceName(resourceName);

			// Read in the XMLfile
			in = bean.getClass().getResourceAsStream(resourceName);

			// create a new "validator" resources object. This thing contains
			// FormSets stored against locale.
			_validatorResources = new ValidatorResources(in);
		}
    }

   /**
    * Perform all form-specific the validation. All form specific validation
    * is performed and then finally a CommonValidatorException object will be
    * thrown if (AND ONLY IF) validation errors were detected. The exception
    * object will contain a collection of error Strings see
    * {@ref CommonValidatorException}.
    *
    * @param validationMappingRes A String containing the name of the
    * mapping resource file.
    * @param formName A String containing the name of the form containing
    * the validation action references.
    * @param beanInstance An instance of the bean to apply validation on.
    * @throws CommonValidatorException - Exception containing collection of
    * messages.
    **/
    public void validate (String validationMappingRes, String formName,
                          Object beanInstance)
    throws CommonValidatorException, SAXException
    {
        InputStream xmlStream = null;
		CommonValidatorException cve = null;
        ValidatorResults results;

        try {
			// Start by setting the "ValidatorResources" object. Its only
            // created if necessary. Contains FormSets stored against locale.
			setValidatorResources(validationMappingRes, beanInstance, xmlStream);

			// Get the form for the current locale and Bean.
			Form form = _validatorResources.getForm(Locale.getDefault(), formName);

			// Instantiate the validator (coordinates the validation
			// while the ValidatorResources implements the validation)
			Validator validator = new Validator(_validatorResources, formName);

			// Tell the validator which bean to validate against.
			validator.setParameter(Validator.BEAN_PARAM, beanInstance);

            // Get the results
			results = validator.validate();

            // Localize a reference for future access.
            setValidatorResults(results);

			// Iterate over each of the properties of the Bean which had messages.
			Iterator propertyNames = results.getPropertyNames().iterator();
			
			while (propertyNames.hasNext()) {
                // There were errors. Instantiate CVE

	    		String propertyName = (String) propertyNames.next();

	    		// Get the Field associated with that property in the Form
	    		Field field = (Field) form.getField(propertyName);

	    		// Look up the formatted name of the field from the Field arg0
	    		String prettyFieldName = _properties.getString(field.getArg(0).getKey());

	    		// Get the result of validating the property.
	    		ValidatorResult result =
                    results.getValidatorResult(propertyName);

                // Get all the actions run against the property, and iterate
                // over their names. Check for invalid results.
				Map actionMap = result.getActionMap();
				Iterator keys = actionMap.keySet().iterator();
				while (keys.hasNext()) {
					String actName = (String) keys.next();
					// Get the Action for that name.
					ValidatorAction action =
                        _validatorResources.getValidatorAction(actName);
					if (!result.isValid(actName)) {
		    			String message = _properties.getString(action.getMsg());
		   				Object[] args = { prettyFieldName };
                		if (cve == null) {
                            cve = new CommonValidatorException();
                        }
                        cve.addMessage(MessageFormat.format(message, args));
					}
				}
            }
		} catch (IOException ex) {
            // Note: This exception shouldn't be reported to user since it
            // wasn't likely caused by user input.
            ex.printStackTrace(); //log this
		} catch (ValidatorException ex) {
            // Note: This exception shouldn't bubble up to user since it
            // wasn't likely caused by user input.
			ex.printStackTrace(); //log this
		} finally {
			// Make sure we close the input stream.
			if (xmlStream != null)
				try { xmlStream.close(); } catch (Exception e) {}
			// Lastly, if we had any invalid fields, throw CVE.
        	if (cve != null)
            	throw cve;
		}
    }
}
