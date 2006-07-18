<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="display" prefix="display" %>
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


<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="selfAction"/>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_PERF" var="MODE_MON_PERF"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="SORTORDER_DEC" var="SORTORDER_DEC"/>

<hq:constant classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" symbol="APPDEF_TYPE_SERVER" var="SERVER_TYPE"/>
<hq:constant classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" symbol="APPDEF_TYPE_SERVICE" var="SERVICE_TYPE"/>
<hq:constant classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" symbol="APPDEF_TYPE_GROUP" var="GROUP_TYPE"/>

<c:choose>
  <%-- platforms, servers, applications --%>
  <c:when test="${not empty childResourceType}">
    <c:set var="childEntityType" value="${childResourceType.appdefTypeId}"/>
    <c:set var="childResourceTypeName" value="${childResourceType.name}"/>
    <c:set var="ctype" value="${childResourceType.appdefTypeKey}"/>
  </c:when>
  <%-- services and groups --%>
  <c:otherwise>
    <c:if test="${not empty resource}">
      <c:set var="childResourceTypeName" value="${resource.appdefResourceTypeValue.name}"/>
      <c:choose>
        <c:when test="${resource.entityId.type == GROUP_TYPE}">
          <c:set var="childEntityType" value="${resource.groupEntType}"/>
          <c:set var="ctype" value="${resource.groupEntResType}"/>
          <c:set var="showHost" value="true"/>
          <c:set var="nameWidth" value="30"/>
        </c:when>
        <c:otherwise>
          <c:set var="childEntityType" value="${resource.entityId.type}"/>
        </c:otherwise>
      </c:choose>
    </c:if>
  </c:otherwise>
</c:choose>
<c:if test="${empty childResourceTypeName}">
  <c:set var="childResourceTypeName" value="BAD CHILD RESOURCE NAME"/>
</c:if>

<c:if test="${empty nameWidth}">
  <c:set var="nameWidth" value="60"/>
</c:if>

<c:choose>
  <c:when test="${childEntityType == SERVER_TYPE}">
    <fmt:message var="ChildTH" key="resource.common.monitor.visibility.ServerTH"/>
    <fmt:message var="HostTH" key="resource.common.monitor.visibility.HostPlatformTH"/>
  </c:when>
  <c:when test="${childEntityType == SERVICE_TYPE}">
    <fmt:message var="ChildTH" key="resource.common.monitor.visibility.ServiceTH"/>
    <fmt:message var="HostTH" key="resource.common.monitor.visibility.HostServerTH"/>
  </c:when>
</c:choose>

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

<!-- CHILD RESOURCES CONTENTS -->
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="pbtListTable">
    <tr>
      <td class="ListCellHeader"><fmt:message key="resource.common.monitor.visibility.performance.DetailForChildResourcesEtc"><fmt:param value="${childResourceTypeName}"/></fmt:message></td>
    </tr>
  </table>

<div id="pbtListDiv">
<c:choose>
  <c:when test="${not empty summaries}">

    <c:forEach var="summary" items="${summaries}" varStatus="status">
        <!-- Here are the menu layers. Give each a unique id and a class of
             menu -->
        <div id="<c:out value="${summary.me.type}_${summary.me.id}_menu"/>"
             class="menu">
          <ul>
          <c:if test="${showHost}">
            <li>
              <div class="BoldText"><c:out value="${HostTH}"/></div>
              <c:out value="${summary.parent.name}"/>
            </li>
          </c:if>
  	        <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.NumURLsTH"/></div>
              <c:out value="${summary.urlCount}"/>
            </li>
  	        <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.RequTH"/></div>
              <c:out value="${summary.requestCount}"/>
            </li>
          </ul>
        </div>
      <c:set var="count" value="${status.count}"/>
    </c:forEach>

    <c:if test="${count > 3}">
      <div class="scrollable">
    </c:if>

  <display:table items="${summaries}" var="summary" action="${sAction}" width="100%" cellspacing="0" cellpadding="0" order="${so}">
      <display:column value="${summary.me.name}" title="${ChildTH}" isLocalizedTitle="false" href="/resource/${summary.me.typeName}/monitor/Visibility.do?mode=${MODE_MON_PERF}&rid=${summary.me.id}&type=${summary.me.type}" sort="true" sortAttr="24" styleClass="ListCell"/>
      <display:column width="8%" property="low.total" title="resource.common.monitor.visibility.LowTH" sort="true" sortAttr="25" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
        <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
      </display:column>
      <display:column width="8%" property="avg.total" title="resource.common.monitor.visibility.AvgTH" sort="true" defaultSort="true" sortAttr="26" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
        <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
      </display:column>
      <display:column width="8%" property="peak.total" title="resource.common.monitor.visibility.PeakTH" sort="true" sortAttr="27" styleClass="ListCellCheckboxLeftLine" headerStyleClass="ListHeaderCheckboxLeftLine">
        <display:metricdecorator unit="ms" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
      </display:column>
      <display:column width="4%" property="me" href="/resource/${summary.me.typeName}/monitor/Visibility.do?mode=${MODE_MON_PERF}&rid=${summary.me.id}&type=${summary.me.type}" title="resource.common.monitor.visibility.MiniTab.More" headerStyleClass="ListHeaderCheckboxLeftLine" styleClass="ListCellCheckboxLeftLine">
      <display:imagedecorator onmouseover="menuLayers.show('${summary.me.type}_${summary.me.id}_menu', event)" onmouseout="menuLayers.hide()" src="/images/icon_menu_down.gif" border="0"/>
      </display:column>
    </display:table>

    <c:if test="${count > 1}">
      </div>
    </c:if>
  </c:when>
  <c:otherwise>
<tiles:insert definition=".resource.common.monitor.visibility.noPerfs"/>
  </c:otherwise>
</c:choose>
</div>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listItems" beanName="summaries"/>
  <tiles:put name="listSize" beanName="summaries" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="psAction"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="defaultSortColumn" value="1"/>
  <tiles:put name="noButtons" value="true"/>
</tiles:insert>
<!--  /  -->
