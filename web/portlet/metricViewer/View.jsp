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

<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-c" prefix="c" %>

<tiles:importAttribute name="portlet"/>

<script type="text/javascript">
function requestMetricsResponse<c:out value="${portlet.token}"/>() {
    var dummyStr = '&hq=' + new Date().getTime();
    var metricsUrl = "<html:rewrite page="/dashboard/ViewMetricViewer.do?token=${portlet.token}"/>" + dummyStr;
	new Ajax.Request(metricsUrl, {method: 'get', onSuccess:showMetricsResponse});
}
onloads.push(requestMetricsResponse<c:out value="${portlet.token}"/>);
</script>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.MetricViewer"/>
  <tiles:put name="subTitle" beanName="portlet" beanProperty="description"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />

  <c:if test="${not empty portlet.token}">
    <tiles:put name="adminToken" beanName="portlet" beanProperty="token"/>
    <c:set var="tableName" value="metricTable${portlet.token}"/>
    <c:set var="noTableName" value="noMetricTable${portlet.token}"/>
  </c:if>
  <c:if test="${empty portlet.token}">
    <c:set var="tableName" value="metricTable"/>
    <c:set var="noTableName" value="noMetricTable"/>
  </c:if>
  <tiles:put name="portletName"><c:out value="${portlet.fullUrl}"/></tiles:put>
</tiles:insert>

  <table width="100%" border="0" cellspacing="0" cellpadding="0" id="<c:out value="${tableName}"/>" class="portletLRBorder">
      <tbody id="mtbody">
    <!-- table rows are inserted here dynamically -->
    </tbody>

 </table>
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="<c:out value="${noTableName}"/>" style="display:none;" class="portletLRBorder">
      <tbody>
    <tr class="ListRow">
            <td class="ListCell">
                <c:url var="path" value="/"/>
                <fmt:message key="dash.home.add.resources.to.display">
                  <fmt:param value="${path}"/>
                </fmt:message>
            </td>
    </tr>
      </tbody>
  </table>

</div>
