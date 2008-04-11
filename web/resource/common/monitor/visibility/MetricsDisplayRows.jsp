<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>

<tiles:importAttribute name="rows"/>
<tiles:importAttribute name="useChart"/>
<tiles:importAttribute name="category"/>
<tiles:importAttribute name="buttonMode" ignore="true"/>
<tiles:importAttribute name="useCheckboxes" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>

<c:if test="${empty useCheckboxes}">
  <c:set var="useCheckboxes" value="true"/>
</c:if>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_CHART_SMSR"
                 var="MODE_MON_CHART_SMSR"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_CHART_SMMR"
                 var="MODE_MON_CHART_SMMR"/>
<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="MODE_MON_CHART_MMSR"
                 var="MODE_MON_CHART_MMSR"/>

<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="MAX_KEY"
  var="max"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="MIN_KEY"
  var="min"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="AVERAGE_KEY"
  var="average"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="LAST_KEY"
  var="last"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="BASELINE_KEY"
  var="baseline"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="HIGH_RANGE_KEY"
  var="high"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="LOW_RANGE_KEY"
  var="low"/>

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

<c:set var="eid" value="${Resource.entityId.type}:${Resource.id}" />
<c:choose>
<c:when test="${NumChildResources <= 18}">
<c:set var="metadataPopupHeight" value="${300 + 26 * NumChildResources}" />
</c:when>
<c:otherwise>
<c:set var="metadataPopupHeight" value="758" />
</c:otherwise>
</c:choose>

<script language="JavaScript1.2">
  MetricsUpdater = Class.create();

  MetricAttributes = new Array( "min", "average", "max", "last", "avail" );

  MetricsUpdater.prototype = {
    initialize: function(container, options) {},

    ajaxUpdate: function(ajaxResponse) {
      for (var i = 0; i < ajaxResponse.childNodes.length; i++) {
        this.setValues(ajaxResponse.childNodes[i]);
      }

      // Update the time
      if (dojo.byId('UpdatedTime') != null) {
        var now = new Date();
        dojo.byId('UpdatedTime').innerHTML = 
          '<fmt:message key="resource.common.monitor.visibility.LastUpdated"/> ' + now.toLocaleString();
      }
    },

    setValues: function(metricValues) {
      for (var i = 0; i < MetricAttributes.length; i++) {
        this.substitute(MetricAttributes[i], metricValues);
      }
    },

    substitute: function( attribute, metricValues) {
      var metric = metricValues.getAttribute('mid');

      if ($(attribute + metric) != null) {
        this.lastSpan = $(attribute + metric);

        var html;
        if (attribute == "avail") {
          var availVal = metricValues.getAttribute("last");
          if (availVal == '100.0%') {
            html = '<html:img page="/images/icon_available_green.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>';
          }
          else if (availVal == '0.0%') {
            html = '<html:img page="/images/icon_available_red.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>';
          }
          else if (availVal == '-1.0%') {
            html = '<html:img page="/images/icon_available_orange.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>';
          }
          else {
            html = '<html:img page="/images/icon_available_yellow.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>';
          }
        }
        else {
          html = metricValues.getAttribute(attribute);
        }

        if (this.lastSpan.innerHTML != html) {
          new Effect.Highlight(this.lastSpan.parentNode);
          this.lastSpan.innerHTML = html;
          new Effect.Pulsate(this.lastSpan);
        }
      }
    }
  }

  var lastUpdate = new Date();

  function getLiveMetrics() {
    var now = new Date();
    if (liveUpdate && (now - lastUpdate) >= refreshInterval) {
      <%--ajaxEngine.sendRequest( 'getLiveMetrics',
                              "eid=<c:out value="${eid}"/>"
                            <c:if test="${not empty childResourceType}">
                              ,"ctype=<c:out value="${ctype}"/>"
                            </c:if>
                            );--%>
    dojo.xhrPOST( {
        url: '<html:rewrite page="/resource/common/monitor/visibility/CurrentMetricValues.do"/>', 
        handleAs: "text",
        timeout: 5000,
        load: function(data, ioArgs){lastUpdate = now;setMetricsRefresh();}
    });
      //lastUpdate = now;
    }
    //setMetricsRefresh();
  }

  function setMetricsRefresh() {
    setTimeout( "getLiveMetrics()", 60*1000 );
  }

