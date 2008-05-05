<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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


<script  src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listResources"/>
<script type="text/javascript">
var pageData = new Array();
var FOO = "chart";
var LIST  = "list"; 
var imagePath = "<html:rewrite page="/images/"/>";

initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

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
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalAction"
    symbol="SELECTOR_GROUP_COMPAT"/>
<hq:constant var="GROUP_ADHOC" 
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalAction"
    symbol="SELECTOR_GROUP_ADHOC"/>
<hq:constant var="CHART"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubForm"
	symbol="CHART_VIEW"/>
<hq:constant var="LIST"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubForm"
	symbol="LIST_VIEW"/>

<c:choose>
  <c:when test="${ResourceHubForm.ff == PLATFORM}">
    <fmt:message var="entityTypeTH" key="resource.type.Platform"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.PlatformTypeTH"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == SERVER}">
    <fmt:message var="entityTypeTH" key="resource.type.Server"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.ServerTypeTH"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == SERVICE}">
    <fmt:message var="entityTypeTH" key="resource.type.Service"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.ServiceTypeTH"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == APPLICATION}">
    <fmt:message var="entityTypeTH" key="resource.type.Application"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == GROUP}">
    <fmt:message var="entityTypeTH" key="resource.type.Group"/>
    <fmt:message var="resourceTypeTH" key="resource.hub.GroupTypeTH"/>
  </c:when>
</c:choose>

<html:form action="/resource/hub/RemoveResource.do">

<tiles:insert definition=".page.title.resource.hub">
  <tiles:put name="titleName"><c:out value="${navHierarchy}"/></tiles:put>
</tiles:insert>

