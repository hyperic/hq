<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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


<tiles:importAttribute name="showRedraw" ignore="true"/>
<c:if test="${empty showRedraw}">
<c:set var="showRedraw" value="false"/>
</c:if>

<tiles:importAttribute name="form" ignore="true"/>
<c:choose>
  <c:when test="${not empty form}">
    <%-- used only for forms that are not MetricDisplayRangeForm --%>
    <tiles:importAttribute name="formName"/>
    <c:set var="startMonth" value="${form.startMonth}"/>
    <c:set var="startDay" value="${form.startDay}"/>
    <c:set var="startYear" value="${form.startYear}"/>
    <c:set var="endMonth" value="${form.endMonth}"/>
    <c:set var="endDay" value="${form.endDay}"/>
    <c:set var="endYear" value="${form.endYear}"/>
  </c:when>
  <c:otherwise>
    <c:set var="formName" value="MetricDisplayRangeForm"/>
    <c:set var="startMonth" value="${MetricDisplayRangeForm.startMonth}"/>
    <c:set var="startDay" value="${MetricDisplayRangeForm.startDay}"/>
    <c:set var="startYear" value="${MetricDisplayRangeForm.startYear}"/>
    <c:set var="endMonth" value="${MetricDisplayRangeForm.endMonth}"/>
    <c:set var="endDay" value="${MetricDisplayRangeForm.endDay}"/>
    <c:set var="endYear" value="${MetricDisplayRangeForm.endYear}"/>
  </c:otherwise>
</c:choose>
<jsu:importScript path="/js/schedule.js" />
<jsu:importScript path="/js/monitorSchedule.js" />
<jsu:script>
 	var jsPath = "<s:url value="/js/"/>";
 	var cssPath = "<s:url value="/css/"/>";
 	var isMonitorSchedule = true;
</jsu:script>
<tiles:insertDefinition name=".portlet.error"/>

<table border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="20%" class="SmokeyLabel"><fmt:message key="resource.common.monitor.visibility.DefineRangeLabel"/></td>
<s:if test="fieldErrors.containsKey('rn')">
    <td width="80%" class="ErrorField">
</s:if>
<s:else>
    <td width="80%" class="SmokeyContent">
</s:else>
      <s:radio list="#{'1':''}" name="a"  value="1"/>
      <fmt:message key="monitoring.baseline.BlockContent.Last"/>&nbsp;&nbsp;
      <s:textfield name="rn" value="%{#attr.rn}" size="2" maxlength="3" styleClass="smallBox" onfocus="toggleRadio('a', 0);"/> 
	  <s:select theme="simple" cssStyle="FilterFormText" name="ru"  onchange="toggleRadio('a', 0);"
					  list="#{'2':getText('resource.common.monitor.visibility.config.Minutes'), '3':getText('resource.common.monitor.visibility.config.Hours'), '4':getText('resource.common.monitor.visibility.metricsToolbar.Days') }" 
					  value="%{#attr.metricsForm.ru}" />

     
<s:if test="fieldErrors.containsKey('rn')">
  <span class="ErrorFieldContent">- <s:fielderror fieldName="rn" /></span>
</s:if>
    </td>
  </tr>

  <tr>
    <td class="SmokeyLabel">&nbsp;</td>
<s:if test="fieldErrors.containsKey('endDate')">
    <td width="80%" class="ErrorField">
</s:if>
<s:else>
    <td width="80%" class="SmokeyContent">
