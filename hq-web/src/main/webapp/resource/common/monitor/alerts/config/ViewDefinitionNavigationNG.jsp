<%@ page language="java" %>
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


<c:choose>
<c:when test="${not empty Resource}">
	
	<s:a action="listDefinitionsAlertsConfigPortal.action">
		<s:param name="mode" value="list"/>
		<s:param name="eid" value="%{#attr.Resource.entityId.appdefKey}"/>
		<fmt:message key="alert.config.props.ReturnLink"/>
	</s:a>
</c:when>
<c:when test="${not empty ResourceType}">

	<s:a action="listDefinitionsAlertsConfigPortal">
		<s:param name="mode" value="list"/>
		<s:param name="aetid" value="%{#attr.ResourceType.appdefTypeKey}"/>
		<fmt:message key="alert.config.props.ReturnLink"/>
	</s:a>
</c:when>
</c:choose>
<br><br>
