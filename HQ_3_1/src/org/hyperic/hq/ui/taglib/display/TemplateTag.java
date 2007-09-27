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

/**
 * This is an abstract class that most tags should inherit from, it provides a
 * number of utility methods that allow tags to read in a template or multiple
 * template files from the web/templates directory, and use those templates as
 * flexible StringBuffers that reread themselves when their matching file
 * changes, etc...
 **/

package org.hyperic.hq.ui.taglib.display;

import java.io.IOException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

abstract public class TemplateTag extends BodyTagSupport
{

   public void write( String val ) throws JspTagException
   {
      try {
         JspWriter out = pageContext.getOut();
         out.write( val );
      } catch( IOException e ) {
         throw new JspTagException( "Writer Exception: " + e );
      }
   }

   public void write( StringBuffer val ) throws JspTagException
   {
      this.write( val.toString() );
   }

}

