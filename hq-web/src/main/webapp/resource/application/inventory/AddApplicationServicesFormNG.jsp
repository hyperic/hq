<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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


<tiles:importAttribute name="resource"/>
<tiles:importAttribute name="availableServices" ignore="true" />
<tiles:importAttribute name="availableServicesCount" ignore="true" />
<tiles:importAttribute name="pendingServices" ignore="true" />
<tiles:importAttribute name="pendingServicesCount" ignore="true" />
<jsu:importScript path="/js/addRemoveWidget.js" />

<c:set var="widgetInstanceName" value="addServices"/>

<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<c:url var="selfJscAction" value="addServicesInventoryApplicationVisibility.action">
  <c:param name="mode" value="addServices"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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

<c:url var="selfPnaAction" value="addServicesInventoryApplicationVisibility.action">
  <c:param name="mode" value="addServices"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPnpAction" value="addServicesInventoryApplicationVisibility.action">
  <c:param name="mode" value="addServices"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPsaAction" value="addServicesInventoryApplicationVisibility.action">
  <c:param name="mode" value="addServices"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPspAction" value="addServicesInventoryApplicationVisibility.action">
  <c:param name="mode" value="addServices"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPaAction" value="addServicesInventoryApplicationVisibility.action">
  <c:param name="mode" value="addServices"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>

<c:url var="selfPpAction" value="addServicesInventoryApplicationVisibility.action">
  <c:param name="mode" value="addServices"/>
  <c:param name="rid" value="${resource.id}"/>
  <c:param name="type" value="${resource.entityId.type}"/>
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
  <c:if test="${not empty param.nameFilter}">
    <c:param name="nameFilter" value="${param.nameFilter}"/>
  </c:if>
</c:url>
<jsu:script>
    function applyNameFilter() {
        goToLocationSelfAndElement(
                'nameFilter',
                'nameFilter',
                '<c:out value="${selfJscAction}" escapeXml="false"/>');
        return false;
    }
</jsu:script>
<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="50%" valign="top">

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.application.inventory.services.ServicesTab"/>
  <tiles:putAttribute name="useFromSideBar" value="true"/>
</tiles:insertDefinition>

    </td>
    <td><img src='<s:url value="/images/spacer.gif"/>' width="40" height="1" border="0"/></td>
    <td>

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.application.inventory.services.AddToServicesTab"/>
  <tiles:putAttribute name="useFromSideBar" value="true"/>
</tiles:insertDefinition>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="FilterLine" width="100%" colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
          <td><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
        </tr>
        <tr>
          <td nowrap class="FilterLabelText" height="35">
            <fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>
          </td>
          <td class="FilterLabelText">
             <div style="float:left;display:inline;"><input type="text" name="nameFilter" maxlength="55" size="10"
                   onKeyPress="if (event.keyCode == 13) return applyNameFilter()"
			value="<c:out value="${param.nameFilter}"/>"/></div>
            <div style="display:inline;width:17px;padding-left:5px;padding-top:3px;"><img src='<s:url value="/images/4.0/icons/accept.png"/>' border="0" onclick="applyNameFilter()" /></div>
          </td>
        </tr>
      </table>
    </td>
    <td>
    </td>
    <td valign="top">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="FilterLine" width="100%"><img src='<s:url value="/images/spacer.gif"/>' height="1" border="0"/></td>
        </tr>
        <tr>
          <td nowrap class="FilterLabelText" height="35">&nbsp;</td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">

        <display:table padRows="true" rightSidebar="true" items="${availableServices}" var="service" action="${selfPaAction}" orderValue="soa" order="${param.soa}" sortValue="sca" sort="${param.sca}" pageValue="pna" page="${param.pna}" pageSizeValue="psa" pageSize="${param.psa}" styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="entityId" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableServices" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column property="name" title="resource.application.inventory.services.ServiceTH" sort="true" sortAttr="22" defaultSort="true" width="50%"/>
          <display:column property="description" title="common.header.Description" width="50%"/>
        </display:table>

      </div>
      <!--  /  -->

  <tiles:insertDefinition name=".ng.toolbar.new">
	<tiles:putAttribute name="useFromSideBar" value="true"/>
	<tiles:putAttribute name="listItems" value="${reqAvailableAppSvcs}"/>
	<tiles:putAttribute name="listSize" value="${reqNumAvailableAppSvcs}"/>
	<tiles:putAttribute name="pageSizeParam" value="psa"/>
	<tiles:putAttribute name="pageSizeAction" value="${selfPsaAction}"/>
	<tiles:putAttribute name="pageNumParam" value="pna"/>
	<tiles:putAttribute name="pageNumAction" value="${selfPnaAction}"/>
  </tiles:insertDefinition>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
      <div id="AddButtonDiv" align="left"><img src='<s:url value="/images/fb_addarrow_gray.gif" />'  border="0" titleKey="AddToList.ClickToAdd" /></div>
      <br>&nbsp;<br>
      <div id="RemoveButtonDiv" align="right"><img src='<s:url value="/images/fb_removearrow_gray.gif" />'  border="0" titleKey="AddToList.ClickToRemove" /></div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table padRows="true" leftSidebar="true" items="${pendingServices}" var="service" action="${selfPpAction}" orderValue="sop" order="${param.sop}" sortValue="scp" sort="${param.scp}" pageValue="pnp" page="${param.pnp}" pageSizeValue="psp" pageSize="${param.psp}" styleId="toTable" width="100%" cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="entityId" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingServices" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column property="name" title="resource.application.inventory.services.ServiceTH" sort="true" sortAttr="22" defaultSort="true" width="50%"/>
          <display:column property="description" title="common.header.Description" width="50%"/>
        </display:table>

      </div>
      <!--  /  -->
<tiles:insertDefinition name=".ng.toolbar.new">
  <tiles:putAttribute name="useToSideBar" value="true"/>
  <tiles:putAttribute name="listItems" value="${reqPendingAppSvcs}"/>
  <tiles:putAttribute name="listSize" value="${reqNumPendingAppSvcs}"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfPspAction}"/>
  <tiles:putAttribute name="pageSizeParam" value="psp"/>
  <tiles:putAttribute name="pageNumAction" value="${selfPnpAction}"/>
  <tiles:putAttribute name="pageNumParam" value="pnp"/>
</tiles:insertDefinition>
    </td>
    <!-- / ADD COLUMN  -->
    
  </tr>
</table>
<!-- / SELECT & ADD -->
