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


<c:url var="viewUsersUrl" value="/alerts/Config.do">
  <c:param name="mode" value="viewUsers"/>
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
<c:url var="viewOthersUrl" value="/alerts/Config.do">
  <c:param name="mode" value="viewOthers"/>
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

<tiles:insert definition=".events.config.view.notifications.tabs">
  <tiles:put name="viewUsersUrl" beanName="viewUsersUrl"/>
  <tiles:put name="viewOthersUrl" beanName="viewOthersUrl"/>
</tiles:insert>

<%--
  I don't particularly *WANT* to use JSP-RT stuff here, but there
  seems to be no better choice since the struts tiles:insert does
  not yet take EL.
--%>
<% String notificationsTile = null; %>
<c:choose>
<c:when test="${param.mode == 'viewUsers' || param.mode == 'viewDefinition'}">
<% notificationsTile = ".events.config.view.notifications.users"; %>
<c:set var="formAction" value="/alerts/RemoveUsers"/>
<c:set var="selfUrl" value="${viewUsersUrl}"/>
<c:set var="addMode" value="addUsers"/>
<c:set var="defaultSortColumn" value="2"/>
</c:when>
<c:when test="${param.mode == 'viewOthers'}">
<% notificationsTile = ".events.config.view.notifications.others"; %>
<c:set var="formAction" value="/alerts/RemoveOthers"/>
<c:set var="selfUrl" value="${viewOthersUrl}"/>
<c:set var="addMode" value="addOthers"/>
<c:set var="defaultSortColumn" value="0"/>
</c:when>
<c:otherwise>
<%-- do nothing --%>
</c:otherwise>
</c:choose>
<script language="JavaScript" src="<html:rewrite page='/js/listWidget.js'/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="list"/>
<script language="JavaScript" type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>
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

<%-- I have to use an RT-expr here.  Yuck.  See comment above. --%>
<tiles:insert definition="<%=notificationsTile%>">
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

<!-- / FORM -->
<br>
