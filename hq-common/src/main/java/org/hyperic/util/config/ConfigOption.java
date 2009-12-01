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

package org.hyperic.util.config;

import java.io.Serializable;

public abstract class ConfigOption 
    implements Serializable, Cloneable {

    private String     optName;           // Option name
    private String     optDesc;           // Option description
    private String     optCat;            // Option category     
    private String     defValue;          // Default value 
    private String     confirm = null;    // The value to double check on
    private Boolean    optional;          // Is the option, optional?

    public ConfigOption() {
    }

    public ConfigOption(String optName, String optDesc, String defValue){
        if(optName == null || optDesc == null)
            throw new IllegalArgumentException();

        this.optName  = optName;
        this.optDesc  = optDesc;
        this.defValue = defValue;
        this.optional = Boolean.FALSE;
    }

    public ConfigOption copy()
        throws CloneNotSupportedException {
        return (ConfigOption)clone();
    }
    
    public abstract void checkOptionIsValid(String value) 
        throws InvalidOptionValueException;

    protected InvalidOptionValueException invalidOption(String msg) {
        return new InvalidOptionValueException(getName() + " " + msg);
    }
    
    public void setDefault(String value){
        this.defValue = value;
    }

    public String getDefault(){
        return this.defValue;
    }

    public void setOptional(boolean optional){
        this.optional = (optional == true ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isOptional(){
        return this.optional.booleanValue();
    }

    public void setName(String name) {
        this.optName = name;
    }

    public String getName(){
        return this.optName;
    }

    public void setDescription(String description) {
        this.optDesc = description;
    }
    
    public String getDescription(){
        return this.optDesc;
    }

    public void setCategory(String category) {
        this.optCat = category;
    }
    
    public String getCategory(){
        return this.optCat;
    }

    public String getConfirm() {
        return confirm;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    public int hashCode() {
        return getName().hashCode();
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof ConfigOption)) {
            return false;
        }
        return ((ConfigOption)obj).getName().equals(getName());
    }

    public String toString () {
        return "[ConfigOption name=" + getName() + "]";
    }
}
