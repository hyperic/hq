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

package org.hyperic.util.pager;

import java.io.Serializable;

/**
 * A utility class to wrap up all the paging/sorting options that
 * are frequently used with finders and other methods that return
 * lists of things.
 */
public class PageControl implements Serializable, Cloneable {

    public static final int SIZE_UNLIMITED = -1;

    public static final int SORT_UNSORTED = 0;
    public static final int SORT_ASC      = 1;
    public static final int SORT_DESC     = 2;

    /** The SQL string representation of the above sort constants
        Note that the indices into this array line up with the
        constant values above for the SORT_XXX constants. */
    public static final String[] SQL_SORTS =
    { "", "ASC", "DESC" };

    public static final PageControl PAGE_ALL
        = new PageControl(0, SIZE_UNLIMITED, true);
    public static final PageControl PAGE_NONE
        = new PageControl(0, 0, true);
    public static final PageControl PAGE_MIN
        = new PageControl(0, 1, true);

    private int _pagenum       = 0;
    private int _pagesize      = SIZE_UNLIMITED;
    private int _sortorder     = SORT_UNSORTED;
    private int _sortattribute = SortAttribute.DEFAULT;
    private boolean _immutable = false;

    private Serializable metaData;  // Meta-data that PageLists have returned


    private PageControl (int pagenum, int pagesize, boolean immutable) {
        this(pagenum, pagesize);
        _immutable = immutable;
    }

    public PageControl () {}
    public PageControl (int pagenum, int pagesize) {
        _pagenum  = pagenum;
        _pagesize = pagesize;
    }
    public PageControl (int pagenum, int pagesize, 
                        int sortorder, int sortattribute) {
        _pagenum       = pagenum;
        _pagesize      = pagesize;
        _sortorder     = sortorder;
        _sortattribute = sortattribute;
    }
    public PageControl (PageControl pc) {
        _pagenum       = pc.getPagenum();
        _pagesize      = pc.getPagesize();
        _sortorder     = pc.getSortorder();
        _sortattribute = pc.getSortattribute();
        metaData       = pc.getMetaData();
    }

    public boolean isAscending  () { return _sortorder == SORT_ASC; }
    public boolean isDescending () { return _sortorder == SORT_DESC; }

    /**
     * sets the initial defaults for the PageControl.  Sort attribute specifies
     * which attribute to sort on.
     * 
     * @param pc
     * @param defaultSortAttr specifies the attribute to sort on.
     * @return PageControl
     */
    public static PageControl initDefaults ( PageControl pc, 
                                             int defaultSortAttr ) {
        if (pc == null) {
            pc = new PageControl();
        }
        else {
            pc = (PageControl) pc.clone();
        }
        
        if (pc.getSortattribute() == SortAttribute.DEFAULT) {
            pc.setSortattribute(defaultSortAttr);
        }
        if (pc.getSortorder() == SORT_UNSORTED) {
            pc.setSortorder(SORT_ASC);
        }
        return pc;
    }

    /** @return The current page number (0-based) */
    public int getPagenum () { return _pagenum; }

    /** @param pagenum Set the current page number to <code>pagenum</code> */
    public void setPagenum (int pagenum) { 
        if (_immutable) throw new IllegalStateException("immutable object");
        _pagenum = pagenum; 
    }

    /** @return The current page size */
    public int getPagesize () { return _pagesize; }

    /** @param pagesize Set the current page size to this value */
    public void setPagesize (int pagesize) { 
        if (_immutable) throw new IllegalStateException("immutable object");
        _pagesize = pagesize; 
    }

    /** @return The sort order used.  This is one of the SORT_XXX constants. */
    public int getSortorder () { return _sortorder; }

    /** @param sortorder Sort order to use, one of the SORT_XXX constants. */
    public void setSortorder (int sortorder) { 
        if (_immutable) throw new IllegalStateException("immutable object");
        _sortorder = sortorder; 
    }

    /** @return The attribute that the sort is based on. */
    public int getSortattribute () { return _sortattribute; }

    /** @param attr Set the attribute that the sort is based on. */
    public void setSortattribute (int attr) {
        if (_immutable) throw new IllegalStateException("immutable object");
        _sortattribute = attr; 
    }

    public Serializable getMetaData() { return this.metaData; }

    public void setMetaData(Serializable metaData) { 
        if (_immutable) throw new IllegalStateException("immutable object");
        this.metaData = metaData;
    }

    /**
     * Get the index of the first item on the page as dictated by the
     * page size and page number.  
     */
    public int getPageEntityIndex(){
        return _pagenum * _pagesize;
    }

    public String toString() {

        // shortcuts for common cases
        if (this == PageControl.PAGE_ALL) return "{ALL}";
        if (this == PageControl.PAGE_NONE) return "{NONE}";
        if (this.equals(PageControl.PAGE_ALL)) return "{ALL}";
        if (this.equals(PageControl.PAGE_NONE)) return "{NONE}";

        StringBuffer s = new StringBuffer("{");
        s.append("pn=" + _pagenum + " ");
        s.append("ps=" + _pagesize + " ");
        
        s.append("so=");
        
        switch(_sortorder) {
        case SORT_ASC:
            s.append("asc ");
            break;
        case SORT_DESC:
            s.append("desc");
            break;
        case SORT_UNSORTED:
            s.append("unsorted ");
            break;
        default:
            s.append(' ');
        }
        
        s.append("sa=" + _sortattribute + " ");
        s.append("}");
        return s.toString();
    }

    public boolean equals ( Object o ) { 
        if ( o instanceof PageControl ) {
            PageControl pc = (PageControl) o;
            return _pagenum == pc.getPagenum()
                && _pagesize == pc.getPagesize()
                && _sortorder == pc.getSortorder()
                && _sortattribute == pc.getSortattribute();
        }
        return false;
    }
    public int hashCode () {
        return (37*_pagenum)
            + (37*_pagesize)
            + (37*_sortorder)
            + (37*_sortattribute);
    }
    
    public Object clone(){
        PageControl res = new PageControl(_pagenum, _pagesize, _sortorder, 
                                          _sortattribute);
        res.metaData = metaData;
        return res;
    }
}
