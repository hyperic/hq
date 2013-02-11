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

package org.hyperic.tools.ant;



import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


public class CryptoTask extends Task {

    private String value;
    private String property;
    private int strength = 256;//set 256 as default just in case.
    private boolean encodeHashAsBase64;
    
    public void setValue(String s) {
        value = s;
    }

    public void setProperty(String s) {
        property = s;
    }
    
    public void setStrength(int strength) {
        this.strength = strength;
    }
    
    public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
        this.encodeHashAsBase64 = encodeHashAsBase64;
    }


    public void execute() throws BuildException {
        validateAttributes();

        // TODO: the following code copies the Md5PlusShaPasswordEncoder, which resides in a dependant project.
        // This class should be moved into this project instead.
        Md5PasswordEncoder md5PwdEncoder = new Md5PasswordEncoder();
        md5PwdEncoder.setEncodeHashAsBase64(encodeHashAsBase64 );
        ShaPasswordEncoder shaPwdEncoder = new ShaPasswordEncoder(strength);
        shaPwdEncoder.setEncodeHashAsBase64(encodeHashAsBase64 );
        
        String md5Encoded = md5PwdEncoder.encodePassword(value, null);
        String encrypted = shaPwdEncoder.encodePassword(md5Encoded, null);

        Project project = getOwningTarget().getProject();
        project.setNewProperty(property, encrypted);
    }

    private void validateAttributes () throws BuildException {
        if (value == null) {
            throw new BuildException("Crypto: No 'value' attribute specified.");
        }
        if (property == null) {
            throw new BuildException("Crypto: No 'property' attribute specified.");
        }
    }
}
