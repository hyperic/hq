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

package org.hyperic.hq.ui.taglib;

import javax.servlet.jsp.tagext.TagSupport;

import org.hyperic.hq.ui.Constants;

/**
 * generate pagination info for a spicified list.
 */
public class PaginationParameters extends TagSupport {

    //----------------------------------------------------instance variables

    /** Holds value of property orderValue. */
    private String orderValue= Constants.SORTORDER_PARAM;
    
    /** Holds value of property i tems. */
    private String items ="items";
    
    /** Holds value of property sortValue. */
    private String sortValue = Constants.SORTCOL_PARAM;
    
    /** Holds value of property action. */
    private String action;
    
    /** Holds value of property maxPages. */
    private String maxPages = Constants.MAX_PAGES.toString();
    
    /** Holds value of property includeFirstLast. */
    private boolean includeFirstLast = false;
    
    /** Holds value of property includePreviousNext. */
    private boolean includePreviousNext = true;
    
    /** Holds value of property pageSize. */
    private String pageSize = Constants.PAGESIZE_DEFAULT.toString();
    
    /** Holds value of property pageValue. */
    private String pageValue = Constants.PAGENUM_PARAM;
   
    /** Holds value of property pageSizeValue. */
    private String pageSizeValue = Constants.PAGESIZE_PARAM;
    
    /** Holds value of property listTotalSsize. */
    private String listTotalSize;
          
    /** Holds value of property defaultSortColumn. */
    private String defaultSortColumn;
    
    /** Holds value of property pageNumber. */
    private String pageNumber;
    
    //----------------------------------------------------constructors

    public PaginationParameters() {
        super();
    }

    //----------------------------------------------------public methods

    /** Getter for property order.
     * @return Value of property order.
     *
     */
    public String getOrderValue() {
        return this.orderValue;
    }
    
    /** Setter for property order.
     * @param order New value of property order.
     *
     */
    public void setOrderValue(String orderValue) {
        this.orderValue = orderValue;
    }
    
    /** Getter for property items.
     * @return Value of property items.
     *
     */
    public String getItems() {
        return this.items;
    }
    
    /** Setter for property items.
     * @param items New value of property items.
     *
     */
    public void setItems(String items) {
        this.items = items;
    }
        
    /** Getter for property pageSize.
     * @return Value of property pageSize.
     *
     */
    public String getPageSizeValue() {
        return this.pageSizeValue;
    }
    
    /** Setter for property pageSize.
     * @param pageSize New value of property pageSize.
     *
     */
    public void setPageSizeValue(String pageSizeValue) {
        this.pageSizeValue = pageSizeValue;
    }
    
    /** Getter for property sort.
     * @return Value of property sort.
     *
     */
    public String getSortValue() {
        return this.sortValue;
    }
    
    /** Setter for property sort.
     * @param sort New value of property sort.
     *
     */
    public void setSortValue(String sortValue) {
        this.sortValue = sortValue;
    }
    
    /** Getter for property action.
     * @return Value of property action.
     *
     */
    public String getAction() {
        return this.action;
    }
    
    /** Setter for property action.
     * @param action New value of property action.
     *
     */
    public void setAction(String action) {
        this.action = action;
    }
    
    /** Getter for property maxPages.
     * @return Value of property maxPages.
     *
     */
    public String getMaxPages() {
        return this.maxPages;
    }
    
    /** Setter for property maxPages.
     * @param maxPages New value of property maxPages.
     *
     */
    public void setMaxPages(String maxPages) {
        this.maxPages = maxPages;
    }
    
    /** Getter for property includeFirstLast.
     * @return Value of property includeFirstLast.
     *
     */
    public boolean isIncludeFirstLast() {
        return this.includeFirstLast;
    }
    
    /** Setter for property includeFirstLast.
     * @param includeFirstLast New value of property includeFirstLast.
     *
     */
    public void setIncludeFirstLast(boolean includeFirstLast) {
        this.includeFirstLast = includeFirstLast;
    }
    
    /** Getter for property includePreviousNext.
     * @return Value of property includePreviousNext.
     *
     */
    public boolean isIncludePreviousNext() {
        return this.includePreviousNext;
    }
    
    /** Setter for property includePreviousNext.
     * @param includePreviousNext New value of property includePreviousNext.
     *
     */
    public void setIncludePreviousNext(boolean includePreviousNext) {
        this.includePreviousNext = includePreviousNext;
    }
    
    /** Getter for property pageSize.
     * @return Value of property pageSize.
     *
     */
    public String getPageSize() {
        return this.pageSize;
    }
    
    /** Setter for property pageSize.
     * @param pageSize New value of property pageSize.
     *
     */
    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }
    
    /** Getter for property listTotalSsize.
     * @return Value of property listTotalSsize.
     *
     */
    public String getListTotalSize() {
        return this.listTotalSize;
    }
    
    /** Setter for property listTotalsize.
     * @param listTotalsize New value of property listTotalsize.
     *
     */
    public void setListTotalSize(String listTotalSize) {
        this.listTotalSize = listTotalSize;
    }
    
    /** Getter for property pageValue.
     * @return Value of property pageValue.
     *
     */
    public String getPageValue() {
        return this.pageValue;
    }
    
    /** Setter for property pageValue.
     * @param pageValue New value of property pageValue.
     *
     */
    public void setPageValue(String pageValue) {
        this.pageValue = pageValue;
    }
    
    /** Getter for property defaultSortColumn.
     * @return Value of property defaultSortColumn.
     *
     */
    public String getDefaultSortColumn() {
        return this.defaultSortColumn;
    }
    
    /** Setter for property defaultSortColumn.
     * @param defaultSortColumn New value of property defaultSortColumn.
     *
     */
    public void setDefaultSortColumn(String defaultSortColumn) {
        this.defaultSortColumn = defaultSortColumn;
    }
    
        /**
     * Release tag state.
     *
     */
    public void release() {                
        super.release();
    }
    
    /** Getter for property string.
     * @return Value of property string.
     *
     */
    public String getPageNumber() {
        return this.pageNumber;
    }
    
    /** Setter for property string.
     * @param string New value of property string.
     *
     */
    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }
    
}    
    
