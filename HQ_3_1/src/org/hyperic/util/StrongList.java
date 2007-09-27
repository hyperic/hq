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

import java.util.ListIterator;

/**
 * StrongList is a collection class, ArrayList that will only accept objects into
 * the collection of a type specified when constructing the collection.
 * StrongCollection's are subclassed to to create a type specific collection
 * (e.g., FooCollection extends StrongCollection). In this example,
 * FooCollection would only accept objects of type Foo.
 *
 * StrongCollection enforces the Collection checks at runtime. Subclassing the
 * StrongCollection class and providing stub implementions of methods like
 * add(Foo element) adds compile time checks.
 *
 * StrongList supports all of the methods of the java.util.List interface.
 *
 * StrongList also returns a ListIterator from the StrongList.listIterator()
 * method call. The ListIterator.add() and ListIterator.remove() call also
 * enforce that only objects allowed by the StrongList will be allowed into the
 * collection.
 *
 * @see java.util.List
 */
public class StrongList extends StrongCollection implements java.util.List
{
    /**
     * Constructs a StrongCollection class. This constructor is provided so that
     * subclasses can use the init() method, which makes it easier to catch
     * exceptions in their constructors. If this constructor is called the next
     * method call must be to init().     
     */
    protected StrongList()
    {
    }

    /**
     * Constructs a StrongList class.
     *
     * @param collection
     *      The java.lang.Class type of the subclassed collection.
     * @param object
     *      The java.lang.Class type of the object this collection will accept.
     */
    public StrongList(java.lang.Class collection, java.lang.Class object)
    {
        super(collection, object);
    }

    /**
     * Constructs a StrongList class. This constructor is used by
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
    protected StrongList(String coll, String obj) throws ClassNotFoundException
    {
        super(coll, obj);
    }
    
    /**
     * Constructs a StrongList class. This constructor allows you to use
     * a strongly typed list that is only type checked at runtime.
     *
     * @param object
     *      The java.lang.Class type of the object this collection will accept.
     */
    public StrongList(java.lang.Class object)
    {
        try
        {
            init(Class.forName("org.hyperic.util.StrongList"), object);
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
     * Constructs a StrongList class. This constructor allows you to use
     * a strongly typed list that is only type checked at runtime.
     *
     * @param object
     *      The java.lang.Class type of the object this collection will accept.
     */
    public StrongList(String object)
    {
        try
        {
            init("org.hyperic.util.StrongList", object);
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
     * Inserts the specified element into specified position in the collection.
     *
     * @param index
     *      The zero-based index where the element should be inserted.
     * @param element
     *      The object to add to the list.
     *
     * @throws ClassCastException
     *      If the element is not of the type accepted by the collection.
     */
    public void add(int index, Object element)
    {
        this.checkObject(element);
        this.m_aList.add(index, element);
    }

    /**
     * Inserts the elements of the specified collection to this collection.
     *
     * @param c
     *      The collection to insert into this collection.
     *
     * @return
     *      true if the objects was successfully added to the collection.
     *
     * @throws ClassCastException
     *      If the specified collection is not the same type as this collection.
     */
    public boolean addAll(int index, java.util.Collection c)
    {
        this.checkCollection(c);
        return this.m_aList.addAll(index, c);
    }

    /**
     * Retries the element at the specified index from the collection.
     *
     * @param index
     *      The zero-based index where the element should be retrieved from.
     *
     * @return
     *      The element at the specified index in the collection
     */
    public Object get(int index)
    {
        return this.m_aList.get(index);
    }

    /**
     * Searches for the first occurrence of the specified element in the collection.
     *
     * @param element
     *      The element to search for.
     *
     * @return
     *      The zero-based index of the specified element or -1 if the element
     *      cannot be found.
     *
     * @throws ClassCastException
     *      If the element is not of the type accepted by the collection.
     */
    public int indexOf(Object element)
    {
        this.checkObject(element);
        return this.m_aList.indexOf(element);
    }

    /**
     * Searches for the last occurrence of the specified element in the collection.
     *
     * @param element
     *      The element to search for.
     *
     * @return
     *      The zero-based index of the specified element or -1 if the
     *      element cannot be found.
     *
     * @throws ClassCastException
     *      If the element is not of the type accepted by the collection.
     */
    public int lastIndexOf(Object element)
    {
        this.checkObject(element);
        return this.m_aList.lastIndexOf(element);
    }

    /**
     * Returns a ListIterator of the elements in the collection.
     *
     * @return
     *      A java.util.ListIterator object.
     */
    public java.util.ListIterator listIterator()
    {
        return listIterator(0);
    }

    /**
     * Returns a ListIterator of the elements in the collection starting at the
     * specified index.
     *
     * @return
     *      A java.util.ListIterator object.
     */
    public java.util.ListIterator listIterator(int index)
    {
        return new ListItr(this.m_aList.listIterator(index));
    }

    /**
     * Removes the specified element from the collection.
     *
     * @param index
     *      The zero-based index of the element to remove from the collection.
     *
     * @return
     *      The element previously at the specified position.
     */
    public Object remove(int index)
    {
        return this.m_aList.remove(index);
    }

    /**
     * Replaces the element at the specified position in the collection.
     *
     * @param index
     *      The zero-based index where the element should be replaced.
     * @param element
     *      The object to store at the specified index in the collection.
     *
     * @throws ClassCastException
     *      If the element is not of the type accepted by the collection.
     */
    public Object set(int index, Object element)
    {
        this.checkObject(element);
        return this.m_aList.set(index, element);
    }

    /**
     * Returns a view of a portion of the collection. The SubList and
     * collection are linked. Changes in one are reflected in the other.
     *
     * @param fromIndex
     *      The zero-based index where the SubList should start from.
     * @param toIndex
     *      The zero-based index where the SubList should go to.
     */
    public java.util.List subList(int fromIndex, int toIndex)
    {
        return this.m_aList.subList(fromIndex, toIndex);
    }

    ///////////////////////////////////////////////////
    // Iterator Class
    protected class ListItr extends Itr implements ListIterator
    {
        protected ListItr(ListIterator i)
        {
            super(i);
        }

        public void add(Object o)
        {
            checkObject(o);
            this.m_itr.add(o);
        }

        public boolean hasPrevious()
        {
            return this.m_itr.hasPrevious();
        }

        public int nextIndex()
        {
            return this.m_itr.nextIndex();
        }

        public Object previous()
        {
            return this.m_itr.previous();
        }

        public int previousIndex()
        {
            return this.m_itr.previousIndex();
        }

        public void set(Object o)
        {
            checkObject(o);
            this.m_itr.set(o);
        }
    }
}
