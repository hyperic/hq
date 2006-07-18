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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * StrongCollection is a collection class that will only accept objects into
 * the collection of a type specified when constructing the collection.
 * StrongCollection's are subclassed to to create a type specific collection
 * (e.g., FooCollection extends StrongCollection). In this example,
 * FooCollection would only accept objects of type Foo.
 *
 * StrongCollection enforces the Collection checks at runtime. Subclassing the
 * StrongCollection class and providing stub implementions of methods like
 * add(Foo element) adds compile time checks.
 *
 * StrongCollection implements all of the methods of the java.util.Collection
 * interface.
 */
public class StrongCollection extends StrongBase implements java.util.Collection
{
    protected static final String CLASS_NOT_FOUND_MSG = "Unexpected ClassNotFoundException. A severe class loader exception has occurred\n";
    
    protected final ArrayList m_aList = new ArrayList();
    
    /**
     * Constructs a StrongCollection class. This constructor is provided so that
     * subclasses can use the init() method, which makes it easier to catch
     * exceptions in their constructors. If this constructor is called the next
     * method call must be to init().     
     */
    protected StrongCollection()
    {
    }
    
    /**
     * Constructs a StrongCollection class. This constructor is used by
     * subclasses that are strongly typed at compile time and runtime. Because
     * this constructor is protected a subclass should provide the public
     * constructor and subclass the methods in this class that accept types.
     *
     * @param collection
     *      The java.lang.Class type of the subclassed collection.
     * @param object
     *      The java.lang.Class type of the object this collection will accept.
     */
    protected StrongCollection(java.lang.Class collection, java.lang.Class object)
    {
        super(collection, object);
    }

    /**
     * Constructs a StrongCollection class. This constructor is used by
     * subclasses that are strongly typed at compile time and runtime. Because
     * this constructor is protected a subclass should provide the public
     * constructor and subclass the methods in this class that accept types.
     *
     * @param collection
     *      The java.lang.String type that specifies the fully qualified class
     *      name of the subclassed collection.
     * @param object
     *      The java.lang.String type that specifies the fully qualified class
     *      name of the class this collection will accept.
     */
    protected StrongCollection(String coll, String obj) throws ClassNotFoundException
    {
        super(coll, obj);
    }
    
