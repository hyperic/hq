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
 * StrongBase is a private helper class that implements the basic methods for
 * enforcing strong collection types.
 */
abstract class StrongBase
{
    private Class m_classCollection;
    private Class m_classObject;

    /**
     * Constructs a StrongBase class. This constructor is provided so that
     * subclasses can use the init() method, which makes it easier to catch
     * exceptions in their constructors. If this constructor is called the next
     * method call must be to init().     
     */
    protected StrongBase()
    {
    }
    
    /**
     * Constructs a StrongBase class. The constructor can only be called by a
     * subclass of StrongBase.
     *
     * @param c
     *      The java.lang.Class object of the collection class that will use
     *      the StrongBase. The collection class must implement the
     *      java.util.Collection interface or a sublcass of this interface.
     * @param obj
     *      The java.lang.Class object of the class that the collection will
     *      contain. The obj Class can be any type that can be implemented
     *      in the Java language.
     *
     * @see java.util.Collection
     */
    protected StrongBase(java.lang.Class c, java.lang.Class obj)
    {
        if(c == null || obj == null)
            throw new IllegalArgumentException("Null pointer passed to the org.hyperic.util.StrongBase constructor.");
            
        this.init(c, obj);
    }

    protected StrongBase(String c, String obj) throws ClassNotFoundException
    {
        this.init(Class.forName(c), Class.forName(obj));
    }
    
    /**
     * Initializes a StrongBase class.
     *
     * @param c
     *      The java.lang.Class object of the collection class that will use
     *      the StrongBase. The collection class must implement the
     *      java.util.Collection interface or a sublcass of this interface.
     * @param obj
     *      The java.lang.Class object of the class that the collection will
     *      contain. The obj Class can be any type that can be implemented
     *      in the Java language.
     *
     * @see java.util.Collection
     */
    protected void init(java.lang.Class coll, java.lang.Class obj)
    {
        if(coll == null || obj == null)
            throw new IllegalArgumentException("Null pointer passed to the org.hyperic.util.StrongBase constructor.");
            
        this.m_classCollection = coll;
        this.m_classObject     = obj;
    }
    
    /**
     * Initializes a StrongBase class.
     *
     * @param c
     *      The java.lang.Class object of the collection class that will use
     *      the StrongBase. The collection class must implement the
     *      java.util.Collection interface or a sublcass of this interface.
     * @param obj
     *      The java.lang.Class object of the class that the collection will
     *      contain. The obj Class can be any type that can be implemented
     *      in the Java language.
     *
     * @see java.util.Collection
     */
    protected void init(String coll, String obj) throws ClassNotFoundException
    {
        init(Class.forName(coll), Class.forName(obj));
    }
    
    /**
     * Checks whether a Collection object is of the type specified when
     * constructing the StrongBase class and throws a ClassCastException
     * if it is not.
     *
     * @param collection
     *      The java.lang.Class object of the collection class that will use
     *      the StrongBase. This methods throws a ClassCastException if the
     *      paramater is not valid.
     *
     * @see java.lang.ClassCastException
     * @see java.util.Collection
     */
    protected void checkCollection(java.util.Collection collection)
    {
        if(this.isValidCollection(collection) == false)
            throw new ClassCastException("A Java Collection of type " + collection + "was passed to a org.hyperic.util.Collection that accepts type " + this.m_classCollection);
    }

    /**
     * Checks whether a object is of the type that the collection accepts
     * and throws a ClassCastException if it is not.
     *
     * @param obj
     *      The java.lang.Class object of the collection class that will use
     *      the StrongBase. This methods throws a ClassCastException if the
     *      paramater is not valid.
     *
     * @see java.lang.ClassCastException
     */
    protected void checkObject(Object obj)
    {
        if(this.isValidObject(obj) == false)
            throw new ClassCastException("A Java Class of type " + obj.getClass() + " was passed to a " + this.m_classCollection + " that accepts type " + this.m_classObject);
    }

    /**
     * Returns the collection Class.
     *
     * @return
     *      The java.lang.Class object of the Collection class.
     *
     * @see java.lang.Class
     */
    protected Class getCollectionClass()
    {
        return this.m_classCollection;
    }

    /**
     * Returns the object Class of the objects accepted by a Collection.
     *
     * @return
     *      The java.lang.Class object of the objects allowed in the
     *      collection.
     *
     * @see java.lang.Class
     */
    protected Class getObjectClass()
    {
        return this.m_classObject;
    }

    /**
     * Returns whether a Collection object is of the type specified when
     * constructing the StrongBase class.
     *
     * @param collection
     *      The java.lang.Class object of the collection class that will use
     *      the StrongBase. This methods throws a ClassCastException if the
     *      paramater is not valid.
     *
     * @return
     *      true if the collection class is valid. false otherwise.
     *
     * @see java.util.Collection
     */
    protected boolean isValidCollection(java.util.Collection collection)
    {
        if(collection == null)
            throw new IllegalArgumentException("A null pointer was passed to a method that does not accept null paramaters.");
            
        boolean bResult;
        
        if( this.m_classCollection.isInstance(collection) )
            bResult = true;
        else
            bResult = false;
            
        return bResult;
    }

    /**
     * Returns whether an object is of the type that the collection accepts.
     *
     * @param obj
     *      The java.lang.Class object of the collection class that will use
     *      the StrongBase.
     *
     * @return
     *      true if the object is allowed in the collection. false otherwise.
     */
    protected boolean isValidObject(Object obj)
    {
        if(obj == null)
            throw new IllegalArgumentException("A null pointer was passed to a method that does not accept null paramaters.");
            
        boolean bResult;
        
        if( this.m_classObject.isInstance(obj) )
            bResult = true;
        else
            bResult = false;
            
        return bResult;
    }
}
