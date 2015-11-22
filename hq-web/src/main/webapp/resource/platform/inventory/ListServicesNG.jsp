<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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


<hq:pageSize var="pageSize"/>

<tiles:importAttribute name="selfAction"/>
<tiles:importAttribute name="ctype" ignore="true"/>
<tiles:importAttribute name="services"/>
<tiles:importAttribute name="ServicesTotalSize"/>


<c:choose>
  <c:when test="${empty ctype}">
    <c:url var="newServiceUrl" value="startPlatformAddNewService.action">
    	<c:param name="mode" value="new"/>
    	<c:param name="eid" value="${Resource.entityId.type}:${Resource.id}"/>
		<c:param name="rid" value="${Resource.id}"/>
		<c:param name="type" value="${Resource.entityId.type}"/>
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="newServiceUrl" value="startPlatformAddNewService.action">
    	<c:param name="mode" value="new"/>
    	<c:param name="ctype" value="${ctype}"/>
    	<c:param name="eid" value="${Resource.entityId.type}:${Resource.id}"/>
		<c:param name="rid" value="${Resource.id}"/>
		<c:param name="type" value="${Resource.entityId.type}"/>
    </c:url>
  </c:otherwise>
</c:choose>

<c:set var="widgetInstanceName" value="listServices"/>

<c:url var="pssAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.resourceType}">
    <c:param name="resourceType" value="${param.resourceType}"/>
  </c:if>
</c:url>

<c:url var="pnsAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.resourceType}">
    <c:param name="resourceType" value="${param.resourceType}"/>
  </c:if>
</c:url>

<c:url var="ssAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.resourceType}">
    <c:param name="resourceType" value="${param.resourceType}"/>
  </c:if>
</c:url>

<c:url var="fsAction" value="${selfAction}"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	<c:out value="wp"/> = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>


<tiles:insertDefinition name=".toolbar.filter.resource">
  <tiles:putAttribute name="defaultKey" value="resource.hub.filter.AllServiceTypes"/>
  <tiles:putAttribute name="optionsProperty" value="${RemoveResourceForm.resourceTypes}"/>
  <tiles:putAttribute name="filterAction" value="${fsAction}"/>
  <tiles:putAttribute name="filterParam" value="resourceType"/>
</tiles:insertDefinition>

<!--  GENERAL PROPERTIES TITLE -->
<!--  /  -->

<!-- tiles:insert page="View_FilterToolbar.jsp"/ -->

<!--  SERVICES CONTENTS -->
  <display:table items="${services}" cellspacing="0" cellpadding="0" width="100%" action="${ssAction}" var="service" pageSizeValue="ps" pageSize="${param.ps}" pageValue="pn" page="${param.pn}" orderValue="so" order="${param.so}" sortValue="sc" sort="${param.sc}">

    <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, wp)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
      <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, wp)" styleClass="listMember"/>
    </display:column>
    <display:column width="55%" property="name" sort="true" sortAttr="5"
                    defaultSort="true" title="resource.server.inventory.services.ServiceTH" 
                    href="viewResourceInventoryServiceVisibility.action?mode=view&rid=${service.id}&type=${service.entityId.type}" />
       
    <display:column width="30%" property="description" title="common.header.Description" />
    <display:column property="id" title="resource.common.monitor.visibility.AvailabilityTH" width="15%" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle">
      <display:availabilitydecorator resource="${service}"/>
    </display:column>
  </display:table>

<!--  /  -->
<tiles:insertDefinition name=".toolbar.list">
  <tiles:putAttribute name="listNewUrl" value="${newServiceUrl}"/>
  <tiles:putAttribute name="deleteOnly"><c:out value="${!useroperations['createService']}"/>"</tiles:putAttribute>
  <tiles:putAttribute name="listItems" value="${services}"/>
  <tiles:putAttribute name="listSize" value="${ServicesTotalSize}"/>
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="pageSizeAction" value="${pssAction}"/>
  <tiles:putAttribute name="pageSizeParam" value="ps"/>
  <tiles:putAttribute name="pageNumAction" value="${pnsAction}"/>
  <tiles:putAttribute name="pageNumParam" value="pn"/>
  <tiles:putAttribute name="defaultSortColumn" value="5"/>
</tiles:insertDefinition>



