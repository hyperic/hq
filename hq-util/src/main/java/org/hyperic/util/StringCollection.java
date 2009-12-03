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

/**
 * StringCollection is a collection class that will only java.lang.String
 * objects into the collection.
 *
 * StringCollection implements all of the methods of the java.util.Collection
 * interface.
 */
public class StringCollection extends StrongCollection
{
    /**
     * Constructs a StringCollection class.
     *
     * @param collection
     *      The java.lang.Class type of the subclassed collection.
     * @param object
     *      The java.lang.Class type of the object this collection will accept.
     */
    public StringCollection()
    {
        try
        {
            init("org.hyperic.util.StringCollection", "java.lang.String");
        }
        catch(ClassNotFoundException e)
        {
            // This error should be impossible because we are trying to get the Class
            // object for the class the class that this code is in (StrongCollection).
            // There would have to be a severe class loader problem for this exception
            // to occur.
            
            throw new InstantiationError(StrongCollection.CLASS_NOT_FOUND_MSG + e);
        }
    }

    public boolean add(String obj)
    {
        return super.add(obj);
    }
    
    public boolean addAll(StringCollection coll)
    {
        return super.addAll(coll);
    }

    public boolean contains(String obj)
    {
        return super.contains(obj);
    }

    public boolean containsAll(StringCollection coll)
    {
        return super.containsAll(coll);
    }

    public boolean remove(String obj)
    {
        return super.remove(obj);
    }

    public boolean removeAll(StringCollection coll)
    {
        return super.removeAll(coll);
    }
    
    public boolean retainAll(StringCollection coll)
    {
        return super.retainAll(coll);
    }

    public String[] toArray(String[] a)
    {
        return (String[])super.toArray(a);
    }
}
