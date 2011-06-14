<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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


<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_DYNAMIC"
  var="dynamic"/>
<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_STATIC"
  var="static"/>
<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_TRENDSUP"
  var="trendsup"/>
<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_TRENDSDOWN"
  var="trendsdown"/>
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_AVAILABILITY" var="availability" />
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_PERFORMANCE" var="performance" />
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_THROUGHPUT" var="throughput" />
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_UTILIZATION" var="utilization" />

<hq:constant 
  classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
  symbol="APPDEF_TYPE_PLATFORM"
  var="platformType"/>
<hq:constant 
  classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
  symbol="APPDEF_TYPE_SERVER"
  var="serverType"/>
<hq:constant 
  classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
  symbol="APPDEF_TYPE_SERVICE"
  var="serviceType"/>
<hq:constant 
  classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
  symbol="APPDEF_TYPE_GROUP"
  var="groupType"/>
<hq:constant 
  classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
  symbol="APPDEF_TYPE_APPLICATION"
  var="application"/>

<c:choose>
  <c:when test="${MetricSummaries[0].resource.entityId.type == platformType}">
    <fmt:message var="resourceTypeTitle" key="resource.hub.filter.platform" />
  </c:when>
  <c:when test="${MetricSummaries[0].resource.entityId.type == serverType}">
    <fmt:message var="resourceTypeTitle" key="resource.hub.filter.server" />
  </c:when>
  <c:when test="${MetricSummaries[0].resource.entityId.type == serviceType}">
    <fmt:message var="resourceTypeTitle" key="resource.hub.filter.service" />
  </c:when>
</c:choose>
<jsu:importScript path="/js/functions.js" />
<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
<c:choose>
  <c:when test="${Resource.entityId.type == platformType}">
    <fmt:message var="appdefType" key="resource.type.Platform" />
  </c:when>
  <c:when test="${Resource.entityId.type == serverType}">
    <fmt:message var="appdefType" key="resource.type.Server" />
  </c:when>
  <c:when test="${Resource.entityId.type == serviceType}">
    <fmt:message var="appdefType" key="resource.type.Service" />
  </c:when>
  <c:when test="${Resource.entityId.type == groupType}">
    <fmt:message var="appdefType" key="resource.type.Group" />
  </c:when>
  <c:when test="${Resource.entityId.type == applicationType}">
    <fmt:message var="appdefType" key="resource.type.Application" />
  </c:when>
</c:choose>
<c:choose>
  <c:when test="${MetricSummaries[0].measurementTemplate.collectionType == dynamic}">
  <fmt:message var="collectionType" key="resource.common.monitor.visibility.metricmetadata.collection.type.dynamic" />
  </c:when>
  <c:when test="${MetricSummaries[0].measurementTemplate.collectionType == static}">
  <fmt:message var="collectionType" key="resource.common.monitor.visibility.metricmetadata.collection.type.static" />
  </c:when>
  <c:when test="${MetricSummaries[0].measurementTemplate.collectionType == trendsup}">
  <fmt:message var="collectionType" key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsup" />
  </c:when>
  <c:when test="${MetricSummaries[0].measurementTemplate.collectionType == trendsdown}">
  <fmt:message var="collectionType" key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsdown" />
  </c:when>
</c:choose>
<c:choose>
  <c:when test="${MetricSummaries[0].measurementTemplate.category.name == availability}">
    <fmt:message var="categoryName" key="resource.common.monitor.visibility.AvailabilityTH" />
  </c:when>
  <c:when test="${MetricSummaries[0].measurementTemplate.category.name == performance}">
    <fmt:message var="categoryName" key="resource.common.monitor.visibility.PerformanceTH" />
  </c:when>
  <c:when test="${MetricSummaries[0].measurementTemplate.category.name == throughput}">
    <fmt:message var="categoryName" key="resource.common.monitor.visibility.UsageTH" />
  </c:when>
  <c:when test="${MetricSummaries[0].measurementTemplate.category.name == utilization}">
    <fmt:message var="categoryName" key="resource.common.monitor.visibility.UtilizationTH" />
  </c:when>
</c:choose>
<c:set var="resourceName" value="${Resource.name}" />
<tiles:insert definition=".page.title.resource.generic">
  <tiles:put name="titleKey" value="resource.common.monitor.visibility.MetricMetadata"/>
  <tiles:put name="titleName" beanName="resourceName" />
