<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="2" class="ListCellHeader">
      <fmt:message key="resource.common.monitor.visibility.options.categories"/></td>
  </tr>
  <tr> 
    <td class="ListCell" width="50%" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="0"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==0}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/><fmt:message key="resource.common.monitor.visibility.AvailabilityTH"/></td>
    <td class="ListCell" width="50%" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="1"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==1}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/>
      <fmt:message key="resource.common.monitor.visibility.UtilizationTH"/></td>
  </tr>
  <tr>
    <td class="ListCell" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="2"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==2}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/>
      <fmt:message key="resource.common.monitor.visibility.UsageTH"/></td>
    <td class="ListCell" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="3"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==3}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/>
      <fmt:message key="resource.common.monitor.visibility.PerformanceTH"/></td>
  </tr>
  <tr>
    <td colspan="2" class="ListCellHeader">
      <fmt:message key="resource.common.monitor.visibility.options.valueTypes"/></td>
  </tr>
  <tr> 
    <td class="ListCell" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="4"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==4}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.dynamic"/></td>
    <td class="ListCell" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="5"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==5}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsup"/></td>
  </tr>
  <tr>
    <td class="ListCell" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="6"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==6}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.trendsdown"/></td>
    <td class="ListCell" nowrap> 
      <input type="checkbox" id="filter" name="filter"  value="7"  
	    <c:forEach var="curFilter" items="${metricsDisplayForm.filter}">
			<c:if test="${curFilter==7}">             
				<c:out value="checked=checked"/>
			</c:if> 
		</c:forEach>
		/>
      <fmt:message key="resource.common.monitor.visibility.metricmetadata.collection.type.static"/></td>
  </tr>
</table>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td class="ListCell" align="right"> 
      <fmt:message key="resource.hub.search.KeywordSearchLabel"/>
      <s:textfield theme="simple" name="keyword" value="%{#attr.metricsDisplayForm.keyword}" size="20" onkeyup="enterClicked(event)"/>
    </td>
    <td class="ListCell" align="left">
      <s:hidden theme="simple" value="%{attr.showAll}" name="MetricsDisplayForm" id="MetricsDisplayForm"/>
	  <input type="hidden" id="filterStr" name="filterStr" value=""/>
      <input type="image" property="filterSubmit" src="/images/4.0/icons/accept.png" onclick="createFilterStr()" border="0"/>
    </td>
  </tr>
</table>
<script>
 function createFilterStr (){
	var finalStr = "";
    var prefix = "";	
	if( document.forms.filterMetricsForm.filter){
		for(var ind= 0; ind < 8; ++ind){
				if(document.forms.filterMetricsForm.filter[ind].checked){
					finalStr += prefix + document.forms.filterMetricsForm.filter[ind].value;
					prefix = ",";
				}
		}
	}
	document.forms.filterMetricsForm.filterStr.value = finalStr;
 }
  function enterClicked (event){
	var isIE = (navigator.userAgent.indexOf("MSIE") != -1);
	if (isIE) {
		createFilterStr();
	}
 }
</script>