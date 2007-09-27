<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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
<tiles:insert definition=".page.title.events.noresource">
  <tiles:put name="titleKey" value="alert.current.detail.noresource.PageTitle"/>
  <tiles:put name="titleName" beanName="ResourceType" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${empty Resource}">
<tiles:insert definition=".page.title.events.noresource">
  <tiles:put name="titleKey" value="alert.current.detail.noresource.PageTitle"/>
  <tiles:put name="titleName" value="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${1 == Resource.entityId.type}">
<tiles:insert definition=".page.title.events.platform">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${2 == Resource.entityId.type}">
<tiles:insert definition=".page.title.events.server">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${3 == Resource.entityId.type}">
<tiles:insert definition=".page.title.events.service">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${4 == Resource.entityId.type}">
<tiles:insert definition=".page.title.events.application">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="alertDef" beanProperty="name"/>
</tiles:insert>
</c:when>
<c:when test="${5 == Resource.entityId.type}">
<tiles:insert definition=".page.title.events.group">
  <tiles:put name="titleKey"><c:out value="${titleKey}"/></tiles:put>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="GroupAlertDefinitionForm" beanProperty="name"/>
</tiles:insert>
</c:when>
</c:choose>
