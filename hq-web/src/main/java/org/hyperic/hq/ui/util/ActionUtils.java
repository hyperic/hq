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

package org.hyperic.hq.ui.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.ui.beans.ConfigValues;
import org.hyperic.util.HypericEnum;
import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

/**
 * Utilities class that provides general convenience methods.
 */
public class ActionUtils {
    
    /**
     * Return a new <code>ActionForward</code> based on the given one
     * but with the specified parameter name and value added to the
     * new forward's path. NOTE: this method would be unnecessary if
     * Struts allowed us to "unfreeze" the <code>ForwardConfig</code>
     * that is the superclass of the forward.
     *
     * @param forward the ActionForward on which the new forward is based
     * @param param the name of the path parameter to add
     * @param value the value of the parameter to add
     * @exception ServletException if encoding the path parameter fails
     */
    public static ActionForward changeForwardPath(ActionForward forward,
                                                  Map params)
        throws Exception {
        String newUrl = changeUrl(forward.getPath(), params);
        ActionForward newForward = new ActionForward( forward.getName(), newUrl,
                                                      forward.getRedirect() );
        return newForward;
    }
    
    public static ActionForward changeForwardPath(ActionForward forward,
                                                  String param,
                                                  String value)
    throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return changeForwardPath(forward, params);
    }

    /**
     * Change a url by appending all of the <code>params</code> to it.
     *
     * @param url the original URL
     * @param params the name-value pairs to append
     * @return the new url
     */
    public static String changeUrl(String url, Map params) throws Exception {
        StringBuffer newUrl = new StringBuffer(url);
        
        if (params != null) {
            int index = url.indexOf('?');
            String separator = index == -1 ? "?" : "&";

            Iterator i = params.keySet().iterator();
            while (i.hasNext()) {
                Object name = i.next();
                Object value = params.get(name);
                try {
                    if ( value != null && value.getClass().isArray() ) {
                        Object[] arr = (Object[])value;
                        for (int j=0; j<arr.length; ++j) {
                            _appendParam(newUrl, separator, name, arr[j]);
                        }
                    } else {
                        _appendParam(newUrl, separator, name, value);
                    }
                    if ("?".equals(separator)) {
                        separator = "&";
                    }
                }
                catch (UnsupportedEncodingException e) {
                    // how on earth could a jvm not support UTF-8??
                    throw new ServletException("could not encode ActionForward path parameters because the JVM does not support UTF-8!?", e);
                }
            }
        }

        return newUrl.toString();
    }

    /**
     * Return a URL path that will return control to the current
     * action. This path is generated by adding the specified
     * parameter to the path of the forward specified as the "input"
     * forward for the given mapping.
     *
     * @param mapping the ActionMapping describing the current
     * action's forwards
     * @param param the name of the path parameter to add
     * @param value the value of the parameter to add
     * @exception ServletException if encoding the path parameter fails or input has not been set
     */
    public static String findReturnPath(ActionMapping mapping, Map params)
        throws Exception {
        ActionForward inputForward = mapping.getInputForward();
        if (inputForward.getPath() == null)
            throw new ServletException("input cannot be null for returnPath on url: " + 
                                        mapping.getPath());
        ActionForward returnForward =
            ActionUtils.changeForwardPath(inputForward, params);
        return returnForward.getPath();
    }

    public static String findReturnPath(ActionMapping mapping,
                                        String param,
                                        String value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return findReturnPath(mapping, params);
    }

    public static List<ConfigValues> getConfigValues(ConfigSchema schema,
                                       ConfigResponse config) {
        List<ConfigValues> values = new ArrayList<ConfigValues>();

        if (schema == null) {
            return values;
        }

        List options = schema.getOptions();
        int size = options.size();

        for (int i=0; i<size; i++) {
            ConfigOption option = (ConfigOption)options.get(i);
            String value = config.getValue(option.getName(), true);

            if (option instanceof StringConfigOption) {
                StringConfigOption strOption =
                    (StringConfigOption)option;
                
                if (strOption.isHidden()) {
                    continue; //Ignore
                }
            }
            else if (option instanceof BooleanConfigOption) {
                if (value == null) {
                    value = String.valueOf(false);
                }
            }

            values.add(new ConfigValues(option.getName(), value));
        }

        return values;
    }

    private static void _appendParam(StringBuffer newPath, String separator,
                                     Object name, Object value)
        throws UnsupportedEncodingException
    {
        newPath.append(separator +
                       URLEncoder.encode(name.toString(),
                                         "UTF-8"));
        newPath.append("=");
        if (value != null) {
            newPath.append(URLEncoder.encode(value.toString(),
                                             "UTF-8"));
        }
    }
    
    /**
     * Convert a list of {@link HypericEnum}s into a list of 
     * {@link LabelValueBean}s
     */
    public static List convertEnumsToLabelBeans(List enums) {
        List res = new ArrayList(enums.size());
        
        for (Iterator i=enums.iterator(); i.hasNext(); ) {
            HypericEnum e = (HypericEnum)i.next();
            
            res.add(new LabelValueBean(e.getValue(), e.getCode() + "")); 
        }
        return res;
    }
    
    /**
     * Extracts request parameters corresponding to the {@link ConfigOption} formal argument.
     * <p> 
     * <b>Note:</b> Multiple values corresponding to a single parameter would be concatenated to a 
     * whitespace delimited string.<br/>
     * <b>Note:</b> Boolean request parameters missing from the request would be considered as provided with a 
     * <code>false</code> value.
     * </p>  
     * 
     * @param configOption Request parameter metadata. 
     * @param prefix Request parameter name prefix. 
     * @param request {@link HttpServletRequest} instance.  
     * @return
     */
    public static final String getRequestConfigOption(final ConfigOption configOption, final String prefix, 
                                                                            final HttpServletRequest request) {  
        String value = null ;
        
        final String reqParamName = prefix + configOption.getName() ; 
        String[] arrRequestParams = request.getParameterValues(reqParamName) ; 
        if(arrRequestParams == null){ 
            //if the 
            if(configOption instanceof BooleanConfigOption) {
                value = Boolean.FALSE.toString() ; 
            }else {
                return null ; 
            }//EO else if the configuration was not of a boolean nature.  
            
        }else if(arrRequestParams.length > 1) { 
            value="";
            for(int regExps=0; regExps < arrRequestParams.length; regExps++) {
                value += arrRequestParams[regExps] + " ";
            }//EO while there are more values 
        }//EO else if option had more than one value 
        else { 
            value = arrRequestParams[0] ; 
        }//EO else if there was a single value 
        
        return value ; 
    }//EOM 
    
}
