<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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


<tiles:importAttribute name="availablePortlets"/>
<tiles:importAttribute name="wide"/>
<tiles:importAttribute name="portlets"/>
<jsu:script>
	<c:choose>
		<c:when test="${wide}">
  			function isWide(portlet) {
		</c:when>
		<c:otherwise>
  			function isNarrow(portlet) {
		</c:otherwise>
	</c:choose>
    <c:forEach var="portlet" items="${portlets}">
      	if (portlet == '<c:out value="${portlet}"/>')
        	return true;
    </c:forEach>
    	return false;
  	}

  	// Check if a valid portlet has been selected. NOTE: The function name must be unique, since it's included
  	// more than once on the same rendered page.
	  function isDivAddContents<c:out value="${wide}" />PortletValid() {
	    portlet = document.getElementById('addContentsPortlet<c:out value="${wide}" />').getElementsByTagName("select")[0].value
	    if (portlet == "bad") { // this is default setting, which must not be allowed through
	      return false
	    } else {
	      return true
	    }
	  }
</jsu:script>

<c:choose>
<c:when test="${not empty availablePortlets }">
  <div id="addContentsPortlet<c:out value="${wide}"/>" class="effectsPortlet">
</c:when>
<c:otherwise>
  <div id="addContentsPortlet<c:out value="${wide}"/>" class="effectsPortlet" style="visibility: hidden">
</c:otherwise>
</c:choose>
<c:set var="selectedDashboardId" value="${sessionScope['.user.dashboard.selected.id']}"/>
<form method="POST" action="<html:rewrite page="/app/dashboard/${selectedDashboardId}/portlets" />" onsubmit="return isDivAddContents<c:out value='${wide}' />PortletValid()">
	<html:hidden property="wide" value="${wide}"/>
  	<table cellpadding="0" cellspacing="0" border="0" width="100%">
    <tr valign="top">
      <td colspan="3" class="ToolbarLine"><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
    </tr>
    <tr valign="top">
      <td colspan="3"><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
    </tr>
    <tr>
      <td colspan="3" class="FormLabel"><fmt:message key="dash.home.FormLabel.AddContent"/></td>
    </tr>
    <tr>
      <td valign="center">
        <select name="portlet">
          <option value="bad" SELECTED>
						<fmt:message key="dash.home.AddContent.select" />
					</option>
					<c:forEach var="portlet" items="${availablePortlets}" >                                                
             <option value="<c:out value="${portlet}"/>"><fmt:message key="${portlet}" /></option>
          </c:forEach>           
        </select>
      </td>
      <td>&nbsp;</td>
      <td width="100%"><html:image page="/images/4.0/icons/add.png" border="0" titleKey="FormButtons.ClickToOk" property="ok"/></td>
    </tr>
  </table>
</form>
</div>
