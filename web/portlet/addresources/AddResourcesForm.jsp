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


<script  src="<html:rewrite page="/js/addRemoveWidget.js"/>" type="text/javascript">
</script>

<tiles:importAttribute name="noFilter" ignore="true"/>

<c:set var="widgetInstanceName" value="addResources"/>

<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:url var="selfAction" value="/Admin.do" context="/dashboard">
  <c:param name="mode" value="${param.mode}"/>
  <c:param name="key" value="${param.key}"/>  
  <c:if test="${not empty param.token}">
    <c:param name="token" value="${param.token}"/>
  </c:if>
</c:url>

<c:url var="selfPnaAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
  </c:if>
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

<c:url var="selfPnFilterAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
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

<c:url var="ffAction" value="${selfAction}">
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

<c:url var="ftAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
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

<c:url var="selfPnpAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
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

<c:url var="selfPsaAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
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

<c:url var="selfPspAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
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

<c:url var="selfPaAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
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

<c:url var="selfPpAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
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

<c:url var="nfAction" value="${selfAction}">
  <c:if test="${not empty param.ff}">
    <c:param name="ff" value="${param.ff}"/>
  </c:if>  
  <c:if test="${not empty param.ft}">
    <c:param name="ft" value="${param.ft}"/>
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
</c:url>


<!--  SELECT & ADD -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
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
  <c:choose>
  <c:when test="${noFilter}">
    <td width="50%" valign="top">
      <!--  FILTER TOOLBAR CONTENTS -->
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
          <td width="5"><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
        </tr>
        <tr>
          <td nowrap class="FilterLabelText" colspan="2"><fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>&nbsp;
              <html:text property="nameFilter" maxlength="55" size="10"
                     onkeypress="if (event.keyCode == 13) return goToLocationSelfAndElement('nameFilter', 'nameFilter', '${nfAction}');" /></div>

              <div style="display:inline;width:17px;padding-left:5px;"><html:img page="/images/4.0/icons/accept.png"
                     border="0" onclick="goToLocationSelfAndElement('nameFilter', 'nameFilter', '${nfAction}');"/></div>
      <html:hidden property="ff"/>
      <html:hidden property="ft"/>
          </td>
          <td><html:img page="/images/spacer.gif" width="5" height="30" border="0"/></td>
        </tr>
      </table>
    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
          <td class="FilterLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
        <tr>
          <td><html:img page="/images/spacer.gif" width="5" height="32" border="0"/></td>
          <td width="100%" class="FilterLabelText">&nbsp;</td>
        </tr>
      </table>
    </td>
  </tr>
  </c:when>
  <c:otherwise>
  <tr>
    <td width="50%" valign="top">
      <!--  FILTER TOOLBAR CONTENTS -->
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
          <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
        </tr>
        <tr>
          <td class="FilterLabelText" nowrap align="right"><fmt:message key="Filter.ViewLabel"/></td>
          <td class="FilterLabelText" width="100%">      
            <html:select property="ff" styleClass="FilterFormText" size="1" onchange="goToSelectLocation(this, 'ff', '${ffAction}');">
              <hq:optionMessageList property="functions" baseKey="resource.hub.filter"/>
            </html:select>
            
            <html:select property="ft" styleClass="FilterFormText" size="1" onchange="goToSelectLocation(this, 'ft', '${ftAction}');">
              <html:option value="-1" key="resource.hub.filter.AllResourceTypes"/>
              <html:optionsCollection property="types"/>
            </html:select>
            
          </td>
          <td><html:img page="/images/spacer.gif" width="5" height="30" border="0"/></td>
        </tr>
        <tr>
          <td nowrap class="FilterLabelText" colspan="2"><fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>&nbsp;
              <html:text property="nameFilter" maxlength="55" size="10"
                     onkeypress="if (event.keyCode == 13) return goToLocationSelfAndElement('nameFilter', 'nameFilter', '${nfAction}');" /></div>

              <div style="display:inline;width:17px;padding-left:5px;"><html:img page="/images/4.0/icons/accept.png"
                     border="0" onclick="goToLocationSelfAndElement('nameFilter', 'nameFilter', '${nfAction}');"/></div>
          </td>
          <td><html:img page="/images/spacer.gif" width="5" height="30" border="0"/></td>
        </tr>
      </table>
    </td>
    <td><html:img page="/images/spacer.gif" width="40" height="1" border="0"/></td>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
          <td class="FilterLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
        <tr>
          <td><html:img page="/images/spacer.gif" width="5" height="62" border="0"/></td>
          <td width="100%" class="FilterLabelText">&nbsp;</td>
        </tr>
      </table>
    </td>
  </tr>
  </c:otherwise>
  </c:choose>
  <c:if test="${Resource.groupType == 2}"> <!-- AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT-->
    <tr>
      <td valign="bottom">
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <tr>
            <td class="FilterLine" width="100%"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
            <td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
          </tr>
          <tr>
            <td nowrap class="FilterLabelText">
              <fmt:message key="resource.group.inventory.Edit.CompatibleResourceTypeLabel"/>
              <c:out value="${Resource.appdefResourceTypeValue.name}"/>
            </td>
            <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
          </tr>
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
            <td width="100%" class="FilterLabelText">&nbsp;</td>
          </tr>
        </table>
      </td>
    </tr>
  </c:if> 
  <c:if test="${Resource.groupType == 1}"> <!-- AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC--> 
    <tr>
      <td valign="bottom">
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <tr>
            <td class="FilterLine" width="100%" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
            <td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
          </tr>
          <tr>
            <td nowrap class="FilterLabelText">
              <fmt:message key="resource.group.inventory.Edit.FilterByTypeLabel"/>
            </td>
              <td class="FilterLabelText">
                  <c:choose>
                      <c:when test="${not sessionScope.modifyDashboard}">
                          <c:out value="${DashboardAddResourceHealthForm.filterBy}"/>
                      </c:when>
                      <c:otherwise>
                          <html:select property="filterBy" styleClass="FilterFormText"
                                       onchange="goToSelectLocation(this, 'filterBy',  '${selfPnFilterAction}');">
                              <html:option value="-1" key="resource.group.inventory.filter.AllResourceTypes"/>
                              <html:optionsCollection property="types"/>
                          </html:select>
                      </c:otherwise>
                  </c:choose>
                  <br>
              </td>
            <td><html:img page="/images/spacer.gif" width="5" height="1" border="0"/></td>
          </tr>

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
            <td width="100%" class="FilterLabelText"><html:img page="/images/spacer.gif" width="5" height="20" border="0"/></td>
          </tr>
        </table>
      </td>
    </tr>
  </c:if> 
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
            <display:checkboxdecorator name="availableResources" onclick="ToggleSelection(this, widgetProperties)" styleClass="availableListMember"/>
          </display:column>
          <display:column property="name" title="common.header.Name" width="40%" 
                    sort="true" defaultSort="true" sortAttr="1"/>
          <display:column property="description" title="common.header.Description" width="59%"/>
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
            <display:checkboxdecorator name="pendingResources" onclick="ToggleSelection(this, widgetProperties)" styleClass="pendingListMember"/>
          </display:column>
          <display:column property="name" title="common.header.Name" width="40%" 
                    sort="true" defaultSort="true" sortAttr="1"/>
          <display:column property="description" title="common.header.Description" width="59%"/>
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

<%--tiles:insert definition=".diagnostics"/--%>
