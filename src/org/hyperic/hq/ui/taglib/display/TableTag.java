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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.TaglibUtils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This tag takes a list of objects and creates a table to display those
 * objects.  With the help of column tags, you simply provide the name of
 * properties (get Methods) that are called against the objects in your list
 * that gets displayed [[reword that...]]
 *
 * This tag works very much like the struts iterator tag, most of the attributes
 * have the same name and functionality as the struts tag.
 *
 * Simple Usage:<p>
 *
 *   <display:table name="list" >
 *     <display:column property="title" />
 *     <display:column property="code" />
 *     <display:column property="dean" />
 *   </display:table>
 *
 * More Complete Usage:<p>
 *
 *   <display:table name="list" pagesize="100">
 *     <display:column property="title"
 *                    title="College Title" width="60%" sort="true"
 *                    href="/display/pubs/college/edit.page"
 *                      paramId="OID"
 *                    paramProperty="OID" />
 *     <display:column property="code" width="10%" sort="true"/>
 *     <display:column property="primaryOfficer.name" title="Dean" width="30%" />
 *     <display:column property="active" sort="true" />
 *   </display:table>
 *
 *
 * Attributes:<p>
 *
 *   name
 *   property
 *   scope
 *   length
 *   offset
 *   pageSize
 *   decorator
 *
 *
 * HTML Pass-through Attributes
 *
 * There are a number of additional attributes that just get passed through to
 * the underlying HTML table declaration.  With the exception of the following
 * few default values, if these attributes are not provided, they will not
 * be displayed as part of the <table ...> tag.
 *
 *   width          - defaults to "100%" if not provided
 *   border         - defaults to "0" if not provided
 *   cellspacing    - defaults to "0" if not provided
 *   cellpadding    - defaults to "2" if not provided
 *   align
 *   nowrapHeader
 *   background
 *   bgcolor
 *   frame
 *   height
 *   hspace
 *   rules
 *   summary
 *   vspace
 *
 **/

public class TableTag extends TablePropertyTag {
    
    private List columns = new ArrayList();
    private int currentNumCols = 0;
    private int numCols = 0;
    private Integer itemCount = null;
    private Decorator dec = null;
    private static Properties prop = null;
    
    // Used by various functions when the person wants to do paging
    private SmartListHelper helper = null;
    
    // User parameters that control the behavior of paging and sorting
    private int sortColumn = Constants.SORTCOL_DEFAULT.intValue();
    private int pageNumber = Constants.PAGENUM_DEFAULT.intValue();
    private int pageSize = Constants.PAGESIZE_DEFAULT.intValue();
    private String sortOrder = Constants.SORTORDER_ASC;
    
    private int offSet;
    private int length;

    private List masterList;
    private List viewableList;
    private Iterator iterator;
    
    private HttpServletResponse res;
    private JspWriter out;
    private HttpServletRequest req;
    private StringBuffer buf = new StringBuffer(8192);
    
    // variables to hold the previous row columns values.
    protected Hashtable previousRow = new Hashtable(10);
    protected Hashtable nextRow = new Hashtable(10);
    private ColumnDecorator[] colDecorators;
    
    private boolean started = false;
    int rowcnt = 0;

    /**
     * static footer added using the footer tag.
     */
    private String footer;

    private static Log log = LogFactory.getLog(TableTag.class.getName());

    public void release() {

        super.release();
        columns = new ArrayList(10);
        currentNumCols = 0;
        numCols = 0;
        
        sortColumn = Constants.SORTCOL_DEFAULT.intValue();
        pageNumber = Constants.PAGENUM_DEFAULT.intValue();
        pageSize = Constants.PAGESIZE_DEFAULT.intValue();
        sortOrder = Constants.SORTORDER_ASC;    
    
        buf = new StringBuffer(8192);
    
        // variables to hold the previous row columns values.
        previousRow = new Hashtable(10);
        nextRow = new Hashtable(10);
        colDecorators = null;
    
        started = false;
        rowcnt = 0;    
        this.footer = null;
    }

    /**
     * Called by interior column tags to help this tag figure out how it is
     * supposed to display the information in the List it is supposed to
     * display
     *
     * @param obj an internal tag describing a column in this tableview
     **/
    public void addColumn( ColumnTag obj ) {
 
        columns.add( obj );
        currentNumCols++;
    }
    
    private void resetColumns() {
        columns = new ArrayList(10);
        currentNumCols = 0;
    }

