<%@ page language="java" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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


<tiles:importAttribute name="titleKey" ignore="true"/>

<c:choose>
<c:when test="${not empty ResourceType}">
<tiles:insertDefinition name=".page.title.events.noresource">
  <tiles:putAttribute name="titleKey" value="alert.current.detail.noresource.PageTitle"/>
  <tiles:putAttribute name="titleName" value="${ResourceType.name}"/>
</tiles:insertDefinition>
</c:when>
<c:when test="${empty Resource}">
<tiles:insertDefinition name=".page.title.events.noresource">
  <tiles:putAttribute name="titleKey" value="alert.current.detail.noresource.PageTitle"/>
  <tiles:putAttribute name="titleName" value="${alertDef.name}"/>
</tiles:insertDefinition>
</c:when>
<c:when test="${1 == Resource.entityId.type}">
<tiles:insertDefinition name=".page.title.events.platform">
  <tiles:putAttribute name="titleKey"><c:out value="${titleKey}"/></tiles:putAttribute>
  <tiles:putAttribute name="titleName" value="${Resource.name}"/>
  <tiles:putAttribute name="subTitleName" value="${alertDef.name}"/>
</tiles:insertDefinition>
</c:when>
<c:when test="${2 == Resource.entityId.type}">
<tiles:insertDefinition name=".page.title.events.server">
  <tiles:putAttribute name="titleKey"><c:out value="${titleKey}"/></tiles:putAttribute>
  <tiles:putAttribute name="titleName" value="${Resource.name}"/>
  <tiles:putAttribute name="subTitleName" value="${alertDef.name}"/>
</tiles:insertDefinition>
</c:when>
<c:when test="${3 == Resource.entityId.type}">
<tiles:insertDefinition name=".page.title.events.service">
  <tiles:putAttribute name="titleKey"><c:out value="${titleKey}"/></tiles:putAttribute>
  <tiles:putAttribute name="titleName" value="${Resource.name}"/>
  <tiles:putAttribute name="subTitleName" value="${alertDef.name}"/>
</tiles:insertDefinition>
</c:when>
<c:when test="${4 == Resource.entityId.type}">
<tiles:insertDefinition name=".page.title.events.application">
  <tiles:putAttribute name="titleKey"><c:out value="${titleKey}"/></tiles:putAttribute>
  <tiles:putAttribute name="titleName" value="${Resource.name}"/>
  <tiles:putAttribute name="subTitleName" value="${alertDef.name}"/>
</tiles:insertDefinition>
</c:when>
<c:when test="${5 == Resource.entityId.type}">
<tiles:insertDefinition name=".page.title.events.group">
  <tiles:putAttribute name="titleKey"><c:out value="${titleKey}"/></tiles:putAttribute>
  <tiles:putAttribute name="titleName" value="${Resource.name}"/>
  <tiles:putAttribute name="subTitleName" value="${GroupAlertDefinitionForm.name}"/>
</tiles:insertDefinition>
</c:when>
</c:choose>
