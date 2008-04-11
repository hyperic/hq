<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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


<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="savedQueriesList"/>
<c:url var="selfAction" value="/dashboard/Admin.do?mode=savedQueries"/>

<script  src="<html:rewrite page="/js/prototype.js"/>" type="text/javascript"></script>
<script  src="<html:rewrite page="/js/scriptaculous.js"/>" type="text/javascript"></script>
<script type="text/javascript">
  var help = "<hq:help/>";
</script>

<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listRoles"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PageTitle" nowrap><fmt:message key="dash.home.SavedQueries.Title"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='2'>
      <html:form action="/dashboard/ModifySavedQueries.do" onsubmit="SavedQueriesForm.order.value=Sortable.serialize('qryOrd')">
      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.SelectedCharts"/>
      </tiles:insert>
           
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

      <html:hidden property="order"/>

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
          <html:link page="${chart.value}"><c:out value="${chart.key}"/></html:link>
        </span>
        </li>
        </c:forEach>
      </ul>

      <script type="text/javascript">
      <!--
        Sortable.create("qryOrd",
          {dropOnEmpty:true,containment:["qryOrd"],constraint:false});
      -->
      </script>
          <c:choose>
             <c:when test="${not sessionScope.modifyDashboard}">
             </c:when>
             <c:otherwise>
                 <tiles:insert definition=".toolbar.list">
                     <tiles:put name="deleteOnly" value="true"/>
                     <%--none of this is being used--%>
                     <tiles:put name="listItems" value="${chartsize}"/>
                     <tiles:put name="listSize" value="${chartsize}"/>
                     <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
                     <tiles:put name="pageSizeAction" beanName="selfAction"/>
                     <tiles:put name="pageNumAction" beanName="selfAction"/>
                     <tiles:put name="defaultSortColumn" value="1"/>
                 </tiles:insert>
             </c:otherwise>
          </c:choose>
      <tiles:insert definition=".form.buttons">
      <c:if test='${not sessionScope.modifyDashboard}'>
        <tiles:put name="cancelOnly" value="true"/>
        <tiles:put name="noReset" value="true"/>
      </c:if>
      </tiles:insert>
      </html:form>
    </td>
  </tr>
  <tr> 
    <td colspan="4"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