</tiles:insert>
<table width="100%" align="center" cellspacing="0" cellpadding="0" border="0">
<tr>
<td class="BlockLabel">
<fmt:message key="resource.common.monitor.visibility.MetricNameTH"/>
</td>
<td class="BlockContent">
<c:out value="${MetricSummaries[0].measurementTemplate.name}" />
</td>
<td class="BlockLabel">
<fmt:message key="common.header.ResourceName"/>
</td>
<td class="BlockContent">
<c:out value="${Resource.name}" />
(<c:out value="${Resource.appdefResourceTypeValue.name}" />
<c:out value="${appdefType}" />)
</td>
</tr>
<tr>
<td class="BlockLabel">
<fmt:message key="resource.common.monitor.meta.header.Category"/>
</td>
<td class="BlockContent">
<c:out value="${categoryName}" />
</td>
<td class="BlockLabel">
<fmt:message key="resource.common.monitor.meta.header.Indicator"/>
</td>
<td class="BlockContent">
<c:out value="${MetricSummaries[0].measurementTemplate.designate}" />
</td>
</tr>
<tr>
<td class="BlockLabel">
<fmt:message key="resource.common.monitor.meta.header.CollectionType"/>
</td>
<td class="BlockContent">
<c:out value="${collectionType}" />
</td>
<td class="BlockLabel">
<fmt:message key="resource.common.monitor.meta.header.BaseUnits"/>
</td>
<td class="BlockContent">
<c:set var="units" value="resource.common.monitor.meta.units.${MetricSummaries[0].measurementTemplate.units}"/>
<fmt:message key="${units}" />
</td>
</tr>
<tr>
<td class="BlockLabel">
<fmt:message key="resource.common.monitor.meta.header.Alias"/>
</td>
<td class="BlockContent">
<c:out value="${MetricSummaries[0].measurementTemplate.alias}" />
</td>
<td class="BlockLabel">
&nbsp;
</td>
<td class="BlockContent">
&nbsp;
</td>
</tr>
</table>

<table width="100%" align="center" cellspacing="0" cellpadding="0" border="0">
<tr>
<td>
<display:table items="${MetricSummaries}" var="summary" width="100%" cellspacing="0" cellpadding="0">
<display:column width="15%" property="resource.name" 
  title="${resourceTypeTitle}" isLocalizedTitle="false" styleClass="ListCell"/>
<display:column width="6%" property="minExpectedValue" title="resource.common.monitor.visibility.metricmetadata.expectedrange.low" styleClass="ListCell">
  <display:metricdecorator unit="${summary.measurementTemplate.units}" defaultKey="common.value.notavail"/>
</display:column>
<display:column width="6%" property="maxExpectedValue" title="resource.common.monitor.visibility.metricmetadata.expectedrange.high" styleClass="ListCell">
  <display:metricdecorator unit="${summary.measurementTemplate.units}" defaultKey="common.value.notavail"/>
</display:column>
<display:column width="6%" property="interval" title="resource.common.monitor.visibility.metricmetadata.collection.interval" styleClass="ListCell">
  <display:datedecorator isElapsedTime="true" isGroup="true"/>
</display:column>

<display:column width="6%" property="enabled" title="resource.common.monitor.visibility.metricmetadata.collection.enabled" styleClass="ListCell">
  <display:booleandecorator flagKey="yesno"/>
</display:column>
<display:column width="15%" property="mtime" title="resource.common.monitor.visibility.metricmetadata.collection.lastModified" styleClass="ListCell">
<display:datedecorator /></display:column>
<display:column width="8%" property="lastValue" title="resource.common.monitor.visibility.metricmetadata.last" styleClass="ListCell">
<display:metricdecorator unit="${summary.measurementTemplate.units}" defaultKey="common.value.notavail"/></display:column>
<display:column width="15%" property="lastValueTimestamp" title="resource.common.monitor.visibility.metricmetadata.last.collectiontime" styleClass="ListCell">
<display:datedecorator /></display:column>
</display:table>
</td>
</tr>
</table>
<table width="95%" align="center" cellspacing="0" cellpadding="0" border="0">
<tr>
<td width="40%">
&nbsp;
</td>
<td width="10%">
<a href="javascript:window.close()">close</a>
</td>
<td width="10%">
<a href="javascript:location.reload()">refresh</a>
</td>
<td width="40%">
&nbsp;
</td>
</tr>
</table>
<tiles:insert definition=".page.footer"/>
