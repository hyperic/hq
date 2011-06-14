<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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

<jsu:script>
	var isButtonClicked = false;
  
  	function checkSubmit() {
    	if (isButtonClicked) {
      		alert('<fmt:message key="error.PreviousRequestEtc"/>');
      		return false;
    	}
  	}
</jsu:script>
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="buttonTable">
  <tr>
    <td colspan="2"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="ToolbarLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr align=left valign=bottom>
    <td width="20%">&nbsp;</td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="10" border="0">
        <tr>
          <td><html:image page="/images/fb_delete.gif" border="0" titleKey="FormButtons.ClickToDelete" property="org.apache.struts.taglib.html.DELETE" onmouseover="imageSwap(this, imagePath + 'fb_delete', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_delete', '');" onmousedown="imageSwap(this, imagePath + 'fb_delete', '_down')" onclick="checkSubmit(); isButtonClicked=true;"/></td>
          <td><html:img page="/images/spacer.gif" width="10" height="1" border="0"/></td>
          <td><html:image page="/images/fb_cancel.gif" border="0" titleKey="FormButtons.ClickToCancel" property="cancel" onmouseover="imageSwap(this, imagePath + 'fb_cancel', '_over');" onmouseout="imageSwap(this, imagePath + 'fb_cancel', '');" onmousedown="imageSwap(this, imagePath + 'fb_cancel', '_down')"/></td>
          <td width="100%"><html:img page="/images/spacer.gif" width="16" height="1" border="0"/></td>
        </tr>
      </table>
    </td>
  </tr>
</table>
