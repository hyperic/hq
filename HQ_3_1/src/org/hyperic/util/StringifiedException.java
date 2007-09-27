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

package org.hyperic.util;

import java.io.Serializable;

/**
 * This class captures information about an exception and stores it
 * in a String-based form.  This makes it easy to transport arbitrary
 * types of exceptions over SOAP.  For example, in CAM's autoinventory scan,
 * when a scan completes, the scan state that is sent to the server could
 * contain a instance of an exception class representing an error that
 * occurred during the scan.  Instead of relying on Axis to know how 
 * to serialize/deserialize every conceivable exception class, we just
 * wrap the exceptions in this class, which makes everything string-based
 * and easy to work with.
 */
public class StringifiedException implements Serializable {

    private long   ctime;
    private String exceptionClass = null;
    private String message = null;
    private String stackTrace = null;
    private String toString = null;

    public StringifiedException () {
        ctime = System.currentTimeMillis();
    }

    public StringifiedException ( Throwable t ) {
        setExceptionClass(t.getClass().getName());
        setMessage(t.getMessage());
        setStackTrace(StringUtil.getStackTrace(t));
        setToString(t.toString());
        ctime = System.currentTimeMillis();
    }

    public long getCTime () { return ctime; }
    public void setCTime ( long ct ) { ctime = ct; }

    public String getExceptionClass () { return exceptionClass; }
    public void setExceptionClass ( String ec ) { exceptionClass = ec; }

    public String getMessage () { return message; }
    public void setMessage ( String m ) { message = m; }

    public String getStackTrace () { return stackTrace; }
    public void setStackTrace ( String st ) { stackTrace = st; }

    public String getToString () { return toString; }
    public void setToString ( String ts ) { toString = ts; }

    public String toString () { return toString; }
}
