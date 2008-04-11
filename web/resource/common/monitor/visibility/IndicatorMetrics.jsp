<%@ page language="java" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
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

<tiles:importAttribute name="problems" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>
<tiles:importAttribute name="hideTools" ignore="true"/>

<!-- Toobar -->
<c:if test="${not hideTools}">
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <td style="padding-top:3px;padding-right:5px;padding-left:5px;padding-bottom:5px;" valign="top"><html:image page="/images/arrow_branch.gif" border="0"/></td>
    <td style="padding-top:3px;padding-bottom:5px;font-size:10px;"><fmt:message key="inform.resource.common.monitor.visibility.SelectResources"/></td>
    <td nowrap style="font-size:10px" width="90">
    <tiles:insert page="/common/components/ActionButton.jsp">
          <tiles:put name="labelKey" value="common.label.ViewMetrics"/>
          <tiles:put name="buttonHref" value="."/>
          <tiles:put name="buttonClick" value="document.ProblemMetricsDisplayForm.submit(); return false;"/>
        </tiles:insert>
        <html:hidden property="fresh" value="false"/>

        <%--<html:image page="/images/fb_addResourcesApply.gif" border="0" onmouseover="imageSwap(this, imagePath + 'fb_addResourcesApply', '_over');" onmouseout="imageSwap(this, imagePath +  'fb_addResourcesApply', '');" onmousedown="imageSwap(this, imagePath +  'fb_addResourcesApply', '_down')"/>
    <html:hidden property="fresh" value="false"/>--%>
    </td>
    <td style="padding-left:5px;padding-right:3px"> <html:image page="/images/icon_info2.gif" onmouseover="menuLayers.show('stepInfo', event)" onmouseout="menuLayers.hide()" border="0"/></td>
  </tr>
</table>
</c:if>

