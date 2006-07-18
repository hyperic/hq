<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-fmt" prefix="c" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
  This file is part of HQ.
  
  HQ is free software; you can redistribute it and/or modify
  it under the terms version 2 of the GNU General Public License as
  published by the Free Software Foundation. This program is distributed
  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE. See the GNU General Public License for more
  details.
  
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA.
 --%>


<table width="100%" cellpadding="3" cellspacing="0" border="0">
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td width="20%">&nbsp;</td>
    <td width="80%">
      <table width="100%" cellpadding="3" cellspacing="0" border="0">
        <tr>
          <td width="18%"><html:checkbox property="low" value="true"/> <fmt:message key="resource.common.monitor.visibility.performance.Low"/></td>
          <td width="18%"><html:checkbox property="avg" value="true"/> <fmt:message key="resource.common.monitor.visibility.performance.Average"/></td>
          <td width="18%"><html:checkbox property="peak" value="true"/> <fmt:message key="resource.common.monitor.visibility.performance.Peak"/></td>
          <td colspan="5"><html:image page="/images/fb_redraw.gif" property="redraw" border="0" onmouseover="imageSwap(this, imagePath + 'fb_redraw', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_redraw', '');" onmousedown="imageSwap(this, imagePath +  'fb_redraw', '_down')"/></td>
          <td width="41%">&nbsp;</td>
        <html:hidden property="req"/>
        <html:hidden property="worst"/>
        <html:hidden property="pn"/>
        <html:hidden property="ps"/>
        <html:hidden property="so"/>
        <html:hidden property="sc"/>
      	</tr>
        <tr>
        <td colspan="5">
        <fmt:message key="resource.common.monitor.visibility.performance.sortText"/>
        </td>
        </tr>
      </table>      
    </td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
