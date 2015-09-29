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

<tiles:importAttribute name="section" ignore="true"/>

<c:if test="${empty section}">
 <c:set var="section" value="server"/>
</c:if>
<c:set var="entityId" value="${Resource.entityId}"/>

<c:choose>
	<c:when test="${section eq 'platform'}">
 		<!--  PAGE TITLE -->
  		<tiles:insertDefinition name=".page.title.resource.platform.full">
   			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
   			<tiles:putAttribute name="eid" value="${entityId.appdefKey}"/>
  		</tiles:insertDefinition>
 		<!-- CONTROL BAR -->
 		<tiles:insertDefinition name="ng.tabs.resource.platform.control.list.detail">
  			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
 		</tiles:insertDefinition>
 	</c:when>
 	<c:when test="${section eq 'service'}">
 		<!--  PAGE TITLE -->
  		<tiles:insertDefinition name=".page.title.resource.service.full">
   			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
   			<tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
  		</tiles:insertDefinition>
 		<!-- CONTROL BAR -->
 		<tiles:insertDefinition name=".ng.tabs.resource.service.control.list.detail">
  			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
 		</tiles:insertDefinition>
 	</c:when>
 	<c:when test="${section eq 'group'}">
 		<!--  PAGE TITLE -->
  		<tiles:insertDefinition name=".page.title.resource.group.full">
   			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
   			<tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
  		</tiles:insertDefinition>
 		<!-- CONTROL BAR -->
 		<tiles:insertDefinition name=".ng.tabs.resource.group.control.current">
  			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
 		</tiles:insertDefinition>
 	</c:when>
 	<c:otherwise>
  		<!--  PAGE TITLE -->
  		<tiles:insertDefinition name=".page.title.resource.server.full">
   			<tiles:putAttribute name="resource" value="${Resource}"/>
   			<tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
   			<tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
   			<tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
			<tiles:putAttribute name="ignoreBreadcrumb" value="false"/>
  		</tiles:insertDefinition>
  		<!-- CONTROL BAR -->
  		<tiles:insertDefinition name=".ng.tabs.resource.server.control.list.detail">
   			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  		</tiles:insertDefinition>
 	</c:otherwise>
</c:choose>
<br>

<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<hq:constant symbol="CONTROL_ENABLED_ATTR" var="CONST_ENABLED" />
<c:choose>
 <c:when test="${requestScope[CONST_ENABLED]}">
<c:set var="curStatusTabKey" value="resource.${section}.ControlStatus.Tab"/>
<!-- CURRENT STATUS -->

<tiles:insertDefinition name=".ng.resource.common.control.currentStatus">
 <tiles:putAttribute name="tabKey" value="resource.${section}.ControlStatus.Tab"/>
 <tiles:putAttribute name="section" value="section"/>
</tiles:insertDefinition>
<br>

<!-- QUICK CONTROL -->
<c:set var="tmpQControl" value=".ng.resource.${section}.control.quickControl"/>

<tiles:insertDefinition name="${tmpQControl}">
 <tiles:putAttribute name="section" value="section"/>
</tiles:insertDefinition>


<br>

  <!-- CONTROL ACTION SCHEDULE- this tile does not exsist bacause ListScheduled.jsp doesn't exsist
  <c:if test="${hasControlActions}">
    <c:set var="tmpScheduled" value=".ng.resource.${section}.control.list.scheduled"/>
    <tiles:insertDefinition name="tmpScheduled">
      <tiles:putAttribute name="section" value="section"/>
    </tiles:insertDefinition>
  </c:if> -->

</c:when>
<c:otherwise>
 <c:choose>
  <c:when test="${section eq 'group'}">
   <c:set var="tmpMessage" >
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/>
   </c:set> 
  </c:when>
  <c:otherwise>
   <c:url var="enableControlLink" value="/resource/${section}/Inventory.do">
    <c:param name="mode" value="editConfig"/>
    <c:param name="rid" value="${Resource.id}"/>
    <c:param name="type" value="${Resource.entityId.type}"/>
   </c:url>
   <c:set var="tmpMessage">
    <fmt:message key="resource.common.control.NotEnabled.ControlNotEnabled"/> <fmt:message key="resource.common.control.NotEnabled.ToEnable"/> <s:a href="%{enableControlLink}"><fmt:message key="resource.common.control.NotEnabled.ConfPropLink"/></s:a> <fmt:message key="resource.common.control.NotEnabled.InInventory"/>
   </c:set>
  </c:otherwise>
 </c:choose>
   <tiles:insertDefinition name=".ng.portlet.notenabled">
    <tiles:putAttribute name="message" value="tmpMessage"/>
   </tiles:insertDefinition>

</c:otherwise>
</c:choose>

<br>

<tiles:insertDefinition name=".page.footer"/>
