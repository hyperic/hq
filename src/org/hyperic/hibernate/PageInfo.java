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

package org.hyperic.hibernate;

import org.hibernate.Query;
import org.hyperic.util.pager.PageControl;

/**
 * A paging class that knows a little about Hibernate.
 */
public class PageInfo {
    private int       _pageNum;
    private int       _pageSize;
    private boolean   _isAll;
    private SortField _sort;
    private boolean   _ascending;
    
    private PageInfo(int pageNum, int pageSize, SortField sort,
                     boolean ascending) 
    {
        _pageNum   = pageNum;
        _pageSize  = pageSize;
        _isAll     = false;
        _sort      = sort;
        _ascending = ascending; 
    }
    
    private PageInfo(SortField sort, boolean ascending) {
        _isAll     = true;
        _sort      = sort;
        _ascending = ascending;
    }
    
    /**
     * Get the page number.  The first page is page 0.
     */
    public int getPageNum() {
        return _pageNum;
    }
    
    /**
     * Get the max # of elements per page 
     */
    public int getPageSize() {
        return _pageSize;
    }
    
    /**
     * Returns the absolute index of the first row that the page points at.
     */
    public int getStartRow() {
        return _pageNum * _pageSize;
    }

    /**
     * Gets the field to sort on
     */
    public SortField getSort() {
        return _sort;
    }

    public boolean isAscending() {
        return _ascending;
    }
    
    public boolean isAll() {
        return _isAll;
    }

    /**
     * Modifies a Hibernate query to conform to the paging specs in this
     * PageInfo object.  This only changes this method makes are to the
     * page offset and result count.  Sort order must be dealt with elsewhere.
     */
    public Query pageResults(Query q) {
        if (!_isAll) {
            q.setFirstResult(getStartRow());
            q.setMaxResults(getPageSize());
        }
        return q;
    }
    
    public static PageInfo create(int pageNum, int pageSize, SortField sort,
                                  boolean ascending) 
    {
        return new PageInfo(pageNum, pageSize, sort, ascending);
    }
    
    /**
     * Returns a page control which will return all elements (not paged)
     */
    public static PageInfo getAll(SortField sort, boolean ascending) {
        return new PageInfo(sort, ascending);
    }
    
    public static PageInfo create(PageControl pc, SortField sort) {
        return new PageInfo(pc.getPagenum(), pc.getPagesize(),
                            sort, pc.isAscending());
    }
}
