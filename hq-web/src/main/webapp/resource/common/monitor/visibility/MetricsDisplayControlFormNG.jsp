<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>


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

<tiles:importAttribute name="form" ignore="true"/>
<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="eid" ignore="true"/>
<tiles:importAttribute name="view" ignore="true"/>

<c:if test="${not empty mode}">
  <input type="hidden" name="mode" value="<c:out value="${mode}"/>"/>
</c:if>
<c:if test="${not empty eid}">
  <input type="hidden" name="eid" value="<c:out value="${eid}"/>"/>
</c:if>
<c:if test="${not empty view}">
  <input type="hidden" name="view" value="<c:out value="${view}"/>"/>
</c:if>

<c:if test="${not empty metricsForm and empty form}">
	<c:set var="form" value="${metricsForm}"/>
</c:if>

<c:if test="${not empty param.ctype}">
	<c:set var="ctype" value="${param.ctype}"/>
</c:if>


<c:choose>
  <c:when test="${not empty form}">
    <%-- used only for forms that are not MetricsDisplayForm --%>
    <tiles:importAttribute name="formName"/>
    <c:set var="readOnly" value="${form.readOnly}"/>
    <c:set var="rangeBegin" value="${form.rb}"/>
    <c:set var="rangeEnd" value="${form.re}"/>
    <c:set var="showBaseline" value="false"/>
  </c:when>
  <c:otherwise>
    <c:set var="formName" value="MetricsDisplayForm"/>
    <c:set var="readOnly" value="${MetricsDisplayForm.readOnly}"/>
    <c:set var="rangeBegin" value="${MetricsDisplayForm.rb}"/>
    <c:set var="rangeEnd" value="${MetricsDisplayForm.re}"/>
    <c:set var="highlighted" value="${MetricsDisplayForm.h}"/>
    <c:set var="showBaseline" value="${MetricsDisplayForm.showBaseline}"/>
  </c:otherwise>
</c:choose>

<hq:dateFormatter var="rb" value="${rangeBegin}"/>
<hq:dateFormatter var="re" value="${rangeEnd}"/>

<!-- Table Content -->
<table width="100%" cellspacing="0" border="0" class="MonitorToolbar">
<c:if test="${showBaseline}">
  <tr valign="middle">
    <td width="20%" align="right"><b><fmt:message key="resource.common.monitor.visibility.metricsToolbar.HighlightMetricsLabel"/></b></td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td>
		  <s:select theme="simple" cssStyle="FilterFormText" name="hv" 
		  list="#{ '':'', '1':getText('resource.common.monitor.visibility.metricsToolbar.LowValue'), '2':getText('resource.common.monitor.visibility.metricsToolbar.AverageValue'), '3':getText('resource.common.monitor.visibility.metricsToolbar.PeakValue'), '4':getText('resource.common.monitor.visibility.metricsToolbar.LastValue') }" 
		  value="%{#attr.hv}" />
          </td>
          <td><fmt:message key="resource.common.monitor.visibility.metricsToolbar.is"/></td>
          <td nowrap>
		  <s:select theme="simple" cssStyle="FilterFormText" name="hp" 
		  list="#{ '':'', '5':getText('resource.common.monitor.visibility.metricsToolbar.5%'), '10':getText('resource.common.monitor.visibility.metricsToolbar.10%'), '20':getText('resource.common.monitor.visibility.metricsToolbar.20%'), '30':getText('resource.common.monitor.visibility.metricsToolbar.30%'), '50':getText('resource.common.monitor.visibility.metricsToolbar.50%'), '60':getText('resource.common.monitor.visibility.metricsToolbar.60%'), '70':getText('resource.common.monitor.visibility.metricsToolbar.70%'), '80':getText('resource.common.monitor.visibility.metricsToolbar.80%'), '90':getText('resource.common.monitor.visibility.metricsToolbar.90%'), '100':getText('resource.common.monitor.visibility.metricsToolbar.100%') }" 
		  value="%{#attr.hp}" />
			<!--TODO test the solution when showBaseline is true-->
			<s:select cssStyle="FilterFormText" name="ht" value="%{#attr.ht}" list="highlightThresholdMenu" headerKey="" headerValue=""/>
          </td>
          <td><input type="image" property="highlight" src='<s:url value="/images/4.0/icons/accept.png"/>' border="0"/></td>
