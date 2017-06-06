<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="/common/Error2.jsp" %>
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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

<jsu:importScript path="/js/addRemoveWidget.js" />

<tiles:importAttribute name="noFilter" ignore="true"/>
<tiles:importAttribute name="addToListUrl" ignore="true"/>

<c:set var="widgetInstanceName" value="addResources"/>
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<c:url var="selfAction" value="${addToListUrl}" >
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
     <tiles:insertDefinition name=".header.tab">
      <tiles:putAttribute name="tabKey" value="resource.group.inventory.Edit.ResourcesTab"/>
      <tiles:putAttribute name="useFromSideBar" value="true"/>
	  <tiles:putAttribute name="portletName" value=""/>
     </tiles:insertDefinition>
    </td>
    <td><img src='<s:url value="/images/spacer.gif" />' width="40" height="1" border="0" /></td>
    <td>
     <tiles:insertDefinition name=".header.tab">
      <tiles:putAttribute name="tabKey" value="resource.group.inventory.Edit.AddResourcesTab"/>
      <tiles:putAttribute name="useToSideBar" value="true"/>
	  <tiles:putAttribute name="portletName" value=""/>
     </tiles:insertDefinition>
    </td>
  </tr>
  <c:choose>
  <c:when test="${noFilter}">
    <td width="50%" valign="top">
      <!--  FILTER TOOLBAR CONTENTS -->
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="FilterLine" colspan="2"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
          <td width="5"><img src='<s:url value="/images/spacer.gif" />' width="5" height="1" border="0" /></td>
        </tr>
        <tr>
          <td nowrap class="FilterLabelText" colspan="2"><fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>&nbsp;
								<s:textfield theme="simple" name="nameFilter" size="10" maxlength="55" onkeypress="if (event.keyCode == 13) return goToLocationSelfAndElement('nameFilter', 'nameFilter', '%{#attr.nfAction}');" />
					 
              <div style="display:inline;width:17px;"><img src='<s:url value="/images/4.0/icons/accept.png" />' onclick="goToLocationSelfAndElement('nameFilter', 'nameFilter', '${nfAction}');" /></div>
      <s:hidden name="ff" theme="simple" />
      <s:hidden name="ft" theme="simple" />
          </td>
          <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="30" border="0" ></td>
        </tr>
      </table>
    </td>
    <td><img src='<s:url value="/images/spacer.gif" />' width="40" height="1" border="0" /></td>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="1" border="0" /></td>
          <td class="FilterLine"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
        </tr>
        <tr>
          <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="32" border="0" /></td>
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
          <td class="FilterLine" colspan="2"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
          <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="1" border="0" /></td>
        </tr>
        <tr>
          <td class="FilterLabelText" nowrap align="right"><fmt:message key="Filter.ViewLabel"/></td>
          <td class="FilterLabelText" width="100%">      

            <s:select theme="simple"  cssStyle="FilterFormText" name="ff" list="functions" listValue="%{getText(value)}"  onchange="goToSelectLocation(this, 'ff', '%{#attr.ffAction}');" />
			
			<s:select theme="simple" cssStyle="FilterFormText" name="ft" list="types" listValue="%{getText(value)}" onchange="goToSelectLocation(this, 'ft', '%{#attr.ftAction}');" />

            
          </td>
          <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="30" border="0" /></td>
        </tr>
        <tr>
          <td nowrap class="FilterLabelText" colspan="2"><fmt:message key="resource.group.inventory.Edit.FilterByNameLabel"/>&nbsp;
				<s:textfield theme="simple" name="nameFilter" size="10" maxlength="55" onkeypress="if (event.keyCode == 13) return goToLocationSelfAndElement('nameFilter', 'nameFilter', '%{#attr.nfAction}');" />

              <div style="display:inline;width:17px;padding-left:5px;"><img src='<s:url value="/images/4.0/icons/accept.png" />' onclick="goToLocationSelfAndElement('nameFilter', 'nameFilter', '${nfAction}');"/></div>
          </td>
          <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="30" border="0" /></td>
        </tr>
      </table>
    </td>
    <td><img src='<s:url value="/images/spacer.gif" />' width="40" height="1" border="0" /></td>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="1" border="0" /></td>
          <td class="FilterLine"><img src='<s:url value="/images/spacer.gif" />'  width="1" height="1" border="0" /></td>
        </tr>
        <tr>
          <td><img src='<s:url value="/images/spacer.gif" />'  width="5" height="62" border="0" /></td>
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
            <td class="FilterLine" width="100%"><img src='<s:url value="/images/spacer.gif" />'  width="1" height="1" border="0" /></td>
            <td><img src='<s:url value="/images/spacer.gif" />'  width="1" height="1" border="0" /></td>
          </tr>
          <tr>
            <td nowrap class="FilterLabelText">
              <fmt:message key="resource.group.inventory.Edit.CompatibleResourceTypeLabel"/>
              <c:out value="${Resource.appdefResourceTypeValue.name}"/>
            </td>
            <td><img src='<s:url value="/images/spacer.gif" />'  width="5" height="1" border="0" /></td>
          </tr>
        </table>
      </td>
      <td>&nbsp;</td>
      <td valign="bottom">
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <tr>
            <td rowspan="2"><img src='<s:url value="/images/spacer.gif" />'  width="5" height="1" border="0" /></td>
            <td class="FilterLine" colspan="2"><img src='<s:url value="/images/spacer.gif" />'  width="1" height="1" border="0" /></td>
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
            <td class="FilterLine" width="100%" colspan="2"><img src='<s:url value="/images/spacer.gif" />'  width="1" height="1" border="0" /></td>
            <td><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
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
            <td><img src='<s:url value="/images/spacer.gif" />' width="5" height="1" border="0" /></td>
          </tr>

        </table>
      </td>
      <td>&nbsp;</td>
      <td valign="bottom">
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <tr>
            <td rowspan="2"><img src='<s:url value="/images/spacer.gif" />' width="5" height="1" border="0" /></td>
            <td class="FilterLine" colspan="2"><img src='<s:url value="/images/spacer.gif" />'  width="1" height="1" border="0" /></td>
          </tr>
          <tr>
            <td width="100%" class="FilterLabelText"><img src='<s:url value="/images/spacer.gif" />' width="5" height="20" border="0" /></td>
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
      <img src='<s:url value="/images/fb_addarrow_gray.gif" />'  border="0" titleKey="AddToList.ClickToAdd" />
     </div>
      <br>&nbsp;<br>
     <div id="RemoveButtonDiv" align="right">
      <img src='<s:url value="/images/fb_removearrow_gray.gif" />'  border="0" titleKey="AddToList.ClickToRemove" />
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

<%--tiles:insert definition=".diagnostics"/--%>
