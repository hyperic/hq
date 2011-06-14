<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="appdefResourceType" ignore="true"/>
<tiles:importAttribute name="childResourcesHealthKey"/>
<tiles:importAttribute name="childResourcesTypeKey"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="internal" ignore="true"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:if test="${empty mode}">
  <c:set var="mode" value="currentHealth"/>
</c:if>

<c:choose>
  <c:when test="${internal}">
    <c:set var="listMembersName" value="internalChildResources"/>
  </c:when>
  <c:otherwise>
    <c:set var="listMembersName" value="deployedChildResources"/>
  </c:otherwise>
</c:choose>

<c:if test="${checkboxes}">
  	<c:set var="widgetInstanceName" value="childResources"/>
	<jsu:script>
    	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
    	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	</jsu:script>
</c:if>

<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_AUTOGROUP" var="AUTOGROUP" />
<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_CLUSTER" var="CLUSTER" />
<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_SINGLETON" var="SINGLETON" />

<c:set var="useAvailStoplightDimensions" value="false" />
<c:if test="${useAvailStoplightDimensions}">
  <c:set var="availStoplightDimensions" value=" width=\"106\" " />
</c:if>

<c:forEach var="summary" items="${summaries}" varStatus="status">
  <c:url var="stoplightUrl" value="/resource/AvailStoplight">
    <c:choose>
      <c:when test="${summary.summaryType == CLUSTER}">
        <c:param name="eid" value="${summary.entityId.appdefKey}" />
        <c:param name="ctype" value="${summary.resourceType.appdefType}:${summary.resourceType.id}" />
      </c:when>
      <c:otherwise>
        <c:param name="rid" value="${Resource.id}" />
        <c:param name="type" value="${Resource.entityId.type}" />
        <c:param name="ctype" value="${summary.resourceType.appdefType}:${summary.resourceType.id}" />
      </c:otherwise>
    </c:choose>
  </c:url>

<div id="<c:out value="${summary.resourceType.name}"/>_menu" class="menu">
  <ul>
    <li><div class="BoldText"><fmt:message key="${childResourcesTypeKey}"/></div>
    <c:choose>
      <c:when test="${summary.summaryType == AUTOGROUP}">
        <fmt:message key="resource.common.monitor.health.autoGroupType"><fmt:param value="${summary.resourceType.name}"/></fmt:message>
      </c:when>
      <c:when test="${summary.summaryType == CLUSTER}">
        <fmt:message key="resource.common.monitor.health.clusterGroupType"><fmt:param value="${summary.resourceType.name}"/></fmt:message>
      </c:when>
      <c:otherwise>
        <c:out value="${summary.resourceType.name}"/>
      </c:otherwise>
    </c:choose>
    </li>
    <li>
    <div class="BoldText"><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></div>
    <img id="<c:out value="${summary.resourceType.name}"/>_menu_availabilityIcon" src="/images/progress-running.gif" original="<c:out value="${stoplightUrl}" escapeXml="false" />" <c:out value="${availStoplightDimensions}" escapeXml="false" /> border="0" height="12">
    </li>
    <c:if test="${not empty url}">
    <hr>
    <li>
        <a href="<c:out value="${url}"/>"><fmt:message key="resource.common.monitor.visibility.GoToResource"/></a>
    </li>
    </c:if>
  </ul>
</div>
<c:set var="count" value="${status.count}"/>
</c:forEach>

<c:if test="${count > 5}">
  <div class="scrollable">
</c:if>
<table border="0" cellpadding="1" cellspacing="0" id="ResourceTable" class="portletLRBorder" width="100%">
  <tr>
    <c:if test="${not empty summaries && checkboxes}">
    <td class="ListHeaderCheckbox" width="3%"><input type="checkbox" onclick="ToggleAllGroup(this, widgetProperties, '<c:out value="${listMembersName}"/>')" name="<c:out value="${listMembersName}"/>All"></td>
    </c:if>

    <td class="ListHeader" colspan="2" align="left"><fmt:message key="${childResourcesHealthKey}"/></td>

    <c:if test="${not empty summaries}">
    <td class="ListHeaderInactive"  width="24" align="center" nowrap><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></td>
    <td class="ListHeaderInactive" width="14">&nbsp;</td>
    </c:if>
  </tr>

