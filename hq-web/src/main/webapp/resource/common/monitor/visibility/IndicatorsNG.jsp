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
 
<tiles:importAttribute name="tabListName" ignore="true" />
<tiles:importAttribute name="entityType" ignore="true" />

<c:set var="entityId" value="${Resource.entityId}" />
<c:set var="eid" value="${Resource.entityId.appdefKey}" />
<c:set var="ctype" value="${param.ctype}" />
<c:set var="view" value="${IndicatorViewsForm.view}" />

<tiles:insertDefinition name=".resource.common.monitor.visibility.dashminitabs">
	<tiles:putAttribute name="selectedIndex" value="0" />
	<tiles:putAttribute name="resourceId" value="${Resource.id}" />
	<tiles:putAttribute name="resourceType" value="${entityId.type}" />
	<tiles:putAttribute name="autogroupResourceType" value="${ctype}" />
	<tiles:putAttribute name="entityType" value="${entityType}" />
	<tiles:putAttribute name="tabListName" value="${tabListName}" />
</tiles:insertDefinition>
<jsu:script>
	<c:url var="baseUrl" value="addIndicatorCharts.action">
		<c:param name="eid" value="${eid}"/>
	</c:url>
	<c:if test="${not empty ctype}">
		<c:url var="baseUrl" value="${baseUrl}">
			<c:param name="ctype" value="${ctype}"/>
		</c:url>
	</c:if>

	var baseUrl = '${baseUrl}';
	
	function addMetric(metric) {
		<c:url var="baseUrl" value="${baseUrl}">
			<c:param name="view" value="${view}"/>
			<c:param name="action" value="add"/>
			<c:param name="addMetric" value="{addMetric}"/>
		</c:url>
	    var frameUrl = unescape('${baseUrl}').replace("{addMetric}", metric);
	
	    window.parent.frames[0].location = frameUrl;
	}

	function reviewAction(option) {
		hqDojo.byId('isDeleteView').value = false;
		hqDojo.byId('isCreate').value = false;
		hqDojo.byId('isGoToView').value = false;
	    var form = document.updateIndicatorCharts;

	    if (option.value == 'go') {
			hqDojo.byId('isGoToView').value = true;
	        hqDojo.byId('viewname').style.display = "";
	        form.view.value = option.text;
	        form.submit();
	    } else if (option.value == 'delete') {
	        form.view.value = "";
			hqDojo.byId('isDeleteView').value = true;
	        form.submit();
	    } else if (option.value == 'create') {
	        hqDojo.byId('viewname').style.display = "";
	        hqDojo.byId('isCreate').value = true;
	        return;
	    } else if (option.value == 'update') {
	        form.view.value = "<c:out value="${view}"/>";
	    }
	    
	    hqDojo.byId('viewname').style.display = "none";
	}
