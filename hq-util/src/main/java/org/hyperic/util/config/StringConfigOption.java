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

public class StringConfigOption extends ConfigOption implements Serializable {
    private int     minLength; /* Minimum length for the option */
    private int     maxLength; /* Maximum length for the option */
    private boolean isSecret;  /* Is it secret?                 */

    // Does the option even get exposed to the user?
    private boolean isHidden = false;

    public StringConfigOption(String optName, String optDesc, String defValue){
        super(optName, optDesc, defValue);

        this.minLength = 0;
        this.maxLength = Integer.MAX_VALUE;  /* Not correct, but good enough */
        this.isSecret  = false;
        this.isHidden  = false;
    }

    public StringConfigOption(String optName, String optDesc){
        super(optName, optDesc, null);

        this.minLength = 0;
        this.maxLength = Integer.MAX_VALUE;  /* Not correct, but good enough */
        this.isSecret  = false;
        this.isHidden  = false;
    }

    public void checkOptionIsValid(String value) 
        throws InvalidOptionValueException {

    	// TODO This validation should probably take optional into account.
    	//      It doesn't so if an option is optional but null is the value
    	//      this method throws an exception.  this doesn't seem right to me
    	//      but needs more investigation...
    	
        
    	if (isOptional() && value == null) {
    		return;
    	}
    	
        int min = getMinLength();

        if ((min == 0) && !isOptional()) {
            min = 1;
        }
        
        if(value == null) {
            throw invalidOption("cannot be null");
        }
        
        if (value.length() < min) {
            throw invalidOption("must be at least " + min + " characters long");
        }

        if (value.length() > getMaxLength()) {
            throw invalidOption("cannot be more than " + 
                                getMaxLength() + " characters long");
        }
    } 

    /**********************
     * Option properties
     **********************/

    public void setMinLength(int len){
        this.minLength = len;
    }

    public int getMinLength(){
        return this.minLength;
    }

    public void setMaxLength(int len){
        this.maxLength = len;
    }

    public int getMaxLength(){
        return this.maxLength;
    }

    public void setSecret(boolean val){
        this.isSecret = val;
    }

    public boolean isSecret(){
        return this.isSecret;
    }

    public void setHidden(boolean val){
        this.isHidden = val;
    }

    public boolean isHidden(){
        return this.isHidden;
    }
}
