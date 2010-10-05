<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

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
<tiles:importAttribute name="showHostPlatform" ignore="true"/>
<tiles:importAttribute name="errKey" ignore="true"/>
<tiles:importAttribute name="tabKey"/>
<tiles:importAttribute name="hostResourcesHealthKey"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:if test="${empty mode}">
	<c:set var="mode" value="currentHealth"/>
</c:if>

<!--  HOST RESOURCES CONTENTS -->
<ul id="hostResourceList" class="resourceList">
	<li class="header">
		<span class="checkboxColumn">
			<c:choose>
				<c:when test="${not empty summaries && checkboxes}">
					<input id="hostResourcesAllCheckbox" type="checkbox" name="hostResourcesAll" />
   				</c:when>
   				<c:otherwise>&nbsp;</c:otherwise>
   			</c:choose>
		</span>
		<span class="nameColumn"><fmt:message key="${tabKey}"/></span>
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
						<c:choose>
							<c:when test="${checkboxes}">
		    					<html:multibox property="host" value="${summary.resourceTypeId}:${summary.resourceId}" styleClass="hostResource"/>
		    				</c:when>
		    				<c:otherwise>&nbsp;</c:otherwise>
		    			</c:choose>
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
							<hq:metric metric="${summary.throughput}" unit="none" defaultKey="common.value.notavail" />
						</c:set>
						<c:set var="metricValue" value="${fn:substringAfter(metricValue, '<span>')}" />
			   			<c:set var="metricValue" value="${fn:substringBefore(metricValue, '</span>')}" />
			   			<div class="resourceCommentIcon" 
			   			     id="comment_<c:out value="${summary.resourceId}" />" 
			   			     resourcename="<c:out value="${summary.resourceTypeName}" />" 
			   			     <c:if test="${showHostPlatform}">
			   			     	parentname="<c:out value="${summary.parentResourceName}" default="PARENT RESOURCE NAME NOT SET"/>"
			   			     	parenteid="<c:out value="${summary.parentResourceTypeId}:${summary.parentResourceId}"/>"
			   			     </c:if>
			   			     metricvalue="<c:out value="${metricValue}" />">&nbsp;</div>
			   		</span>
				</li>
			</c:forEach>
			<c:if test="${empty summaries}">
				<li>
		  			<c:if test="${empty errKey}">
		    			<c:set var="errKey" value="resource.common.monitor.visibility.NoHealthsEtc" />
		  			</c:if>
		  			<tiles:insert definition=".resource.common.monitor.visibility.HostHealthError">
		    			<tiles:put name="errKey" beanName="errKey" />
		  			</tiles:insert>
		  		</li>
			</c:if>
		</ul>
	</li>
</ul>
<div id="hostResourceInfoPopup" class="menu popup">
	<p>
		<span class="BoldText">
			<fmt:message key="${hostResourcesHealthKey}"/> <fmt:message key="resource.common.monitor.visibility.TypeTH"/>
		</span><br/>
	    <span id="hostResourceInfoPopupNameField"></span>
	</p>
    <c:if test="${showHostPlatform}">
    	<p>
	    	<span class="BoldText"><fmt:message key="resource.common.monitor.visibility.HostPlatformTH"/></span><br/>
	    	<a id="hostResourceInfoPopupParentLink" href="#"></a>
    	</p>
    </c:if>
    <p>
	    <span class="BoldText"><fmt:message key="resource.common.monitor.visibility.USAGETH"/></span><br/>
		<span id="hostResourceInfoPopupMetricValueField"></span>           	
    </p>
</div>
<script>
	dojo11.addOnLoad(function() {
		var masterCheckbox = dojo11.byId("hostResourcesAllCheckbox");

		if (masterCheckbox) {
			dojo11.connect(masterCheckbox, "onclick", function(e) {
				var cb = e.target;
				var ul = cb.parentNode.parentNode.parentNode;
		
				dojo11.query("input.hostResource", ul).forEach(function(el) {
					el.checked = cb.checked;
				});
			});
		}
			
		var list = dojo11.byId("hostResourceList");
		
		dojo11.connect(list, "onmouseover", function(e) {
			var el = e.target;
			
			if (dojo11.hasClass(el, "resourceCommentIcon")) {
				var nameEl = dojo11.byId("hostResourceInfoPopupNameField");
				var metricValueEl = dojo11.byId("hostResourceInfoPopupMetricValueField");
				var parentLink = dojo11.byId("hostResourceInfoPopupParentLink");
				
				nameEl.innerHTML = el.attributes["resourcename"].value;
				metricValueEl.innerHTML = el.attributes["metricvalue"].value;

				if (parentLink) {
					<c:url var="parentLink" value="/resource/platform/monitor/Visibility.do">
  						<c:param name="mode" value="${param['mode']}"/>
  					</c:url>
					var baseUrl = "<c:out value="${parentLink}" />";
					
					parentLink.innerHTML = el.attributes["parentname"].value;
					parentLink.href = baseUrl + "&eid=" + el.attributes["parenteid"].value;
				}
				
				menuLayers.show("hostResourceInfoPopup", e);
			}
		})
		
		dojo11.connect(list, "onmouseout", function(e) {
			if (dojo11.hasClass(e.target, "resourceCommentIcon")) {
				menuLayers.hide();
			}
		});
	});
</script>