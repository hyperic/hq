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

import java.util.List;
import javax.servlet.jsp.PageContext;

/**
 * This class provides some basic functionality for all objects which serve
 * as decorators for the objects in the List being displayed.
 **/

public abstract class Decorator implements Cloneable
{
   private PageContext ctx = null;
   private List list = null;

   private Object obj = null;
   private int viewIndex = -1;
   private int listIndex = -1;

   public Decorator() {

   }

   public void init( PageContext ctx, List list ) {
      this.ctx = ctx;
      this.list = list;
   }

   public String initRow( Object obj, int viewIndex, int listIndex ) {
      this.obj = obj;
      this.viewIndex = viewIndex;
      this.listIndex = listIndex;
      return "";
   }

   public String finishRow() {
      return "";
   }

   public void finish() {
   
   }

   public void setPageContext(PageContext context) {
       this.ctx = context;
   }
   
   public PageContext getPageContext() { return this.ctx; }
   public List getList() { return this.list; }

   public Object getObject() { return this.obj; }
   public int getViewIndex() { return this.viewIndex; }
   public int getListIndex() { return this.listIndex; }
   
   public void release() {
       ctx = null;
       list = null;
       obj = null;
       viewIndex = -1;
       listIndex = -1;
   }
}
