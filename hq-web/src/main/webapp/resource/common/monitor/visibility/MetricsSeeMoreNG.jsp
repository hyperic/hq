<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
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


<tiles:importAttribute name="widgetInstanceName"/>
<tiles:importAttribute name="useConfigureButton" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>

<c:if test="${empty useConfigureButton}">
  <c:set var="useConfigureButton" value="true"/>
</c:if>

<!--  METRICS GET CURRENT -->
<c:if test="${useConfigureButton}">
<c:choose>
  <c:when test="${not empty childResourceType}">
    <c:set var="resourceTypeName" value="autogroup"/>
  </c:when>
  <c:otherwise>
    <c:set var="resourceTypeName" value="${Resource.entityId.typeName}"/>
  </c:otherwise>
</c:choose>
 <table width="100%" cellpadding="5" cellspacing="0" border="0" class="MonitorToolBar">
  <tr>
    <td width="100%" align="right" nowrap>&nbsp;
	
	
    <c:choose>
      <c:when test="${displayMetrics_showAll}">
        <fmt:message key="resource.common.monitor.visibility.HideNoneMetrics"/>
      </c:when>
      <c:otherwise>
        <fmt:message key="resource.common.monitor.visibility.ShowAllMetrics"/>
      </c:otherwise>
    </c:choose>
    </td>
    <td>
	
    <s:hidden theme="simple" name="showAll" value="%{#attr.displayMetrics_showAll}"/>
	<script>
		var clickedType = '';
	</script>
    <jsu:script>
	    function reverseListing() {
	      document.forms.MetricsDisplayForm.showAll.value =
	        '<c:out value="${!displayMetrics_showAll}"/>';
		  document.forms.MetricsDisplayForm.action = "metricsDisplayAction.action";
	      document.forms.MetricsDisplayForm.submit();
	    }
		
		function metricDisplayFormSubmision() {
			if(clickedType == 'ok' || clickedType == 'remove' || clickedType == 'userset' ){	
					document.forms.MetricsDisplayForm.action = "metricsDisplayAction.action";
			}
			if(document.getElementById("collectionInterval")){
				var curCollectionInterval = document.getElementById("collectionInterval").value;
				if(curCollectionInterval && !(!isNaN(parseFloat(curCollectionInterval)) && isFinite(curCollectionInterval))){
						document.getElementById("collectionInterval").value =0;
				}
			}
	    }
		
		
	</jsu:script>
    <s:a href="javascript:reverseListing();"><img src='<s:url value="/images/4.0/icons/accept.png"/>' border="0"/></s:a>
    </td>
  </tr>
</table>
</c:if>
<!--  /  -->
