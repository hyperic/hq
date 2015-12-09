<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
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


<%-- if the attributes are not available, we can't display this tile: an error probably occured --%>
<c:choose>
<c:when test="${AvailableUsers == null}">
<!-- error occured -->
<tiles:insertTemplate template="/common/NoRightsNG.jsp"/>
</c:when>
<c:otherwise>

<s:form name="AddAlertUsersForm" method="POST" action="addAlertUsersAction">

<tiles:insertDefinition name=".page.title.events">
  <tiles:putAttribute name="titleKey" value="alert.config.edit.AddNotifications"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".portlet.error"/>

<c:set var="availListSize" value="${AvailableUsers.getTotalSize()}" />
<c:set var="pendListSize" value="${PendingUsers.getTotalSize()}" />
<tiles:insertTemplate template="/resource/common/monitor/alerts/config/DefinitionUsersFormNG.jsp">
  <tiles:putAttribute name="availableUsers" value="${AvailableUsers}"/>
  <tiles:putAttribute name="numAvailableUsers" value="${availListSize}"/>
  <tiles:putAttribute name="pendingUsers" value="${PendingUsers}"/>
  <tiles:putAttribute name="numPendingUsers" value="${pendListSize}"/>
</tiles:insertTemplate>

<tiles:insertDefinition name=".form.buttons">
    <tiles:putAttribute name="addToList" value="true"/>
	<tiles:putAttribute name="cancelAction"  value="cancelAlertUsersAction" />
	<tiles:putAttribute name="resetAction"  value="resetAlertUsersAction" />
</tiles:insertDefinition>

<tiles:insertDefinition name=".page.footer"/>
<s:hidden theme="simple" name="ad"    value="%{#attr.ad}"/>
<s:hidden theme="simple" name="rid"   value="%{#attr.rid}"/>
<s:hidden theme="simple" name="type"  value="%{#attr.type}"/>
<s:hidden theme="simple" name="aetid" value="%{#attr.aetid}"/>

</s:form>

</c:otherwise>
</c:choose>
