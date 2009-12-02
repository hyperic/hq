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

package org.hyperic.hq.autoinventory;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.util.StringifiedException;

public class ScanMethodState implements Serializable {

    private static final long serialVersionUID = -8762869412017148433L;

    private String methodClass;
    private StringifiedException[] exceptions;
    private AIServerValue[] servers; 
    private String status;

    public ScanMethodState () {}

    public ScanMethodState (String methodClass) {
        setMethodClass(methodClass);
        setStatus("not yet started");
        setExceptions(null);
    }

    public String getMethodClass () { return methodClass; }
    public void setMethodClass ( String methodClass ) {
        this.methodClass = methodClass;
    }

    public StringifiedException[] getExceptions () {
        return exceptions;
    }
    public void setExceptions ( StringifiedException[] exceptions ) {
        this.exceptions = exceptions;
    }
    public void addException ( StringifiedException ex ) {
        if ( this.exceptions == null ) {
            this.exceptions = new StringifiedException[] { ex };

        } else {
            StringifiedException[] newExceptions
                = new StringifiedException[this.exceptions.length+1];
            System.arraycopy(this.exceptions, 0, 
                             newExceptions, 0, 
                             this.exceptions.length);
            newExceptions[newExceptions.length-1] = ex;
            this.exceptions = newExceptions;
        }
    }
    public void addExceptions ( Throwable[] throwables ) {

        StringifiedException[] sExceptions
            = new StringifiedException[throwables.length];
        for ( int i=0; i<throwables.length; i++ ) {
            sExceptions[i] = new StringifiedException(throwables[i]);
        }

        if ( this.exceptions == null ) {
            this.exceptions = sExceptions;

        } else {
            StringifiedException[] newExceptions
                = new StringifiedException[this.exceptions.length
                                           + sExceptions.length];
            System.arraycopy(this.exceptions, 0, 
                             newExceptions, 0, 
                             this.exceptions.length);
            System.arraycopy(sExceptions, 0, 
                             newExceptions, this.exceptions.length,
                             sExceptions.length);
            this.exceptions = newExceptions;
        }
    }

    public AIServerValue[] getServers () { return servers; }
    public void setServers ( AIServerValue[] servers ) { this.servers = servers; }

    public String getStatus () { return status; }
    public void setStatus ( String status ) { this.status = status; }

}