    /**
     * Constructs a StrongCollection class. This constructor allows you to use
     * a strongly typed collection that is only type checked at runtime.
     *
     * @param object
     *      The java.lang.Class type of the object this collection will accept.
     */
    public StrongCollection(java.lang.Class object)
    {
        try
        {
            init(Class.forName("org.hyperic.util.StrongCollection"), object);
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
    
    /**
     * Constructs a StrongCollection class. This constructor allows you to use
     * a strongly typed collection that is only type checked at runtime.
     *
     * @param object
     *      The java.lang.Class type of the object this collection will accept.
     */
    public StrongCollection(String object)
    {
        try
        {
            init("org.hyperic.util.StrongCollection", object);
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

    /**
     * Adds an element to the collection.
     *
     * @param obj
     *        The object to add to the collection.
     *
     * @return
     *      true of object was successfully added to the collection.
     *
     * @throws ClassCastException
     *      If the element is not of the type accepted by the collection.
     */
    public boolean add(Object obj)
    {
        this.checkObject(obj);
        return this.m_aList.add(obj);
    }

    /**
     * Adds the elements of the specified collection to this collection.
     *
     * @param c
     *      The collection to add to this collection.
     *
     * @return
     *      true if the objects was successfully added to the collection.
     *
     * @throws ClassCastException
     *      If the specified collection is not the same type as the collection
     *      it's being added to.
     */
    public boolean addAll(java.util.Collection c)
    {
        this.checkCollection(c);
        return this.m_aList.add(c);
    }

    /**
     * Removes all of the elements from the collection.
     */
    public void clear()
    {
        this.m_aList.clear();
    }

    /**
     * Determines whether the collection contains the specified element.
     *
     * @param obj
     *      The object to test for in the collection.
     *
     * @return
     *      true if the collection contains the specified element.
     *
     * @throws ClassCastException
     *      If the element is not of the type accepted by the collection
     */
    public boolean contains(Object obj)
    {
        this.checkObject(obj);
        return this.m_aList.contains(obj);
    }

    /**
     * Determines whether the collection contains all of the elements in the
     * specified collection.
     *
     * @param obj
     *      The collection containing the element to check for.
     *
     * @return
     *      true if the collection contains the specified objects.
     *
     * @throws ClassCastException
     *      If the specified collection is not the same type as the collection
     *      it's being added to.
     */
    public boolean containsAll(java.util.Collection c)
    {
        this.checkCollection(c);
        return this.m_aList.containsAll(c);
    }

    /**
     * Determines whether the collection contains any elements.
     *
     * @return
     *      true if the collection contains no elements.
     */
    public boolean isEmpty()
    {
        return this.m_aList.isEmpty();
    }

    /**
     * Returns an Iterator of the elements in the collection.
     *
     * @return
     *      A java.util.Iterator object.
     */
    public java.util.Iterator iterator()
    {
        return new Itr(this.m_aList.listIterator());
    }

    /**
     * Removes the specified element from the collection.
     *
     * @param obj
     *      The object to remove from the collection.
     *
     * @return
     *      true if object was successfully removed from the collection.
     *
     * @throws ClassCastException
     *      If the element is not of the type accepted by the collection
     */
    public boolean remove(Object obj)
    {
        this.checkObject(obj);
        return this.m_aList.remove(obj);
    }

    /**
     * Removes all of the elements in the specified collection from this
     * collection.
     *
     * @param c
     *      The collection of objects to remove.
     *
     * @return
     *      true if the elements were successfully removed from the collection.
     *
     * @throws ClassCastException
     *      If the specified collection is not the same type as the collection
     *      it's being added to.
     */
    public boolean removeAll(java.util.Collection c)
    {
        this.checkCollection(c);
        return this.m_aList.removeAll(c);
    }

    /**
     * Removes all of the elements in the collection except those in the
     * specified collection.
     *
     * @param c
     *      The collection of objects to retain.
     *
     * @return
     *      true if the elements were succesfully retained.
     *
     * @throws ClassCastException
     *      If the specified collection is not the same type as the collection
     *      it's being added to.
     */
    public boolean retainAll(java.util.Collection c)
    {
        this.checkCollection(c);
        return this.m_aList.retainAll(c);
    }

    /**
     * The number of elements in the collection.
     *
     * @return
     *      The number of elements in the collection.
     */
    public int size()
    {
        return this.m_aList.size();
    }

    /**
     * Returns an array containing all of the elements in the collection.
     *
     * @return
     *      An array that contains all of the elements in the collection.
     */
    public Object[] toArray()
    {
        return this.m_aList.toArray();
    }

    /**
     * Returns an array containing all of the elements in the collection whose
     * runtime type is that of the specified array. The specied array must be
     * of the type specified when the StrongCollection class was instantiated.
     *
     * @return
     *      An array that contains all of the elements in the collection.
     *
     * @throws ClassCastException
     *      If the array is is not of the type accepted by the collection
     */
    public Object[] toArray(Object[] a)
    {
        this.checkObject(a);
        return this.m_aList.toArray(a);
    }

    /**
     * Returns a string of all of the elements in the collection.
     *
     * @return
     *      A string with all of the elements in the collection.
     */
    public String toString()
    {
        return this.m_aList.toString();
    }

    /**
     * reverses the element order
     *
     */
    public void reverse() {
        Collections.reverse(m_aList);
    }


    ///////////////////////////////////////////////////
    // Iterator Class

    protected class Itr implements Iterator
    {
        protected final ListIterator m_itr;

        protected Itr(ListIterator i)
        {
            this.m_itr = i;
        }

        public boolean hasNext()
        {
            return m_itr.hasNext();
        }

        public Object next()
        {
            return m_itr.next();
        }

        public void remove()
        {
            m_itr.remove();
        }
    }
}
