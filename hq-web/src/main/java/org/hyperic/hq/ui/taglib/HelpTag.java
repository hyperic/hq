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


import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.Configurator;
import org.hyperic.hq.ui.Constants;

import org.apache.commons.lang.StringUtils;

/**
 * return the link to the help system.
 */
public class HelpTag extends VarSetterBaseTag {
    private static String HELP_BASE_URL = "http://pubs.vmware.com/hyperic58/index.jsp";
    private boolean context = true;
	private String key = "";
	private final static Log log = LogFactory.getLog(Configurator.class.getName());  ;
    
    //----------------------------------------------------public methods
    public boolean isContext() {
        return context;
    }

    public void setContext(boolean context) {
        this.context = context;
    }
	
    public String getKey() {
        return key;
    }
	
    public void setKey(String key) {
        this.key = key;
    }
	
    public int doStartTag() throws JspException{
        JspWriter output = pageContext.getOut();
        String helpURL = HELP_BASE_URL;
        String helpUrlFromProFile  = null;
        
        if (context) {            
            ServletContext servletContext = pageContext.getServletContext();
            if (servletContext!= null) {
                helpUrlFromProFile = (String)servletContext.getAttribute("helpBaseURL");
                log.debug("helpUrlFromPropertyFile=" + helpUrlFromProFile);                
            }
            if (!StringUtils.isEmpty(helpUrlFromProFile)) {
                helpURL = helpUrlFromProFile;
            }
  
            
      /* ignore context for now   
       *   String helpContext = (String) pageContext.getRequest().getAttribute(Constants.PAGE_TITLE_KEY);
            
            if ( helpContext != null)
                helpURL = helpURL + "/?key=ui-" + helpContext;
*/
        }
        //helpURL+=key;		
        
        try{
            output.print( helpURL );
        }
        catch(IOException e){
            throw new JspException(e);        
        }
        
        return SKIP_BODY;
    }
}