<c:choose>
  <c:when test="${highlighted}">
          <td width="100%"><a href="javascript:document.${formName}.submit()" onclick="clickLink('${formName}', 'clear')"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.ClearHighlightingBtn"/></a</td>
  </c:when>
  <c:otherwise>
          <td width="100%">&nbsp;</td>
  </c:otherwise>
</c:choose>
        </tr>
      </table>
    </td>
  </tr>
</c:if>
  <tr valign="middle">
    <td class="boldText" style="text-align:right;"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.MetricDisplayRangeLabel"/></td>
	<input type="hidden" id="advancedBtnClicked" name="advancedBtnClicked" value="false"/>
	<input type="hidden" id="prevBtnClicked" name="prevBtnClicked" value="false"/>
	<input type="hidden" id="nextBtnClicked" name="nextBtnClicked" value="false"/>
<c:choose>
  <c:when test="${readOnly}">
    <td>
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td><s:a href="#" onclick="submitLastPeriod('prevBtnClicked');"> <img  src='<s:url value="/images/tbb_pageleft.gif"/>' border="0"/></s:a></td>
          <td nowrap><fmt:message key="resource.common.monitor.visibility.metricsToolbar.DateRange"><fmt:param value="${rb}"/><fmt:param value="${re}"/></fmt:message></td>
          <td><s:a href="#" onclick="submitLastPeriod('nextBtnClicked');"> <img  src='<s:url value="/images/tbb_pageright.gif"/>' border="0"/></s:a></td>
          <td width="100%" style="padding-left: 5px;">
		  
		  <c:if test="${isCompareGroups}">
			<a href="#" onclick="submitLastEihtHours()">   
		  </c:if>
		  <c:if test="${not isCompareGroups or empty isCompareGroups}">
			<a href='<s:url value="alertMetricsControlAction.action?eid=%{#attr.eid}&ctype=%{#attr.ctype}&view=%{#attr.view}&alertDefaults=true&a=1&rn=8&ru=3"/>'>
		  </c:if>	
            <c:if test="${form.a != 1 || (rangeEnd - rangeBegin) > 172800000}">
              <fmt:message key="resource.common.monitor.visibility.now"/></a>&nbsp;<fmt:message key="common.label.Pipe"/>&nbsp;
          </c:if>
          <a href="#" onclick="advancedDialog.show();return false;"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.EditRangeBtn"/></a></td>
        </tr>
      </table>
    </td>
  </c:when>
  <c:otherwise>
    <td>
      <table width="100%" cellpadding="0" cellspacing="3" border="0">
        <tr>
          <td><s:a href="#" onclick="submitLastPeriod('prevBtnClicked');"> <img src='<s:url value="/images/tbb_pageleft.gif"/>' border="0"/></s:a></td>
          <td><fmt:message key="resource.common.monitor.visibility.metricsToolbar.Last"/></td>
          <td nowrap>
            <s:select theme="simple" cssStyle="FilterFormText" name="rn" value="%{#attr.metricsForm.rn}" id="simpleRn" list="#attr.metricsForm.rnMenu"/>
            <s:select theme="simple" cssStyle="FilterFormText" name="ru" id="simpleRu"
					  list="#{'2':getText('resource.common.monitor.visibility.config.Minutes'), '3':getText('resource.common.monitor.visibility.config.Hours'), '4':getText('resource.common.monitor.visibility.metricsToolbar.Days') }" 
					  value="%{#attr.metricsForm.ru}" />
          </td>
          <td> 
		  <input type="hidden" id="rangeBtnClicked" name="rangeBtnClicked" value=""/>
		  
		  <s:a href="#" onclick="submitLastPeriod('rangeBtnClicked');"> <img src='<s:url value="/images/4.0/icons/accept.png"/>' border="0"/></s:a></td>
          <td width="100%" style="padding-left: 5px;">
		   <c:if test="${isCompareGroups}">
			<a href="#" onclick="submitLastEihtHours()">   
		   </c:if>
		   <c:if test="${not isCompareGroups or empty isCompareGroups}">
			<a href='<s:url value="/alertMetricsControlAction.action?eid=%{#attr.eid}&ctype=%{#attr.ctype}&view=%{#attr.view}&alertDefaults=true&a=1&rn=8&ru=3"/>'>
		   </c:if>
            
            <c:if test="${form.a != 1 || (rangeEnd - rangeBegin) > 172800000}">
              <fmt:message key="resource.common.monitor.visibility.now"/></a>&nbsp;<fmt:message key="common.label.Pipe"/>&nbsp;
            </c:if>
			<a href="#" onclick="advancedDialog.show();return false;"><fmt:message key="resource.common.monitor.visibility.metricsToolbar.AdvancedSettingsBtn"/></a>
          </td>
        </tr>
      </table>
    </td>
