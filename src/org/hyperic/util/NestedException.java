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

import java.util.Vector;

/**
 * A simple class that provides for nested exceptions.
 */
public abstract class NestedException extends Exception {

    protected Vector _exception = new Vector();

    /**
     * Empty constructor, not very useful
     */
    public NestedException() {}

    /**
     * String constructor, a little more useful
     */
    public NestedException(String s) {
        super(s);
    }

    /**
     * Useful for revealing the original error.
     *
     * Constructor takes something throwable and adds it to a collection
     * that can later be dumped out by toString() and/or printStackTrace()
     */
    public NestedException(Throwable t) {
        super(t);
    }

    /**
     * Useful for revealing the original error with a bonus message.
     *
     * Constructor takes a string and something throwable.  Adds the
     * throwable item to a collection that can later be dumped out by 
     * toString() and/or printStackTrace()
     */
    public NestedException(String s, Throwable t) {
        super(s, t);
        addException(t);
    }

    /**
     * Useful for revealing the original error when there may be a stack
     * of them, plus you get an extra special bonus message!
     *
     * Constructor takes a string and a Vector of Throwables.  Adds the
     * throwable items to a collection that can later be dumped out by 
     * toString() and/or printStackTrace()
     *
     * Use this when nesting try/catch blocks
     *
     * The advantage of this one is that you don't need to construct your
     * exception in advance, just create the Vector and add() them as you
     * go.  In the end, throw with this constructor if the Vector's
     * size() != 0
     */
    public NestedException(String s, Vector excV) {
        super(s);
        int size = excV.size();
        // Iterators are highly over-rated
        for (int i = 0 ; i < size ; i++ ) {
            addException((Throwable) excV.get(i));
        }
    }

    public void addException(Throwable t) {
        _exception.addElement(t);
    }

    /**
     * Checks to see if this NestedException contains any exception
     * of the given type.  If it does, this method returns the first
     * such instance found.  This DOES descend into exceptions 
     * contained within this exception, so for example if you were looking
     * for an exception of type T1, and this NestedException was of type T2
     * and contained another NestedException of T3, which in turn contained
     * an exception of type T1, this method won't return null, because 
     * the exception is nested by this object.
     * <br><br>
     * Thorougly confused?  Ask Jonathan - he's the bastard writing this
     * kludgy workaround because jbrewer changed this class during the time 
     * that Jonathan's SNMPmgr code was out of the build cycle.  Jonathan's 
     * code depended on a "getBaseException" method in this class, which is 
     * now meaningless because there can be multiple (in a Vector) base 
     * exceptions.  Grrr....
     *
     * @param exceptionType The exception type to look for.
     * @return The first instance of the exception type found, 
     * or null if not found.
     */
    public Throwable getExceptionOfType ( Class exceptionType ) {
        int size = _exception.size();
        Object o = null;
        for ( int i=0; i<size; i++ ) {
            o = _exception.get(i);
            if ( o != null 
                 && exceptionType.isAssignableFrom(o.getClass()) 
                 && o instanceof Throwable ) {
                return (Throwable) o;
            }
            
            if(o instanceof NestedException){
                Throwable res;

                res = ((NestedException)o).getExceptionOfType(exceptionType);
                if(res != null){
                    return res;
                }
            }

        }
        return null;
    }
}
