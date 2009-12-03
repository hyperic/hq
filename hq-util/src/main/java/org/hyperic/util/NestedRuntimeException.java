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

import java.io.PrintStream;

/**
 * This is just like NestedException, but it's a RuntimeException.
 * And here's a classic case for multiple inheritance - or rather
 * why RuntimeException isn't an interface.  But anyway, we get
 * around that by using containment and delegating all method calls
 * to our internal NestedException.
 */
public class NestedRuntimeException extends RuntimeException {

    private NestedException theRealDeal;

    public NestedRuntimeException() {
        theRealDeal = new NestedEx();
        theRealDeal.fillInStackTrace();
    }

    public NestedRuntimeException(String s) {
        theRealDeal = new NestedEx(s);
        theRealDeal.fillInStackTrace();
    }

    public NestedRuntimeException(Throwable t) {
        theRealDeal = new NestedEx(t);
        theRealDeal.fillInStackTrace();
    }

    public NestedRuntimeException(String s, Throwable t) {
        theRealDeal = new NestedEx(s, t);
        theRealDeal.fillInStackTrace();
    }

    public void addException(Throwable t) {
        theRealDeal.addException(t);
    }

    public Throwable getExceptionOfType ( Class exceptionType ) {
        return theRealDeal.getExceptionOfType(exceptionType);
    }

    public String toString() { 
        return theRealDeal.toString();
    }

    public void printStackTrace(PrintStream out) {
        theRealDeal.printStackTrace(out);
    }

    public void printStackTrace(){
        theRealDeal.printStackTrace();
    }

    public String getMessage () {
        return theRealDeal.getMessage();
    }

    public String getLocalizedMessage() {
        return theRealDeal.getLocalizedMessage();
    }

    /**
     * We need this class because NestedException is an abstract class.
     */
    class NestedEx extends NestedException {
        public NestedEx () { super(); }
        public NestedEx (String s) { super(s); }
        public NestedEx (Throwable t) { super(t); }
        public NestedEx (String s, Throwable t) { super(s, t); }
    }
}
