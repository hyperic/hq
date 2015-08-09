<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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


<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="resources"/>
<c:url var="selfAction" value="resourceHealthPortletControl.action">
</c:url>
<script  src="<s:url value="/js/prototype.js" />" type="text/javascript"></script>
<script  src="<s:url value="/js/scriptaculous.js" />" type="text/javascript"></script>
<script  src="<s:url value="/js/listWidget.js" />" type="text/javascript"></script>

<jsu:script>
    var pageData = new Array();
    initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
    widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
    var help = '<hq:help/>';
</jsu:script>

<c:set var="listSize" value="${fn:length(resourceHealthList)}" />

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><img src='<s:url  value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0"/></td>
    <td><img src='<s:url  value="/images/spacer.gif"/>' width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PortletTitle" nowrap><fmt:message key="dash.home.ResourceHealth.Settings.Title"/></td>
    <td width="32%"><img src='<s:url  value="/images/spacer.gif"/>' width="2" height="32" alt="" border="0"/></td>
    <td width="1%"><img src='<s:url  value="/images/spacer.gif"/>' width="1" height="1" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"></td>
    <td colspan="3"><img src='<s:url  value="/images/spacer.gif"/>' width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan="2">
      <s:form action="updateResourceHealthModifyPortlet" onsubmit="ResourceHealthForm.order.value=Sortable.serialize('resOrd')"  id="ResourceHealthForm" >
	  <s:hidden theme="simple" id="key" name="key" value=".ng.dashContent.resourcehealth.resources"/>
<div id="narrowlist_false">
	  <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.SelectedResources"/>
		<tiles:putAttribute name="portletName" value=""/>
      </tiles:insertDefinition>
</div>
    <table class="table" class="table" width="100%" border="0" cellspacing="0" cellpadding="0">
    <tr class="tableRowHeader">
    <th width="1%" class="ListHeaderCheckbox">
        <c:choose>
        <c:when test="${not sessionScope.modifyDashboard}">
            <input type="checkbox" onclick="ToggleAll(this, widgetProperties, true)" name="listToggleAll" disabled="true">
        </c:when>
        <c:otherwise>
            <input type="checkbox" onclick="ToggleAll(this, widgetProperties, true)" name="listToggleAll">
        </c:otherwise>
        </c:choose>
    </th>
    <th class="tableRowInactive"><fmt:message key="dash.settings.ListHeader.Resource"/></th>
    </tr></table>

      <ul id="resOrd" class="boxy" style="background-color: #F2F4F7;">
      <c:forEach var="resource" items="${resourceHealthList}">
        <li class="tableCell" id="<c:out value="item_${resource.entityId}"/>">
        <span style="cursor: move;">
		<!-- Add HTML Checkbox -->
		<s:checkbox theme="simple" class="listMember" id="ids" name="ids" fieldValue="%{#attr.resource.entityId}" value="false" disabled="%{!#attr.modifyDashboard}" onclick="ToggleSelection(this, widgetProperties, true)" cssStyle="listMember" />
        <c:out value="${resource.name}"/>
        <c:if test="${not empty resource.description}">
          <fmt:message key="parenthesis">
            <fmt:param>
                <c:out value="${resource.description}"/>
            </fmt:param>
          </fmt:message>
        </c:if></span>
        </li>
      </c:forEach>


      </ul>
      <script type="text/javascript">
        Sortable.create("resOrd",
          {dropOnEmpty:true,containment:["resOrd"],constraint:'vertical'});   
      </script>
       <c:choose>
              <c:when test="${not sessionScope.modifyDashboard}">
               
              </c:when>
              <c:otherwise>
                  <tiles:insertDefinition name=".ng.toolbar.addToList">
                      <tiles:putAttribute name="addToListUrl" value="resourceHealthAddResourcesPortletControl.action"  />
                      <tiles:putAttribute name="listItems" value="${resourceHealthList}"/>
                      <tiles:putAttribute name="listSize" value="${listSize}" />
                      <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
                      <tiles:putAttribute name="pageSizeAction" value="${selfAction}"/>
                      <tiles:putAttribute name="pageNumAction" value="${selfAction}"/>
                      <tiles:putAttribute name="defaultSortColumn" value="1"/>
                  </tiles:insertDefinition>
              </c:otherwise>
          </c:choose>
      <s:hidden theme="simple" name="order"/>
	  <tiles:insertDefinition name=".form.buttons">
		  <c:if test='${sessionScope.modifyDashboard}'>
			<tiles:putAttribute name="cancelAction"  value="cancelResourceHealthModifyPortlet" />
			<tiles:putAttribute name="resetAction"   value="resetResourceHealthModifyPortlet" />
		  </c:if>
      </tiles:insertDefinition>
      </s:form>
		
    </td>
  </tr>
  <tr> 
    <td colspan="3"><img src='<s:url  value="/images/spacer.gif"/>' width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
