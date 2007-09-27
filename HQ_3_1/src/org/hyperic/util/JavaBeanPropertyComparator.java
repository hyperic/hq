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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * This is a comparator that can be used for in-process sorting of a
 * collection of java beans.
 *
 */

public final class JavaBeanPropertyComparator implements Comparator {
    /**
     * Constant value representing an ascending sort.
     */
    public static final int ASCENDING = 0;

    /**
     * Constant value representing a descending sort.
     */
    public static final int DESCENDING = 1;

    // member data
    private String propertyName;
    private int sortOrder = ASCENDING;

    /**
     * Construct a comparator with <code>{@link ASCENDING}</code> sort
     * order and the given property name.
     *
     * @param propertyName the name of the property to use for sorting
     */
    public JavaBeanPropertyComparator(String propertyName) {
        this(propertyName, ASCENDING);
    }

    /**
     * Construct a comparator with the given sort order and property
     * name.
     *
     * @param propertyName the name of the property to use for sorting
     * @param sortOrder <code>{@link ASCENDING}</code> or <code>{@link
     * DESCENDING}</code>
     */
    public JavaBeanPropertyComparator(String propertyName, int sortOrder) {
        if ( null == propertyName || 0 == propertyName.length() ) {
            throw new IllegalArgumentException
                ("propertyName cannot be null or empty");
        }
        this.propertyName = propertyName;
        this.sortOrder = sortOrder;
    }

    /**
     * <p>If both <code>o1</code> and <code>o2</code> have the
     * appropriate java bean property, compare the String
     * representation of that property value on each.</p>
     *
     * <p>If one of the objects has the property and the other
     * doesn't, the one with the property will be considered "less"
     * than the one without the property.  If neither object has the
     * property, 0 will be returned.</p>
     *
     * <p>If the sort order is <code>DESCENDING</code>, the value
     * returned will be negated.</p>
     *
     * @param o1 the first java bean to compare
     * @param o2 the second java bean to compare
     *
     * @return the value of the string-comparison of the two bean's
     * property values
     */
    public int compare(Object o1, Object o2) {
        String val1 = getProperty(o1);
        String val2 = getProperty(o2);

        int retVal;
        if (null == val1) {
            if (null == val2) {
                retVal = 0;
            } else {
                // o2 is "less"
                retVal = 1;
            }
        } else {
            if (null == val2) {
                // o1 is "less"
                retVal = -1;
            } else {
                retVal = val1.compareTo(val2);
            }
        }

        if (ASCENDING == sortOrder) {
            return retVal;
        } else {
            return 0 - retVal;
        }
    }

    /**
     * Return true if the passed-in <code>obj</code> is the same as
     * this.
     *
     * @param obj the object being tested for equality to this
     *
     * @return true if the object is the same as this, false otherwise
     */
    public boolean equals(Object obj) {
        try {
            JavaBeanPropertyComparator that = (JavaBeanPropertyComparator)obj;
            return (this.propertyName.equals(that.propertyName) &&
                    this.sortOrder == that.sortOrder);
        } catch (ClassCastException e) {
            return false;
        }
    }

    //----------------------------------------------------------------
    //-- private helpers
    //----------------------------------------------------------------
    private String getProperty(Object obj) {
        try {
            PropertyDescriptor pd =
                new PropertyDescriptor( propertyName, obj.getClass() );
            Method m = pd.getReadMethod();
            Object value = m.invoke(obj, (Object[])null/* method args */);
            return String.valueOf(value);
        } catch (IntrospectionException e) {
            // no such property
            return null;
        } catch (IllegalAccessException e) {
            // no such property
            return null;
        } catch (InvocationTargetException e) {
            // no such property
            return null;
        }
    }

    //----------------------------------------------------------------
    //-- for testing
    //----------------------------------------------------------------
    public static void main(String[] args) {
        JavaBeanPropertyComparator asc = new JavaBeanPropertyComparator("foo", ASCENDING);
        JavaBeanPropertyComparator desc = new JavaBeanPropertyComparator("foo", DESCENDING);

        Foo foo1 = new Foo("aaa");
        Foo foo2 = new Foo("aaa");
        Foo foo3 = new Foo("bbb");
        NoFoo noFoo = new NoFoo();

        System.out.println("ascending compare(foo1, foo2)=" + asc.compare(foo1, foo2));
        System.out.println("ascending compare(foo1, foo3)=" + asc.compare(foo1, foo3));
        System.out.println("ascending compare(foo1, noFoo)=" + asc.compare(foo1, noFoo));
        System.out.println("descending compare(foo1, foo2)=" + desc.compare(foo1, foo2));
        System.out.println("descending compare(foo1, foo3)=" + desc.compare(foo1, foo3));
        System.out.println("descending compare(foo1, noFoo)=" + desc.compare(foo1, noFoo));
    }

    private static class Foo {
        private String foo;
        public Foo(String foo) {
            this.foo = foo;
        }
        public void setFoo(String foo) {
            this.foo = foo;
        }
        public String getFoo() {
            return foo;
        }
    } 

    private static class NoFoo {
        public NoFoo() { }
    }
} 

// EOF
