<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="hq" prefix="hq" %>
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


<script language="JavaScript" src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript">
</script>


<c:set var="widgetInstanceName" value="addResources"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:url var="selfPnaAction" value="/resource/group/Inventory.do">
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

<c:url var="selfPnFilterNameAction" value="/resource/group/Inventory.do">
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

<c:url var="selfPnFilterAction" value="/resource/group/Inventory.do">
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

<c:url var="selfPnpAction" value="/resource/group/Inventory.do">
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

<c:url var="selfPsaAction" value="/resource/group/Inventory.do">
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

<c:url var="selfPspAction" value="/resource/group/Inventory.do">
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

<c:url var="selfPaAction" value="/resource/group/Inventory.do">
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

<c:url var="selfPpAction" value="/resource/group/Inventory.do">
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

<script language="JavaScript"> <!--
    function applyNameFilter() {
        goToLocationSelfAndElement(
                'nameFilter',
                'nameFilter',
                '<c:out value="${selfPnFilterNameAction}" escapeXml="false"/>');
        return false;
    }
// -->
</script>

<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<!--  SELECT & ADD -->
  <tr>
    <td width="50%" valign="top">
     <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="resource.group.inventory.Edit.ResourcesTab"/>
      <tiles:put name="useFromSideBar" value="true"/>
     </tiles:insert>
    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>
     <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="resource.group.inventory.Edit.AddResourcesTab"/>
      <tiles:put name="useToSideBar" value="true"/>
     </tiles:insert>
    </td>
  </tr>
	<tr>
		<td valign="bottom">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
        <c:choose>
            <c:when test="${Resource.groupType == CONST_ADHOC_PSS  }">
              <tr>
                  <td class="FilterLine" width="100%" colspan="4"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
                  <td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
              </tr>
              <tr>
                  <td nowrap class="FilterLabelText">
                      <fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>
                  </td>
                  <td class="FilterLabelText" valign="bottom">
                      <div style="float:left;display:inline;"><input type="text" name="nameFilter" maxlength="55" size="10"
                             onKeyPress="if (event.keyCode == 13) return applyNameFilter()"
                             value="<c:out value="${param.nameFilter}"/>"/></div>
                      <div style="display:inline;width:17px;padding-left:5px;padding-top:3px;"><html:img page="/images/dash-button_go-arrow.gif"
                             border="0" onclick="applyNameFilter()"/></div>
                  </td>
                  <td nowrap class="FilterLabelText">
                      <fmt:message key="resource.group.inventory.Edit.FilterByTypeLabel"/>
                  </td>
                  <td class="FilterLabelText">
                      <html:select property="filterBy" styleClass="FilterFormText"
                            onchange="goToSelectLocation(this, 'filterBy',  '${selfPnFilterAction}');">
                          <hq:optionMessageList property="availResourceTypes" baseKey="resource.hub.filter"/>
                      </html:select>
                  </td>
                  <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
              </tr>
            </c:when>
            <c:when test="${ Resource.groupType == CONST_COMPAT_PS ||
                             Resource.groupType == CONST_COMPAT_SVC }">
              <tr>
                  <td class="FilterLine" width="100%" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1"
border="0"/></td>
                  <td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
              </tr>
              <tr>
                  <td nowrap class="FilterLabelText" colspan=2>
                      <div style="float:left;display:inline;"><fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>&nbsp;
                      <input type="text" name="nameFilter" maxlength="55" size="10"
                             onKeyPress="if (event.keyCode == 13) return applyNameFilter()"
                             value="<c:out value="${param.nameFilter}"/>"/></div>

                      <div style="display:inline;width:17px;padding-left:5px;padding-top:3px;"><html:img page="/images/dash-button_go-arrow.gif"
                             border="0" onclick="applyNameFilter()"/></div>
                  </td>
                  <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
              </tr>
            </c:when>
            <c:when test="${Resource.groupType == CONST_ADHOC_GRP }">
              <tr>
                        <td class="FilterLine" width="100%" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
                        <td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
                    </tr>
                    <tr>
                <td nowrap class="FilterLabelText">
                  <fmt:message key="resource.group.inventory.Edit.FilterByTypeLabel"/>
                </td>
                <td class="FilterLabelText">
                  <html:select property="filterBy" styleClass="FilterFormText" onchange="goToSelectLocation(this, 'filterBy',  '${selfPnFilterAction}');">
                    <html:optionsCollection property="availResourceTypes"/>
                  </html:select><br>
                </td>
                <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
                    </tr>
            </c:when>
            <c:otherwise >
              <tr>
                        <td class="FilterLine" width="100%"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
                        <td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
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
                <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
                    </tr>
            </c:otherwise>

        </c:choose>
			</table>

		</td>
		<td>&nbsp;</td>
		<td valign="bottom">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
				<tr>
					<td rowspan="2"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
					<td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
				</tr>
				<tr>
           <%--
             -- Support three different spacer patterns. The first is for adhoc
             -- grp of grp where you have a single selector. The second supports
             -- a height of 32 pixels to accomodate the slector and name filter.
             -- third is a normal spacer pattern.  --%>
          <c:choose>
              <c:when test="${ Resource.groupType == CONST_ADHOC_GRP }">
                <td width="100%" class="FilterLabelText"><html:img page="/images/spacer.gif" width="5" height="18" border="0"/></td>
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
<tiles:insert definition=".toolbar.new">
  <tiles:put name="useFromSideBar" value="true"/>
  <tiles:put name="listItems" beanName="AvailableResources"/>
  <tiles:put name="listSize" beanName="NumAvailableResources"/>
  <tiles:put name="pageSizeParam" value="psa"/>
  <tiles:put name="pageSizeAction" beanName="selfPsaAction"/>
  <tiles:put name="pageNumParam" value="pna"/>
  <tiles:put name="pageNumAction" beanName="selfPnaAction"/>
</tiles:insert>

    </td>
    <!-- / SELECT COLUMN  -->

    <!--  ADD/REMOVE COLUMN  -->
    <td id="<c:out value="${widgetInstanceName}"/>AddRemoveButtonTd">
     <div id="AddButtonDiv" align="left">
      <html:img page="/images/fb_addarrow_gray.gif" border="0" titleKey="AddToList.ClickToAdd"/>
     </div>
      <br>&nbsp;<br>
     <div id="RemoveButtonDiv" align="right">
      <html:img page="/images/fb_removearrow_gray.gif" border="0" titleKey="AddToList.ClickToRemove"/>
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

<tiles:insert definition=".toolbar.new">
  <tiles:put name="useToSideBar" value="true"/>
  <tiles:put name="listItems" beanName="PendingResources"/>
  <tiles:put name="listSize" beanName="NumPendingResources"/>
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