<c:if test="${not empty ResourceSummary}">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ResourceHubBlockTitle" width="100%">
<c:choose>
  <c:when test="${ResourceHubForm.ff == PLATFORM}">
      <fmt:message key="resource.hub.filter.platform"/> (<c:out value="${ResourceSummary.platformCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="platformUrl" value="/ResourceHub.do">
      <c:param name="ff" value="${PLATFORM}"/>
      <c:param name="view" value="${ResourceHubForm.view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <html:link href="${platformUrl}"><fmt:message key="resource.hub.filter.platform"/> (<c:out value="${ResourceSummary.platformCount}"/>)</html:link>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ResourceHubForm.ff == SERVER}">
      <fmt:message key="resource.hub.filter.server"/> (<c:out value="${ResourceSummary.serverCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="serverUrl" value="/ResourceHub.do">
      <c:param name="ff" value="${SERVER}"/>
      <c:param name="view" value="${ResourceHubForm.view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <html:link href="${serverUrl}"><fmt:message key="resource.hub.filter.server"/> (<c:out value="${ResourceSummary.serverCount}"/>)</html:link>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ResourceHubForm.ff == SERVICE}">
      <fmt:message key="resource.hub.filter.service"/> (<c:out value="${ResourceSummary.serviceCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="serviceUrl" value="/ResourceHub.do">
      <c:param name="ff" value="${SERVICE}"/>
      <c:param name="view" value="${ResourceHubForm.view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <html:link href="${serviceUrl}"><fmt:message key="resource.hub.filter.service"/> (<c:out value="${ResourceSummary.serviceCount}"/>)</html:link>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ResourceHubForm.ff == GROUP && ResourceHubForm.g == GROUP_COMPAT}">
      <fmt:message key="resource.hub.filter.compatibleGroups"/> (<c:out value="${ResourceSummary.compatGroupCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="groupUrl" value="/ResourceHub.do">
      <c:param name="ff" value="${GROUP}"/>
      <c:param name="g" value="${GROUP_COMPAT}"/>
      <c:param name="view" value="${ResourceHubForm.view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <html:link href="${groupUrl}"><fmt:message key="resource.hub.filter.compatibleGroups"/> (<c:out value="${ResourceSummary.compatGroupCount}"/>)</html:link>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ResourceHubForm.ff == GROUP && ResourceHubForm.g == GROUP_ADHOC}">
      <fmt:message key="resource.hub.filter.mixedGroups"/> (<c:out value="${ResourceSummary.groupCountAdhocGroup + ResourceSummary.groupCountAdhocPSS + ResourceSummary.groupCountAdhocApp}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="groupUrl" value="/ResourceHub.do">
      <c:param name="ff" value="${GROUP}"/>
      <c:param name="g" value="${GROUP_ADHOC}"/>
      <c:param name="view" value="${ResourceHubForm.view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <html:link href="${groupUrl}"><fmt:message key="resource.hub.filter.mixedGroups"/> (<c:out value="${ResourceSummary.groupCountAdhocGroup + ResourceSummary.groupCountAdhocPSS + ResourceSummary.groupCountAdhocApp}"/>)</html:link>
  </c:otherwise>
</c:choose>
<fmt:message key="common.label.Pipe"/>
<c:choose>
  <c:when test="${ResourceHubForm.ff == APPLICATION}">
      <fmt:message key="resource.hub.filter.application"/> (<c:out value="${ResourceSummary.applicationCount}"/>)
  </c:when>
  <c:otherwise>
    <c:url var="appUrl" value="/ResourceHub.do">
      <c:param name="ff" value="${APPLICATION}"/>
      <c:param name="view" value="${ResourceHubForm.view}"/>
      <c:if test="${not empty param.keywords}">
        <c:param name="keywords" value="${param.keywords}"/>
      </c:if>
    </c:url>
    <html:link href="${appUrl}"><fmt:message key="resource.hub.filter.application"/> (<c:out value="${ResourceSummary.applicationCount}"/>)</html:link>
  </c:otherwise>
</c:choose>
    </td>
  </tr>
</table>
</c:if>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<!--  FILTER TOOLBAR CONTENTS -->
<html:hidden property="view"/>

<c:url var="ftAction" value="/ResourceHub.do">
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
  <c:param name="ff" value="${ResourceHubForm.ff}"/>
  <c:param name="view" value="${ResourceHubForm.view}"/>
</c:url>

<c:url var="fgAction" value="/ResourceHub.do">
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
  <c:param name="ff" value="${ResourceHubForm.ff}"/>
  <c:param name="view" value="${ResourceHubForm.view}"/>
</c:url>

<c:choose>
  <c:when test="${not empty ResourceHubForm.keywords}">
    <c:set var="initSearchVal" value="${ResourceHubForm.keywords}"/>
  </c:when>
  <c:otherwise>
    <fmt:message var="initSearchVal" key="resource.hub.search.KeywordSearchText"/>
  </c:otherwise>
</c:choose>

<div class="FilterImage" style="padding: 4px; border-top: 1px solid #ABB1C7;text-align: right;">
	<c:choose>
	  <c:when test="${ResourceHubForm.view == CHART}">
	    <html:img page="/images/SubHub_ChartView_on.gif" alt="Chart View" width="104" height="15" border="0"/>
	  </c:when>
	  <c:otherwise>
	    <html:link page="/ResourceHub.do" onclick="ResourceHubForm.view.value = 'chart'; ResourceHubForm.submit(); return false;"><html:img page="/images/SubHub_ChartView_off.gif" alt="Chart View" width="104" height="15" border="0" onmouseover="imageSwap (this, imagePath + 'SubHub_ChartView', '_over')" onmouseout="imageSwap (this, imagePath + 'SubHub_ChartView', '_off')"/></html:link>
	  </c:otherwise>
	</c:choose>

	<c:choose>
	  <c:when test="${ResourceHubForm.view == LIST}">
	    <html:img page="/images/SubHub_ListView_on.gif" alt="List View" width="104" height="15" border="0"/>
	  </c:when>
	  <c:otherwise>
	    <html:link page="/ResourceHub.do" onclick="ResourceHubForm.view.value = 'list'; ResourceHubForm.submit(); return false;"><html:img page="/images/SubHub_ListView_off.gif" alt="List View" width="104" height="15" border="0" onmouseover="imageSwap (this, imagePath + 'SubHub_ListView', '_over')" onmouseout="imageSwap (this, imagePath + 'SubHub_ListView', '_off')"/></html:link>
	  </c:otherwise>
	</c:choose>
</div>
<!--  /  -->

<!--  RESOURCE HUB CONTENTS -->
<c:url var="sAction" value="/ResourceHub.do">
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
  <c:param name="ff" value="${ResourceHubForm.ff}"/>
  <c:param name="view" value="${ResourceHubForm.view}"/>
</c:url>

<c:choose>
  <c:when test="${ResourceHubForm.view == LIST}">
    <script type="text/javascript">
      function refreshAvail() {
        var now = new Date()
    <c:forEach var="resource" items="${AllResources}">
        <c:out value="document.avail${resource.entityId.id}.src"/> =
          '<html:rewrite page="/resource/Availability"/>?timeout=30000&eid=' +
          '<c:out value="${resource.entityId.appdefKey}"/>#' + now.valueOf();
    </c:forEach>
        setAvailRefresh()
      }
      
      function setAvailRefresh() {
        setTimeout( "refreshAvail()", 60*1000 );
      }

      onloads.push( setAvailRefresh );
    </script>
    <c:choose>
    <c:when test="${empty Indicators || empty AllResources}">
    <display:table items="${AllResources}" var="resource" action="${sAction}" width="100%" cellspacing="0" cellpadding="0">
      <display:column width="1%" property="entityId.appdefKey" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
        <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
      </display:column>
      <display:column width="5%" property="id" title="nbsp" nowrap="true">
        <display:quicknavdecorator resource="${resource}"/>
      </display:column>
      <display:column width="30%" property="name" title="${entityTypeTH}" isLocalizedTitle="false" href="/Resource.do?eid=${resource.entityId.appdefKey}" sort="true" sortAttr="5" defaultSort="true"/>
      <c:if test="${not empty resourceTypeTH}">
      <display:column width="30%" property="id" title="${resourceTypeTH}" isLocalizedTitle="false">
        <display:resourcedecorator resource="${resource}" type="true"/>
      </display:column>
      </c:if>
      <display:column width="24%" property="description" title="common.header.Description"/>
    <c:if test="${ResourceHubForm.ff == GROUP}">
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
    <a href="<c:out value="${sAction}&so=asc"/>"><c:out value="${entityTypeTH}"/><html:img border="0" page="/images/tb_sortdown.gif"/></a>
    </c:when>
    <c:otherwise>
    <a href="<c:out value="${sAction}&so=dec"/>"><c:out value="${entityTypeTH}"/><html:img border="0" page="/images/tb_sortup.gif"/></a>
    </c:otherwise>
    </c:choose>
    </th>
    <c:forEach items="${Indicators}" var="indicator">
      <th class="tableRowInactive" align="middle"><c:out value="${indicator.name}"/></th>
    </c:forEach>
      <th class="tableRowInactive" align="middle"><fmt:message key="resource.common.monitor.visibility.AvailabilityTH"/></th>
    </tr>
    <c:forEach items="${AllResources}" var="resource">
    <tr class="tableRowOdd">
    <td class="ListCellCheckbox" align="left" valign="top"><input type="checkbox" onclick="ToggleSelection(this, widgetProperties)" class="listMember" name="resources" value="<c:out value="${resource.entityId}"/>"></td>
    <td class="tableCell" align="left" nowrap valign="top">
    <html:link page="/resource/${section}/monitor/Visibility.do?mode=currentHealth" paramId="eid" paramName="resource" paramProperty="entityId"><html:img page="/images/icon_hub_m.gif" width="11" height="11" alt="" border="0"/></html:link>
    <html:link page="/resource/${section}/Inventory.do?mode=view" paramId="eid" paramName="resource" paramProperty="entityId"><html:img page="/images/icon_hub_i.gif" width="11" height="11" alt="" border="0"/></html:link>
    <c:if test="${resource.entityId.type == 1 || resource.entityId.type == 2 || resource.entityId.type == 3}">
    <html:link page="/alerts/Config.do?mode=list" paramId="eid" paramName="resource" paramProperty="entityId"><html:img page="/images/icon_hub_a.gif" width="11" height="11" alt="" border="0"/></html:link>
    </c:if>
    </td>
      <td class="tableCell" align="left" valign="top"><span class="SpanPopup1"><html:link page="/Resource.do" paramId="eid" paramName="resource" paramProperty="entityId"><c:out value="${resource.name}"/></html:link><c:if test="${not empty resource.description}"><span><c:out value="${resource.description}" escapeXml="false"/></span></c:if></span></td>
      <!-- Insert metrics tile here -->
      <tiles:insert definition=".resource.hub.metrics">
        <tiles:put name="eid" beanName="resource" beanProperty="entityId"/>
        <tiles:put name="Indicators" beanName="Indicators"/>
      </tiles:insert>
      <td class="tableCell" align="middle" valign="top">
        <html:img imageName="avail${resource.id}" page="/resource/Availability?timeout=30000" paramId="eid" paramName="resource" paramProperty="entityId"/>
      </td>
    </tr>
    </c:forEach>
    </table>
    </c:otherwise>
    </c:choose>
  </c:when>
  <c:otherwise>
    <tiles:insert definition=".resource.common.monitor.visibility.minicharts">
      <tiles:put name="Resources" beanName="AllResources"/>
    </tiles:insert>
  </c:otherwise>
</c:choose>
<!--  /  -->

<c:url var="psAction" value="/ResourceHub.do">
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
  <c:param name="ff" value="${ResourceHubForm.ff}"/>
  <c:param name="view" value="${ResourceHubForm.view}"/>
</c:url>

<c:url var="pnAction" value="/ResourceHub.do">
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
    <c:param name="fg" value="${param.g}"/>
  </c:if>
  <c:if test="${not empty param.fg}">
    <c:param name="g" value="${param.fg}"/>
  </c:if>
  <c:param name="ff" value="${ResourceHubForm.ff}"/>
  <c:param name="view" value="${ResourceHubForm.view}"/>
</c:url>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listNewUrl" value="/resource/platform/Inventory.do?mode=new"/>
  <tiles:put name="deleteOnly" value="true"/>
  <tiles:put name="includeGroup" value="true"/>
  <tiles:put name="listItems" beanName="AllResources"/>
  <tiles:put name="listSize" beanName="AllResources" beanProperty="totalSize"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="pageSizeAction" beanName="psAction"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>  
  <tiles:put name="defaultSortColumn" value="5"/>
</tiles:insert>

<html:hidden property="ff"/>
<html:hidden property="g"/>
<html:hidden property="pn"/>

</html:form>
<tiles:insert definition=".page.footer"/>

<script type="text/javascript">
  clearIfAnyChecked();
</script>
