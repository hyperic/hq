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

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;

import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.hyperic.hq.ui.Constants;

/**
 * generate pagination info for a spicified list.
 */
public class Pagination extends PaginationParameters {

    //----------------------------------------------------instance variables

    private String order = Constants.SORTORDER_DEFAULT;

    private int sort = Constants.SORTCOL_DEFAULT.intValue();

    private int page = Constants.PAGENUM_DEFAULT.intValue();
    
    private String path;
    private int maxPages = Constants.MAX_PAGES.intValue();
    private int listTotalSize;
    private int pageNumber;
    private int pageSize = Constants.PAGESIZE_DEFAULT.intValue();
    
    public Pagination() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Release tag state.
     *
     */
    public void release() {
        super.release();
        maxPages = Constants.MAX_PAGES.intValue();
        page = Constants.PAGENUM_DEFAULT.intValue();
        order = Constants.SORTORDER_DEFAULT;
        sort = Constants.SORTCOL_DEFAULT.intValue();
        path = null;
        listTotalSize = -1;
        pageSize = Constants.PAGESIZE_DEFAULT.intValue();
        pageNumber = Constants.PAGENUM_DEFAULT.intValue();
    }

//-------------------------------------------------------------  helper methods

    public final int doStartTag() throws JspException {


        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        JspWriter out = pageContext.getOut();

        evaluateAttributes();
        determinePageCount();
        getRequestParams(request);

        if(pageSize == Constants.PAGESIZE_ALL.intValue())
            return SKIP_BODY;

        path = request.getContextPath();

        try{
            out.write( createPagination() );
        }
        catch(Exception e){
            throw new JspException("could not generate output ",e);
        }

        return SKIP_BODY;
    }

    /** Use the jstl expression expression language to evaluate a field.
     *
     * @param name
     * @param value
     * @param type The Class type of the object you expect.
     *
     * @return The object found
     * @exception NullAttributeException Thrown if the value is null.
     */
    private Object evalAttr(String name, String value, Class type)
        throws JspTagException {

        try {
            return ExpressionUtil.evalNotNull( "spider", name, value,
                                                type, this, pageContext );
        }
        catch (NullAttributeException ne) {
            throw new JspTagException( name + " not found");
        }
        catch (JspException je) {
            throw new JspTagException( je.toString() );
        }

    }

    private boolean evalAttr(String name, boolean value) throws JspTagException {
        return ( (Boolean) evalAttr(name, new Boolean(value).toString(), Boolean.class) ).booleanValue();
    }

    private String evalAttr(String name, String value) throws JspTagException {
        return (String) evalAttr(name, value, String.class);
    }

   protected void evaluateAttributes()throws JspTagException{
        Integer temp = null;

        temp = (Integer) evalAttr("pageSize", getPageSize(), Integer.class );
        if(temp != null){
            pageSize = temp.intValue();
            temp = null;
        }

        temp = (Integer) evalAttr( "maxPages", getMaxPages(), Integer.class ) ;
        if( temp  != null ){
            maxPages =  temp.intValue();
            temp = null;
        }

        temp = (Integer) evalAttr( "listTotalSize", getListTotalSize(), Integer.class);
        if(temp != null){
            listTotalSize = temp.intValue();
            temp = null;
        }

        temp = (Integer) evalAttr( "pageNumber", getPageNumber(), Integer.class);
        if(temp != null){
            pageNumber = temp.intValue();
            temp = null;
        }

        setAction( evalAttr("action", getAction() ) );
        setOrderValue( evalAttr( "orderValue", getOrderValue() ) );
        setSortValue( evalAttr( "sortValue", getSortValue() ) );
        setDefaultSortColumn( evalAttr("defaultSortColumn", getDefaultSortColumn() ) );
        setIncludeFirstLast( evalAttr( "includeFirstLast", isIncludeFirstLast() ) );
        setIncludePreviousNext( evalAttr( "includePreviousNext", isIncludePreviousNext() ) );
        setPageValue( evalAttr( "pageValue", getPageValue() ) );
        setPageSizeValue( evalAttr( "pageSizeValue", getPageSizeValue() ) );
    }


    protected String createPagination() throws Exception{

        StringBuffer output = new StringBuffer();
        int sets = determineSets();

        output.append("<table border=\"0\" class=\"ToolbarContent\" >");
        output.append("<tr>");

        if (sets > 1){
            output.append("<td align=\"right\" nowrap><b>");
            output.append( RequestUtils.message(pageContext, null, null, "ListToolbar.ListSetLabel", null) );
            output.append("</b></td>");

            //generate select box
            output.append("<td>");
            output.append( createSetListSelect(sets) );
            output.append("</select>");  
            output.append("</td>");
            output.append("<td><img src=");
            output.append(path);
            output.append(" \"/images/spacer.gif\" height=\"1\" width=\"10\" border=\"0\"></td>");
        }
        output.append("<td>");
        output.append( createDots(sets ) );
        output.append("</td>");
        output.append("</tr>");
        output.append("</table>");


        return output.toString();

    }

