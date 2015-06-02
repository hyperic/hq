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
<jsu:importScript path="/js/functions.js" />
<jsu:importScript path="/js/listWidget.js" />

<c:set var="widgetInstanceName" value="listResources"/>
<jsu:script>
	var pageData = new Array();
	var FOO = "chart";
	var LIST  = "list"; 
	var imagePath = "/images/";
	
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<hq:constant var="PLATFORM"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM"/>
<hq:constant var="SERVER"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER"/>
<hq:constant var="SERVICE" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE"/>
<hq:constant var="APPLICATION"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION"/>
<hq:constant var="GROUP" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP"/>
<hq:constant var="GROUP_COMPAT" 
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalActionNG"
    symbol="SELECTOR_GROUP_COMPAT"/>
<hq:constant var="GROUP_ADHOC" 
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalActionNG"
    symbol="SELECTOR_GROUP_ADHOC"/>
<hq:constant var="CHART"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubFormNG"
	symbol="CHART_VIEW"/>
<hq:constant var="LIST"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubFormNG"
	symbol="LIST_VIEW"/>

<c:choose>
  <c:when test="${ff == PLATFORM}">
    <fmt:message var="entityTypeTH" key="resource.type.Platform"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.PlatformTypeTH"/>
  </c:when>
  <c:when test="${ff == SERVER}">
    <fmt:message var="entityTypeTH" key="resource.type.Server"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.ServerTypeTH"/>
  </c:when>
  <c:when test="${ff == SERVICE}">
    <fmt:message var="entityTypeTH" key="resource.type.Service"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.ServiceTypeTH"/>
  </c:when>
  <c:when test="${ff == APPLICATION}">
    <fmt:message var="entityTypeTH" key="resource.type.Application"/>
  </c:when>
  <c:when test="${ff == GROUP}">
    <fmt:message var="entityTypeTH" key="resource.type.Group"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.GroupTypeTH"/>
  </c:when>
</c:choose>

<s:form name="ResourceHubForm" action="removeResource">

<tiles:insertDefinition name=".page.title.resource.hub">
  <tiles:putAttribute name="titleName"><span id="browseFilters"><c:out value="${navHierarchy}" escapeXml="false" /></span></tiles:putAttribute>
</tiles:insertDefinition>

