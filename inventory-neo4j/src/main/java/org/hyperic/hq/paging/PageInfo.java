package org.hyperic.hq.paging;

import java.io.Serializable;

public class PageInfo implements Serializable {
    public static final int SIZE_UNLIMITED = -1;

    public static final int SORT_UNSORTED = 0;
    public static final int SORT_ASC = 1;
    public static final int SORT_DESC = 2;

    public static final PageInfo PAGE_ALL = new PageInfo(0, SIZE_UNLIMITED, true);
    public static final PageInfo PAGE_NONE = new PageInfo(0, 0, true);
    public static final PageInfo PAGE_MIN = new PageInfo(0, 1, true);

    private int pageNum = 0;
    private int pageSize = SIZE_UNLIMITED;
    private int sortOrder = SORT_UNSORTED;
    private boolean immutable = false;
    private String sortAttribute;
    private Class<?> sortAttributeType;

    private PageInfo(int pagenum, int pagesize, boolean immutable) {
        this(pagenum, pagesize);
        this.immutable = immutable;
    }

    public PageInfo() {
    }

    public PageInfo(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public PageInfo(int pageNum, int pageSize, int sortOrder, String sortAttribute,
                    Class<?> sortAttributeType) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.sortOrder = sortOrder;
        this.sortAttribute = sortAttribute;
        this.sortAttributeType = sortAttributeType;
    }

    public boolean isAscending() {
        return sortOrder == SORT_ASC;
    }

    public boolean isDescending() {
        return sortOrder == SORT_DESC;
    }

    /** @return The current page number (0-based) */
    public int getPageNum() {
        return pageNum;
    }

    /** @param pagenum Set the current page number to <code>pagenum</code> */
    public void setPagNnum(int pageNum) {
        if (immutable)
            throw new IllegalStateException("immutable object");
        this.pageNum = pageNum;
    }

    /** @return The current page size */
    public int getPageSize() {
        return pageSize;
    }

    /** @param pagesize Set the current page size to this value */
    public void setPageSize(int pageSize) {
        if (immutable)
            throw new IllegalStateException("immutable object");
        this.pageSize = pageSize;
    }

    /** @return The sort order used. This is one of the SORT_XXX constants. */
    public int getSortOrder() {
        return sortOrder;
    }

    /** @param sortorder Sort order to use, one of the SORT_XXX constants. */
    public void setSortOrder(int sortOrder) {
        if (immutable)
            throw new IllegalStateException("immutable object");
        this.sortOrder = sortOrder;
    }

    /**
     * Get the index of the first item on the page as dictated by the page size
     * and page number.
     */
    public int getPageEntityIndex() {
        return pageNum * pageSize;
    }

    public String getSortAttribute() {
        return sortAttribute;
    }

    public void setSortAttribute(String sortAttribute) {
        if (immutable)
            throw new IllegalStateException("immutable object");
        this.sortAttribute = sortAttribute;
    }

    public Class<?> getSortAttributeType() {
        return sortAttributeType;
    }

    public void setSortAttributeType(Class<?> sortAttributeType) {
        if (immutable)
            throw new IllegalStateException("immutable object");
        this.sortAttributeType = sortAttributeType;
    }

    public String toString() {

        // shortcuts for common cases
        if (this == PageInfo.PAGE_ALL)
            return "{ALL}";
        if (this == PageInfo.PAGE_NONE)
            return "{NONE}";
        if (this.equals(PageInfo.PAGE_ALL))
            return "{ALL}";
        if (this.equals(PageInfo.PAGE_NONE))
            return "{NONE}";

        StringBuffer s = new StringBuffer("{");
        s.append("pn=" + pageNum + " ");
        s.append("ps=" + pageSize + " ");

        s.append("so=");

        switch (sortOrder) {
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

        s.append("sa=" + sortAttribute + " ");
        s.append("}");
        return s.toString();
    }

    public boolean equals(Object o) {
        if (o instanceof PageInfo) {
            PageInfo pi = (PageInfo) o;
            return pageNum == pi.getPageNum() &&
                   pageSize == pi.getPageSize() &&
                   sortOrder == pi.getSortOrder() &&
                   ((sortAttribute == null && pi.getSortAttribute() == null) || (sortAttribute != null && sortAttribute
                       .equals(pi.getSortAttribute())));
        }
        return false;
    }

    public int hashCode() {
        int hashCode = (37 * pageNum) + (37 * pageSize) + (37 * sortOrder);
        if (sortAttribute != null) {
            hashCode += (37 * sortAttribute.hashCode());
        }
        return hashCode;
    }
}
