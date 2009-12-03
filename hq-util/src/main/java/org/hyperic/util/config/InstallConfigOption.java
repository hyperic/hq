package org.hyperic.util.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InstallConfigOption extends ConfigOption implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List _values = new ArrayList(); // Values the enum holds
    
    /**
     * This constructor allows you to create an InstallConfigOption
     * and behaves pretty much like the EnumConfigOption class, however
     * it deals with ConfigOptionDisplay objects rather than raw Strings.
     */
    public InstallConfigOption(String optName, String optDesc, ConfigOptionDisplay defValue, ConfigOptionDisplay[] installOptionValues)
    {
        this(optName, optDesc, defValue, installOptionValues, null);
    }
    
    public InstallConfigOption(String optName, String optDesc, ConfigOptionDisplay defValue, ConfigOptionDisplay[] installOptionValues, String confirm) {
        super(optName, optDesc, defValue.getName());
        
        for (int i = 0; i < installOptionValues.length; i++) {
            if (installOptionValues[i] != null && installOptionValues[i].getName().length() > 0) {
                _values.add(installOptionValues[i]);
            }
        }
        
        if (confirm != null) setConfirm(confirm);
    }

    public void addValue(ConfigOptionDisplay option){
        _values.add(option);
    }

    public List getValues(){
        return _values;
    }

    public void checkOptionIsValid(String value) throws InvalidOptionValueException {
        boolean valid = false;
        
        for (int x = 0; x < _values.size(); x++) {
            if (((ConfigOptionDisplay) _values.get(x)).getName().equals(value)) {
                valid = true;
                    
                break;
            }
        }
        
        if (!valid) throw invalidOption("must be one of options presented below. value: " + value);
    }

    public String getDefault() {
        String defVal = super.getDefault();
        
        //if no default was specified, use the first in the list
        if ((defVal == null) && (_values.size() > 0)) {
                defVal = ((ConfigOptionDisplay) _values.get(0)).getName();
        }
        
        return defVal;
    }

    protected Object clone() throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        return super.clone();
    }
}
