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


<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_ADHOC_PSS" var="CONST_ADHOC_PSS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_ADHOC_GRP" var="CONST_ADHOC_GRP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_ADHOC_APP" var="CONST_ADHOC_APP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_COMPAT_PS" var="CONST_COMPAT_PS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_COMPAT_SVC" var="CONST_COMPAT_SVC" />

<jsu:importScript path="/js/addRemoveWidget.js" />
<c:set var="widgetInstanceName" value="addResources"/>
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<c:url var="selfPnaAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
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

<c:url var="selfPnFilterNameAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
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
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
  </c:if>
</c:url>

<c:url var="selfPnFilterAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
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

<c:url var="selfPnpAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
  </c:if>
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

<c:url var="selfPsaAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
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

<c:url var="selfPspAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
  </c:if>
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

<c:url var="selfPaAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
  </c:if>
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

<c:url var="selfPpAction" value="startAddGroupResources.action">
  <c:param name="mode" value="addResources"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${Resource.entityId.type}"/>
  <c:if test="${not empty param.filterBy}">
    <c:param name="filterBy" value="${param.filterBy}"/>
  </c:if>
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
                '<c:out value="${selfPnFilterNameAction}" escapeXml="false"/>');
        return false;
    }
</jsu:script>
<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<!--  SELECT & ADD -->
  <tr>
     <td width="50%" valign="top">
	
     <tiles:insertDefinition name=".header.tab">
      <tiles:putAttribute name="tabKey" value="resource.group.inventory.Edit.ResourcesTab"/>
      <tiles:putAttribute name="useFromSideBar" value="true"/>
     </tiles:insertDefinition>
    </td>

    <td><img src='<s:url value="/images/spacer.gif"/>' width="40" height="1" border="0"/></td>
    <td>
     <tiles:insertDefinition name=".header.tab">
      <tiles:putAttribute name="tabKey" value="resource.group.inventory.Edit.AddResourcesTab"/>
      <tiles:putAttribute name="useToSideBar" value="true"/>
     </tiles:insertDefinition>
    </td>
  </tr>
	<tr>
		<td valign="bottom">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
        <c:choose>
            <c:when test="${Resource.groupType == CONST_ADHOC_PSS  }">
              <tr>
                  <td class="FilterLine" width="100%" colspan="4"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
                  <td><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
              </tr>
              <tr>
                  <td nowrap class="FilterLabelText">
                      <fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>
                  </td>
                  <td class="FilterLabelText" valign="bottom">
                      <div style="float:left;display:inline;"><input type="text" name="nameFilter" maxlength="55" size="10"
			     onKeyPress="if (event.keyCode == 13) return applyNameFilter()"
				value="<c:out value="${param.nameFilter}"/>"/></div>
                      <div style="display:inline;width:17px;padding-left:5px;padding-top:3px;"><img src='<s:url value="/images/4.0/icons/accept.png"/>' border="0" onclick="applyNameFilter()"/></div>
                  </td>
                  <td nowrap class="FilterLabelText">
                      <fmt:message key="resource.group.inventory.Edit.FilterByTypeLabel"/>
                  </td>
                  <td class="FilterLabelText">
				  	  
                     <s:select name="filterBy" styleClass="FilterFormText" onchange="goToSelectLocation(this, 'filterBy',  '%{#attr.selfPnFilterAction}');" list="#request.availResourceTypes" ></s:select>
					   
					
                  </td>
                  <td><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0"/></td>
              </tr>
            </c:when>
            <c:when test="${ Resource.groupType == CONST_COMPAT_PS ||
                             Resource.groupType == CONST_COMPAT_SVC }">
              <tr>
                  <td class="FilterLine" width="100%" colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
                  <td><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
              </tr>
              <tr>
                  <td nowrap class="FilterLabelText" colspan=2>
                      <div style="float:left;display:inline;"><fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>&nbsp;
                      <input type="text" name="nameFilter" maxlength="55" size="10"
			     onKeyPress="if (event.keyCode == 13) return applyNameFilter()"
				value="<c:out value="${param.nameFilter}"/>"/></div>

                      <div style="display:inline;width:17px;padding-left:5px;padding-top:3px;"><img src='<s:url value="/images/4.0/icons/accept.png"/>'
                             border="0" onclick="applyNameFilter()"/></div>
                  </td>
                  <td><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0"/></td>
              </tr>
            </c:when>
            <c:when test="${Resource.groupType == CONST_ADHOC_GRP }">
              <tr>
                        <td class="FilterLine" width="100%" colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
                        <td><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
                    </tr>
                    <tr>
                <td nowrap class="FilterLabelText">
                  <fmt:message key="resource.group.inventory.Edit.FilterByTypeLabel"/>
                </td>
                <td class="FilterLabelText">
					  <s:select name="filterBy" styleClass="FilterFormText" onchange="goToSelectLocation(this, 'filterBy',  '%{#attr.selfPnFilterAction}');" list="#request.availResourceTypes" ></s:select>					
                 <br>
                </td>
                <td><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0"/></td>
                    </tr>
            </c:when>
            <c:otherwise >
              <tr>
                        <td class="FilterLine" width="100%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
                        <td><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
                    </tr>
                    <tr>
                <td nowrap class="FilterLabelText">
                <c:choose>
                    <c:when test="${Resource.groupType == CONST_ADHOC_APP  }">
                        <fmt:message key="resource.group.inventory.Edit.GroupTypeLabel"/>
                    </c:when>
                    <c:otherwise>
                        <fmt:message key="resource.group.inventory.Edit.CompatibleResourceTypeLabel"/>
                    </c:otherwise>
                </c:choose>
                  <c:out value="${Resource.appdefResourceTypeValue.name}"/>
                </td>
                <td><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0"/></td>
                    </tr>
            </c:otherwise>

        </c:choose>
			</table>

		</td>
		<td>&nbsp;</td>
		<td valign="bottom">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
				<tr>
					<td rowspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" border="0"/></td>
					<td class="FilterLine" colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
				</tr>
				<tr>
           <%--
             -- Support three different spacer patterns. The first is for adhoc
             -- grp of grp where you have a single selector. The second supports
             -- a height of 32 pixels to accomodate the slector and name filter.
             -- third is a normal spacer pattern.  --%>
          <c:choose>
              <c:when test="${ Resource.groupType == CONST_ADHOC_GRP }">
                <td width="100%" class="FilterLabelText"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="18" border="0"/></td>
              </c:when>
              <c:when test="${Resource.groupType == CONST_ADHOC_PSS ||
                              Resource.groupType == CONST_COMPAT_PS ||
                              Resource.groupType == CONST_COMPAT_SVC }">
                <td width="100%" class="FilterLabelText" height="32">&nbsp;</td>
              </c:when>
              <c:otherwise>
                <td width="100%" class="FilterLabelText" height="10">&nbsp;</td>
              </c:otherwise>
            </c:choose>

				</tr>
			</table>

		</td>
	</tr>

  <tr>
    <!--  SELECT COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div id="<c:out value="${widgetInstanceName}"/>FromDiv">

	<display:table padRows="true" rightSidebar="true" items="${AvailableResources}" var="resource" action="${selfPaAction}"
                    orderValue="soa" order="${param.soa}" sortValue="sca" sort="${param.sca}" pageValue="pna"
                    page="${param.pna}" pageSizeValue="psa" pageSize="${param.psa}" styleId="fromTable" width="100%"
                    cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="entityId.appdefKey" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"fromToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="availableResource" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column property="name" title="common.header.Name" width="40%"
                    sort="true" defaultSort="true" sortAttr="1"/>
          <c:choose>
          <c:when test="${Resource.groupType == CONST_COMPAT_PS &&
                          Resource.groupEntType == 2}">
          <display:column width="59%" property="hostName" title="resource.common.monitor.visibility.HostPlatformTH"/>
          </c:when>
          <c:when test="${Resource.groupType == CONST_COMPAT_SVC}">
          <display:column width="59%" property="hostName" title="resource.common.monitor.visibility.HostServerTH"/>
          </c:when>
          <c:otherwise>
          <display:column width="59%" property="id" title="resource.group.inventory.TypeTH">
            <display:resourcedecorator resource="${resource}" type="true"/>
          </display:column>
          </c:otherwise>
          </c:choose>
        </display:table>

      </div>
      <!--  /  -->