<c:if test="${not empty ResourceSummary}">
	<div class="hubContainer">
	<table width="100%" cellpadding="0" cellspacing="0" border="0">
	  <tr>
		<td class="ResourceHubBlockTitle" width="100%">
	<c:choose>
	  <c:when test="${ff == PLATFORM}">
		  <fmt:message key="resource.hub.filter.platform"/> (<c:out value="${ResourceSummary.platformCount}"/>)
	  </c:when>
	  <c:otherwise>
		<c:url var="platformUrl" value="/resourceHub.action">
		  <c:param name="ff" value="${PLATFORM}"/>
		  <c:param name="view" value="${view}"/>
		  <c:if test="${not empty param.keywords}">
			<c:param name="keywords" value="${param.keywords}"/>
		  </c:if>
		</c:url>
		<s:a href="%{#attr.platformUrl}"><fmt:message key="resource.hub.filter.platform"/> (<c:out value="${ResourceSummary.platformCount}"/>)</s:a>
	  </c:otherwise>
	</c:choose>
	<fmt:message key="common.label.Pipe"/>
	<c:choose>
	  <c:when test="${ff == SERVER}">
		  <fmt:message key="resource.hub.filter.server"/> (<c:out value="${ResourceSummary.serverCount}"/>)
	  </c:when>
	  <c:otherwise>
		<c:url var="serverUrl" value="/resourceHub.action">
		  <c:param name="ff" value="${SERVER}"/>
		  <c:param name="view" value="${view}"/>
		  <c:if test="${not empty param.keywords}">
			<c:param name="keywords" value="${param.keywords}"/>
		  </c:if>
		</c:url>
		<s:a href="%{#attr.serverUrl}"><fmt:message key="resource.hub.filter.server"/> (<c:out value="${ResourceSummary.serverCount}"/>)</s:a>
	  </c:otherwise>
	</c:choose>
	<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ff == SERVICE}">
      <fmt:message key="resource.hub.filter.service"/> (<c:out value="${ResourceSummary.serviceCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="serviceUrl" value="/resourceHub.action">
      <c:param name="ff" value="${SERVICE}"/>
      <c:param name="view" value="${view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <s:a href="%{#attr.serviceUrl}"><fmt:message key="resource.hub.filter.service"/> (<c:out value="${ResourceSummary.serviceCount}"/>)</s:a>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ff == GROUP && g == GROUP_COMPAT}">
      <fmt:message key="resource.hub.filter.compatibleGroups"/> (<c:out value="${ResourceSummary.compatGroupCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="groupUrl" value="/resourceHub.action">
      <c:param name="ff" value="${GROUP}"/>
      <c:param name="g" value="${GROUP_COMPAT}"/>
      <c:param name="view" value="${view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <s:a href="%{#attr.groupUrl}"><fmt:message key="resource.hub.filter.compatibleGroups"/> (<c:out value="${ResourceSummary.compatGroupCount}"/>)</s:a>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ff == GROUP && g == GROUP_ADHOC}">
      <fmt:message key="resource.hub.filter.mixedGroups"/> (<c:out value="${ResourceSummary.groupCountAdhocGroup + ResourceSummary.groupCountAdhocPSS + ResourceSummary.groupCountAdhocApp}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="groupUrl" value="/resourceHub.action">
      <c:param name="ff" value="${GROUP}"/>
      <c:param name="g" value="${GROUP_ADHOC}"/>
      <c:param name="view" value="${view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <s:a href="%{#attr.groupUrl}"><fmt:message key="resource.hub.filter.mixedGroups"/> (<c:out value="${ResourceSummary.groupCountAdhocGroup + ResourceSummary.groupCountAdhocPSS + ResourceSummary.groupCountAdhocApp}"/>)</s:a>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ff == APPLICATION}">
      <fmt:message key="resource.hub.filter.application"/> (<c:out value="${ResourceSummary.applicationCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="appUrl" value="/resourceHub.action">
      <c:param name="ff" value="${APPLICATION}"/>
      <c:param name="view" value="${view}"/>
      <c:param name="pn" value="0"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <s:a href="%{#attr.appUrl}"><fmt:message key="resource.hub.filter.application"/> (<c:out value="${ResourceSummary.applicationCount}"/>)</s:a>
  </c:otherwise>
</c:choose>
    </td>
  </tr>
</table>
</c:if>
<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<!--  FILTER TOOLBAR CONTENTS -->
<s:hidden theme="simple" name="view"/>

<c:url var="ftAction" value="/resourceHub.action">
  <c:if test="${not empty param.keywords}">
    <c:param name="keywords" value="${param.keywords}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.g}">
    <c:param name="g" value="${param.g}"/>
  </c:if>
  <c:param name="ff" value="${ff}"/>
  <c:param name="view" value="${view}"/>
</c:url>

<c:url var="fgAction" value="/resourceHub.action">
  <c:if test="${not empty param.keywords}">
    <c:param name="keywords" value="${param.keywords}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.g}">
    <c:param name="g" value="${param.g}"/>
  </c:if>
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
  </c:if>
  <c:param name="ff" value="${ff}"/>
  <c:param name="view" value="${view}"/>
</c:url>

<c:choose>
  <c:when test="${not empty keywords}">
    <c:set var="initSearchVal" value="${keywords}"/>
  </c:when>
  <c:otherwise>
    <fmt:message var="initSearchVal" key="resource.hub.search.KeywordSearchText"/>
  </c:otherwise>
</c:choose>
<div class="FilterImage" style="padding: 4px;text-align: right;">
	<c:choose>
		<c:when test="${view == LIST}">
	    	<input type="button" id="showChartBtn" class="button42"  
				       value="<fmt:message key='resource.view.chart' />" 
				       onclick="ResourceHubForm.view.value = 'chart'; ResourceHubForm.submit(); return false;" />
		</c:when>
	  	<c:when test="${view == CHART}">
	    	<input type="button" id="showChartBtn" class="button42"  
				       value="<fmt:message key='resource.view.list' />" 
				       onclick="ResourceHubForm.view.value = 'list'; ResourceHubForm.submit(); return false;" />
	  	</c:when>
	</c:choose>
</div>
<!--  /  -->
<!--  RESOURCE HUB CONTENTS -->
<c:url var="sAction" value="/resourceHub.action">
  	<c:if test="${not empty param.keywords}">
    	<c:param name="keywords" value="${param.keywords}"/>
  	</c:if>
  	<c:if test="${not empty param.ps}">
    	<c:param name="ps" value="${param.ps}"/>
  	</c:if>
  	<c:if test="${not empty param.pn}">
    	<c:param name="pn" value="${param.pn}"/>
  	</c:if>
  	<c:if test="${not empty param.ft}">
    	<c:param name="ft" value="${param.ft}"/>
  	</c:if>
  	<c:if test="${not empty param.g}">
    	<c:param name="g" value="${param.g}"/>
  	</c:if>
  	<c:param name="ff" value="${ff}"/>
  	<c:param name="view" value="${view}"/>
</c:url>
<c:choose>
  <c:when test="${view == LIST}">
  	<jsu:script>
      	function refreshAvail() {
	        var now = new Date()
	    <c:forEach var="resource" items="${AllResources}">
	        <c:out value="document.avail${resource.entityId.id}.src"/> =
	          '<s:url action="/resource/Availability"/>?timeout=30000&eid=' +
	          '<c:out value="${resource.entityId.appdefKey}"/>#' + now.valueOf();
	    </c:forEach>
	        setAvailRefresh()
	      }
	      
	      function setAvailRefresh() {
	        setTimeout( "refreshAvail()", 60*1000 );
	      }
	</jsu:script>
	<jsu:script onLoad="true">	      
   	  	setAvailRefresh();
	</jsu:script>
	<c:choose>
    <c:when test="${empty Indicators || empty AllResources}">
    <display:table items="${AllResources}" var="resource" action="${sAction}" width="100%" cellspacing="0" cellpadding="0">
      <display:column width="1%" property="entityId.appdefKey" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
        <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
      </display:column>
      <display:column width="5%" property="id" title="nbsp" nowrap="true">
        <display:quicknavdecorator resource="${resource}"/>
      </display:column>
      <display:column width="30%" property="name" title="${entityTypeTH}" isLocalizedTitle="false" href="/Resource.action?eid=${resource.entityId.appdefKey}" sort="true" sortAttr="5" defaultSort="true"/>
      <c:if test="${not empty resourceTypeTH}">
      <display:column width="30%" property="id" title="${resourceTypeTH}" isLocalizedTitle="false">
        <display:resourcedecorator resource="${resource}" type="true"/>
      </display:column>
      </c:if>
      <display:column width="24%" property="description" title="common.header.Description"/>
    <c:if test="${ff == GROUP}">
      <display:column property="totalSize" title="common.header.Members" sortAttr="1" align="center"/>
    </c:if>
      <display:column width="10%" property="id" title="resource.common.monitor.visibility.AvailabilityTH" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle" sortAttr="1">
        <display:availabilitydecorator resource="${resource}"/>
      </display:column>
    </display:table>
    </c:when>
	<c:otherwise>
    <table class="table" width="100%" border="0" cellspacing="0" cellpadding="0">
    <tr class="tableRowHeader">

    <th width="1%" class="ListHeaderCheckbox"><input type="checkbox" onclick="ToggleAll(this, widgetProperties)" name="listToggleAll"></th>
    <th width="5%" class="tableRowInactive">&nbsp;</th>
    <th width="30%" class="tableRowSorted">
    	<c:choose>
    		<c:when test="${param.so == 'dec'}">
    			<c:url var="sortedAction" value="${sAction}">
    				<c:param name="so" value="asc" />
    			</c:url>
    			<a href="${sortedAction}">${entityTypeTH}<img border="0" src='<s:url value="/images/tb_sortdown.gif"/>'/></a>
    		</c:when>
    		<c:otherwise>
    			<c:url var="sortedAction" value="${sAction}">
    				<c:param name="so" value="dec" />
    			</c:url>
    			<a href="${sortedAction}">${entityTypeTH}<img border="0" src='<s:url value="/images/tb_sortup.gif"/>'/></a>
    		</c:otherwise>
    	</c:choose>
    </th>
	    <c:forEach items="${Indicators}" var="indicator">
	      	<th class="ListHeaderCheckbox">${indicator.name}</th>
	    </c:forEach>
	  	<th class="ListHeaderCheckbox"><fmt:message key="resource.common.monitor.visibility.AvailabilityTH"/></th>
    </tr>
	<c:forEach items="${AllResources}" var="resource">
    <tr class="tableRowOdd">
    <td class="ListCellCheckbox" align="left" valign="top"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties)" class="listMember" name="resources" value="<c:out value="${resource.entityId}"/>"></td>
    <td class="tableCell" align="left" nowrap valign="top">
<!-- todo fix-->  
  <s:a action="/resource/%{#attr.section}/monitor/Visibility" >
    	<s:param name="eid" value="%{#attr.resource.entityId}" />
    	<s:param name="mode" value="currentHealth" />
    	<img src='<s:url value="/images/icon_hub_m.gif"/>' width="11" height="11" alt="" border="0"/>
    </s:a>
    <s:a action="/resource/%{#attr.section}/Inventory" >
	<s:param name="eid" value="%{#attr.resource.entityId}" />
    	<s:param name="mode" value="view"/>
    	<img src='<s:url value="/images/icon_hub_i.gif"/>' width="11" height="11" alt="" border="0"/>
    </s:a>
    <c:if test="${resource.entityId.type == 1 || resource.entityId.type == 2 || resource.entityId.type == 3}">
    	<s:a action="/alerts/Config" >
			<s:param name="eid" value="%{#attr.resource.entityId}" />
    		<s:param name="mode" value="list"/>
    		<img src='<s:url value="/images/icon_hub_a.gif"/>' width="11" height="11" alt="" border="0"/>
    	</s:a>
    </c:if>
<!-- todo fix-->  	
    </td>
      <td class="tableCell" align="left" valign="top">
      	<span class="SpanPopup1">
      		<s:a action="/Resource" >
			<s:param name="eid" value="%{#attr.resource.entityId}" />
			${resource.name}</s:a><c:if test="${not empty resource.description}"><span><c:out value="${resource.description}" /></span></c:if></span></td>
      <!-- Insert metrics tile here -->
      <c:forEach items="${indicatorsMap[resource.entityId]}" var="metric">
        <td class="tableCell" align="middle" valign="top">
        <c:choose>
        <c:when test="${not empty metric}">
            <c:out value="${metric}"/>
        </c:when>
        <c:otherwise>&nbsp;</c:otherwise>
        </c:choose>
        </td>
      </c:forEach>
      <td class="tableCell" align="middle" valign="top">
		<img imageName="avail${resource.id}" src='<s:url value="/resource/Availability?timeout=30000" ><s:param name="eid" >resource.entityId</s:param></s:url>'/>
      </td>
    </tr>
    </c:forEach>
    </table>
	</c:otherwise>
	</c:choose>
	</c:when>
	  <c:otherwise>
		<tiles:insertDefinition name=".resource.common.monitor.visibility.minicharts">
		  <tiles:putAttribute  name="Resources" value="${AllResources}"/>
		</tiles:insertDefinition>
  </c:otherwise>
</c:choose>	
<c:url var="psAction" value="resourceHub.action">
  <c:if test="${not empty param.keywords}">
    <c:param name="keywords" value="${param.keywords}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
  </c:if>
  <c:if test="${not empty param.g}">
    <c:param name="g" value="${param.g}"/>
  </c:if>
  <c:if test="${not empty param.fg}">
    <c:param name="fg" value="${param.fg}"/>
  </c:if>
  <c:if test="${not empty param.unavail}">
    <c:param name="unavail" value="${param.unavail}"/>
  </c:if>
  <c:if test="${not empty param.own}">
    <c:param name="own" value="${param.own}"/>
  </c:if>
  <c:if test="${not empty param.any}">
    <c:param name="any" value="${param.any}"/>
  </c:if>
  <c:param name="ff" value="${ff}"/>
  <c:param name="view" value="${view}"/>
</c:url>

<c:url var="pnAction" value="resourceHub.action">
  <c:if test="${not empty param.keywords}">
    <c:param name="keywords" value="${param.keywords}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
  </c:if>
  <c:if test="${not empty param.g}">
    <c:param name="g" value="${param.g}"/>
  </c:if>
  <c:if test="${not empty param.fg}">
    <c:param name="fg" value="${param.fg}"/>
  </c:if>
  <c:if test="${not empty param.unavail}">
    <c:param name="unavail" value="${param.unavail}"/>
  </c:if>
  <c:if test="${not empty param.own}">
    <c:param name="own" value="${param.own}"/>
  </c:if>
  <c:if test="${not empty param.any}">
    <c:param name="any" value="${param.any}"/>
  </c:if>
  <c:param name="ff" value="${ff}"/>
  <c:param name="view" value="${view}"/>
</c:url>

<c:url var="listNewUrl" value="/resource/platform/Inventory.action">
	<c:param name="mode" value="new"/>
</c:url>
<tiles:insertDefinition name=".toolbar.list">
  <tiles:putAttribute name="listNewUrl" value="${listNewUrl}"/>
  <tiles:putAttribute name="deleteOnly" value="true"/>
  <tiles:putAttribute name="includeGroup" value="true"/>
  <tiles:putAttribute name="listItems" value="${AllResources}"/>
  <tiles:putAttribute name="listSize" value="${totalSize}"/>
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="pageSizeAction" value="${psAction}"/>
  <tiles:putAttribute name="pageNumAction" value="${pnAction}"/>  
  <tiles:putAttribute name="defaultSortColumn" value="5"/>
   <c:if test="${not canModify}">
 	 <tiles:putAttribute name="hideAlertDefinitionActions" value="true" />
   </c:if>
</tiles:insertDefinition>
</div>
<s:hidden theme="simple" name="ff"/>
<s:hidden theme="simple" name="g"/>
<s:hidden theme="simple" name="pn"/>

</s:form>
<tiles:insertDefinition name=".resource.common.addToGroup"/>
<tiles:insertDefinition name=".page.footer"/>

