<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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

 <% 
	String agentType = request.getHeader("User-Agent");
	boolean iemode=false;
	if (agentType.contains("MSIE")) {
		iemode = true;
	}
 %>

<% if (!iemode) { %>
<c:if test="${empty portletErrorMessage}"> 
	<c:set var="portletErrorMessage">
		<s:fielderror />
	</c:set>
</c:if>

<c:if test="${empty portletErrorMessage}"> 
	<c:set var="portletErrorMessage">
		<s:actionerror />
	</c:set>
</c:if>

<c:if test="${empty portletErrorMessage}"> 
	<c:set var="portletErrorMessage">
		<s:if test="%{customActionErrorMessagesForDisplay != null && customActionErrorMessagesForDisplay.length() > 0}">
			<ul class="errorMessage">
				<li><span><s:property value="customActionErrorMessagesForDisplay" /> </span></li>
			</ul>
		</s:if>	
	</c:set>
</c:if>

<c:if test="${not empty portletErrorMessage}"> 
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock" width="100%"><c:out value="${portletErrorMessage}" escapeXml="false"/></td>
  </tr>
</table>
</c:if>
<c:if test="${not empty IsResourceUnconfigured}"> 
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	  <tr>
		<td class="ErrorBlock"><img src='<s:url value="/images/tt_error.gif" />'  width="10" height="11" alt="" border="0"/></td>
		<td class="ErrorBlock" width="100%"><c:out value="${isResourceConfiguredError}" escapeXml="false"/> <a href='<s:url value="%{#attr.isResourceConfiguredErrorAction}" />'><fmt:message key="resource.common.control.NotEnabled.ConfPropLink"/> </a>	 </td>
	  </tr>
</table>
</c:if>
<% } else { %>
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<s:if test="hasErrors()">
		<s:iterator value="fieldErrors.values" var="it">
			<s:iterator value="it">
			  <tr>
				<td class="ErrorBlock"><img src='<s:url value="/images/tt_error.gif" />'  width="10" height="11" alt="" border="0"/></td>
				<td class="ErrorBlock" width="100%"><s:property /></td>
			  </tr>
		  </s:iterator>
		</s:iterator>
</s:if>
<s:if test="hasActionErrors()">
		<s:iterator value="actionErrors" var="it">
			  <tr>
				<td class="ErrorBlock"><img src='<s:url value="/images/tt_error.gif" />'  width="10" height="11" alt="" border="0"/></td>
				<td class="ErrorBlock" width="100%"><s:property /></td>
			  </tr>
		</s:iterator>
</s:if>
<s:if test="hasCustomErrorMessages()">

		<s:iterator value="customActionErrorMessages" var="it">
			  <tr>
				<td class="ErrorBlock"><img src='<s:url value="/images/tt_error.gif" />'  width="10" height="11" alt="" border="0"/></td>
				<td class="ErrorBlock" width="100%"><s:property /></td>
			  </tr>
		</s:iterator>
</s:if>
</table>
<c:if test="${not empty IsResourceUnconfigured}"> 
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	  <tr>
		<td class="ErrorBlock"><img src='<s:url value="/images/tt_error.gif" />'  width="10" height="11" alt="" border="0"/></td>
		<td class="ErrorBlock" width="100%"><c:out value="${isResourceConfiguredError}" escapeXml="false"/> <a href='<s:url value="%{#attr.isResourceConfiguredErrorAction}" />'><fmt:message key="resource.common.control.NotEnabled.ConfPropLink"/> </a>	 </td>
	  </tr>
</table>
</c:if>


<% } %>
