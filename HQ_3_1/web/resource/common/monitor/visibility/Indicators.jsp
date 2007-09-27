<%@ page language="java" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html" prefix="html" %>
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


<tiles:importAttribute name="tabListName" ignore="true"/>
<tiles:importAttribute name="entityType" ignore="true"/>

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="eid" value="${Resource.entityId.appdefKey}"/>
<c:set var="ctype" value="${param.ctype}"/>
<c:set var="view" value="${IndicatorViewsForm.view}"/>

<tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
  <tiles:put name="selectedIndex" value="0"/>
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  <tiles:put name="autogroupResourceType" beanName="ctype"/>
  <tiles:put name="entityType" beanName="entityType"/>
  <tiles:put name="tabListName" beanName="tabListName"/>
</tiles:insert>

<script language="JavaScript">
var baseUrl = "<html:rewrite page="/resource/common/monitor/visibility/IndicatorCharts.do"/>";
baseUrl += "?eid=<c:out value="${eid}"/>";
<c:if test="${not empty ctype}">
  baseUrl += "&ctype=<c:out value="${ctype}"/>";
</c:if>
  
function addMetric(metric) {
    var frameUrl =
        baseUrl + "&view=<c:out value="${view}"/>";
    frameUrl += "&action=add&addMetric=" + metric;

    window.parent.frames[0].location = frameUrl;
}

function reviewAction(option) {
    var form = document.IndicatorViewsForm;
    if (option.value == 'go') {
        $('viewname').style.display = "";
        form.view.value = option.text;
        form.submit();
    }
    else if (option.value == 'delete') {
        form.view.value = "";
        form.submit();
    }
    else if (option.value == 'create') {
        $('viewname').style.display = "";
        return;
    }
    else if (option.value == 'update') {
        form.view.value = "<c:out value="${view}"/>";
    }
    $('viewname').style.display = "none";
}

</script>

<html:form action="/resource/common/monitor/visibility/IndicatorCharts.do" method="GET" onsubmit="this.view.disabled=false">
<input type="hidden" name="eid" value="<c:out value="${eid}"/>">
<c:if test="${not empty ctype}">
  <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>">
</c:if>

<table width="680" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td class="ListHeaderInactive" nowrap="true"><fmt:message key="resource.common.monitor.visibility.IndicatorCharts"/></td>
    <td class="ListHeaderInactive" valign="middle" align="right" width="100%">
      <table cellspacing="2">
        <tr>
          <td>
            <fmt:message key="Filter.ViewLabel"/>
            <html:select property="action" onchange="reviewAction(this.options[this.selectedIndex]);">
              <option value="update">
                <fmt:message key="resource.common.monitor.visibility.view.Update"/>
                <c:out value="${view}"/>
              </option>
              <option value="create">
                <fmt:message key="resource.common.monitor.visibility.view.New"/>
              </option>
              <c:if test="${not empty IndicatorViewsForm.views[1]}">
              <option value="delete">
                <fmt:message key="resource.common.monitor.visibility.view.Delete"/>
                <c:out value="${view}"/>
              </option>
              <option disabled="true">
                <fmt:message key="resource.common.monitor.visibility.view.Separator"/>
              </option>
              <option disabled="true">
                <fmt:message key="resource.common.monitor.visibility.view.Goto"/>
              </option>
              <c:forEach var="viewname" items="${IndicatorViewsForm.views}">
              <option value="go"><c:out value="${viewname}"/></option>
              </c:forEach>
              </c:if>
            </html:select>
            <span id="viewname" style="display: none;">
            <fmt:message key="common.label.Name"/>
            <html:text size="20" property="view"/>
            </span>
          </td>
          <td align="right">
            <!-- Use hidden input because IE doesn't pass value of of image -->
            <input type="hidden" name="update" value="<c:out value="${view}"/>">
            <input type="image" name="submit" src="<html:rewrite page="/images/dash-button_go-arrow.gif"/>" border="0" alt="Apply Chart View"/>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <c:if test="${not empty availabilityMetrics}">
  <tr>
    <td colspan="2"> 
      <table cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td width="8">
            <html:img page="/images/timeline_ul.gif" height="10"/> 
          </td>
        <c:forEach var="avail" items="${availabilityMetrics}" varStatus="status">
          <td width="9">
            <div onmousedown="overlay.moveOverlay(this);overlay.showTimePopupTopMetricChart(<c:out value="${status.count - 1}"/>, event)">
            <c:choose>
              <c:when test="${avail.value == 1}">
                <html:img page="/images/timeline_green.gif" height="10" width="9" border="0"/> 
              </c:when>
              <c:when test="${avail.value == -0.01}">
                <html:img page="/images/timeline_orange.gif" height="10" width="9" border="0"/>
              </c:when>
              <c:when test="${avail.value <= 0}">
                <html:img page="/images/timeline_red.gif" height="10" width="9" border="0"/>
              </c:when>
              <c:when test="${avail.value > 0 && avail.value < 1}">
                <html:img page="/images/timeline_yellow.gif" height="10" width="9" border="0"/>
              </c:when>
              <c:otherwise>
                <html:img page="/images/timeline_unknown.gif" height="10" width="9" border="0"/>
              </c:otherwise>
            </c:choose>
          </div>

          </td>
        </c:forEach>
          <td width="10" align="left">
            <html:img page="/images/timeline_ur.gif" height="10"/>
          </td>
          <td class="BoldText" style="padding-left: 4px;">
        <c:choose>
          <c:when test="${entityType eq 'application'}">
            <fmt:message key="resource.common.monitor.visibility.availability.value">
              <fmt:param value="${availMetricsAttr}"/>
            </fmt:message>
          </c:when>
          <c:otherwise>
            <c:url var="availChart" value="/resource/common/monitor/visibility/ViewDesignatedChart.do">
              <c:param name="eid" value="${eid}"/>
              <c:if test="${not empty ctype}">
                <c:param name="ctype" value="${ctype}"/>
              </c:if>
            </c:url>
            <a href="<c:out value="${availChart}"/>">
            <fmt:message key="resource.common.monitor.visibility.availability.value">
              <fmt:param value="${availMetricsAttr}"/>
            </fmt:message>
          </a>
          </c:otherwise>
        </c:choose>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  </c:if>
  <tr>
    <td colspan="2"> 
        <div id="charttop" style="padding-top: 1px; padding-bottom: 1px; z-index: 0;">
        <c:url var="chartUrl" value="/resource/common/monitor/visibility/IndicatorCharts.do">
          <c:param name="eid" value="${eid}" />
          <c:if test="${not empty ctype}">
            <c:param name="ctype" value="${ctype}" />
          </c:if>
          <c:param name="view" value="${view}"/>
          <c:choose>
            <c:when test="${ProblemMetricsDisplayForm.fresh}">
              <c:param name="action" value="fresh"/>
            </c:when>
            <c:otherwise>
              <c:param name="action" value="refresh"/>
            </c:otherwise>
          </c:choose>
        </c:url>
        <iframe id="chartFrame" src="<c:out value="${chartUrl}"/>"
                marginwidth="0" marginheight="0" frameborder="no"
                scrolling="auto"
                style="border-style: none; height: 450px; width: 100%;"></iframe>
      </div>
      <div id="chartbottom"></div>
    </td>
  </tr>
  <tr>
    <td colspan="2">
      <tiles:insert definition=".resource.common.monitor.visibility.timeline">
        <c:if test="${entityType eq 'autogroup'}">
          <tiles:put name="hideLogs" value="true"/>
        </c:if>
      </tiles:insert>
    </td>
  </tr>
</table>

</html:form>
