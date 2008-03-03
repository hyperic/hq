<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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

<c:if test="${not alertDef.deleted}">

<tiles:importAttribute name="formAction" ignore="true"/>
<tiles:importAttribute name="addMode" ignore="true"/>
<tiles:importAttribute name="defaultSortColumn" ignore="true"/>

<c:url var="viewUrl" value="/Config.do" context="/alerts">
  <c:choose>
  <c:when test="${not empty Resource}">
    <c:param name="eid" value="${Resource.entityId.appdefKey}"/>
  </c:when>
  <c:otherwise>
    <c:param name="aetid" value="${ResourceType.appdefTypeKey}"/>
  </c:otherwise>
  </c:choose>
  <c:param name="ad" value="${alertDef.id}"/>
</c:url>

<c:url var="selfUrl" value="${viewUrl}">
  <c:param name="mode" value="${param.mode}"/>
</c:url>
<c:url var="viewUsersUrl" value="${viewUrl}">
  <c:param name="mode" value="viewUsers"/>
</c:url>
<c:url var="viewOthersUrl" value="${viewUrl}">
  <c:param name="mode" value="viewOthers"/>
</c:url>
<c:url var="viewEscalationUrl" value="${viewUrl}">
  <c:param name="mode" value="viewEscalation"/>
</c:url>
<c:url var="viewOpenNMSUrl" value="${viewUrl}">
  <c:param name="mode" value="viewOpenNMS"/>
</c:url>

<tiles:insert definition=".events.config.view.notifications.tabs">
  <tiles:put name="viewUsersUrl" beanName="viewUsersUrl"/>
  <tiles:put name="viewOthersUrl" beanName="viewOthersUrl"/>
  <tiles:put name="viewEscalationUrl" beanName="viewEscalationUrl"/>
  <tiles:put name="viewEscalationUrl" beanName="viewOpenNMSUrl"/>
</tiles:insert>

<script language="JavaScript" src="<html:rewrite page='/js/listWidget.js'/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="list"/>
<script language="JavaScript" type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:choose>
<c:when test="${not empty formAction}">
<!-- FORM -->
<html:form action="${formAction}">
<html:hidden property="ad" value="${alertDef.id}"/>
<c:choose>
<c:when test="${not empty Resource}">
  <html:hidden property="rid" value="${Resource.id}"/>
  <html:hidden property="type" value="${Resource.entityId.type}"/>
</c:when>
<c:otherwise>
  <html:hidden property="aetid" value="${ResourceType.appdefTypeKey}"/>
</c:otherwise>
</c:choose>

<tiles:insert beanName="notificationsTile">
  <tiles:put name="selfUrl" beanName="selfUrl"/>
</tiles:insert>

<%-- if the attributes are not available, we can't display this tile: an error probably occured --%>
  <c:choose>
    <c:when test="${null == notifyList || empty listSize}">
      <!-- permission error occured -->
      <fmt:message key="alert.config.error.no.permission"/>
    </c:when>
    <c:otherwise>
      <html:hidden property="so" value="${param.so}"/>
      <html:hidden property="sc" value="${param.sc}"/>
    
      <tiles:insert definition=".toolbar.addToList">
  <c:choose>
  <c:when test="${not empty Resource}">
    <tiles:put name="addToListUrl"><c:out value="/alerts/Config.do?mode=${addMode}&eid=${Resource.entityId.appdefKey}&ad=${alertDef.id}"/></tiles:put>
  </c:when>
  <c:otherwise>
    <tiles:put name="addToListUrl"><c:out value="/alerts/Config.do?mode=${addMode}&aetid=${ResourceType.appdefTypeKey}&ad=${alertDef.id}"/></tiles:put>
  </c:otherwise>
  </c:choose>
        <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
        <tiles:put name="listItems" beanName="notifyList"/>
        <tiles:put name="listSize" beanName="listSize"/>
        <tiles:put name="defaultSortColumn"><c:out value="${defaultSortColumn}"/></tiles:put>
        <tiles:put name="pageNumAction" beanName="selfUrl"/>
      <tiles:put name="pageSizeAction" beanName="selfUrl"/>
      </tiles:insert>
    </c:otherwise>
  </c:choose>

</html:form>
</c:when>
<c:otherwise>
<tiles:insert beanName="notificationsTile">
  <tiles:put name="selfUrl" beanName="selfUrl"/>
</tiles:insert>
</c:otherwise>
</c:choose>

<!-- / FORM -->
</c:if>
<br>