</s:else>
      <s:radio list="#{'2':''}" name="a" var="a" value="2"/>
      <fmt:message key="monitoring.baseline.BlockContent.WithinRange"/>
      <br>

      <table width="100%" border="0" cellspacing="3" cellpadding="5">
        <tr> 
          <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="20" border="0"/></td>
          <td>
            <fmt:message key="monitoring.baseline.BlockContent.From"/></td>
          <td width="100%">
            <s:select theme="simple" cssStyle="FilterFormText"  name="startMonth" styleClass="startMonth" onchange="toggleRadio('a', 1); changeMonitorDropDown('startMonth', 'startDay', 'startYear');" 
              list="#{'0':'01 (Jan)','1':'02 (Feb)','2':'03 (Mar)','3':'04 (Apr)','4':'05 (May)','5':'06 (Jun)','6':'07 (Jul)','7':'08 (Aug)','8':'09 (Sep)','9':'10 (Oct)','10':'11 (Nov)','11':'12 (Dec)'}"			  
			  value="%{#attr.metricsForm.startMonth}" />&nbsp;/&nbsp;
            <s:select theme="simple" name="startDay" styleClass="startDay" onchange="toggleRadio('a', 1);" 
              list="#{'1':'01','2':'02','3':'03','4':'04','5':'05','6':'06','7':'07','8':'08','9':'09','10':'10','11':'11','12':'12','13':'13','14':'14','15':'15','16':'16','17':'17','18':'18','19':'19','20':'20','21':'21','22':'22','23':'23','24':'24','25':'25','26':'26','27':'27','28':'28','29':'29','30':'30','31':'31'}"
            value="%{#attr.startDay}" />&nbsp;/&nbsp;
            <s:select theme="simple" name="startYear" cssStyle="startYear" onchange="toggleRadio('a', 1); changeMonitorDropDown('startMonth', 'startDay', 'startYear');" 
              list="#attr.metricsForm.yearOptions"
			  value="%{#attr.metricsForm.startYear}"/>&nbsp;
            &nbsp;@&nbsp;
            <s:textfield name="startHour" value="%{#attr.startHour}" styleClass="smallBox" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;:&nbsp;<s:textfield name="startMin" value="%{#attr.startMin}" styleClass="smallBox" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;
            <s:select name="startAmPm" onchange="toggleRadio('a', 1);" 
              list="#{'am':'AM','pm':'PM'}"
            value="%{#attr.metricsForm.startAmPm}"/>&nbsp;
        </tr>
        <tr> 
          <td>&nbsp;</td>
          <td>
            <fmt:message key="monitoring.baseline.BlockContent.To"/>&nbsp;</td>
          <td width="100%" nowrap>
		  <s:select theme="simple" cssStyle="FilterFormText"  name="endMonth" styleClass="endMonth" onchange="toggleRadio('a', 1); changeMonitorDropDown('endMonth', 'endDay', 'endYear');" 
              list="#{'0':'01 (Jan)','1':'02 (Feb)','2':'03 (Mar)','3':'04 (Apr)','4':'05 (May)','5':'06 (Jun)','6':'07 (Jul)','7':'08 (Aug)','8':'09 (Sep)','9':'10 (Oct)','10':'11 (Nov)','11':'12 (Dec)'}"			  
			  value="%{#attr.metricsForm.endMonth}" />&nbsp;/&nbsp;
           <s:select theme="simple" name="endDay" styleClass="endDay" onchange="toggleRadio('a', 1);" 
              list="#{'1':'01','2':'02','3':'03','4':'04','5':'05','6':'06','7':'07','8':'08','9':'09','10':'10','11':'11','12':'12','13':'13','14':'14','15':'15','16':'16','17':'17','18':'18','19':'19','20':'20','21':'21','22':'22','23':'23','24':'24','25':'25','26':'26','27':'27','28':'28','29':'29','30':'30','31':'31'}"
            value="%{#attr.metricsForm.endDay}" />&nbsp;/&nbsp; 
            <s:select theme="simple" name="endYear" cssStyle="endYear" onchange="toggleRadio('a', 1); changeMonitorDropDown('endMonth', 'endDay', 'endYear');" 
              list="#attr.metricsForm.yearOptions"
			  value="%{#attr.metricsForm.endYear}"/>&nbsp;
			&nbsp;@&nbsp;
            <s:textfield name="endHour" value="%{#attr.endHour}" styleClass="smallBox" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;:&nbsp;<s:textfield name="endMin" value="%{#attr.endMin}" styleClass="smallBox" size="2" maxlength="2" onfocus="toggleRadio('a', 1);"/>&nbsp;
            <s:select name="endAmPm" onchange="toggleRadio('a', 1);" 
              list="#{'am':'AM','pm':'PM'}"
            value="%{#attr.metricsForm.endAmPm}"/>&nbsp;
			</tr>
<s:if test="fieldErrors.containsKey('endDate')">
        <tr> 
          <td colspan="2">&nbsp;</td>
          <td>
            <span class="ErrorFieldContent">- <s:fielderror fieldName="endDate" />
</span>
          </td>
        </tr>
</s:if>
      </table>

    </td>
  </tr>
  <c:if test="${showRedraw}">
  <tr>
      <td class="SmokeyContent" colspan="2" align="center"><input type="image"" property="advanced" src='<s:url value="/images/fb_redraw.gif"/>' border="0" onmouseover="imageSwap(this, imagePath + 'fb_redraw', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_redraw', '');" onmousedown="imageSwap(this, imagePath +  'fb_redraw', '_down')"/></td>
  </tr>
  </c:if>
  <tr>
    <td class="SmokeyLabel">&nbsp;</td>
    <td class="SmokeyContent"><span class="CaptionText"><fmt:message key="resource.common.monitor.visibility.TheseSettings"/></span></td>
  </tr>
</table>
 