<!-- LIST ITEMS -->
<tiles:insertDefinition name=".ng.toolbar.new">
  <tiles:putAttribute name="useFromSideBar" value="true"/>
  <tiles:putAttribute name="listItems" value="${AvailableResources}"/>
  <tiles:putAttribute name="listSize" value="${NumAvailableResources}"/>
  <tiles:putAttribute name="pageSizeParam" value="psa"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfPsaAction}"/>
  <tiles:putAttribute name="pageNumParam" value="pna"/>
  <tiles:putAttribute name="pageNumAction" value="${selfPnaAction}"/>
</tiles:insertDefinition>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
     <div id="AddButtonDiv" align="left">
      <img src='<s:url value="/images/fb_addarrow_gray.gif"/>' border="0" titleKey="AddToList.ClickToAdd"/>
     </div>
      <br>&nbsp;<br>
     <div id="RemoveButtonDiv" align="right">
      <img src='<s:url value="/images/fb_removearrow_gray.gif"/>' border="0" titleKey="AddToList.ClickToRemove"/>
     </div>
    </td>
    <!-- / ADD/REMOVE COLUMN  -->

    <!--  ADD COLUMN  -->
    <td width="50%" valign="top">
      <!--  TABLED LIST CONTENTS (SELECT COLUMN) -->
      <div  id='<c:out value="${widgetInstanceName}"/>ToDiv'>

        <display:table padRows="true" leftSidebar="true" items="${PendingResources}" var="resource" action="${selfPpAction}"
                    orderValue="sop" order="${param.sop}" sortValue="scp" sort="${param.scp}" pageValue="pnp"
                    page="${param.pnp}" pageSizeValue="psp" pageSize="${param.psp}" styleId="toTable" width="100%"
                    cellpadding="0" cellspacing="0" border="0">
          <display:column width="1%" property="entityId.appdefKey" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"toToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
            <display:checkboxdecorator name="pendingResource" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column property="name" title="common.header.Name" width="40%"
                    sort="true" defaultSort="true" sortAttr="1"/>
          <c:choose>
          <c:when test="${Resource.groupType == CONST_COMPAT_PS &&
                          Resource.groupEntType == 2}">
          <display:column width="59%" property="hostName" title="resource.common.monitor.visibility.HostPlatformTH"/>
          </c:when>
          <c:when test="${Resource.groupType == CONST_COMPAT_SVC}">
          <display:column width="59%" property="hostName" title="resource.common.monitor.visibility.HostServerTH"/>
          </c:when>
          <c:otherwise>
          <display:column width="59%" property="id" title="resource.group.inventory.TypeTH">
            <display:resourcedecorator resource="${resource}" type="true"/>
          </display:column>
          </c:otherwise>
          </c:choose>
        </display:table>

      </div>
      <!--  /  -->

<tiles:insertDefinition name=".ng.toolbar.new">
  <tiles:putAttribute name="useToSideBar" value="true"/>
  <tiles:putAttribute name="listItems" value="${PendingResources}"/>
  <tiles:putAttribute name="listSize" value="${NumPendingResources}"/>
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