    /**
     * When the tag starts, we just initialize some of our variables, and do a
     * little bit of error checking to make sure that the user is not trying
     * to give us parameters that we don't expect.
     *
     * @return value returned by super.doStartTag()
     **/
    public int doStartTag() throws JspException {

        prop =(Properties) pageContext.getServletContext().getAttribute(Constants.PROPS_TAGLIB_NAME);    
        req = (HttpServletRequest) this.pageContext.getRequest();
        res = (HttpServletResponse)this.pageContext.getResponse();
        out = pageContext.getOut();

        evaluateAttributes();

        // Load our table decorator if it is requested
        this.dec = this.loadDecorator();
        if(this.dec != null) {
            this.dec.init(this.pageContext, viewableList);
        }
        
        return super.doStartTag();
    }
    
    
    /**
     * Make the next collection element available and loop, or
     * finish the iterations if there are no more elements.
     *
     * @exception JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {

        if (!started) {
            numCols = currentNumCols;

            // build an array of column decorator objects - 1 for each column tag
            colDecorators = new ColumnDecorator[currentNumCols];
            for (int c = 0; c < currentNumCols; c++) {
                ColumnTag       tmpTag = (ColumnTag)columns.get(c);
                ColumnDecorator coldec = tmpTag.getDecorator();
                colDecorators[c] = coldec;
                if (colDecorators[c] != null) {
                    colDecorators[c].init( this.pageContext, masterList );
                }
            }

            viewableList = getViewableData();
            buf.append( this.getTableHeader() );
            iterator = viewableList.iterator(); 
            started = true;
        }

        if(iterator.hasNext()) {

            Object row = iterator.next();
            String tmpvar = getVar();

            if (tmpvar != null) {
                /* put this in a var in the page scope so that the user can have access to it
                 * in jstl expression language.
                 */
                pageContext.setAttribute(tmpvar, row);
                TaglibUtils.setScopedVariable(pageContext, "request", tmpvar, row);
            }
            buf.append( generateRow( row, rowcnt ) );        
            rowcnt++;
            resetColumns();
            
            return (EVAL_BODY_AGAIN);
        } 
        else {
            resetColumns();
            return (SKIP_BODY);
        }
        
    }

    /**
     * Draw the table.  This is where everything happens, we figure out what
     * values we are supposed to be showing, we figure out how we are supposed
     * to be showing them, then we draw them.
     */
    public int doEndTag() throws JspException {
	
        buf.append( this.getTableFooter() );
        buf.append( "</table>\n" );

    	write( buf );

        // a little clean up.
        started = false;
        buf = new StringBuffer( 8192 );
        rowcnt = 0;
        release();

        return EVAL_PAGE;
        
    }
    
    
    /**
     * This returns a list of all of the data that will be displayed on the
     * page via the table tag.  This might include just a subset of the total
     * data in the list due to to paging being active, or the user asking us
     * to just show a subset, etc...<p>
     *
     * The list that is returned from here is not the original list, but it
     * does contain references to the same objects in the original list, so that
     * means that we can sort and reorder the list, but we can't mess with the
     * data objects in the list.
     */
    public List getViewableData() throws JspException {
        //display  the entinre list if thats what the user wants
        if( pageSize == Constants.PAGESIZE_ALL.intValue() )
            return masterList;

        //just return the list            
        helper = new SmartListHelper( masterList, pageSize, prop, itemCount );            
        return masterList;                    

    }
    
    
    /**
     * This method will sort the data in either ascending or decending order
     * based on the user clicking on the column headers.
     *
     * @param viewableData The list passed into this mehtod will be sorted.
     */
    protected void sortDataIfNeeded( List viewableData ) {
        
        // At this point we have all the objects that are supposed to be shown
        // sitting in our internal list ready to be shown, so if they have clicked
        // on one of the titles, then sort the list in either ascending or
        // decending order...
        
        ColumnTag tag = (ColumnTag) this.columns.get( this.sortColumn );
        
        // If it is an explicit value, then sort by that, otherwise sort by
        // the property...
        
        if( tag.getValue() != null ) {
            // Todo, figure out how to sort this better....
            //Collections.sort( (List)collection, tag.getValue() );
        } else {
            Collections.sort( viewableData, new BeanSorter(tag.getProperty(),
                                                           this.dec ) );
        }
        
        if( (Constants.SORTORDER_DEC).equals(sortOrder)){
            Collections.reverse( viewableData );
        }
        
    }
    
    /**
     * Format the row as HTML.
     *
     * @param row The list object to format as HTML.
     * @return The object formatted as HTML.
     */
    protected StringBuffer generateRow(Object row, int rowcnt)
        throws JspException
    {
        StringBuffer buf = new StringBuffer(8192);

        if( this.dec != null ) {
            String rt = this.dec.initRow( row, rowcnt, rowcnt + offSet );
            if( rt != null ) buf.append( rt );
        }

        try {
            for (int c = 0; c < currentNumCols; c++) {
                if (colDecorators[c] != null) {
                    colDecorators[c].initRow(row, rowcnt, rowcnt  +
                        (pageSize * (this.pageNumber - 1)));
                }
            }
            
        } catch (Throwable t) {
            throw new JspException(t);
        }
        pageContext.setAttribute("smartRow", row);
        
        // Start building the row to be displayed...
        buf.append( "<tr" );
        
        if( rowcnt % 2 == 0 ) {
            buf.append(" class=\"tableRowOdd\"");
        } else {
            buf.append(" class=\"tableRowEven\"");
        }
        
        buf.append( ">\n" );
        
        if (isLeftSidebar()) {
            buf.append("<td class=\"ListCellLine\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        // Bounce through our columns and pull out the data from this object
        // that we are currently focused on (lives in "smartRow").
        
        for( int i = 0; i < currentNumCols; i++ ) {
            ColumnTag tag = (ColumnTag)this.columns.get( i );
            
            buf.append( "<td " );
            buf.append( tag.getCellAttributes() );
            buf.append( ">" );
            
            // Get the value to be displayed for the column            
            Object value = null;
            if( tag.getValue() != null ) {
                value = evalAttr("value", tag.getValue(), Object.class,
                                 tag.getNulls());
             } else {
                if ( tag.getProperty().equals( "ff" ) ) {
                    value = String.valueOf( rowcnt );
                } else if ( tag.getProperty().equals( "null" )) {
                    value = ""; /* user doesn't want output, using c:set or something */
                } else {
                    value = this.lookup(pageContext, "smartRow",
                                        tag.getProperty(), null, true);
                    
                    if (colDecorators[i] != null) {
                        try {
                            value = colDecorators[i].decorate(value);
                        } catch (RuntimeException e) {
                            throw new JspException("Decorator " + colDecorators[i] 
                                + " encountered a problem: ", e);
                        }catch (Exception e) {
                            throw new JspException("Decorator " + colDecorators[i] 
                                + " encountered a problem: ", e);
                        }
                     }
                }
            }
            
            // By default, we show null values as empty strings, unless the
            // user tells us otherwise.
            if(value == null || "".equals(value.toString().trim()) ) {
                if( tag.getNulls() == null &&
                    prop.getProperty("basic.htmlNullValue") != null) {
                    value = prop.getProperty( "basic.htmlNullValue" );
                } else {
                    value = tag.getNulls();
                }
            }
            
            // String to hold what's left over after value is chopped
            String leftover = "";
            boolean chopped = false;
            String tempValue = "";
            if( value != null ) {
                tempValue = value.toString();
            }

            // trim the string if a maxLength or maxWords is defined
            if( tag.getMaxLength() > 0 && tempValue.length() > tag.getMaxLength() ) {
                leftover = "..." + tempValue.substring( tag.getMaxLength(), tempValue.length() );
                value = tempValue.substring( 0, tag.getMaxLength() ) + "...";
                chopped = true;
            } else if ( tag.getMaxWords() > 0 ) {
                StringBuffer tmpBuffer = new StringBuffer();
                StringTokenizer st = new StringTokenizer( tempValue );
                int numTokens = st.countTokens();
                if( numTokens > tag.getMaxWords() ) {
                    int x = 0;
                    while( st.hasMoreTokens() && ( x < tag.getMaxWords() ) ) {
                        tmpBuffer.append( st.nextToken() + " " );
                        x++;
                    }
                    leftover = "..." + tempValue.substring( tmpBuffer.length(), tempValue.length() );
                    tmpBuffer.append( "..." );
                    value = tmpBuffer;
                    chopped = true;
                }
            }
            
            // set up a link to the data being displayed in this column if requested
            if( tag.getAutolink() != null && tag.getAutolink().equals( "true" ) ) {
                value = this.autoLink( value.toString() );
            }
            
            String href = null;
            if (value != null && value.toString().length() > 0) {
                // set up a link if href="" property is defined
                if( tag.getHref() != null ) {
                    try {
                        href = (String) evalAttr("href", tag.getHref(),
                                                 String.class);
                    }
                    catch (NullAttributeException ne) {
                        throw new JspException("bean " + tag.getHref() +
                                               " not found");
                    }

                    if( tag.getParamId() != null ) {
                        String name = tag.getParamName();

                        if( name == null ) {
                            name = "smartRow";
                        }

                        Object param = this.lookup( pageContext, name,
                                                    tag.getParamProperty(),
                                                    tag.getParamScope(), true );

                        // URL escape params
                        // PR: 7709
                        String paramId = null;
                        String paramVal = null;
                        String tmp = param instanceof String ? (String) param :
                            param.toString();
                        try {
                            paramId = URLEncoder.encode(tag.getParamId(),
                                                        "UTF-8");
                            paramVal = URLEncoder.encode(tmp, "UTF-8");
                        }
                        catch (UnsupportedEncodingException e) {
                            throw new JspException(
                                "could not encode ActionForward path " +
                                "parameters because the JVM does not support " +
                                "UTF-8!?", e);
                        }

                        // flag to determine if we should use a ? or a &
                        int index = href.indexOf('?');
                        String separator = "";
                        if (index == -1) {
                            separator = "?";
                        } else {
                            separator = "&";
                        }
                        // if value has been chopped, add leftover as title
                        if( chopped ) {
                            value = "<a href=\"" + href + separator + paramId +
                                "=" + paramVal + "\" title=\"" + leftover +
                                "\">" + value + "</a>";
                        } else {
                            value = "<a href=\"" + href + separator + paramId +
                                "=" + paramVal + "\">" + value + "</a>";
                        }
                    } else /* tag.getParamId() == null */ {
                        // if value has been chopped, add leftover as title
                        if( chopped ) {
                            value = "<a href=\"" + href + "\" title=\"" + leftover + "\">" + value + "</a>";
                        } else {
                            value = "<a href=\"" + href + "\">" + value + "</a>";
                        }
                    }
                }            
            }
            
            if( chopped && href == null ) {
                
                buf.append( value.toString().substring( 0, value.toString().length() - 3 ) );
                buf.append( "<a style=\"cursor: help;\" title=\"" + leftover +
                "\">" );
                buf.append( value.toString().substring( value.toString().length() - 3,
                value.toString().length() ) + "</a>" );
            } else {
                buf.append( value );
            }
            
            buf.append( "</td>\n" );
        }
        
        // special case, if they didn't provide any columns.
        if( currentNumCols == 0 ) {
            buf.append( "<td class=\"tableCell\">" );
            buf.append( row.toString() );
            buf.append( "</td>" );
        }
        
        if (isRightSidebar()) {
            buf.append("<td class=\"ListCellLine\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        buf.append( "</tr>\n" );
        
        if( this.dec != null ) {
            String rt = this.dec.finishRow();
            if( rt != null ) {
                buf.append( rt );
            }
        }
        
        for (int c = 0; c < currentNumCols; c++) {
            if (colDecorators[c] != null) {
                colDecorators[c].finishRow();
            }
        }                
        
        if( this.dec != null ) {
            this.dec.finish();
        }
        this.dec = null;
        
        for (int c = 0; c < currentNumCols; c++){
            if (colDecorators[c] != null)
                colDecorators[c].finish();
        }
        
        return buf;
    }

    private String spacerImg() {
        return req.getContextPath() + "/images/spacer.gif";
    }

    private String makeUrl(boolean qs) throws JspException {
        HttpServletRequest req = (HttpServletRequest)this.pageContext.getRequest();
        
        String url = getAction();
        if (url == null) {
            url = req.getRequestURI();
        }
        
        Map params = RequestUtils.computeParameters(pageContext, getParamId(),
						                            getParamName(),
						                            getParamProperty(),
						                            getParamScope(), null,
						                            null, null, false);
        
        try {
            url = RequestUtils.computeURL(pageContext, null, url, null, null,
                                          params, null, false);
        } catch (Exception e) {
            throw new JspException("couldn't compute URL" + e);
        }
        
        if (qs) {
            // flag to determine if we should use a ? or a &
            int index = url.indexOf('?');
            String separator = index == -1 ? "?" : "&";
            
            String qString = req.getQueryString();
            if( qString != null && !qString.equals( "" ) ) {
                url += separator + qString + "&";
            } else {
                url += separator;
            }
        }
        
        return url;
    }
    
    /**
     * Generates the table header, including the first row of the table which
     * displays the titles of the various columns.
     *
     * @return Table header in HTML format
     */
    protected String getTableHeader() throws JspException {

        StringBuffer buf = new StringBuffer(1024);
        HttpServletRequest req = (HttpServletRequest)this.pageContext.getRequest();
        String url = makeUrl(false);
        
        buf.append( "<table" );
        buf.append( this.getTableAttributes() );
        buf.append( ">\n" );
        
        // If they don't want the header shown for some reason, then stop here.
        if( prop.getProperty("basic.show.header") != null &&
                !prop.getProperty( "basic.show.header" ).equals( "true" ) ) {
            return buf.toString();
        }
        
        buf.append( "<tr class=\"tableRowHeader\">\n" );
        
        if (isLeftSidebar()) {
            buf.append("<td class=\"ListCellLineEmpty\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        // if one of the columns declares a colspan, we'll set this to
        // the number of subsequent columns to skip
        int colsToSkip = 0;

        for( int i = 0; i < currentNumCols; i++ ) {

            LocalizedColumnTag tag = (LocalizedColumnTag) this.columns.get(i);

            if (colsToSkip > 0) {
                // we're in the middle of a colspan, so skip this
                // column
                colsToSkip--;
                continue;
            }
            else if (tag.getHeaderColspan() != null) {
                Integer colspan = (Integer) evalAttr("headerColspan",
                                                     tag.getHeaderColspan(),
                                                     Integer.class);

                // start the colspan
                colsToSkip = colspan.intValue();
            }

            boolean isDefaultSort = tag.isDefaultSort();

            int sortAttr = -1;
	    
            // bizapp requires a specific constant to tell it
            // which object type and attribute to sort on
            Integer sortAttrInt = tag.getSortAttr();
            if (sortAttrInt != null) {
                sortAttr = sortAttrInt.intValue();
            }

            if (sortAttr == -1) {
                sortAttr = i;
            }
            
            buf.append( "<th" );
            if( tag.getWidth() != null )
                buf.append( " width=\"" + tag.getWidth() + "\"" );
            
            if( tag.getAlign() != null )
                buf.append( " align=\"" + tag.getAlign() + "\"" );

            // if nowrapHeader
            if (getNowrapHeader() != null) {
                buf.append(" nowrap=\"true\"");
            }


            if (colsToSkip > 0) {
                buf.append(" colspan=\"" + colsToSkip + "\"");
                // decrement colsToSkip to account for the declaring
                // column
                colsToSkip--;
            }

            //table header class stuff
            if( sortColumn == -1 && isDefaultSort ){
                buf.append( " class=\"tableRowSorted\">" );
            }
            else if( sortColumn == sortAttr ){
                buf.append( " class=\"tableRowSorted\">" );
            }
            else if( tag.getHeaderStyleClass() != null ) {
                buf.append( " class=\"" + tag.getHeaderStyleClass() + "\">" );
            }
            else if( "true".equals( tag.getSort() ) )
                buf.append( " class=\"tableCellHeader\">" );
            else {
                buf.append( " class=\"tableRowInactive\">" );
            }

            String header = 
                (String) evalAttr("title", tag.getTitle(), String.class);

            if( header == null ) {
                if (tag.getIsLocalizedTitle()) {
                    header = StringUtil.toUpperCaseAt( tag.getProperty(), 0 );
                }
                else {
                    header = "<img src=\"" + spacerImg() + "\" width=\"1\" height=\"1\" border=\"0\"/>";
                }
            }
            
            if( tag.getSort() != null ) {
                String sortimg = null;
                int index = url.indexOf('?');
                String separator = index == -1 ? "?" : "&";
                
                if( (Constants.SORTORDER_ASC).equals(sortOrder) ) {
                    buf.append( "<a href=\"");
                    buf.append(url);
                    buf.append(separator);
                    buf.append(getOrderValue());
                    buf.append("=");
                    buf.append(Constants.SORTORDER_DEC);
                    buf.append("&");
                    buf.append(getSortValue());
                    buf.append("=");
                    buf.append(sortAttr);
                    buf.append("\">");
                    if( (sortColumn == -1 && isDefaultSort) ||
                        (sortColumn == sortAttr ) )
                        sortimg = "/images/tb_sortup.gif"; /* XXX make this a property */
                    
                    } else {
                        buf.append( "<a href=\"" + url + separator + getOrderValue() + "="+ Constants.SORTORDER_ASC +"&" + getSortValue()  +"=" + sortAttr + "\">" );
                        if( (sortColumn == -1 && isDefaultSort) ||
                            (sortColumn == sortAttr ) ) {
                            sortimg = "/images/tb_sortdown.gif"; /* XXX make this a property */
                        }
                    }
                buf.append( header );
                if (sortimg != null && !"".equals(sortimg)) {
                    buf.append("<img border=\"0\" src=\"");
                    buf.append(req.getContextPath());
                    buf.append(sortimg);
                    buf.append("\" >");
                }
                buf.append( "</a>" );
            } else {
                buf.append( header );
            }
            
            buf.append( "</th>\n" );
        }
        
        // Special case, if they don't provide any columns.
        if( currentNumCols == 0) {
            buf.append( "<td><b>" + prop.getProperty( "error.msg.no_column_tags" ) +
            "</b></td>" );
        }

        if (isRightSidebar()) {
            buf.append("<td class=\"ListCellLineEmpty\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        buf.append( "</tr>\n" );

        if (this.footer != null) {
            buf.append("<tfoot>");
            buf.append(this.footer);
            buf.append("</tfoot>");
            // reset footer
            this.footer = null;
        }

        String ret = buf.toString();
        return ret;
    }
    
    
    /**
     * Generates table footer with links for export commands.
     *
     * @return HTML formatted table footer
     **/
    protected String getTableFooter() throws JspException {

        StringBuffer buf = new StringBuffer( 1024 );
        
        HttpServletRequest req = (HttpServletRequest)this.pageContext.getRequest();
        String url = makeUrl(true);
        
        if( getExport()!= null ) {
            buf.append( "<tr><td align=\"left\" width=\"100%\" colspan=\"" + currentNumCols + "\">" );
            buf.append( "<table width=\"100%\" border=\"0\" cellspacing=\"0\" " );
            buf.append( "cellpadding=0><tr class=\"tableRowAction\">" );
            buf.append( "<td align=\"left\" valign=\"bottom\" class=\"" );
            buf.append( "tableCellAction\">" );
            
            // Figure out what formats they want to export, make up a little string
            
            String formats = "";
            if( prop.getProperty( "export.csv" ) != null &&
                    prop.getProperty( "export.csv" ).equals( "true" ) ) {
                formats += "<a href=\"" + url + "exportType=1\">" +
                prop.getProperty( "export.csv.label" ) + "</a>\n";
            }
            
            if( prop.getProperty( "export.excel" ) != null &&
                    prop.getProperty( "export.excel" ).equals( "true" ) ) {
                if( !formats.equals( "" ) ) formats += prop.getProperty( "export.banner.sepchar" );
                formats += "<a href=\"" + url + "exportType=2\">" +
                prop.getProperty( "export.excel.label" ) + "</a>\n";
            }
            
            if( prop.getProperty( "export.xml" ) != null &&
                    prop.getProperty( "export.xml" ).equals( "true" ) ) {
                if( !formats.equals( "" ) ) formats += prop.getProperty( "export.banner.sepchar" );
                formats += "<a href=\"" + url + "exportType=3\">" +
                prop.getProperty( "export.xml.label" ) + "</a>\n";
            }
            
            Object[] objs = {formats};
            if (prop.getProperty( "export.banner" ) != null) {
                buf.append( MessageFormat.format( prop.getProperty( "export.banner" ), objs ) );
            }
            buf.append( "</td></tr>" );
            buf.append( "</table>\n" );
            buf.append( "</td></tr>" );
        }

        String tmpEmptyMsg = getEmptyMsg();
        if (isPadRows()) {
            int tableSize = this.pageSize;
            if (tableSize < 1) {
                tableSize = Constants.PAGESIZE_DEFAULT.intValue();
                
            }
            if (tableSize > rowcnt) {
                String src = spacerImg();
                for( int i = rowcnt; i < tableSize; i++ ) {
                    buf.append("<tr class=\"ListRow\">\n");
                    if (isLeftSidebar()) {
                        buf.append("<td class=\"ListCellLine\"><img src=\"");
                        buf.append(src);
                        buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
                    }
                    for( int j = 0; j < numCols; j++ ) {
                        buf.append("<td class=\"ListCell\">&nbsp;</td>\n");
                    }
                    if (isRightSidebar()) {
                        buf.append("<td class=\"ListCellLine\"><img src=\"");
                        buf.append(src);
                        buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
                    }
                    buf.append("</tr>\n");
                }
            }
        } else if (tmpEmptyMsg != null && (rowcnt == 0)) {
            // there is a message to display when there are no rows
            String src = spacerImg();
            buf.append("<tr class=\"ListRow\">\n");
            if (isLeftSidebar()) {
                buf.append("<td class=\"ListCellLine\"><img src=\"");
                buf.append(src);
                buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
            }
            for( int j = 0; j < numCols; j++ ) {
                buf.append("<td class=\"ListCell\"");
                if (j == 1) {
                    buf.append(" nowrap=\"true\"><i>").append(tmpEmptyMsg).append("</i>");
                } else {
                    buf.append(">&nbsp;");
                }
                buf.append("</td>\n");
            }
            if (isRightSidebar()) {
                buf.append("<td class=\"ListCellLine\"><img src=\"");
                buf.append(src);
                buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
            }
            buf.append("</tr>\n"); 
        }
        
        String tmpstr = buf.toString();
        return tmpstr;
    }
    
    /**
     *   This takes a cloumn value and grouping index as the argument.
     *   It then groups the column and returns the appropritate string back to the
     *   caller.
     */
    protected String group(String value, int group) {
        
        if((group == 1) & this.nextRow.size() > 0) { // we are at the begining of the next row so copy the contents from .
            // nextRow to the previousRow.
            this.previousRow.clear();
            this.previousRow.putAll( nextRow );
            this.nextRow.clear();
        }
        
        
        if(!this.nextRow.containsKey(new Integer(group))) {
            // Key not found in the nextRow so adding this key now... remember all the old values.
            this.nextRow.put(new Integer(group), new String(value));
        }
        
        /**
         *  Start comparing the value we received, along with the grouping index.
         *  if no matching value is found in the previous row then return the value.
         *  if a matching value is found then this value should not get printed out
         *  so reuturn ""
         **/
        
        if(this.previousRow.containsKey(new Integer(group))) {
            for( int x = 1; x <= group; x++ ) {
                
                if(!((String)this.previousRow.get(new Integer(x))).equals(
                    ((String)this.nextRow.get(new Integer(x))))) {
                    // no match found so return this value back to the caller.
                    return value;
                }
            }
        }
        
        /**
         * This is used, for when there is no data in the previous row,
         * It gets used only the firt time.
         **/
        
        if(this.previousRow.size() == 0) {
            return value;
        }
        
        
        // There is corresponding value in the previous row so this value need not be printed, return ""
        return "<!-- returning from table tag -->"; // we are done !.
    }
    
    /**
     * Takes all the table pass-through arguments and bundles them up as a
     * string that gets tacked on to the end of the table tag declaration.<p>
     *
     * Note that we override some default behavior, specifically:<p>
     *
     *  width        defaults to 100% if not provided
     *  border       defaults to 0 if not provided
     *  cellspacing  defaults to 1 if not provided
     *  cellpadding  defaults to 2 if not provided
     **/
    
    protected String getTableAttributes() {
        StringBuffer results = new StringBuffer();
        
        if( getStyleClass()!= null ) {
            results.append( " class=\"" );
            results.append( getStyleClass() );
            results.append( "\"" );
        } else {
            results.append( " class=\"table\"" );
        }
        
        if( getStyleId() != null ) {
            results.append( " id=\"" );
            results.append( getStyleId() );
            results.append( "\"" );
        }
        
        if( getWidth() != null ) {
            results.append( " width=\"" );
            results.append( getWidth() );
            results.append( "\"" );
        } else {
            results.append( " width=\"100%\"" );
        }
        
        if( getBorder() != null ) {
            results.append( " border=\"" );
            results.append( getBorder());
            results.append( "\"" );
        } else {
            results.append( " border=\"0\"" );
        }
        
        if( getCellspacing() != null ) {
            results.append( " cellspacing=\"" );
            results.append( getCellspacing());
            results.append( "\"" );
        } else {
            results.append( " cellspacing=\"1\"" );
        }
        
        if( getCellpadding() != null ) {
            results.append( " cellpadding=\"" );
            results.append( getCellpadding() );
            results.append( "\"" );
        } else {
            results.append( " cellpadding=\"2\"" );
        }
        
        if( getAlign() != null ) {
            results.append( " align=\"" );
            results.append( getAlign() );
            results.append( "\"" );
        }
        
        if( getBackground() != null ) {
            results.append( " background=\"" );
            results.append( getBackground() );
            results.append( "\"" );
        }
        
        if( getBgcolor() != null ) {
            results.append( " bgcolor=\"" );
            results.append( getBgcolor() );
            results.append( "\"" );
        }
        
        if( getFrame() != null ) {
            results.append( " frame=\"" );
            results.append( getFrame() );
            results.append( "\"" );
        }
        
        if( getHeight() != null ) {
            results.append( " height=\"" );
            results.append( getHeight() );
            results.append( "\"" );
        }
        
        if( getHspace() != null ) {
            results.append( " hspace=\"" );
            results.append( getHspace() );
            results.append( "\"" );
        }
        
        if( getRules() != null ) {
            results.append( " rules=\"" );
            results.append( getRules() );
            results.append( "\"" );
        }
        
        if( getSummary() != null ) {
            results.append( " summary=\"" );
            results.append( getSummary() );
            results.append( "\"" );
        }
        if( getVspace() != null ) {
            results.append( " vspace=\"" );
            results.append( getVspace() );
            results.append( "\"" );
        }
        
        return results.toString();
    }
    
    
    /**
     * This functionality is borrowed from struts, but I've removed some
     * struts specific features so that this tag can be used both in a
     * struts application, and outside of one.
     *
     * Locate and return the specified bean, from an optionally specified
     * scope, in the specified page context.  If no such bean is found,
     * return <code>null</code> instead.
     *
     * @param pageContext Page context to be searched
     * @param name Name of the bean to be retrieved
     * @param scope Scope to be searched (page, request, session, application)
     *  or <code>null</code> to use <code>findAttribute()</code> instead
     *
     * @exception JspException if an invalid scope name is requested
     */
    
    public Object lookup( PageContext pageContext,
			  String name,
			  String scope )
	throws JspException {

        Object bean = null;
        if( scope == null )
            bean = pageContext.findAttribute( name );
        else if( scope.equalsIgnoreCase( "page" ) )
            bean = pageContext.getAttribute( name, PageContext.PAGE_SCOPE );
        else if( scope.equalsIgnoreCase( "request" ) )
            bean = pageContext.getAttribute( name, PageContext.REQUEST_SCOPE );
        else if( scope.equalsIgnoreCase( "session" ) )
            bean = pageContext.getAttribute( name, PageContext.SESSION_SCOPE );
        else if( scope.equalsIgnoreCase( "application" ) )
            bean =
            pageContext.getAttribute( name, PageContext.APPLICATION_SCOPE );
        else {
            Object[] objs = {name, scope};
            if (prop.getProperty( "error.msg.cant_find_bean" ) != null) {            
                String msg =
                MessageFormat.format( prop.getProperty( "error.msg.cant_find_bean" ), objs );            
                throw new JspException( msg );
            } else {
                throw new JspException( "Could not find " + name + " in scope " + scope);
            
            }
        }
        
        return ( bean );
    }
    
    
    /**
     * This functionality is borrowed from struts, but I've removed some
     * struts specific features so that this tag can be used both in a
     * struts application, and outside of one.
     *
     * Locate and return the specified property of the specified bean, from
     * an optionally specified scope, in the specified page context.
     *
     * @param pageContext Page context to be searched
     * @param name Name of the bean to be retrieved
     * @param property Name of the property to be retrieved, or
     *  <code>null</code> to retrieve the bean itself
     * @param scope Scope to be searched (page, request, session, application)
     *  or <code>null</code> to use <code>findAttribute()</code> instead
     *
     * @exception JspException if an invalid scope name is requested
     * @exception JspException if the specified bean is not found
     * @exception JspException if accessing this property causes an
     *  IllegalAccessException, IllegalArgumentException,
     *  InvocationTargetException, or NoSuchMethodException
     */
    
    public Object lookup(PageContext pageContext, String name,
			             String property, String scope, boolean useDecorator)
	       throws JspException
    {

        if (useDecorator && this.dec != null) {
            // First check the decorator, and if it doesn't return a value
            // then check the inner object...         
            try {
                if (property == null) {
                    return this.dec;
                }
                return ( PropertyUtils.getProperty( this.dec, property ) );
            } catch ( IllegalAccessException e ) {
                Object[] objs = {name, this.dec};
                if (prop.getProperty( "error.msg.illegal_access_exception" ) != null) {         
                    throw new JspException( MessageFormat.
                        format( prop.getProperty( "error.msg.illegal_access_exception" ), 
							      objs ) );
                } else {
                    throw new JspException("IllegalAccessException trying to fetch " +
                        "property " + name + " from bean " + dec);
                }
            } catch ( InvocationTargetException e ) {
                Object[] objs = {name, this.dec};
                if (prop.getProperty( "error.msg.invocation_target_exception" ) != null) {         
                    throw new JspException( MessageFormat.
                        format( prop.getProperty( "error.msg.invocation_target_exception" ), 
                                  objs ) );
                } else {
                    throw new JspException("InvocationTargetException trying to fetch " +
                        "property " + name + " from bean " + dec);
                }

            } catch ( NoSuchMethodException e ) {
                throw new JspException(" bean property getter not found");
            }
        }
        
        // Look up the requested bean, and return if requested
        Object bean = this.lookup( pageContext, name, scope );
        if( property == null ) return ( bean );
        
        if( bean == null ) {
            Object[] objs = {name, scope};
            if (prop.getProperty("error.msg.cant_find_bean") != null) {
                throw new JspException(MessageFormat.format( prop.getProperty("error.msg.cant_find_bean"), objs ) );
            } else {
                throw new JspException("Could not find bean " + name + "in scope " + scope);
            }
        }
        
        // Locate and return the specified property
        try {
            return ( PropertyUtils.getProperty( bean, property ) );
        } catch( IllegalAccessException e ) {
            Object[] objs = {property, name};
            if (prop.getProperty( "error.msg.illegal_access_exception" ) != null) {         
                throw new JspException( MessageFormat.
                    format( prop.getProperty( "error.msg.illegal_access_exception" ), 
                              objs ) );
            } else {
                throw new JspException("IllegalAccessException trying to fetch " +
                    "property " + property + " from bean " + name);
            }
        } catch( InvocationTargetException e ) {
            Object[] objs = {property, name};
            if (prop.getProperty( "error.msg.invocation_target_exception" ) != null) {         
                throw new JspException( MessageFormat.
                    format( prop.getProperty( "error.msg.invocation_target_exception" ), 
                              objs ) );
            } else {
                throw new JspException("InvocationTargetException trying to fetch " +
                    "property " + name + " from bean " + dec);
            }        
        }  catch (NoSuchMethodException e ) {
            throw new JspException(" bean getter for property " + property + 
                "  not found in bean " + name);
        }    
    }
    
    
    /**
     * If the user has specified a decorator, then this method takes care of
     * creating the decorator (and checking to make sure it is a subclass of
     * the TableDecorator object).  If there are any problems loading the
     * decorator then this will throw a JspException which will get propogated
     * up the page.
     */
    
    protected Decorator loadDecorator() throws JspException {
        
        if( getDecorator() == null || getDecorator().length() == 0 ) {
            return null;
        }
        try {
            Class c = Class.forName( getDecorator() );
            
            if(!Class.forName("org.hyperic.hq.ui.taglib.display.Decorator").
                isAssignableFrom(c)) {
                throw new JspException("Invalid decorator");
            }
            Decorator d = (Decorator)c.newInstance();
            return d;
        } catch(Exception e) {
            throw new JspException("failure loading and instanting decorator " + 
                e.toString() );
        }
    }
    
    /**
     * This takes the string that is passed in, and "auto-links" it, it turns
     * email addresses into hyperlinks, and also turns things that looks like
     * URLs into hyperlinks as well.  The rules are currently very basic, In
     * Perl regex lingo...
     *
     * Email:  \b\S+\@[^\@\s]+\b
     * URL:    (http|https|ftp)://\S+\b
     *
     * I'm doing this via brute-force since I don't want to be dependent on a
     * third party regex package.
     */
    protected String autoLink(String data) {
        String work = new String( data );
        int index = -1;
        String results = "";

        if( data == null || data.length() == 0 ) return data;
        
        // First check for email addresses.
        
        while( ( index = work.indexOf( "@" ) ) != -1 ) {
            int start = 0;
            int end = work.length() - 1;
            
            // scan backwards...
            for( int i = index; i >= 0; i-- ) {
                if( Character.isWhitespace( work.charAt( i ) ) ) {
                    start = i + 1;
                    break;
                }
            }
            
            // scan forwards...
            for( int i = index; i <= end; i++ ) {
                if( Character.isWhitespace( work.charAt( i ) ) ) {
                    end = i - 1;
                    break;
                }
            }
            
            String email = work.substring( start, ( end - start + 1 ) );
            
            results = results + work.substring( 0, start ) +
            "<a href=\"mailto:" + email + "\">" + email + "</a>";
            
            if( end == work.length() ) {
                work = "";
            } else {
                work = work.substring( end + 1 );
            }
        }
        
        work = results + work;
        results = "";
        
        // Now check for urls...
        
        while( ( index = work.indexOf( "http://" ) ) != -1 ) {
            int end = work.length() - 1;
            
            // scan forwards...
            for( int i = index; i <= end; i++ ) {
                if( Character.isWhitespace( work.charAt( i ) ) ) {
                    end = i - 1;
                    break;
                }
            }
            
            String url = work.substring( index, ( end - index + 1 ) );
            
            results = results + work.substring( 0, index ) +
            "<a href=\"" + url + "\">" + url + "</a>";
            
            if( end == work.length() ) {
                work = "";
            } else {
                work = work.substring( end + 1 );
            }
        }
        
        results += work;
        return results;
    }
    
    /**
     * Called by the setProperty tag to override some default behavior or text
     * string.
     */
    public void setProperty( String name, String value ) {
        prop.setProperty( name, value );
    }
    
    /**
     * Sets the content of the footer. Called by a nested footer tag.
     * @param string footer content
     */
    public void setFooter(String string)
    {
        this.footer = string;
    }

    /**
     * Is this the first iteration?
     * @return boolean <code>true</code> if this is the first iteration
     */
    protected boolean isFirstIteration()
    {
        return this.rowcnt == 0;
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
        return evalAttr(name, value, type, null);
    }

    private Object evalAttr(String name, String value, Class type, Object nulls)
        throws JspTagException {
            
        try {
            return ExpressionUtil.evalNotNull( "display", name, value,
            type, this, pageContext );
        }
        catch (NullAttributeException ne) {
            if (nulls != null) {
                return nulls;
            }
            throw new JspTagException( "Attribute " + name +
                                       " not found in TableTag");
        }
        catch (JspException je) {
            throw new JspTagException( je.toString() );
        }
        
    }
    
    protected void evaluateAttributes() throws JspTagException{
        masterList = (List) evalAttr( "items", getItems(), List.class );
        offSet = ( (Integer) evalAttr("offset", getOffset(), Integer.class ) ).intValue();
        length = ( (Integer) evalAttr("length", getLength(), Integer.class) ).intValue();
        
        setAction( (String) evalAttr("action", getAction(), String.class ) );
        setProperty( (String) evalAttr("property", getProperty(), String.class ) );
        setScope( (String) evalAttr("scope", getScope(), String.class ) );
        setDecorator( (String) evalAttr("decorator", getDecorator(), String.class ) );
        setExport( (String) evalAttr("export", getExport(), String.class ) );
        setWidth( (String) evalAttr("width", getWidth(), String.class ) );
        setBorder( (String) evalAttr("border", getBorder(), String.class ) );
        setCellspacing( (String) evalAttr("cellspacing", getCellspacing(), String.class ) );
        setCellpadding( (String) evalAttr("cellpadding", getCellpadding(), String.class ) );
        setAlign( (String) evalAttr("align", getAlign(), String.class ) );
        setNowrapHeader( (String) evalAttr("nowrapHeader", getNowrapHeader(), String.class ) );
        setBackground( (String) evalAttr("background", getBackground(), String.class ) );
        setBgcolor( (String) evalAttr("bgcolor", getBgcolor(), String.class ) );
        setFrame( (String) evalAttr("frame", getFrame(), String.class ) );
        setHeight( (String) evalAttr("height", getHeight(), String.class ) );
        setHspace( (String) evalAttr("hspace", getHspace(), String.class ) );
        setRules( (String) evalAttr("rules", getRules(), String.class ) );
        setSummary( (String) evalAttr("summary", getSummary(), String.class ) );
        setVspace( (String) evalAttr("vspace", getVspace(), String.class ) );
        setStyleClass( (String) evalAttr("styleClass", getStyleClass(), String.class ) );
        setStyleId( (String) evalAttr("styleId", getStyleId(), String.class ) );
        setSortValue( (String) evalAttr("sortValue", getSortValue(), String.class ) );
        setPageValue( (String) evalAttr("pageValue", getPageValue(), String.class ) );
        setPageSizeValue( (String) evalAttr("pageSizeValue", getPageSizeValue(), String.class ) );
        setOrderValue( (String) evalAttr("orderValue", getOrderValue(), String.class ) );
        setEmptyMsg( (String) evalAttr("emptyMsg", getEmptyMsg(), String.class ));

        Integer pn = (Integer) evalAttr("page", getPage(), Integer.class);
        if (pn == null || pn.intValue() == 0) {
            String pnp = req.getParameter(getPageValue());
            if (pnp != null && ! pnp.equals("")) {
                pn = new Integer(pnp);
            }
            if (pn == null || pn.intValue() == 0) {
                pn = Constants.PAGENUM_DEFAULT;
            }
        }
        pageNumber = pn.intValue();
        
        Integer ps = (Integer)evalAttr("pageSize", getPageSize(), Integer.class);
        if (ps == null || ps.intValue() == 0) {
            String psp = req.getParameter(getPageSizeValue());
            if (psp != null && ! psp.equals("")) {
                ps = new Integer(psp);
            }
            if (ps == null || ps.intValue() == 0) {
                ps = Constants.PAGESIZE_DEFAULT;
            }
        }
        this.pageSize = ps.intValue();
        
        Integer sc = (Integer) evalAttr("sort", getSort(), Integer.class);
        if (sc == null || sc.intValue() == 0) {
            String scp = req.getParameter(getSortValue());
            if (scp != null && ! scp.equals("")) {
                sc = new Integer(scp);
            }
            if (sc == null || sc.intValue() == 0) {
                sc = new Integer(-1);
            }
        }
        this.sortColumn = sc.intValue();
        
        String sortOrder = (String)evalAttr("order", getOrder(), String.class);
        if (sortOrder == null || sortOrder.equals("")) {
            sortOrder = req.getParameter(getOrderValue());
            if (sortOrder == null || sortOrder.equals("")) {
                sortOrder = Constants.SORTORDER_DEFAULT;
            }
        }
        this.sortOrder = sortOrder;
    }   
}