    /**
     * Returns a string containing the nagivation bar that allows the user
     * to move between pages within the list.
     *
     * The urlFormatString should be a URL that looks like the following:
     *
     * http://.../somepage.page?pn={0}
     */    
    protected String createSetListSelect(int sets) {
        
        //ok now we should generate the select box                           
        StringBuffer msg = new StringBuffer();

        //generate the set list message
        msg.append("<select name=\"").append(this.getPageValue()).
            append("\" size=\"1\" onchange=\"goToSelectLocation(this, '").append(getPageValue()).append("',  '").
            append(getAction()).append("');\">") ;
            
        //generate the the select box with heach of the lists individually.  
        for(int i = 0; i < sets; i++){
                        
            int set = (i * maxPages); 
            msg.append("<option value=\"").append(set).append("\" " );
            
            if (pageNumber >= set)
                msg.append(" selected=\"selected\" ");
            int display = i + 1; 
            msg.append(">").append(display).append("</option>");
            
        }      
               
        return msg.toString();
    }

    /**
     * Returns a string containing the nagivation bar that allows the user
     * to move between pages within the list.
     *
     * The urlFormatString should be a URL that looks like the following:
     *
     * http://.../somepage.page?pn={0}
     */    
    protected String createDots(int sets) {            
        // flag to determine if we should use a ? or a &
        int index = getAction().indexOf('?');
        String separator = index == -1 ? "?" : "&";
        MessageFormat form =
            new MessageFormat(getAction() + separator + getPageValue() + "={0}" );

        int currentPage = this.page;
        int pageCount = determinePageCount();

        int startPage = 0;
        int endPage = maxPages;

        int currentSet = currentPage / maxPages;
        if (sets >= 1){
            startPage = currentSet * maxPages;
            endPage = startPage + maxPages;

            if (endPage > pageCount)
                endPage = pageCount;
        }

        if( pageCount == 1 || pageCount == 0 )
            return "&nbsp;";

        if( currentPage < maxPages ) {
            if( pageCount < endPage )
                endPage = pageCount;
        }        
                
        StringBuffer msg = new StringBuffer();        
        //passing sorting, ordering, and pageSize along with every request.
        StringBuffer pagMsg = new StringBuffer();                
        pagMsg.append("&").append(getOrderValue()).append("=").append(order).
            append("&").append(getSortValue()).append("=").append(sort).
            append("&").append(getPageSizeValue()).append("=").append(pageSize);

        if( currentPage == startPage ) {                        
            msg.append("<td><img src=\"").append(path).
                append("/images/tbb_pageleft_gray.gif\" width=\"13\" height=\"16\" border=\"0\"/></td>");            
        }
        else {
            Object[] objs = {new Integer( currentPage - 1 )};
            msg.append("<td><a href=\"").append(form.format( objs )).append(pagMsg.toString()).
                 append("\">").append("<img src=\"").append(path).
                 append("/images/tbb_pageleft.gif\" width=\"13\" height=\"16\" border=\"0\"/></a></td>");            
        }

        int displayNumber = startPage;
        for( int i = startPage; i < endPage; i++ ) {
            displayNumber += 1;
            if( i == currentPage ) {
                msg.append("<td>").append(displayNumber).append("</td>") ;
            } 
            else {
                Object[] v = {new Integer( i )};                                                                                
                msg.append("<td><a href=\"").append(form.format( v )).
                    append(pagMsg.toString()).append("\">").append(displayNumber).append("</a></td>");
            }

        }
        
        if( currentPage == endPage - 1) {            
            msg.append("<td><img src=\"").append(path).
                append("/images/tbb_pageright_gray.gif\" width=\"13\" height=\"16\" border=\"0\"/></td>");            
        }
        else {
            Object[] objs = {new Integer( currentPage + 1 )};
            msg.append("<td><a href=\"").append(form.format( objs )).append(pagMsg.toString()).
                append("\"><img src=\"").append(path).append("/images/tbb_pageright.gif\" width=\"13\" height=\"16\" border=\"0\"/></a></td>");
        }
        
        return msg.toString();
    }

    private int determineSets(){
        int pageCount = determinePageCount();
        int sets;

        int div = pageCount / Constants.MAX_PAGES.intValue();
        int mod = pageCount % Constants.MAX_PAGES.intValue();

        sets = ( mod == 0 ) ? div : div + 1;
        return sets;
    }
    private int determinePageCount(){
        int pageCount = 0;
        int size;

        size = listTotalSize;

        int div = size / pageSize;
        int mod = size % pageSize;
        pageCount = ( mod == 0 ) ? div : div + 1;

        return pageCount;

    }

    public void getRequestParams( HttpServletRequest request ){

        String order = (String) request.getParameter( getOrderValue() );
        String sort = (String) request.getParameter( getSortValue() );
        String page = (String) request.getParameter( getPageValue() );
        String pageSize = (String) request.getParameter( getPageSizeValue() );

        if(order != null)
            this.order = order;
        if(sort != null){
            try{
                this.sort = Integer.parseInt( sort );
            }
            catch(NumberFormatException e){
                this.sort = Integer.parseInt( getDefaultSortColumn() );
            }

        }
        else{
            this.sort = Integer.parseInt( getDefaultSortColumn() );
        }
        if(page != null){
            try{
                this.page = Integer.parseInt( page );
            }
            catch( NumberFormatException e ){
                //stick with the default value
            }

        }
        if(pageSize != null){
            try{
                this.pageSize = Integer.parseInt( pageSize );
            }
            catch( NumberFormatException e ){
                //stick with the default value
            }
        }
    }

    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;
    }
}