</script>

<c:forEach var="metricDisplaySummary" items="${rows}">
  <c:url var="metadataLink" value="/resource/common/monitor/Visibility.do">
    <c:param name="mode" value="metricMetadata"/>
    <c:param name="m" value="${metricDisplaySummary.templateId}"/>
    <c:param name="eid" value="${eid}"/>
    <c:choose>
    <c:when test="${not empty childResourceType}">
      <c:param name="ctype" value="${ctype}"/>
    </c:when>
    </c:choose>
  </c:url>
  <c:url var="chartLink" value="/resource/common/monitor/Visibility.do">
    <c:param name="eid" value="${Resource.entityId}"/>
    <c:param name="m" value="${metricDisplaySummary.templateId}"/>
    <c:choose>
      <c:when test="${metricDisplaySummary.availUp > 1}">
        <c:param name="mode" value="${MODE_MON_CHART_SMMR}"/>
      </c:when>
      <c:otherwise>
        <c:param name="mode" value="${MODE_MON_CHART_SMSR}"/>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <%-- 
      the measurement bean doesn't know the resource type, it really
      should.... the favorites can't link to the charts properly without
      them (see pr 7223)
      --%>
      <c:when test="${not empty childResourceType}">
        <c:param name="ctype" value="${ctype}"/>
      </c:when>
    </c:choose>
  </c:url>
  <tr class="ListRow">
  <c:if test="${useCheckboxes}">
    <td class="ListCellCheckbox"><html:multibox property="m"
    onclick="ToggleSelectionTwoButtons(this, mdsWidgetProps, '${buttonMode}');"
    value="${metricDisplaySummary.templateId}" 
    styleClass="availableListMember"/></td>
  </c:if>
  <c:if test="${useChart}">
    <td class="ListCellCheckbox">
    <c:choose>
      <c:when test="${not empty metricDisplaySummary.metrics}">
        <a href="<c:out value="${chartLink}" />"><html:img page="/images/icon_chart.gif" width="10" height="10" alt="" border="0"/></a>
      </c:when>
      <c:otherwise>
        &nbsp;
      </c:otherwise>
    </c:choose>
    </td>
  </c:if>
  <c:choose>
  <c:when test="${useChart}">
    <td class="ListCellPrimary" nowrap>
    <c:choose>
      <c:when test="${not empty metricDisplaySummary.metrics}">
        <a href="<c:out value="${chartLink}" />"><c:out value="${metricDisplaySummary.label}" /></a></td>
      </c:when>
      <c:otherwise>
        <c:out value="${metricDisplaySummary.label}" />
      </c:otherwise>
    </c:choose>
  </c:when>
  <c:otherwise>
    <td class="ListCellPrimary" nowrap><c:out value="${metricDisplaySummary.label}" /></td>
  </c:otherwise>
  </c:choose>
    <c:choose>
      <%-- used for favorites --%>
      <c:when test="${MetricsDisplayForm.showMetricSource}">
        <td class="ListCellLeftLineNoPadding" align="center">
        <c:out value="${metricDisplaySummary.metricSource}" default="&nbsp;" escapeXml="false"/>
        </td>
      </c:when>
      <c:otherwise>
      <!-- metric source not shown -->
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${MetricsDisplayForm.showNumberCollecting}">
        <td class="ListCellLeftLineNoPadding" align="center">
          <c:choose>
            <c:when test="${not empty metricDisplaySummary.availUp}">
              <c:out value="${metricDisplaySummary.availUp}" />
            </c:when>
            <c:otherwise>0</c:otherwise>
          </c:choose>
        </td>
      </c:when>
      <c:otherwise>
      <!-- number collecting not shown -->
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${metricDisplaySummary.label == 'Availability'}">
    <td class="ListCellRight" nowrap> &nbsp; </td>
    <td class="ListCellRight" nowrap><span id="<c:out value="average${metricDisplaySummary.templateId}"/>"><c:out value="${metricDisplaySummary.metrics[average].valueFmt}" /></span></td>
    <td class="ListCellRight" nowrap> &nbsp; </td>
    <td class="ListCellCheckboxLeftLine" nowrap>
    <span id="<c:out value="avail${metricDisplaySummary.templateId}"/>">
    <c:choose>
      <c:when test="${metricDisplaySummary.metrics[last].value == 1}">
      <html:img page="/images/icon_available_green.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>
      </c:when>
      <c:when test="${metricDisplaySummary.metrics[last].value == 0}">
      <html:img page="/images/icon_available_red.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>
      </c:when>
      <c:when test="${metricDisplaySummary.metrics[last].value == -0.01}">
      <html:img page="/images/icon_available_orange.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>
      </c:when>
      <c:otherwise>
      <html:img page="/images/icon_available_yellow.gif" width="12" height="12" alt="" border="0" align="absmiddle"/>
      </c:otherwise>
    </c:choose>
    </span>
    </td>
      </c:when>
      <c:when test="${not empty metricDisplaySummary.metrics && metricDisplaySummary.collectionType == dynamic}">
    <td class="ListCellRight" nowrap>
      <span id="<c:out value="min${metricDisplaySummary.templateId}"/>"><c:out value="${metricDisplaySummary.metrics[min].valueFmt}" /></span>
    </td>
    <td class="ListCellRight" nowrap>
      <span id="<c:out value="average${metricDisplaySummary.templateId}"/>"><c:out value="${metricDisplaySummary.metrics[average].valueFmt}" /></span>
    </td>
    <td class="ListCellRight" nowrap>
      <span id="<c:out value="max${metricDisplaySummary.templateId}"/>"><c:out value="${metricDisplaySummary.metrics[max].valueFmt}" /></span>
    </td>
    <td class="ListCellRight" nowrap>
      <span id="<c:out value="last${metricDisplaySummary.templateId}"/>"><c:out value="${metricDisplaySummary.metrics[last].valueFmt}" /></span>
    </td>
      </c:when>
      <c:otherwise>
    <td class="ListCellRight" nowrap><fmt:message key="common.label.Dash"/></td>
    <td class="ListCellRight" nowrap><fmt:message key="common.label.Dash"/></td>
    <td class="ListCellRight" nowrap><fmt:message key="common.label.Dash"/></td>
    <td class="ListCellRight" nowrap>
      <c:choose>
        <c:when test="${not empty metricDisplaySummary.metrics}">
          <span id="<c:out value="last${metricDisplaySummary.templateId}"/>"><c:out value="${metricDisplaySummary.metrics[last].valueFmt}" /></span>
        </c:when>
        <c:otherwise>
          <fmt:message key="common.label.Dash"/>
        </c:otherwise>
      </c:choose>
    </td>
      <!-- baseline not shown -->
      </c:otherwise>
    </c:choose>
    <td class="ListCellRight" style="text-align: center;">
      <c:choose>
        <c:when test="${metricDisplaySummary.interval > 0}">
          <hq:dateFormatter value="${metricDisplaySummary.interval}" time="true"/>
        </c:when>
        <c:otherwise>
          <c:choose>
            <c:when test="${metricDisplaySummary.collecting}">
              <fmt:message key="resource.common.monitor.visibility.config.DIFFERENT"/>
            </c:when>
            <c:otherwise>
              <fmt:message key="resource.common.monitor.visibility.config.NONE"/>
            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>

    </td>
    <td class="ListCellCheckbox">
    <c:choose>
      <c:when test="${not empty metricDisplaySummary.metrics}">
        <a href="" onclick="window.open('<c:out value="${metadataLink}" />','_metricMetadata','width=800,height=<c:out value="${metadataPopupHeight}" />,scrollbars=yes,toolbar=no,left=80,top=80,resizable=yes'); return false;">
        <html:img page="/images/icon_info.gif" width="11" height="11" alt="" border="0"/></a>
      </c:when>
      <c:otherwise>&nbsp;</c:otherwise>
    </c:choose>
    </td>
  </tr>
</c:forEach>
