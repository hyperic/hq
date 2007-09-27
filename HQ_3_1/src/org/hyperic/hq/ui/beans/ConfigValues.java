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

package org.hyperic.hq.ui.beans;

import java.util.List;

import org.hyperic.util.config.ConfigOption;

/**
 * Bean to hold config option and its value
 * **/

public class ConfigValues extends ConfigOption    {

    private String prefix;
    private String option ;
    private String value;
    private boolean isSecret = false;
    private boolean isEnumeration = false;
    private boolean isArray = false;
    private boolean isDir = false;
    private boolean isBoolean = false;
    private List enumValues;
    private String description;

    public ConfigValues(String option, String value) {
        this.option = option;
        this.value = value;
        this.prefix = "";
    }

    public ConfigValues(String option, String value, boolean isSecret) {
        this.option = option;
        this.value = value;
        this.isSecret = isSecret;
        this.prefix = "";
    }
    
    public ConfigValues() {
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

    public List getEnumValues() {
        return enumValues;
    }

    public void setEnumValues (List enumValues) {
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
