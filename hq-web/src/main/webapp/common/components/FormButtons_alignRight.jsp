<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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


<tiles:importAttribute name="cancelOnly" ignore="true"/>

<!-- FORM BUTTONS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr>
    <td colspan="3" class="ToolbarLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr>
    <td width="50%">&nbsp;</td>
		<td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
		<td width="50%">
      <table width="100%" cellpadding="0" cellspacing="7" border="0">
        <tr>
<c:if test="${empty cancelOnly}">
          <td><html:link href="javascript:history.back(1)"><html:img page="/images/fb_ok.gif" border="0" titleKey="FormButtons.ClickToOk" onmouseover="imageSwap(this, imagePath + 'fb_ok', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_ok', '');" onmousedown="imageSwap(this, imagePath +  'fb_ok', '_down')"/></html:link></td>
          <td><html:img page="/images/spacer.gif" width="10" height="1" border="0"/></td>
		  <td><html:img page="/images/fb_reset.gif" border="0" titleKey="FormButtons.ClickToReset"  onmouseover="imageSwap(this, imagePath + 'fb_reset', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_reset', '');" onmousedown="imageSwap(this, imagePath + 'fb_reset', '_down')"/></td>
</c:if>
          <td><html:link href="javascript:history.back(1)"><html:img page="/images/fb_cancel.gif" border="0" titleKey="FormButtons.ClickToCancel" onmouseover="imageSwap(this, imagePath + 'fb_cancel', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_cancel', '');" onmousedown="imageSwap(this, imagePath + 'fb_cancel', '_down')"/></html:link></td>
		  <td width="100%"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<!-- /  -->
