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
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<tiles:importAttribute name="portlet"/>

<jsu:script>
	function requestMetricsResponse${portlet.token}() {
        hqDojo.xhrGet({ 
            url: "<s:url value="/dashboard/ViewMetricViewer.do?"/>", 
            handleAs: "json", 
            content: { 
                hq: (new Date()).getTime(), 
                token: "<c:out value="${portlet.token}"/>" 
            }, 
            load: function(response, args) { 
                showMetricsResponse(response, args); 
                setTimeout("requestMetricsResponse<c:out value="${portlet.token}"/>()", portlets_reload_time); 
            }, 
            error: function(response, args) { 
                reportError(response, args); 
                setTimeout("requestMetricsResponse<c:out value="${portlet.token}"/>()", portlets_reload_time); 
            } 
        }); 
	}
	
	hqDojo.ready(function() {
		requestMetricsResponse${portlet.token}();
	});
</jsu:script>

<div class="effectsPortlet">
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="dash.home.MetricViewer"/>
  <tiles:putAttribute name="subTitle" value="${sessionScope.portlet.description}"/>
  <tiles:putAttribute name="adminUrl" value="${sessionScope.adminUrl.bean_property}" />
  <tiles:putAttribute name="portletName" value="${sessionScope.portlet.fullUrl}"/>
</tiles:insertDefinition>
  <c:if test="${not empty portlet.token}">
    <tiles:putAttribute name="adminToken" value="${portlet.token}"/>
    <c:set var="tableName" value="metricTable${portlet.token}"/>
    <c:set var="noTableName" value="noMetricTable${portlet.token}"/>
  </c:if>
  <c:if test="${empty portlet.token}">
    <c:set var="tableName" value="metricTable"/>
    <c:set var="noTableName" value="noMetricTable"/>
  </c:if>
  <table width="100%" border="0" cellspacing="0" cellpadding="0" id="<c:out value="${tableName}"/>" class="portletLRBorder">
      <tbody id="mtbody">
    <!-- table rows are inserted here dynamically -->
    </tbody>

 </table>
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="<c:out value="${noTableName}"/>" style="display:none;" class="portletLRBorder">
      <tbody>
    <tr class="ListRow">
            <td class="ListCell">
                <c:url var="path" value="/images/4.0/icons/properties.gif"/>
                <fmt:message key="dash.home.add.resources.to.display">
                  <fmt:param value="${path}"/>
                </fmt:message>
            </td>
    </tr>
      </tbody>
  </table>

</div>
