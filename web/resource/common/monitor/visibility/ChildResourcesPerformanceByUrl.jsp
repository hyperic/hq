<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
<%@ taglib uri="display" prefix="display" %>
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


<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="crpbuWidget" value="childResourcesPerformanceByUrl"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${crpbuWidget}"/>');
crpbuWidgetProps = getWidgetProperties('<c:out value="${crpbuWidget}"/>');
</script>

<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="detailMode" ignore="true"/>
<tiles:importAttribute name="useChart" ignore="true"/>
<tiles:importAttribute name="selfAction"/>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_CHART_SMSR"
                 var="MODE_MON_CHART_SMSR"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_URL" var="MODE_MON_URL"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="SORTORDER_DEC" var="SORTORDER_DEC"/>

<c:if test="${not empty resource}">
  <c:set var="rid" value="${resource.id}"/>
  <c:set var="type" value="${resource.entityId.type}"/>
</c:if>

<%-- for v1, we don't show any charts --%>
<c:set var="useChart" value="false"/>

<%-- the usual sort order default is asc, but for this page we want it
  -- to be descending, so we have to set our own var --%>
<c:choose>
  <c:when test="${not empty param.so}">
    <c:set var="so" value="${param.so}"/>
  </c:when>
  <c:otherwise>
    <c:set var="so" value="${SORTORDER_DEC}"/>
  </c:otherwise>
</c:choose>

<c:url var="psAction" value="${selfAction}">
  <c:param name="so" value="${so}"/>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.low}">
    <c:param name="low" value="${PerformanceForm.low}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.avg}">
    <c:param name="avg" value="${PerformanceForm.avg}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.peak}">
    <c:param name="peak" value="${PerformanceForm.peak}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.req}">
    <c:param name="req" value="${PerformanceForm.req}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.worst}">
    <c:param name="worst" value="${PerformanceForm.worst}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
  <c:param name="so" value="${so}"/>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.low}">
    <c:param name="low" value="${PerformanceForm.low}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.avg}">
    <c:param name="avg" value="${PerformanceForm.avg}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.peak}">
    <c:param name="peak" value="${PerformanceForm.peak}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.req}">
    <c:param name="req" value="${PerformanceForm.req}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.worst}">
    <c:param name="worst" value="${PerformanceForm.worst}"/>
  </c:if>
</c:url>

<c:url var="sAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.low}">
    <c:param name="low" value="${PerformanceForm.low}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.avg}">
    <c:param name="avg" value="${PerformanceForm.avg}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.peak}">
    <c:param name="peak" value="${PerformanceForm.peak}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.req}">
    <c:param name="req" value="${PerformanceForm.req}"/>
  </c:if>
  <c:if test="${not empty PerformanceForm.worst}">
    <c:param name="worst" value="${PerformanceForm.worst}"/>
  </c:if>
</c:url>

<c:url var="iconPath" value="/images/icon_chart.gif"/>

<!-- CHILD RESOURCES CONTENTS -->
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="pbuListTable">
    <tr>
      <td class="ListCellHeader"><fmt:message key="resource.common.monitor.visibility.performance.DetailForURLEtc"></fmt:message></td>
    </tr>
  </table>

<div id="pbuListDiv">
<c:choose>
  <c:when test="${not empty summaries}">
    <display:table items="${summaries}" var="summary" action="${sAction}" width="100%" cellspacing="0" cellpadding="0" order="${so}">

    <c:if test="${detailMode ne 'appUrl' && useChart}">
    <display:column width="1%" property="me.name" title="<input type=\"checkbox\" onclick=\"ToggleAllChart(this, crpbuWidgetProps, 'add', 'availableListMember')\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
      <display:checkboxdecorator name="url" onclick="ToggleSelectionTwoButtons(this, crpbuWidgetProps, 'add')" styleClass="availableListMember"/>
    </display:column>
    <display:column width="1%" value="<img src=\"${iconPath}\" height=\"10\" width=\"10\" border=\"0\" alt=\"\">" href="/resource/common/monitor/Visibility.do?mode=${MODE_MON_CHART_SMSR}&rid=${rid}&type=${type}" paramId="url" paramName="summary" paramProperty="me.name" headerColspan="2" title="resource.common.monitor.visibility.URLsTH" sort="true" sortAttr="24" styleClass="ListCell"/>
    </c:if>

    <c:choose>
      <c:when test="${detailMode eq 'appUrl'}">
    <display:column width="67%" value="${summary.me.name}" href="/resource/application/monitor/Visibility.do?mode=${MODE_MON_URL}&rid=${rid}&type=${type}" paramId="url" paramName="summary" paramProperty="me.name" title="resource.common.monitor.visibility.URLsTH" sort="true" sortAttr="24" styleClass="ListCell"/>
      </c:when>
      <c:when test="${useChart}">
    <display:column width="67%" value="${summary.me.name}" href="/resource/common/monitor/Visibility.do?mode=${MODE_MON_CHART_SMSR}&rid=${rid}&type=${type}" paramId="url" paramName="summary" paramProperty="me.name"/>
      </c:when>
      <c:otherwise>
    <display:column width="67%" property="me.name" title="resource.common.monitor.visibility.URLsTH" sort="true" sortAttr="24" styleClass="ListCell" nowrap="true">
      <display:pathdecorator preChars="8" postChars="20" strict="true" styleClass="ListCellPopup4"/>
    </display:column>
      </c:otherwise>
    </c:choose>

    <display:column width="8%" property="requestCount" title="resource.common.monitor.visibility.RequTH" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine"/>
    <display:column width="8%" property="low.total" title="resource.common.monitor.visibility.LowTH" sort="true" sortAttr="25" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
      <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
    </display:column>
    <display:column width="8%" property="avg.total" title="resource.common.monitor.visibility.AvgTH" sort="true" defaultSort="true" sortAttr="26" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
      <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
    </display:column>
    <display:column width="8%" property="peak.total" title="resource.common.monitor.visibility.PeakTH" sort="true" sortAttr="27" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
      <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
    </display:column>
  </display:table>
  </c:when>
  <c:otherwise>
<tiles:insert definition=".resource.common.monitor.visibility.noPerfs"/>
  </c:otherwise>
</c:choose>
</div>

<tiles:insert definition=".resource.common.monitor.visibility.metricsToolbar">
  <tiles:put name="widgetInstanceName" beanName="crpbuWidget"/>
  <tiles:put name="useChartButton" beanName="useChart"/>
  <tiles:put name="useCurrentButton" value="false"/>
  <tiles:put name="usePager" value="true"/>
  <tiles:put name="listItems" beanName="summaries"/>
  <tiles:put name="listSize" beanName="summaries" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="psAction"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="defaultSortColumn" value="26"/>
</tiles:insert>
<!--  /  -->
