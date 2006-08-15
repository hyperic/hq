<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="display" prefix="display" %>
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
<tiles:insert page="/common/NoRights.jsp"/>
</c:when>
<c:otherwise>

<script language="JavaScript" src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="addUsers"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>
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

<c:url var="selfPnpAction" value="/alerts/Config.do">
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

<c:url var="selfPsaAction" value="/alerts/Config.do">
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

<c:url var="selfPspAction" value="/alerts/Config.do">
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

<c:url var="selfPaAction" value="/alerts/Config.do">
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

<c:url var="selfPpAction" value="/alerts/Config.do">
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

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.role.users.UsersTab"/>
  <tiles:put name="useFromSideBar" value="true"/>
</tiles:insert>

    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alerts.config.AssignUsersToAlertDefinitionTab"/>
  <tiles:put name="useToSideBar" value="true"/>
</tiles:insert>

    </td>
  </tr>
  <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">

        <display:table padRows="true" rightSidebar="true" items="${availableUsers}" var="user" action="${selfPaAction}" orderValue="soa" order="${param.soa}" sortValue="sca" sort="${param.sca}" pageValue="pna" page="${param.pna}" pageSizeValue="psa" pageSize="${param.psa}" styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableUser" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column value="${user.firstName}" title="admin.role.users.FirstNameTH" width="33%"/>
          <display:column value="${user.lastName}" title="admin.role.users.LastNameTH" width="33%"/>
          <display:column value="${user.name}" title="admin.role.users.UsernameTH" sort="true" sortAttr="3" defaultSort="true" width="33%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insert definition=".toolbar.new">
  <tiles:put name="useFromSideBar" value="true"/>
  <tiles:put name="listItems" beanName="availableUsers"/>
  <tiles:put name="listSize" beanName="numAvailableUsers"/>
  <tiles:put name="pageSizeParam" value="psa"/>
  <tiles:put name="pageSizeAction" beanName="selfPsaAction"/>
  <tiles:put name="pageNumParam" value="pna"/>
  <tiles:put name="pageNumAction" beanName="selfPnaAction"/>
</tiles:insert>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
      <div id="AddButtonDiv" align="left"><html:img page="/images/fb_addarrow_gray.gif" border="0" titleKey="AddToList.ClickToAdd"/></div>
      <br>&nbsp;<br>
      <div id="RemoveButtonDiv" align="right"><html:img page="/images/fb_removearrow_gray.gif" border="0" titleKey="AddToList.ClickToRemove"/></div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table padRows="true" leftSidebar="true" items="${pendingUsers}" var="user" action="${selfPpAction}" orderValue="sop" order="${param.sop}" sortValue="scp" sort="${param.scp}" pageValue="pnp" page="${param.pnp}" pageSizeValue="psp" pageSize="${param.psp}" styleId="toTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingUser" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column value="${user.firstName}" title="admin.role.users.FirstNameTH" width="33%"/>
          <display:column value="${user.lastName}" title="admin.role.users.LastNameTH" width="33%"/>
          <display:column value="${user.name}" title="admin.role.users.UsernameTH" sort="true" sortAttr="3" defaultSort="true" width="33%"/>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insert definition=".toolbar.new">
  <tiles:put name="newButtonKey" value="alerts.config.addusers.NewUserButton"/>
  <tiles:put name="useToSideBar" value="true"/>
  <tiles:put name="listItems" beanName="pendingUsers"/>
  <tiles:put name="listSize" beanName="numPendingUsers"/>
  <tiles:put name="pageSizeParam" value="psp"/>
  <tiles:put name="pageSizeAction" beanName="selfPspAction"/>
  <tiles:put name="pageNumParam" value="pnp"/>
  <tiles:put name="pageNumAction" beanName="selfPnpAction"/>
</tiles:insert>

    </td>
    <!-- / ADD COLUMN  -->
	
  </tr>
</table>
<!-- / SELECT & ADD -->

</c:otherwise>
</c:choose>
