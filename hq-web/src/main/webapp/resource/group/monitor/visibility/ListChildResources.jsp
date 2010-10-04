<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2010], VMware, Inc.
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


<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="appdefResourceType" ignore="true"/>
<tiles:importAttribute name="childResourcesTypeKey"/>
<tiles:importAttribute name="childResourcesHealthKey" ignore="true"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:if test="${empty mode}">
  	<c:set var="mode" value="currentHealth"/>
</c:if>

<c:if test="${empty childResourcesHealthKey}">
  	<c:set var="childResourcesHealthKey" value="resource.service.monitor.visibility.MembersTab"/>
</c:if>

<ul id="childResourceList" class="resourceList">
	<li class="header">
		<span class="checkboxColumn">
			<c:choose>
				<c:when test="${not empty summaries && checkboxes}">
					<input id="groupMembersAllCheckbox" type="checkbox" name="groupMembersAll" />
				</c:when>
				<c:otherwise>&nbsp;</c:otherwise>
			</c:choose>
		</span>
		<span class="nameColumn"><fmt:message key="${childResourcesHealthKey}"/></span>
		<span class="availColumn"><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></span>
		<span class="commentColumn">&nbsp;</span>
	</li>
	<li style="border-bottom:0px;">
		<ul class="resourceListContainer">
			<c:forEach var="summary" items="${summaries}">
				<c:url var="gotoResourceLink" value="/resource/${summary.resourceEntityTypeName}/monitor/Visibility.do">
					<c:param name="mode" value="${mode}" />
					<c:param name="type" value="${summary.resourceTypeId}" />
					<c:param name="rid" value="${summary.resourceId}" />
				</c:url>
				<li>
					<span class="checkboxColumn">
						<c:if test="${checkboxes}">
							<html:multibox property="eids" value="${summary.entityId}" styleClass="childResource" />
						</c:if>
					</span>
					<span class="nameColumn">
						<a href="<c:out value="${gotoResourceLink}" />"><c:out value="${summary.resourceName}"/></a>
					</span>
					<span class="availColumn">
						<tiles:insert page="/resource/common/monitor/visibility/AvailIcon.jsp">
		   					<tiles:put name="availability" beanName="summary" beanProperty="availability" />
						</tiles:insert>
					</span>
					<span class="commentColumn">
						<c:set var="metricValue">
							<hq:metric metric="${summary.throughput}" unit="${summary.throughputUnits}" defaultKey="common.value.notavail" />
						</c:set>
						<c:set var="metricValue" value="${fn:substringAfter(metricValue, '<span>')}" />
						<c:set var="metricValue" value="${fn:substringBefore(metricValue, '</span>')}" />
						<div class="resourceCommentIcon" 
						     id="comment_<c:out value="${summary.resourceId}" />" 
						     resourcename="<c:out value="${summary.resourceTypeName}" />" 
						     metricvalue="<c:out value="${metricValue}" />">&nbsp;</div>
					</span>
				</li>
			</c:forEach>
			<c:if test="${empty summaries}">
				<li style="padding-left:25px;">
					<i><fmt:message key="resource.common.monitor.visibility.NoHealthsEtc"/></i>
				</li>
		  	</c:if>
		</ul>
	</li>	  	
</ul>
<div id="resourceInfoPopup" class="menu popup">
	<p>
		<span class="BoldText"><fmt:message key="${childResourcesTypeKey}"/></span><br/>
   		<span id="resourceInfoPopupNameField"></span>
	</p>
	<p>
	   	<span class="BoldText"><fmt:message key="resource.common.monitor.visibility.USAGETH"/></span><br/>
		<span id="resourceInfoPopupMetricValueField"></span>           	
	</p>
</div>
<script>
	dojo11.addOnLoad(function() {
		var masterCheckbox = dojo11.byId("groupMembersAllCheckbox")
		
		if (masterCheckbox) {
			dojo11.connect(masterCheckbox, "onclick", function(e) {
				var cb = e.target;
				var ul = cb.parentNode.parentNode.parentNode;
	
				dojo11.query("input.childResource", ul).forEach(function(el) {
					el.checked = cb.checked;
				});
			});
		}
		
		var list = dojo11.byId("childResourceList");

		dojo11.connect(list, "onmouseover", function(e) {
			var el = e.target;
			
			if (dojo11.hasClass(el, "resourceCommentIcon")) {
				var nameEl = dojo11.byId("resourceInfoPopupNameField");
				var metricValueEl = dojo11.byId("resourceInfoPopupMetricValueField");

				nameEl.innerHTML = el.attributes["resourcename"].value;
				metricValueEl.innerHTML = el.attributes["metricvalue"].value;
				
				menuLayers.show("resourceInfoPopup", e);
			}
		})
		
		dojo11.connect(list, "onmouseout", function(e) {
			if (dojo11.hasClass(e.target, "resourceCommentIcon")) {
				menuLayers.hide();
			}
		});
	});
</script>