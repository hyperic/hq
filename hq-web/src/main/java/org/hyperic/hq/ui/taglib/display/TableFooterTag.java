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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TableFooterTag extends BodyTagSupport
{

    /**
     * logger.
     */
    private static Log log = LogFactory.getLog(TableFooterTag.class);

    /**
     * is this the first iteration?
     */
    private boolean firstIteration;

    /**
     * @see javax.servlet.jsp.tagext.Tag#doEndTag()
     */
    public int doEndTag() throws JspException {
        if (this.firstIteration) {
            TableTag tableTag =
                (TableTag) findAncestorWithClass(this, TableTag.class);

            if( tableTag == null ) {
                throw new JspException( "Can not use footer tag outside of a " +
                    "TableTag. Invalid parent = null" );
            }

            // add column header only once
            log.debug("first call to doEndTag, setting footer");

            if (getBodyContent() != null) {
                tableTag.setFooter(getBodyContent().getString());
            }

            this.firstIteration = false;
        }

        return EVAL_PAGE;
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public int doStartTag() throws JspException
    {
        TableTag tableTag =
            (TableTag) findAncestorWithClass(this, TableTag.class);

        if( tableTag == null ) {
            throw new JspException( "Can not use footer tag outside of a " +
                "TableTag. Invalid parent = null" );
        }

        // add column header only once
        this.firstIteration = tableTag.isFirstIteration();
        return this.firstIteration ? EVAL_BODY_BUFFERED : SKIP_BODY;

    }
}
