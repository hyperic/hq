<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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
<c:set var="eid" value="${param.eid}"/>
<c:set var="ctype" value="${param.ctype}"/>

<html>
<head>
<META Http-Equiv="Cache-Control" Content="no-cache">
<META Http-Equiv="Pragma" Content="no-cache">
<META Http-Equiv="Expires" Content="0">

<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
</head>

<body style="background-color: #DBE3F5;">

<div id="slowScreenSplash" align="center" class="wait" style="top:20%;left:22%;">
<c:choose>
<c:when test="${not empty chartDataKeys}">
  <fmt:message key="resource.common.monitor.visibility.request.wait"/><img src="<html:rewrite page="/images/4.0/icons/ajax-loader.gif" />" alt="">
</c:when>
<c:otherwise>
  <!-- Some weird tag bug that forces me to use the single tag syntax, rather than open/close -->
  <c:set var="fmtBegin"><hq:dateFormatter value="${begin}"/></c:set>
  <c:set var="fmtEnd"><hq:dateFormatter value="${end}"/></c:set>
  <fmt:message key="resource.common.monitor.visibility.no.indicators">
    <fmt:param value="${fmtBegin}"/>
    <fmt:param value="${fmtEnd}"/>
  </fmt:message>
</c:otherwise>
</c:choose>
</div>

<ul id="root" class="boxy">
</ul>
	<script type="text/javascript">
		var djConfig = {};
		djConfig.parseOnLoad = true;
		djConfig.baseUrl = '/static/js/dojo/1.5/dojo/';
		djConfig.scopeMap = [ [ "dojo", "hqDojo" ], [ "dijit", "hqDijit" ], [ "dojox", "hqDojox" ] ];
	</script>
	<!--[if IE]>
	<script type="text/javascript">
		// since dojo has trouble when it comes to using relative urls + ssl, we
		// use this workaorund to provide absolute urls.
		function qualifyURL(url) {
			var a = document.createElement('img');
		    a.src = url;
		    return a.src;
		}
				
		djConfig.modulePaths = {
		    "dojo": qualifyURL("/static/js/dojo/1.5/dojo"),
		    "dijit":  qualifyURL("/static/js/dojo/1.5/dijit"),
		    "dojox":  qualifyURL("/static/js/dojo/1.5/dojox")
	  	};
	</script>
	<![endif]-->
	<script src="<html:rewrite page='/static/js/dojo/1.5/dojo/dojo.js'/>" type="text/javascript"></script>
	<script src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
	<script src="<html:rewrite page="/js/prototype.js"/>" type="text/javascript"></script>
	<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>
	<script src="<html:rewrite page="/js/lib/lib.js"/>" type="text/javascript"></script>
	<script>
		var indicatorCharts = new Array();
	
		<c:forEach var="metric" varStatus="status" items="${chartDataKeys}">
		   <c:choose>
		     <c:when test="${xlib}">
		     		indicatorCharts[indicatorCharts.length] = {
		     	     		"entityId" : "<c:out value="${metric.entityId}" />",
		     	     		"entityType" : "<c:out value="${metric.entityId.type}" />",
		
		     			    <c:if test="${not empty metric.childType}">
		     	     		"ctype" : "<c:out value="${metric.childType}" />",
		     			    </c:if>
		     			    
		     	     		"metricId" : "<c:out value="${metric.templateId}" />",
		     	     		"metricLabel" : "<c:out value="${metric.label}"/>",
		     	     		"metricSource" : "<c:out value="${metric.metricSource}"/>",
		     	     		"minMetric" : "<c:out value="${metric.minMetric.valueFmt}"/>",
		     	     		"avgMetric" : "<c:out value="${metric.avgMetric.valueFmt}"/>",
		     	     		"maxMetric" : "<c:out value="${metric.maxMetric.valueFmt}"/>",
		     	     		"unitUnits" : "<c:out value="${metric.unitUnits}"/>",
		     	     		"unitScale" : "<c:out value="${metric.unitScale}"/>",
		     	     		"index" : "<c:out value="${status.index}"/>",
		     	     		"timeToken" : "<c:out value="${IndicatorViewsForm.timeToken}"/>"     	     		
		     		};
		     </c:when>
		     <c:otherwise>
					indicatorCharts[indicatorCharts.length] = {
							"error" : "<fmt:message key="error.NoXLibInstalled"/>"
					};
		     </c:otherwise>
		   </c:choose>
			   
		</c:forEach>
		var props = { 
				  "name" : "MyIndicatorChartsManager",
				  "view" : "<c:out value="${IndicatorViewsForm.view}"/>",
				  "eid" : "<c:out value="${eid}"/>",
				  "baseUrl" : "/resource/common/monitor/visibility/IndicatorCharts.do",
				  "displaySize" : <c:out value="${IndicatorViewsForm.displaySize}"/>
		  };
		
		  <c:if test="${not empty ctype}">
		  props.ctype = "<c:out value="${ctype}"/>";
		  </c:if>
		
		  props.labels = {
				  "low" : "<fmt:message key="resource.common.monitor.visibility.LowTH"/>",
				  "avg" : "<fmt:message key="resource.common.monitor.visibility.AvgTH"/>",
				  "peak" : "<fmt:message key="resource.common.monitor.visibility.PeakTH"/>"
		  }
		  
		  var MyIndicatorChartsManager = new hyperic.indicator_charts_manager(props, indicatorCharts);
		
		  <c:if test="${not empty chartDataKeys}">
		  MyIndicatorChartsManager.loadChart(0);
		  </c:if>
	</script>
</body>
</html>

