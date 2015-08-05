<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
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


<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="savedQueriesList"/>
<c:url var="selfAction" value="/dashboard/Admin.action">
	<c:param name="mode" value="savedQueries"/>
</c:url>
<c:set var="widgetInstanceName" value="listRoles"/>
<jsu:importScript path="/js/prototype.js" />
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var help = "<hq:help/>";
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
         
    <td rowspan="99"><img src= '<s:url value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0"/></td>
    <td><img src='<s:url value="/images/spacer.gif"/>' width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PortletTitle" nowrap><fmt:message key="dash.home.SavedQueries.Title"/></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif"/>' width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><img src='<s:url  value= "/images/spacer.gif"/>' width="1" height="1" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"></td>
    <td colspan="3"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='2'>
      <s:form action="updateSavedQueriesModifyPortlet" id= "SavedQueriesForm" >
      <div id="narrowlist_false">
      <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.SelectedCharts"/>
		<tiles:putAttribute name="portletName" value=""/>
      </tiles:insertDefinition>
           </div>
      <table class="table" width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr class="tableRowHeader">
          <th width="100%" class="tableRowSorted">
          <c:choose>
          <c:when test="${not sessionScope.modifyDashboard}">
            <input type="checkbox" onclick="ToggleAll(this, widgetProperties)" name="listToggleAll" disabled="true">
          </c:when>
          <c:otherwise>
            <input type="checkbox" onclick="ToggleAll(this, widgetProperties)" name="listToggleAll">
          </c:otherwise>
          </c:choose>
          <fmt:message key="dash.settings.ListHeader.ResourceChart"/></th>
        </tr>
      </table>

      <s:hidden theme="simple" name="order" id="order" />

      <ul id="qryOrd" class="boxy" style="background-color: #F2F4F7;">
        <c:forEach var="chart" items="${charts}" varStatus="status">
        <li class="tableCell" id="<c:out value="item_${status.count}"/>">
        <span style="cursor: move;">
          <c:choose>
          <c:when test="${not sessionScope.modifyDashboard}">
            <input type="checkbox" onclick="ToggleSelection(this, widgetProperties)" disabled="true" class="listMember" name="charts" value="|<c:out value="${chart.key},${chart.value}"/>">             
          </c:when>
          <c:otherwise>
            <input type="checkbox" onclick="ToggleSelection(this, widgetProperties)" class="listMember" name="charts" value="|<c:out value="${chart.key},${chart.value}"/>">
          </c:otherwise>
          </c:choose>
          <s:a href="%{#attr.chart.value}">${chart.key}</s:a>
        </span>
        </li>
        </c:forEach>
      </ul>
          <c:choose>
             <c:when test="${not sessionScope.modifyDashboard}">
             </c:when>
             <c:otherwise>
                 <tiles:insertDefinition  name=".toolbar.list">
                     <tiles:putAttribute name="deleteOnly" value="true"/>
                     <%--none of this is being used--%>
                     <tiles:putAttribute name="listItems" value="${chartsize}"/>
                     <tiles:putAttribute name="listSize" value="${chartsize}"/>
                     <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
                     <tiles:putAttribute name="pageSizeAction" value="${selfAction}"/>
                     <tiles:putAttribute name="pageNumAction" value="${selfAction}"/>
                     <tiles:putAttribute name="defaultSortColumn" value="1"/>
                 </tiles:insertDefinition>
             </c:otherwise>
          </c:choose>
		 

      <tiles:insertDefinition name=".form.buttons">
      <c:if test='${sessionScope.modifyDashboard}'>
        <tiles:putAttribute name="cancelAction" value="cancelSavedQueriesModifyPortlet"/>
        <tiles:putAttribute name="resetAction" value="resetSavedQueriesModifyPortlet"/>
      </c:if>
      </tiles:insertDefinition>
      </s:form>
    </td>
  </tr>
  <tr> 
    <td colspan="4"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
