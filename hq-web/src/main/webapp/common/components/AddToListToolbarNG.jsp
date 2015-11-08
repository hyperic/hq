<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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


<%-- the context-relative url for the "add to list button", and the
  -- (optional) name and value for a request parameter to be attached
  -- to the url.
  --%>
<tiles:importAttribute name="addToListParamName" ignore="true"/>
<tiles:importAttribute name="addToListParamValue" ignore="true"/>
<tiles:importAttribute name="useDisableBtn" ignore="true"/>
<tiles:importAttribute name="useEnableIndicatorsBtn" ignore="true"/>
<tiles:importAttribute name="useDisableIndicatorsBtn" ignore="true"/>

<c:choose>
  <c:when test="${not empty useDisableBtn && useDisableBtn}">
    <c:set var="removeImg" value="/images/tbb_disablecollection_gray.gif"/>
  </c:when>
  <c:otherwise>
    <c:set var="removeImg" value="/images/tbb_removefromlist_gray.gif"/>
    <c:set var="disableRemoveButton" value="true"/>
  </c:otherwise>
</c:choose>

<%--
  -- shows flag indicating whether to show the addToList button or not
  -- set this value to false to not show the addToList button.
  --%>
<tiles:importAttribute name="showAddToListBtn" ignore="true"/>

<c:if test="${empty showAddToListBtn || showAddToListBtn}">
  <c:set var="showAddToListBtn" value="true"/>
  <tiles:importAttribute name="addToListUrl"/>
</c:if>

<tiles:importAttribute name="showRemoveBtn" ignore="true"/>
<c:if test="${empty showRemoveBtn}">
  <c:set var="showRemoveBtn" value="true"/>
</c:if>

<%-- the unique name of the list widget to which this toolbar is
  -- attached; used to enable/disable the "remove from list" button
  --%>
<tiles:importAttribute name="widgetInstanceName" ignore="true"/>

<%-- the collection of items displayed in the attached list widget
  --%>
<tiles:importAttribute name="listItems"/>

<%-- the total size of the collection of items displayed in the attached
  -- list widget
  --%>
<tiles:importAttribute name="listSize"/>

<%-- the name of the request parameter that controls the page size of the
  -- attached list
  --%>
<tiles:importAttribute name="pageSizeParam" ignore="true"/>

<%-- the root-relative url that the paging widgets use to refresh the
  -- web page
  --%>
<tiles:importAttribute name="pageSizeAction"/>

<%-- the name of the request parameter that determines the
  -- currently-viewed page of the attached list
  --%>
<tiles:importAttribute name="pageNumParam" ignore="true"/>

<%-- the root-relative url that the paging widgets use to refresh the
  -- web page
  --%>
<tiles:importAttribute name="pageNumAction"/>

<%-- the sort attribute the paginator will fall back to if it cannot
  -- find a sort attribute in the request parameters
  --%>
<tiles:importAttribute name="defaultSortColumn" ignore="true"/>

<%-- whether or not to show the paging controls
  --%>
<tiles:importAttribute name="showPagingControls" ignore="true"/>

<%-- whether or not to show interval form controls. See 2.1.5 mockup for an example.
  --%>
<tiles:importAttribute name="showIntervalControls" ignore="true"/>
<jsu:script>
  var goButtonLink;
</jsu:script>

<c:if test="${not empty addToListParamName && not empty addToListParamValue}">
	<c:url var="addToListUrl" value="${addToListUrl}">
      	<c:param name="${addToListParamName}" value="${addToListParamValue}"/>
    </c:url>
</c:if>

