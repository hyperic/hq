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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.taglib.html.Constants;
import org.apache.struts.taglib.html.OptionsCollectionTag;
import org.apache.struts.taglib.html.SelectTag;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.RequestUtils;

/**
 * <p>A JSP tag that will take a java.util.List and render a set of
 * HTML <code>&lt;option ...&gt;</code> markup using the resource
 * bundle.</p>
 *
 * <p>The attributes are:<ul>
 * <li><b>list</b> - the name of the list containing the key suffixes</li>
 * <li><b>baseKey</b> - the base key in the resource bundle</li>
 * </ul></p>
 *
 * <p>This tag will look up resources using:
 * <code>&lt;basekey&gt;.&lt;listelement&gt;.toString()</code>.  Thus,
 * if your base key was <code>foo.bar.baz</code> and your list
 * contained <code>[1, 2, 3]</code>, the resources rendered would be
 * <code>foo.bar.baz.1</code>, <code>foo.bar.baz.2</code> and
 * <code>foo.bar.baz.3</code>.</p>.
 *
 */
public class OptionMessageListTag extends OptionsCollectionTag {
    private final Log _log = LogFactory.getLog(OptionMessageListTag.class);

    private String _bundle = org.apache.struts.Globals.MESSAGES_KEY;
    private String _locale = org.apache.struts.Globals.LOCALE_KEY;
    private String _baseKey;

    /**
     * Set the name of the resource bundle to use.
     *
     * @param list the el expression for the list variable
     */
    public void setBundle(String bundle) {
        _bundle = bundle;
    }

    /**
     * Set the locale to use.
     *
     * @param list the el expression for the list variable
     */
    public void setLocale(String locale) {
        _locale = locale;
    }

    /**
     * Set the value of the base key in the application resource
     * bundle.
     *
     * @param delimiter the text to be printed between list items
     */
    public void setBaseKey(String baseKey) {
        _baseKey = baseKey;
    }
    
    /**
     * Process the tag, generating and formatting the list.
     *
     * @exception JspException if the scripting variable can not be
     * found or if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        try {
            SelectTag selectTag = (SelectTag) pageContext.getAttribute(Constants.SELECT_KEY);
            Object collection = TagUtils.getInstance().lookup(pageContext, name, property, null);

            if (collection == null) {
                _log.warn("OptionMessageList tag was looking for bean=" + 
                          name + " property=" + property + " but it wasn't " +
                          "found");
                throw new JspTagException("Unable to find bean=" + name + 
                                          " property=" + property);
            }
                
            JspWriter out = pageContext.getOut();
            StringBuffer sb = new StringBuffer();
            for (Iterator i = getIterator(collection); i.hasNext(); ) {
                Object next = i.next();
                String value, key;

                if (next instanceof LabelValueBean) {
                    LabelValueBean bean = (LabelValueBean) next;
                    value = bean.getValue();
                    key = _baseKey + '.' + bean.getLabel();
                } else {
                    value = String.valueOf( next );
                    key = _baseKey + '.' + value;
                }
                
                String label = TagUtils.getInstance().message(pageContext, _bundle, _locale, key);
                
                addOption(sb, label, value, selectTag.isMatched(value));
            }
            out.write(sb.toString());

            return SKIP_BODY;
        } catch (IOException e) {
            _log.warn("Unabel to generate message list", e);
            throw new JspTagException(e.toString());
        } catch (JspException e) {
            _log.warn("Unabel to generate message list", e);
            throw new JspTagException(e.toString());
        } catch (Exception e) {
            _log.warn("Unable to generate message list", e);
            throw new JspTagException(e.toString());
        }
    }

    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;        
    }

    public void release() {
        _bundle  = null;
        _locale  = null;
        _baseKey = null;
        super.release();
    }

    protected Iterator getIterator(Object collection) throws JspException {
        try {
            return super.getIterator(collection);
        } catch (ClassCastException e) {
            List list;
            if (collection.getClass().isArray()) {
                if (collection instanceof short[]) {
                    short[] arr = (short[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Short(arr[i]) );
                    }
                } else if (collection instanceof int[]) {
                    int[] arr = (int[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Integer(arr[i]) );
                    }
                } else if (collection instanceof long[]) {
                    long[] arr = (long[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Long(arr[i]) );
                    }
                } else if (collection instanceof float[]) {
                    float[] arr = (float[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Float(arr[i]) );
                    }
                } else if (collection instanceof double[]) {
                    double[] arr = (double[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Double(arr[i]) );
                    }
                } else if (collection instanceof byte[]) {
                    byte[] arr = (byte[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Byte(arr[i]) );
                    }
                } else if (collection instanceof char[]) {
                    char[] arr = (char[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Character(arr[i]) );
                    }
                } else if (collection instanceof boolean[]) {
                    boolean[] arr = (boolean[])collection;
                    list = new ArrayList(arr.length);
                    for (int i=0; i<arr.length; ++i) {
                        list.add( new Boolean(arr[i]) );
                    }
                } else {
                    list = new ArrayList();
                }
            } else {
                list = new ArrayList();
            }
            return list.iterator();
        }
    }
}