<c:forEach var="summary" items="${summaries}">

<c:choose>
  <c:when test="${summary.summaryType == AUTOGROUP}">
    <c:url var="url" value="/resource/autogroup/monitor/Visibility.do">
      <c:param name="mode" value="${mode}" />
      <c:param name="eid" value="${Resource.entityId.type}:${Resource.id}"/>
      <c:choose>
        <c:when test="${not empty appdefResourceType && appdefResourceType == 4}"> <!-- AppdefEntityConstants.APPDEF_TYPE_APPLICATION-->
          <c:param name="ctype" value="3:${summary.resourceType.id}" />
        </c:when>
        <c:otherwise>
          <c:choose>
            <c:when test="${not empty childResourceType}">
              <c:param name="ctype" value="${childResourceType}:${summary.resourceType.id}" />
            </c:when>
          <c:otherwise>
            <c:param name="ctype" value="${summary.resourceType.id}"/>
          </c:otherwise>
        </c:choose>
        </c:otherwise>
      </c:choose>
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="url" value="/resource/${summary.entityId.typeName}/monitor/Visibility.do">
      <c:param name="mode" value="${mode}" />
      <c:param name="eid" value="${summary.entityId.appdefKey}" />
    </c:url>
    </c:otherwise>
  </c:choose>

  <tr class="ListRow">
    <c:if test="${checkboxes}">
    <td class="ListCellCheckbox"><html:multibox property="child" value="${summary.resourceType.appdefTypeKey}" styleClass="${listMembersName}" onchange="ToggleGroup(this, widgetProperties)"/></td>
    </c:if>

    <td width="1%" class="ListCellCheckbox">
    <c:choose>
      <c:when test="${summary.summaryType == AUTOGROUP}">
      <div class="autoGroupIcon">&nbsp;</div>
      </c:when>
      <c:when test="${summary.summaryType == CLUSTER}">
      <div class="clusterIcon">&nbsp;</div>
      </c:when>
      <c:otherwise>
      &nbsp;
      </c:otherwise>
    </c:choose>
    </td>
    <td class="ListCell">
    <c:choose>
      <c:when test="${empty url}">
        <c:out value="${summary.resourceType.name}"/>
      </c:when>
      <c:when test="${summary.summaryType == AUTOGROUP}">
        <a href="<c:out value="${url}" />"><c:out value="${summary.resourceType.name}"/></a>
      </c:when>
      <c:otherwise>
        <a href="<c:out value="${url}" />"><c:out value="${summary.entityName}"/></a>
      </c:otherwise>
    </c:choose>
    </td>
    <td class="ListCellCheckbox">
    <tiles:insert page="/resource/common/monitor/visibility/AvailIcon.jsp">
        <tiles:put name="availability" beanName="summary" beanProperty="availability" />
    </tiles:insert>
    </td>

    <td class="ListCellCheckbox resourceCommentIcon" 
    	onmouseover="var img = document.getElementById('<c:out value="${summary.resourceType.name}" />_menu_availabilityIcon'); if (img.src.indexOf('/images/progress-running.gif')) { img.setAttribute('src', img.getAttribute('original')); }; menuLayers.show('<c:out value="${summary.resourceType.name}" />_menu', event)" 
        onmouseout="menuLayers.hide()">
	&nbsp;
    </td>
  </tr>
    
    </c:forEach>

</table>
<c:if test="${count > 5}">
  </div>
</c:if>
<!--  /  -->

<c:if test="${empty summaries}">
  <tiles:insert definition=".resource.common.monitor.visibility.noHealths"/>
</c:if>