</jsu:script>
<s:form action="updateIndicatorCharts"
	       method="GET" onsubmit="this.view.disabled=false">
	<input type="hidden" name="eid" value="<c:out value="${eid}"/>">
	<input type="hidden" name="ctype" value="<c:out value="${param.ctype}"/>">
	<input type="hidden" name="isGoToView" id="isGoToView" value="false"/>
	<input type="hidden" name="isDeleteView" id="isDeleteView" value="false"/>
	
	<c:if test="${not empty ctype}">
		<input type="hidden" name="ctype" value="<c:out value="${ctype}"/>">
	</c:if>

	<table width="680" cellpadding="2" cellspacing="0" border="0">
		<tr>
			<td class="ListHeaderInactive" nowrap="true">
				<fmt:message key="resource.common.monitor.visibility.IndicatorCharts" />&nbsp;
				<fmt:message key="common.label.Pipe" />&nbsp;
				<c:url var="resourceCurrentHealthUrl" value="alertMetricsControlAction.action">
					<c:param name="eid" value="${eid}"/>
					<c:param name="a" value="1"/>
					<c:param name="rn" value="8"/>
					<c:param name="ru" value="3"/>
					<c:param name="ctype" value="${ctype}"/>
					<c:param name="view" value="${view}"/>
					<c:param name="alertDefaults" value="true"/>
				</c:url>
				<a href="${resourceCurrentHealthUrl}">
					<fmt:message key="resource.common.monitor.visibility.now" />
				</a>
			</td>
			<td class="ListHeaderInactive" valign="middle" align="right" width="100%">
				<table cellspacing="2">
					<tr>
						<td>
							<fmt:message key="Filter.ViewLabel" /> 
							<select name="action" id="action" onchange="reviewAction(this.options[this.selectedIndex]);">
								<option value="update">
									<fmt:message key="resource.common.monitor.visibility.view.Update" /> 
									<c:out value="${view}" />
								</option>
								<option value="create">
									<fmt:message key="resource.common.monitor.visibility.view.New" />
								</option>
								<c:if test="${not empty IndicatorViewsForm.views[1]}">
									<option value="delete">
										<fmt:message key="resource.common.monitor.visibility.view.Delete" /> 
										<c:out value="${view}" />
									</option>
									<option disabled="true">
										<fmt:message key="resource.common.monitor.visibility.view.Separator" />
									</option>
									<option disabled="true">
										<fmt:message key="resource.common.monitor.visibility.view.Goto" />
									</option>
									
									<c:forEach var="viewname" items="${IndicatorViewsForm.views}">
										<option value="go"><c:out value="${viewname}" /></option>
									</c:forEach>
								</c:if>
							</select> 
							<span id="viewname" style="display: none;"> 
								<input type="hidden" name="isCreate" id="isCreate" value="false">
								<fmt:message key="common.label.Name" /> 
								<s:textfield theme="simple" size="20" name="view" value="%{#attr.view}" />
							</span>
						</td>
						<td align="right">
							<!-- Use hidden input because IE doesn't pass value of of image -->
							<input type="hidden" name="update" value="<c:out value="${view}"/>">
							<input type="image" name="submit" src='<s:url value="/images/dash-button_go-arrow.gif"/>' border="0" alt="Apply Chart View" />
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
								<img src='<s:url value="/images/timeline_ul.gif"/>' height="10" />
							</td>
							<c:forEach var="avail" items="${availabilityMetrics}" varStatus="status">
								<td width="9">
										<c:choose>
											<c:when test="${avail.value == 1}">
												<c:set var="timelineIndicatorColor" value="timelineGreen" />
											</c:when>
											<c:when test="${avail.value == -0.01}">
												<c:set var="timelineIndicatorColor" value="timelineOrange" />
											</c:when>
											 <c:when test="${avail.value == -0.02}">
 	 	 	 									<c:set var="timelineIndicatorColor" value="timelineBlack" />
 	 	 	 								</c:when>
											<c:when test="${avail.value <= 0}">
												<c:set var="timelineIndicatorColor" value="timelineRed" />
											</c:when>
											<c:when test="${avail.value > 0 && avail.value < 1}">
												<c:set var="timelineIndicatorColor" value="timelineYellow" />
											</c:when>
											<c:otherwise>
												<c:set var="timelineIndicatorColor" value="timelineUnknown" />
											</c:otherwise>
										</c:choose>
									<div class="${timelineIndicatorColor}"
									     onmouseover="overlay.moveOverlay(this);overlay.showTimePopupTopMetricChart(<c:out value="${status.count - 1}"/>, event)"
									     onmouseout="overlay.hideTimePopup(this)"></div>
								</td>
							</c:forEach>
							<td width="10" align="left">
								<img src='<s:url value="/images/timeline_ur.gif"/>' height="10" />
							</td>
							<td class="BoldText" style="padding-left: 4px;">
								<c:choose>
									<c:when test="${entityType eq 'application'}">
										<fmt:message key="resource.common.monitor.visibility.availability.value">
											<fmt:param value="${availMetricsAttr}" />
										</fmt:message>
									</c:when>
									<c:otherwise>
										<c:url var="availChart" value="viewDesignatedChartAction.action">
											<c:param name="eid" value="${eid}" />
											<c:if test="${not empty ctype}">
												<c:param name="ctype" value="${ctype}" />
											</c:if>
										</c:url>
										<a href="<c:out value="${availChart}"/>"> 
											<fmt:message key="resource.common.monitor.visibility.availability.value">
												<fmt:param value="${availMetricsAttr}" />
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
					<c:url var="chartUrl" value="freshIndicatorCharts.action">
						<c:param name="eid" value="${eid}" />
						
						<c:if test="${not empty ctype}">
							<c:param name="ctype" value="${ctype}" />
						</c:if>
					
						<c:param name="view" value="${view}" />
						<c:param name="action" value="fresh" />
					</c:url> 
					<iframe id="chartFrame" src="<c:out value="${chartUrl}"/>" 
					        marginwidth="0" marginheight="0" frameborder="no" scrolling="auto"
							style="border-style: none; height: 450px; width: 100%;"></iframe>
				</div>
				<div id="chartbottom"></div>
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<tiles:insertDefinition name=".resource.common.monitor.visibility.timeline">
                    <tiles:putAttribute name="entityType" value="${entityType}" />
					<c:if test="${entityType eq 'autogroup'}">
						<tiles:putAttribute name="hideLogs" value="true" />
					</c:if>
				</tiles:insertDefinition>
			</td>
		</tr>
	</table>
</s:form>
