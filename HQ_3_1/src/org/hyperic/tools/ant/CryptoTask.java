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

import java.io.IOException;

import org.hyperic.util.security.MD5;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class CryptoTask extends Task {

    private String value;
    private String property;

    public void setValue(String s) {
        value = s;
    }

    public void setProperty(String s) {
        property = s;
    }

    public void execute() throws BuildException {
        validateAttributes();

        String encrypted;
        try {
            encrypted = MD5.getEncodedDigest(value);
        } catch (IOException e) {
            throw new BuildException(e.getMessage(), e);
        }

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
