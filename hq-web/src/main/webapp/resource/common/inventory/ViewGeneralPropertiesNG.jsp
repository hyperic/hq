<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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


<!-- XXX: delete this set and add another set links -->

<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="resourceModifier" ignore="true"/>
<tiles:importAttribute name="groupType" ignore="true"/>
<tiles:importAttribute name="editUrl" ignore="true"/>

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="PLATFORM" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="SERVER" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE" var="SERVICE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION" var="APPLICATION" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP" var="GROUP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_DYNAMIC" var="DYNAMIC_GROUP" />

<c:if test="${not empty resourceModifier}">
  <hq:owner var="modifierStr" owner="${resourceModifier}"/>
</c:if>
<c:if test="${not empty resourceOwner}">
  <hq:owner var="ownerStr" owner="${resourceOwner}"/>
</c:if>

<!--  /  -->

<!--  TYPE AND HOST PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr>
    <td class="BlockLabel"><fmt:message key="common.label.Description"/>
    <td width="30%" class="BlockContent"><c:out value="${resource.description}" /></td>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.common.inventory.props.DateCreatedLabel"/></td>
    <td width="30%" class="BlockContent"><hq:dateFormatter value="${resource.CTime}"/></td>
  </tr>
  <tr>
    <c:choose>
      <c:when test="${resource.entityId.type == SERVER || resource.entityId.type == SERVICE}">
        <td colspan="2" class="BlockLabel">&nbsp;</td>
      </c:when>
      <c:otherwise>
        <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.LocationLabel"/>
        <td class="BlockContent"><c:out value="${resource.location}"/></td>
      </c:otherwise>
    </c:choose>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.DateModLabel"/></td>
    <td class="BlockContent"><hq:dateFormatter value="${resource.MTime}"/></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.security.ResourceTypeLabel"/></td>
    <td class="BlockContent">
      <c:choose>
      <c:when test="${resource.entityId.type == PLATFORM}">
        <c:out value="${resource.platformType.name}"/>
      </c:when>
      <c:when test="${resource.entityId.type == SERVER}">
        <c:out value="${resource.serverType.name}"/>
      </c:when>
      <c:when test="${resource.entityId.type == SERVICE}">
        <c:out value="${resource.serviceType.name}"/>
      </c:when>
      <c:when test="${resource.entityId.type == APPLICATION}">
        <fmt:message key="resource.type.Application"/>
      </c:when>
      <c:when test="${resource.entityId.type == GROUP && !(groupType == DYNAMIC_GROUP)}">
        <fmt:message key="resource.type.Group"/>
      </c:when>
      <c:when test="${resource.entityId.type == GROUP && groupType == DYNAMIC_GROUP}">
          <fmt:message key="resource.type.dynamicGroup"/>
      </c:when>
      </c:choose>
    </td>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.ModByLabel"/></td>
    <td class="BlockContent"><c:out value="${modifierStr}" escapeXml="false" /></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

<c:choose>
  <c:when test="${resource.entityId.type == PLATFORM}">
    <c:set var="entityIdType" value="platform"/>
  </c:when>
  <c:when test="${resource.entityId.type == SERVER}">
    <c:set var="entityIdType" value="server"/>
  </c:when>
  <c:when test="${resource.entityId.type == SERVICE}">
    <c:set var="entityIdType" value="service"/>
  </c:when>
  <c:when test="${resource.entityId.type == APPLICATION}">
    <c:set var="entityIdType" value="application"/>
    <c:set var="canModify" value="${useroperations['modifyApplication']}"/>
  </c:when>
  <c:when test="${resource.entityId.type == GROUP && !(groupType == DYNAMIC_GROUP)}">
    <c:set var="entityIdType" value="group"/>
    <c:set var="canModify" value="${(webUser.id == resourceOwner.id) || useroperations['modifyResourceGroup']}"/>
  </c:when>
</c:choose>

<c:if test="${empty editUrl}">
	<c:set var="editUrl" value="startEditPlatformGeneralProperties.action"/>
</c:if>
<c:url var="editUrl" value="${editUrl}">
	<c:param name="mode" value="edit"/>
	<c:param name="eid" value="${resource.entityId.appdefKey}"/>
</c:url>

<c:if test="${canModify}">
	<tiles:insertDefinition name=".toolbar.edit">
	  	<tiles:putAttribute name="editUrl" value="${editUrl}"/>
	</tiles:insertDefinition>
</c:if>
