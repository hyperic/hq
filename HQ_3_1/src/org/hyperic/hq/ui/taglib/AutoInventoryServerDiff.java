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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;

/**
 * Generates a String representing the diff for an AIServer
 */
public class AutoInventoryServerDiff extends TagSupport {

    private String resource;
    
    public AutoInventoryServerDiff() { super(); }

    public int doStartTag() throws JspException{

        JspWriter output = pageContext.getOut();
        String diffString;
        AIServerValue serverValue;
        try {
            serverValue = (AIServerValue) ExpressionUtil.evalNotNull("spider", 
                                                        "resource", 
                                                        getResource(), 
                                                        AIServerValue.class, 
                                                        this, 
                                                        pageContext );
        }
        catch (NullAttributeException ne) {
            throw new JspTagException( " typeId not found");
        }
        catch (JspException je) {
            throw new JspTagException( je.toString() );
        }
                                         
        diffString = AIQueueConstants.getServerDiffString(serverValue.getQueueStatus(), 
                                               serverValue.getDiff());          
        try{
            output.print( diffString);            
        }
        catch(IOException e){
            throw new JspException(e);        
        }
        return SKIP_BODY;
    }   
    
    public String getResource() {
        return this.resource;
    }    
    public void setResource(String resource) {
        this.resource = resource;
    }
}
