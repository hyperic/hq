<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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


<tiles:importAttribute name="multiResource" ignore="true"/>
<c:if test="${empty multiResource}">
  <c:set var="multiResource" value="false"/>
</c:if>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />
<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>
<html:hidden property="ctype"/>
<html:hidden property="mode" value="${param.mode}"/>

<c:forEach var="mid" items="${ViewChartForm.origM}">
<html:hidden property="origM" value="${mid}"/>
</c:forEach>

<table width="100%" cellpadding="3" cellspacing="0" border="0">
  <tr>
    <td colspan="6" class="BlockBottomLine"><html:img
      page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td width="30" rowspan="2"><html:img
      page="/images/spacer.gif" width="30" height="1" border="0"/></td>
    <td width="125">
      <html:hidden property="showValues"/>
      <input type="checkbox" name="showValuesCB" <c:if test="${ViewChartForm.showValues}">checked</c:if> onclick="javascript:checkboxToggled('showValuesCB', 'showValues');">
      <html:img page="/images/icon_actual.gif" width="11"
      height="11" border="0"/> <fmt:message
      key="resource.common.monitor.visibility.chart.Actual"/>
    </td>
    <td width="125">
      <html:hidden property="showPeak"/>
      <input type="checkbox" name="showPeakCB" <c:if test="${ViewChartForm.showPeak}">checked</c:if> onclick="javascript:checkboxToggled('showPeakCB', 'showPeak');">
      <html:img page="/images/icon_peak.gif" width="11" height="11"
      border="0"/> <fmt:message
      key="resource.common.monitor.visibility.chart.Peak"/>
    </td>
    <td>
      <html:hidden property="showAverage"/>
      <input type="checkbox" name="showAverageCB" <c:if test="${ViewChartForm.showAverage}">checked</c:if> onclick="javascript:checkboxToggled('showAverageCB', 'showAverage');">
      <html:img page="/images/icon_average.gif" width="11" height="11"
      border="0"/> <fmt:message
      key="resource.common.monitor.visibility.chart.Average"/>
    </td>
    <td>
      <html:hidden property="showLow"/>
      <input type="checkbox" name="showLowCB" <c:if test="${ViewChartForm.showLow}">checked</c:if> onclick="javascript:checkboxToggled('showLowCB', 'showLow');">
      <html:img page="/images/icon_low.gif" width="11" height="11"
      border="0"/> <fmt:message
      key="resource.common.monitor.visibility.chart.Low"/>
    </td>
    <td rowspan="2" valign="top">

<table border="0"><tr><td class="LinkBox">
    <c:if test="${not multiResource}">
      <c:url var="alertLink" value="/alerts/Config.do">
        <c:param name="mode" value="new"/>
        <c:param name="rid" value="${Resource.id}"/>
        <c:param name="type" value="${Resource.entityId.type}"/>
        <c:if test="${not empty metric}">
          <c:param name="metricId" value="${metric.id}"/>
          <c:param name="metricName" value="${metric.template.name}"/>
        </c:if>
      </c:url>
      <html:link href="${alertLink}"><fmt:message key="resource.common.monitor.visibility.NewAlertLink"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
    </c:if>
      <html:hidden property="saveChart" value="false"/>
      <html:link href="#" onclick="return MyMetricChart.saveToDashboard();"><fmt:message key="resource.common.monitor.visibility.SaveChartToDash"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>

    <c:if test="${not empty back}">
        <html:link page="${back}"><fmt:message key="resource.common.monitor.visibility.Back2Resource"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
    </c:if>

    <c:if test="${not empty metric}">
      <a href="#" onclick="javascript:MyMetricChart.exportData(exportParam);"><fmt:message key="resource.common.monitor.visibility.ExportLink"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></a><br>
    </c:if>
    
</td></tr></table>

    </td>
  </tr>
  <tr>
    <td colspan="3">
      <html:image page="/images/fb_redraw.gif" property="redraw" border="0"
      onmouseover="imageSwap(this, imagePath + 'fb_redraw', '_over');"
      onmouseout="imageSwap(this, imagePath +  'fb_redraw', '');"
      onmousedown="imageSwap(this, imagePath +  'fb_redraw', '_down')" tabindex="1" accesskey="r"/>
    </td>
  </tr>
  <tr>
    <td colspan="6" class="BlockBottomLine"><html:img
      page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<jsu:script>
	document.forms["ViewChartForm"].elements["showValuesCB"].focus();
</jsu:script>