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

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.JspException;

import org.hyperic.hq.ui.taglib.display.*;

/**
 * One line description of what this class does.
 *
 * More detailed class description, including examples of usage if applicable.
 **/

public class SetPropertyTag extends BodyTagSupport implements Cloneable
{
   private String name;
   private String value;

   public void setName( String v ) { this.name = v; }
   public void setValue( String v ) { this.value = v; }

   public String getName() { return this.name; }
   public String getValue() { return this.value; }

   // --------------------------------------------------------- Tag API methods

   /**
    * Passes attribute information up to the parent TableTag.<p>
    *
    * When we hit the end of the tag, we simply let our parent (which better
    * be a TableTag) know what the user wants to change a property value, and
    * we pass the name/value pair that the user gave us, up to the parent
    *
    * @throws javax.servlet.jsp.JspException if this tag is being used outside of a
    *    <display:list...> tag.
    **/

   public int doEndTag() throws JspException {
      Object parent = this.getParent();

      if( parent == null ) {
         throw new JspException( "Can not use column tag outside of a " +
                                 "TableTag. Invalid parent = null" );
      }

      if( !( parent instanceof TableTag ) ) {
         throw new JspException( "Can not use column tag outside of a " +
                                 "TableTag. Invalid parent = " +
                                 parent.getClass().getName() );
      }

      ((TableTag)parent).setProperty( this.name, this.value );

      return super.doEndTag();
   }

}
