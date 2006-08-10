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

package org.hyperic.hq.ui.taglib.display;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.RequestUtils;

/**
 * Extends column for internationalization of title.
 *
 *   title          - the title displayed for this column.  if this is omitted
 *                    then the property name is used for the title of the column
 *                    (optional)
 */
public class LocalizedColumnTag extends ColumnTag 
    implements Cloneable {
   
    public boolean defaultSort = false;
    public boolean isLocalizedTitle = true;
    public Integer sortAttr = null;
    public String headerColspan = null;
    
    public boolean isDefaultSort() {
        return this.defaultSort;
    }
    
    public void setDefaultSort(boolean b) {
        defaultSort = b;
    }
    
    public boolean getIsLocalizedTitle() {
        return this.isLocalizedTitle;
    }
    
    public void setIsLocalizedTitle(boolean b) {
        isLocalizedTitle = b;
    }
    
    public Integer getSortAttr() {
        return this.sortAttr;
    }
    
    public void setSortAttr(Integer i) {
        sortAttr = i;
    }

    public String getHeaderColspan() {
        return headerColspan;
    }
    
    public void setHeaderColspan(String s) {
        headerColspan = s;
    }

    /** internationalize the title */
    public String getTitle() {
        if (isLocalizedTitle) {
            Locale userLocale =
                RequestUtils.getUserLocale(
                    (HttpServletRequest) pageContext.getRequest(), null);
            try {
                return TagUtils.getInstance().message(pageContext, null,
                                                      userLocale.toString(),
                                                      super.getTitle());
            } catch (JspException e) {
                // Do not localize then
            }
        }
        return super.getTitle();
    }

    /**
     * process the contents of anything within the body.
     */
    /**
     * When the tag starts, we just initialize some of our variables, and do a
     * little bit of error checking to make sure that the user is not trying
     * to give us parameters that we don't expect.
     **/
    public int doStartTag() throws JspException {
        HttpServletRequest req =
            (HttpServletRequest) this.pageContext.getRequest();

        if (getHref() != null) {
            setHref( req.getContextPath() + getHref() );
        }

        return super.doStartTag();
    }

    public void release() {
        super.release();
        defaultSort = false;
        isLocalizedTitle = true;
        sortAttr = null;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("LocalizedColumnTag(");
        buf.append(super.toString());
        buf.append(getTitle());
        buf.append(".");
        buf.append(getProperty());
        buf.append(".");
        buf.append(getHeaderColspan());
        buf.append(".");
        buf.append(getHref());
        buf.append(")");
        return buf.toString();
    }

}

