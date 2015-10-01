<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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


<tiles:importAttribute name="availableUsers" ignore="true"/>
<tiles:importAttribute name="numAvailableUsers" ignore="true"/>
<tiles:importAttribute name="pendingUsers" ignore="true"/>
<tiles:importAttribute name="numPendingUsers" ignore="true"/>

<%-- if the attributes are not available, we can't display this tile: an error probably occured --%>
<c:choose>
<c:when test="${empty numAvailableUsers}">
<!-- error occured -->
<tiles:insertTemplate template="/common/NoRights.jsp"/>
</c:when>
<c:otherwise>
<jsu:importScript path="/js/addRemoveWidget.js" />
<c:set var="widgetInstanceName" value="addUsers"/>
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<c:url var="selfPnaAction" value="/alerts/Config.do">
  <c:param name="mode" value="addUsers"/>
  <c:param name="ad" value="${alertDef.id}"/>
  <c:choose>
    <c:when test="${not empty aetid}">
      <c:param name="aetid" value="${aetid}"/>
    </c:when>
    <c:otherwise>
      <c:param name="eid" value="${eid}"/>
    </c:otherwise>
  </c:choose>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPnpAction" value="addUsersAlertsConfigPortal.action">
  <c:param name="mode" value="addUsers"/>
  <c:param name="ad" value="${alertDef.id}"/>
  <c:choose>
    <c:when test="${not empty aetid}">
      <c:param name="aetid" value="${aetid}"/>
    </c:when>
    <c:otherwise>
      <c:param name="eid" value="${eid}"/>
    </c:otherwise>
  </c:choose>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPsaAction" value="addUsersAlertsConfigPortal.action">
  <c:param name="mode" value="addUsers"/>
  <c:param name="ad" value="${alertDef.id}"/>
  <c:choose>
    <c:when test="${not empty aetid}">
      <c:param name="aetid" value="${aetid}"/>
    </c:when>
    <c:otherwise>
      <c:param name="eid" value="${eid}"/>
    </c:otherwise>
  </c:choose>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPspAction" value="addUsersAlertsConfigPortal.action">
  <c:param name="mode" value="addUsers"/>
  <c:param name="ad" value="${alertDef.id}"/>
  <c:choose>
    <c:when test="${not empty aetid}">
      <c:param name="aetid" value="${aetid}"/>
    </c:when>
    <c:otherwise>
      <c:param name="eid" value="${eid}"/>
    </c:otherwise>
  </c:choose>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPaAction" value="addUsersAlertsConfigPortal.action">
  <c:param name="mode" value="addUsers"/>
  <c:param name="ad" value="${alertDef.id}"/>
  <c:choose>
    <c:when test="${not empty aetid}">
      <c:param name="aetid" value="${aetid}"/>
    </c:when>
    <c:otherwise>
      <c:param name="eid" value="${eid}"/>
    </c:otherwise>
  </c:choose>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
</c:url>

<c:url var="selfPpAction" value="addUsersAlertsConfigPortal.action">
  <c:param name="mode" value="addUsers"/>
  <c:param name="ad" value="${alertDef.id}"/>
  <c:choose>
    <c:when test="${not empty aetid}">
      <c:param name="aetid" value="${aetid}"/>
    </c:when>
    <c:otherwise>
      <c:param name="eid" value="${eid}"/>
    </c:otherwise>
  </c:choose>
  <c:if test="${not empty param.pna}">
    <c:param name="pna" value="${param.pna}"/>
  </c:if>
  <c:if test="${not empty param.psa}">
    <c:param name="psa" value="${param.psa}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.pnp}">
    <c:param name="pnp" value="${param.pnp}"/>
  </c:if>
  <c:if test="${not empty param.psp}">
    <c:param name="psp" value="${param.psp}"/>
  </c:if>
</c:url>

<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="50%" valign="top">

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="admin.role.users.UsersTab"/>
  <tiles:putAttribute name="useFromSideBar" value="true"/>
</tiles:insertDefinition>

    </td>
    <td><img src='<s:url value="/images/spacer.gif"/>' width="40" height="1" border="0"/></td>
    <td>

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="alerts.config.AssignUsersToAlertDefinitionTab"/>
  <tiles:putAttribute name="useToSideBar" value="true"/>
</tiles:insertDefinition>

    </td>
  </tr>
  <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">

        <display:table padRows="true" rightSidebar="true" items="${availableUsers}" var="user" action="${selfPaAction}" orderValue="soa" order="${param.soa}" sortValue="sca" sort="${param.sca}" pageValue="pna" page="${param.pna}" pageSizeValue="psa" pageSize="${param.psa}" styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\"/>" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableUser" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column property="firstName" title="admin.role.users.FirstNameTH" width="33%"/>
          <display:column property="lastName" title="admin.role.users.LastNameTH" width="33%"/>
          <display:column property="name" title="admin.role.users.UsernameTH" sort="true" sortAttr="3" defaultSort="true" width="33%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insertDefinition name=".ng.toolbar.new">
  <tiles:putAttribute name="useFromSideBar" value="true"/>
  <tiles:putAttribute name="listItems" value="${availableUsers}"/>
  <tiles:putAttribute name="listSize" value="${numAvailableUsers}"/>
  <tiles:putAttribute name="pageSizeParam" value="psa"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfPsaAction}"/>
  <tiles:putAttribute name="pageNumParam" value="pna"/>
  <tiles:putAttribute name="pageNumAction" value="${selfPnaAction}"/>
</tiles:insertDefinition>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
      <div id="AddButtonDiv" align="left"><img src='<s:url value="/images/fb_addarrow_gray.gif"/>' border="0" titleKey="AddToList.ClickToAdd"/></div>
      <br>&nbsp;<br>
      <div id="RemoveButtonDiv" align="right"><img src='<s:url value="/images/fb_removearrow_gray.gif"/>' border="0" titleKey="AddToList.ClickToRemove"/></div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table padRows="true" leftSidebar="true" items="${pendingUsers}" var="user" action="${selfPpAction}" orderValue="sop" order="${param.sop}" sortValue="scp" sort="${param.scp}" pageValue="pnp" page="${param.pnp}" pageSizeValue="psp" pageSize="${param.psp}" styleId="toTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\"/>" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingUser" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column property="firstName" title="admin.role.users.FirstNameTH" width="33%"/>
          <display:column property="lastName" title="admin.role.users.LastNameTH" width="33%"/>
          <display:column property="name" title="admin.role.users.UsernameTH" sort="true" sortAttr="3" defaultSort="true" width="33%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insertDefinition name=".ng.toolbar.new">
  <tiles:putAttribute name="newButtonKey" value="alerts.config.addusers.NewUserButton"/>
  <tiles:putAttribute name="useToSideBar" value="true"/>
  <tiles:putAttribute name="listItems" value="${pendingUsers}"/>
  <tiles:putAttribute name="listSize" value="${numPendingUsers}"/>
  <tiles:putAttribute name="pageSizeParam" value="psp"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfPspAction}"/>
  <tiles:putAttribute name="pageNumParam" value="pnp"/>
  <tiles:putAttribute name="pageNumAction" value="${selfPnpAction}"/>
</tiles:insertDefinition>

    </td>
    <!-- / ADD COLUMN  -->
	
  </tr>
</table>
<!-- / SELECT & ADD -->

</c:otherwise>
</c:choose>
