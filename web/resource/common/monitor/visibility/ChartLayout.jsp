<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<c:choose>
<c:when test="${not empty toDashboard}">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td rowspan="3" class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="4">
        <tr class="ListHeader">
          <td><fmt:message key="dash.home.SavedQueries"/></td>
        </tr>
        <tr class="ListRow">
          <td>
            <fmt:message key="resource.common.monitor.visibility.error.ChartRemoved"/>
          </td>
        </tr>
        <tr class="ListRow">
          <td>
            <html:link page="/Dashboard.do"><fmt:message key="alert.current.detail.link.noresource.Rtn"/></html:link>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</c:when>
<c:otherwise>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_CHART_SMSR"
                 var="MODE_MON_CHART_SMSR"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_CHART_MMSR"
                 var="MODE_MON_CHART_MMSR"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_CHART_SMMR"
                 var="MODE_MON_CHART_SMMR"/>

<script  src="<html:rewrite page="/js/chart.js"/>" type="text/javascript"></script>
<script type="text/javascript">
    function RefreshChartForm() {
        var forms = document.getElementsByTagName('form');

          for (i = 0; i < forms.length; i++) {
           //alert(forms[i].name)
          document.forms[0].submit();
    }

 }

setInterval("RefreshChartForm()",300000); // 5 minute page refresh  300000
</script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="4">
      <c:choose>
      <c:when test="${param.mode == MODE_MON_CHART_MMSR}">
      <c:set var="metricName"><fmt:message key="resource.common.monitor.visibility.MultipleMetric"/></c:set>
      </c:when>
      <c:otherwise>
      <c:set var="metricName" value="${ViewChartForm.chartName}"/>
      </c:otherwise>
      </c:choose>
      <tiles:insert definition=".page.title.resource.generic">
      <tiles:put name="titleKey" value="resource.common.monitor.visibility.SingleResourceChartPageTitle"/>
      <c:choose>
      <c:when test="${not empty ViewChartForm.ctype}">
        <tiles:put name="titleName"><hq:inventoryHierarchy resource="${Resource.entityId.appdefKey}" ctype="${ViewChartForm.ctype}"/></tiles:put>
      </c:when>
      <c:otherwise>
        <tiles:put name="titleName"><hq:inventoryHierarchy resource="${Resource.entityId.appdefKey}" /></tiles:put>
      </c:otherwise>
      </c:choose>
      <tiles:put name="subTitleName" beanName="metricName"/>

      </tiles:insert>
    </td>
  </tr>
  <tr>
    <td class="PageTitle">
      <html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/>
    </td>
    <td>
      <html:img page="/images/spacer.gif" width="75" height="1" alt="" border="0"/>
    </td>
    <td width="100%">
      <tiles:insert definition=".portlet.confirm"/>
      <html:form action="/resource/common/monitor/visibility/ViewChart">
      <html:hidden property="chartName" value="${Resource.name}: ${metricName}"/>
      <c:choose>
      <c:when test="${param.mode == MODE_MON_CHART_SMSR}">
      <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.chart"/>
      &nbsp;<br>
      <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.partrsrcs"/>
      &nbsp;<br>
      </c:when>
      
      <c:when test="${param.mode == MODE_MON_CHART_MMSR}">
      <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.chart">
      <tiles:put name="multiMetric" value="true"/>
      </tiles:insert>
      &nbsp;<br>
      <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.partrsrcs">
      <tiles:put name="multiMetric" value="true"/>
      </tiles:insert>
      &nbsp;<br>
      </c:when>
      
      <c:when test="${param.mode == MODE_MON_CHART_SMMR}">
      <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.chart">
      <tiles:put name="multiResource" value="true"/>
      </tiles:insert>
      &nbsp;<br>
      <tiles:insert definition=".resource.common.monitor.visibility.charts.metric.partrsrcs">
      <tiles:put name="multiResource" value="true"/>
      </tiles:insert>
      &nbsp;<br>
      </c:when>
      </c:choose>
      </html:form>
    </td>
    <td>
      <html:img page="/images/spacer.gif" width="80" height="1" alt="" border="0"/>
    </td>
  </tr>
</table>
</c:otherwise>
</c:choose>
