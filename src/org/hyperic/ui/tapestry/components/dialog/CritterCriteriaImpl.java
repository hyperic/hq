package org.hyperic.ui.tapestry.components.dialog;

import java.io.Serializable;

public class CritterCriteriaImpl implements CritterCriteria, Serializable {

    private static final long serialVersionUID = 184098239473L;
    
    private Object _selectedType;
    private String _stringValue;

    public CritterCriteriaImpl() {
    }

    public Object getSelectedType() {
        return _selectedType;
    }

    public void setSelectedType(Object selectedType) {
        _selectedType = selectedType;
    }

    public String getStringValue() {
        return _stringValue;
    }

    public void setStringValue(String stringValue) {
        _stringValue = stringValue;
    }

}
