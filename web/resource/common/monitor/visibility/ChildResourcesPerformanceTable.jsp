<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<tiles:importAttribute name="perfSummaries" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="selfAction"/>
<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="urlMode" ignore="true"/>
<tiles:importAttribute name="url" ignore="true"/>

<c:if test="${empty mode}">
  <c:set var="mode" value="type"/>
</c:if>

      <table width="100%" cellpadding="0" cellspacing="0" border="0" class="MonitorBlock">
        <tr>
          <td>
<tiles:insert definition=".resource.common.monitor.visibility.metricsDisplayControlForm">
  <tiles:put name="form" beanName="PerformanceForm"/>
  <tiles:put name="formName" value="PerformanceForm"/>
</tiles:insert>

            <!-- Table Content -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
	      <tr>
                <td class="MonitorChartBlock">
<tiles:insert definition=".resource.common.monitor.visibility.performance.controlForm"/>
                </td>
              </tr>
              <tr>
                <td class="MonitorChartCell" align="center">
<c:choose>
  <c:when test="${mode eq 'type'}">
                  <html:img page="/resource/PerformanceChart?imageWidth=780&imageHeight=588&perfChartType=${mode}" border="0"/>
  </c:when>
  <c:when test="${mode eq 'url'}">
                  <html:img page="/resource/PerformanceChart?imageWidth=780&imageHeight=588&perfChartType=${mode}" border="0"/>
  </c:when>
  <c:when test="${mode eq 'urldetail'}">
                  <html:img page="/resource/PerformanceChart?imageWidth=780&imageHeight=588&perfChartType=${mode}" border="0"/>
  </c:when>
</c:choose>
</td>
              </tr>
              <tr>
                <td class="MonitorChartBlock">
            <tiles:insert page="/resource/common/monitor/visibility/ChartTimeIntervalToolbar.jsp">
            <tiles:put name="rangeNow" beanName="PerformanceForm" beanProperty="rangeNow"/>
            <tiles:put name="begin"><c:out value="${PerformanceForm.rbDate.time}"/></tiles:put>
            <tiles:put name="end"><c:out value="${PerformanceForm.reDate.time}"/></tiles:put>
            <tiles:put name="prevProperty" value="prev"/>
            <tiles:put name="nextProperty" value="next"/>
            </tiles:insert>
                </td>
              </tr>

<c:if test="${false}">
              <tr>
                <td>
<c:choose>
  <c:when test="${mode eq 'type'}">
<tiles:insert definition=".resource.common.monitor.visibility.childResources.performance.byType">
  <tiles:put name="summaries" beanName="perfSummaries"/>
  <tiles:put name="resource" beanName="resource"/>
  <tiles:put name="childResourceType" beanName="childResourceType"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
  </c:when>
  <c:when test="${mode eq 'url'}">
<tiles:insert definition=".resource.common.monitor.visibility.childResources.performance.byUrl">
  <tiles:put name="summaries" beanName="perfSummaries"/>
  <tiles:put name="resource" beanName="resource"/>
  <tiles:put name="childResourceType" beanName="childResourceType"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
  <tiles:put name="detailMode" beanName="urlMode"/>
</tiles:insert>
  </c:when>
  <c:when test="${mode eq 'urldetail'}">
<tiles:insert definition=".resource.common.monitor.visibility.performance.urlDetail">
  <tiles:put name="summaries" beanName="perfSummaries"/>
  <tiles:put name="url" beanName="url"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
  </c:when>
</c:choose>
                </td>
              </tr>
</c:if>
            </table>
            <!--  /  -->

          </td>
      	</tr>
      </table>
