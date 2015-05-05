package org.hyperic.hq.ui.validator;

import org.apache.commons.validator.GenericValidator;

import com.opensymphony.xwork2.validator.ValidationException;
import com.opensymphony.xwork2.validator.validators.FieldValidatorSupport;

public class IdenticalFieldsValidator extends FieldValidatorSupport {

	private String compareTo;

	public String getCompareTo() {
		return compareTo;
	}

	public void setCompareTo(String compareTo) {
		this.compareTo = compareTo;
	}

	public void validate(Object object) throws ValidationException {

		String firstValue = null;
		String secondValue = null;
		String fieldName = getFieldName();
		Object value = this.getFieldValue(fieldName, object);

		Object compareToValue = this.getFieldValue(compareTo, object);

		if ((value instanceof String)) {
			firstValue = (String) value;
		}

		if ((compareToValue instanceof String)) {
			secondValue = (String) compareToValue;
		}

		if (GenericValidator.isBlankOrNull(firstValue)) {
			if (GenericValidator.isBlankOrNull(secondValue)) {
				return;
			}
			addFieldError(fieldName, object);
			return;
		}
		if (!firstValue.equals(secondValue)) {
			addFieldError(fieldName, object);
		}

	}

}