<s:if test="fieldErrors.containsKey('rn')">

  <tr>
    <td>&nbsp;</td>
    <td colspan="3" class="ErrorField"><span class="ErrorFieldContent"><s:fielderror fieldName="rn"/></span></td>
  </tr>
</s:if>
  </c:otherwise>
</c:choose>
  </tr>
</table>
<div id="advancedAnchor" style="position: relative; visibility: hidden;"></div>
<div id="advancedContainer">
<div id="advancedDisplay" class="dialog" style="display: none;">
	<tiles:insertDefinition name=".resource.common.monitor.visibility.embeddedMetricDisplayRange">
      	<c:if test="${not empty form}">
	    	<tiles:putAttribute name="form" value="${form}"/>
          	<tiles:putAttribute name="formName" value="${formName}"/>
        </c:if>
    </tiles:insertDefinition>
</div>
</div>

<jsu:script>
	var advancedDialog = null;
</jsu:script>
<jsu:script onLoad="true">	
	advancedDialog = new hqDijit.Dialog({
            id: 'advancedDisplay',
            refocus: true,
            autofocus: false,
            opacity: 0,
            title: "<fmt:message key="resource.common.monitor.visibility.metricsToolbar.EditRangeBtn" />"
    }, hqDojo.byId('advancedDisplay'));
	

	var showHolder = advancedDialog.show;
	var hideHolder = advancedDialog.hide;
	var toggleControl = function (id, enabled) {
		var obj = hqDojo.byId(id);
			
		if (obj) {
			obj.disabled = !enabled;
			
			if (enabled == true) {
				obj.style.visibility = "visible";
			} else {
				obj.style.visibility = "hidden";
			}
		}
	}
	var updateAdvanced =  function(isOn){
		var advancedBtnClicked = hqDojo.byId("advancedBtnClicked");
		if(advancedBtnClicked){
			advancedBtnClicked.value = isOn;
		}
	}	
	advancedDialog.show = function() {
		updateAdvanced(true);
		toggleControl("simpleRn", false);
		toggleControl("simpleRu", false);
		showHolder.call(this);
	}
		
	advancedDialog.hide = function() {
		updateAdvanced(false);
		toggleControl("simpleRn", true);
		toggleControl("simpleRu", true);
		hideHolder.call(this);
	}
	submitLastPeriod  =  function (buttonId){
		toggleControl('rn',false);
		toggleControl('ru',false);
		var range = hqDojo.byId(buttonId);
		if(range){
			range.value = true;
		}
		metricsControlAction.submit();
	}
	submitLastEihtHours = function(){	
		hqDojo.byId('simpleRn').value=8;
		hqDojo.byId('simpleRu').value=3;
		metricsControlAction.submit();
	}
    hqDojo.place(hqDojo.byId('advancedDisplay'), hqDojo.byId('advancedContainer'), "last");
	
	
</jsu:script>
<script>
	var submitLastPeriod  = null;
</script>