<c:choose>
  <c:when test="${not empty problems}">   
    <c:set var="eid" value="${Resource.entityId.appdefKey}" />

    <c:forEach var="metric" items="${problems}" varStatus="status">
      <c:set var="resourceType" value="${metric.type}"/>
      <c:url var="metadataLink" value="/resource/common/monitor/Visibility.do">
        <c:param name="mode" value="metricMetadata"/>
        <c:param name="m" value="${metric.templateId}"/>
        <c:param name="eid" value="${eid}"/>
        <c:choose>
          <c:when test="${not empty ctype}">
            <c:param name="ctype" value="${ctype}"/>
          </c:when>
          <c:otherwise>
            <c:if test="${eid != metric.appdefKey}">
              <c:param name="ctype" value="${metric.appdefKey}"/>
            </c:if>
          </c:otherwise>
        </c:choose>
      </c:url>

      <c:choose>
        <c:when test="${metric.entityCount <= 18}">
          <c:set var="metadataPopupHeight" value="${300 + 26 * count}"/>
        </c:when>
        <c:otherwise>
          <c:set var="metadataPopupHeight" value="326"/>
        </c:otherwise>
      </c:choose>

    <!-- Here are the menu layers. Give each a unique id and a class of menu -->
      <div id="metric_menu_<c:out value="${metric.templateId}"/>" class="menu">
        <ul>
          <li>
            <div class="BoldText">
            <c:choose>
              <c:when test="${metric.single}">
                <fmt:message key="resource.common.monitor.visibility.problemMetric.Type">
                  <fmt:param value="${metric.type}"/>
                </fmt:message>
              </c:when>
              <c:otherwise>
                <fmt:message key="resource.common.monitor.visibility.problemMetric.TypeCount">
                  <fmt:param value="${metric.type}"/>
                  <fmt:param value="${metric.entityCount}"/>
                </fmt:message>
              </c:otherwise>
            </c:choose>
          </div>
        <c:if test="${metric.earliest > 0}">
  	      <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.problemMetric.Began"/></div>
          <hq:dateFormatter value="${metric.earliest}"/></li>
        </c:if>
        <hr>
        <c:url var="chartLink" value="/resource/common/monitor/Visibility.do">
          <c:param name="m" value="${metric.templateId}"/>
          <c:choose>
            <c:when test="${metric.single}">
              <c:param name="mode" value="chartSingleMetricSingleResource"/>
              <c:param name="eid" value="${metric.appdefKey}"/>
            </c:when>
            <c:otherwise>
              <c:param name="mode" value="chartSingleMetricMultiResource"/>
              <c:param name="eid" value="${eid}"/>
              <c:param name="ctype" value="${metric.appdefKey}"/>
            </c:otherwise>
          </c:choose>
        </c:url>
        <c:choose>
          <c:when test="${metric.single}">
            <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${metric.appdefKey},${metric.templateId}')"/>
          </c:when>
          <c:otherwise>
            <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${eid},${metric.templateId},${metric.appdefKey}')"/>
          </c:otherwise>
        </c:choose>
        <li>
        <a href="<c:out value="${scriptUrl}"/>"><fmt:message key="resource.common.monitor.visibility.problemMetric.ChartMetric"/></a>
  	    <html:link href="${chartLink}"><fmt:message key="resource.common.monitor.visibility.problemMetric.FullChart"/></html:link>
        </li>
        <hr>
  	    <li><html:link href="" onclick="window.open('${metadataLink}','_metricMetadata','width=800,height=${metadataPopupHeight},scrollbars=yes,toolbar=no,left=80,top=80,resizable=yes'); return false;">
          <fmt:message key="resource.common.monitor.visibility.problemMetric.MetricData"/></html:link></li>
      </ul>
    </div>
    <c:set var="count" value="${status.count}"/>
  </c:forEach>

  <c:if test="${count > 7}">
    <div id="metricsDiv" class="scrollable">

    <script type="text/javascript">
      function setMetricsHeight() {
        var metricsDiv = dojo.byId('metricsDiv');
        var bottom = overlay.findPosY(dojo.byId('timetop'));
        var top = overlay.findPosY(metricsDiv);

        metricsDiv.style.height = (bottom - top) + 'px';
      }

      onloads.push( setMetricsHeight );
    </script>

  </c:if>

  <table width="100%" border="0" cellpadding="1" cellspacing="0">
    <tr class="tableRowHeader">
      <th class="ListHeaderInactive">
          <fmt:message key="resource.common.monitor.visibility.MiniTab.All"/>
      </th>
      <th class="ListHeaderInactive" colspan="2"width="4%"><fmt:message key="nbsp"/></th>
    </tr>
  <c:forEach var="metric" items="${problems}">
    <c:choose>
      <c:when test="${metric.single}">
        <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${metric.appdefKey},${metric.templateId}');menuLayers.hide()"/>
      </c:when>
    <c:otherwise>
      <c:set var="scriptUrl" value="javascript:menuLayers.hide();addMetric('${eid},${metric.templateId},${metric.appdefKey}')"/>
    </c:otherwise>
    </c:choose>

    <c:if test="${resourceType != metric.type}">
      <c:set var="resourceType" value="${metric.type}"/>
      <tr>
        <td class="ListCell" colspan="3"><span class="BoldText" style="margin-left: 2px;"><c:out value="${resourceType}"/></span></td>
      </tr>
    </c:if>

    <tr>
      <td class="ListCell" style="padding-left: 10px;"><c:out value="${metric.name}"/></td>
      </td>
      <td class="ListCell">
      <html:img page="/images/comment.gif" onmouseover="menuLayers.show('metric_menu_${metric.templateId}', event)" onmouseout="menuLayers.hide()" border="0"/>
      </td>
      <td class="ListCell">
      <a href="<c:out value="${scriptUrl}"/>"><html:img page="/images/icon_menu.gif" border="0"/></a>
      </td>
    </tr>
  </c:forEach>
  </table>
  <c:if test="${count > 7}">
    </div>
  </c:if>
  </c:when>
  <c:otherwise>
    <table class="table" width="100%" border="0" cellspacing="0" cellpadding="1">
      <tr class="tableRowHeader">
        <th class="ListHeaderInactive">
            <fmt:message key="resource.common.monitor.visibility.MiniTab.All"/>
        </th>
      </tr>
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="resource.common.monitor.visibility.no.problems.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>

<div id="stepInfo" class="menu">
  <ul>
    <li><div class="BoldText"><fmt:message key="inform.resource.common.monitor.visibility.infoStep1"/></div></li>
    <li><div class="BoldText"><fmt:message key="inform.resource.common.monitor.visibility.infoStep2"/></div></li>
    <li><div class="BoldText"><fmt:message key="inform.resource.common.monitor.visibility.infoStep3"/></div></li>
  </ul>
</div>
