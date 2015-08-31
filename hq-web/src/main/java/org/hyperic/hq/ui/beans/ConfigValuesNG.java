package org.hyperic.hq.ui.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.InvalidOptionValueException;

public class ConfigValuesNG extends ConfigOption {

    private String prefix;
    private String option ;
    private String value;
    private boolean isSecret = false;
    private boolean isEnumeration = false;
    private boolean isArray = false;
    private boolean isDir = false;
    private boolean isBoolean = false;
    private Map<String,String> enumValues;
    private String description;

    public ConfigValuesNG(String option, String value) {
        this.option = option;
        this.value = value;
        this.prefix = "";
    }

    public ConfigValuesNG(String option, String value, boolean isSecret) {
        this.option = option;
        this.value = value;
        this.isSecret = isSecret;
        this.prefix = "";
    }
    
    public ConfigValuesNG() {
    //empty constructor
    }

    public String getValue() {
        return value ;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public void setPrefix (String prefix) {
        this.prefix = prefix;
    }
    public String getShortOption() {
        return option.substring(prefix.length());
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public String getDescription() {
        return description ;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getIsSecret() {
        return isSecret;
    }

    public void setIsSecret(boolean isSecret) {
        this.isSecret = isSecret;
    }

    public boolean getIsBoolean() {
        return isBoolean;
    }

    public void setIsBoolean(boolean boolVal) {
        this.isBoolean = boolVal;
    }

    public boolean getIsEnumeration() {
        return isEnumeration;
    }

    public void setIsEnumeration(boolean isEnumeration) {
        this.isEnumeration = isEnumeration;
    }

    public boolean getIsDir() {
        return isDir;
    }
    
    public void setIsDir(boolean dir) {
        isDir = dir;
    }
    
    public boolean getIsArray() {
        return isArray;
    }

    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }

    public Map<String,String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues (Map<String,String> enumValues) {
        this.enumValues = enumValues;
    }

   public boolean getOptional() {
       return super.isOptional();
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

          str.append(" option  = " + getOption() + " " + "value = " + 
                    getValue());

	  str.append('}');

	  return(str.toString());
   }

   public void checkOptionIsValid(String ignore) {}

}
