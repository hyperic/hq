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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import org.hyperic.tools.db.priv.PrivilegeCheck;
import org.hyperic.tools.db.priv.PrivilegeCheckException;
import org.hyperic.tools.db.priv.PrivilegeCheckFactory;

public class DBPrivCheckTask extends Task {

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private String property = null;
    private String errMsgProperty = null;
    private boolean quiet = true;
    private Project project = null;

    public void setJdbcUrl ( String jdbcUrl ) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setProperty ( String property ) {
        this.property = property;
    }

    public void setErrMsgProperty ( String errMsgProperty ) {
        this.errMsgProperty = errMsgProperty;
    }

    public void setJdbcUser ( String jdbcUser ) {
        this.jdbcUser = jdbcUser;
    }

    public void setJdbcPassword ( String jdbcPassword ) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }
    
    public boolean beQuiet() {
        return quiet;
    }

    public void execute () throws BuildException {
        validate();

        project = getProject();
        String errMsg;
        PrivilegeCheck checker = null;
        
        try {
            checker = PrivilegeCheckFactory.getChecker(jdbcUrl,jdbcUser,jdbcPassword);
            String privError = checker.isPrivileged();
            if (privError == null) {
                if (property != null) {
                    project.setProperty(property, "true");
                }
                return;
            }

            // this is a valid user who can login but can't pass
            // our smoke test
            errMsg = "User " + jdbcUser + " does not have the " +
                "required privileges on database: " +
                jdbcUrl + "\n" + privError;
            if (errMsgProperty != null) {
                project.setProperty(errMsgProperty, errMsg);
                return;
            }
            throw new BuildException(errMsg);
            
        } catch ( PrivilegeCheckException e ) {
            if (!beQuiet()) e.printStackTrace();
            errMsg = "Could not connect to check privileges on database: "
                + jdbcUrl;
            if (errMsgProperty != null) {
                project.setProperty(errMsgProperty, errMsg);
                return;
            }
            throw new BuildException(errMsg);

        } catch (IllegalStateException e) {
            if (errMsgProperty != null) {
                project.setProperty(errMsgProperty, e.getMessage());
                return;
            }
            throw new BuildException(e.getMessage());

        } finally {
            if (checker != null) checker.cleanup();
        }
    }

    private void validate() throws BuildException {
        if (jdbcUrl == null)
            throw new BuildException("No jdbcUrl was set, can't continue");        
    }

}