<!--  ADD TO LIST TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <c:if test="${showAddToListBtn}">
        <td width="40"><s:a href="%{#attr.addToListUrl}"><img src='<s:url value="/images/tbb_addtolist.gif"/>' width="85" height="16" border="0"/></s:a></td>
    </c:if>
    <c:if test="${showRemoveBtn}">
        <c:choose>
	    <c:when test="${disableRemoveButton}">
	    <td width="40" id="<c:out value="${widgetInstanceName}"/>DeleteButtonTd"><div id="<c:out value="${widgetInstanceName}"/>DeleteButtonDiv"><img src='<s:url value="%{#attr.removeImg}"/>'  border="0"/></div></td>
	    </c:when>
	    <c:otherwise>
	    <td width="40" id="<c:out value="${widgetInstanceName}"/>DeleteButtonTd"><div id="<c:out value="${widgetInstanceName}"/>DeleteButtonDiv"><img src='<s:url value="%{#attr.removeImg}"/>' border="0" property="remove"/></div></td>
	    </c:otherwise>
	    </c:choose>
    </c:if>

<c:if test="${not empty showIntervalControls and showIntervalControls}">
  <td class="BoldText" nowrap><fmt:message key="resource.common.monitor.visibility.config.CollectionIntervalForSelectedLabel"/></td>
 <td><s:textfield size="4" maxlength="4" name="collectionInterval" value="%{#attr.collectionInterval}"/></td>
 <td>
	<s:select theme="simple"  name="collectionUnit"  value="%{#attr.collectionUnit}" 
	list="#{'60000':getText('resource.common.monitor.visibility.config.Minutes'),'3600000':getText('resource.common.monitor.visibility.config.Hours')}" 
	cssClass="FilterFormText" size="1"/>
</td>
    <td width="100%" id="<c:out value="${widgetInstanceName}"/>GoButtonTd"><div id="<c:out value="${widgetInstanceName}"/>GoButtonDiv"><img src='<s:url value="/images/tbb_go_gray.gif"/>' border="0"/></div></td>
</c:if>

<c:if test="${useEnableIndicatorsBtn}">
<td class="BoldText" nowrap>
  <fmt:message key="resource.common.monitor.visibility.config.EnableSelectedAsIndicators"/>
</td>
<td id="<c:out value="${widgetInstanceName}"/>EnableIndButtonTd"><div id="<c:out value="${widgetInstanceName}"/>EnableIndButtonDiv"><img src='<s:url value="/images/tbb_go_gray.gif"/>' border="0"/></div></td>
</c:if>

<c:if test="${useDisableIndicatorsBtn}">
<td class="BoldText" nowrap>
  <fmt:message key="resource.common.monitor.visibility.config.DisableSelectedAsIndicators"/>
</td>
<td id="<c:out value="${widgetInstanceName}"/>DisableIndButtonTd"><div id="<c:out value="${widgetInstanceName}"/>DisableIndButtonDiv"><img src='<s:url value="/images/tbb_go_gray.gif"/>' border="0"/></div></td>
</c:if>

<c:choose>
<c:when test="${empty showPagingControls or showPagingControls}">
<tiles:insertDefinition name=".controls.paging">
  <tiles:putAttribute name="listItems" value="${listItems}"/>
  <tiles:putAttribute name="listSize" value="${listSize}"/>
  <tiles:putAttribute name="pageSizeParam" value="${pageSizeParam}"/>
  <tiles:putAttribute name="pageSizeAction" value="${pageSizeAction}"/>
  <tiles:putAttribute name="pageNumParam" value="${pageNumParam}"/>
  <tiles:putAttribute name="pageNumAction" value="${pageNumAction}"/>
  <tiles:putAttribute name="defaultSortColumn" value="${defaultSortColumn}"/>
</tiles:insertDefinition>
</c:when>
<c:otherwise>
  <td width="100%">&nbsp;</td>
</c:otherwise>
</c:choose>
  </tr>

<%-- need another row to display collectionInterval error message --%>
<c:if test="${not empty showIntervalControls and showIntervalControls}">
 <s:if test="fieldErrors.containsKey('collectionInterval')">
 
 <tr>
  <td width="40">&nbsp;</td>
  <td width="40">&nbsp;</td>
  <td class="ErrorField" nowrap="true" colspan="3"><span class="ErrorFieldContent">- <s:fielderror fieldName="collectionInterval" /></span></td>
  <td width="100%">&nbsp;</td>
 </tr>
 </s:if>
</c:if>

</table>
<!--  /  -->
