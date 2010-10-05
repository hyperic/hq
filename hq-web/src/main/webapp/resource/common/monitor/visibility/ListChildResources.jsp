<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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
<tiles:importAttribute name="childResourcesHealthKey"/>
<tiles:importAttribute name="childResourcesTypeKey"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="internal" ignore="true"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:if test="${empty mode}">
  <c:set var="mode" value="currentHealth"/>
</c:if>

<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_AUTOGROUP" var="AUTOGROUP" />
<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_CLUSTER" var="CLUSTER" />

<ul id="childResourceList" class="resourceList">
	<li class="header">
		<span class="checkboxColumn">
			<c:choose>
				<c:when test="${not empty summaries && checkboxes}">
					<input id="childResourcesAllCheckbox" type="checkbox" name="childResourcesAll" />
				</c:when>
				<c:otherwise>&nbsp;</c:otherwise>
			</c:choose>
		</span>
		<span class="nameColumn"><fmt:message key="${childResourcesHealthKey}"/></span>
		<span class="availColumn"><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></span>
		<span class="commentColumn">&nbsp;</span>
	</li>
	<c:forEach var="summary" items="${summaries}">
		<c:choose>
  			<c:when test="${summary.summaryType == AUTOGROUP}">
    			<c:url var="gotoResourceLink" value="/resource/autogroup/monitor/Visibility.do">
      				<c:param name="mode" value="${mode}" />
      				<c:param name="eid" value="${Resource.entityId.type}:${Resource.id}"/>
      				<c:choose>
        				<c:when test="${not empty appdefResourceType && appdefResourceType == 4}"> <!-- AppdefEntityConstants.APPDEF_TYPE_APPLICATION-->
          					<c:param name="ctype" value="3:${summary.resourceType.id}" />
        				</c:when>
        				<c:otherwise>
          					<c:choose>
            					<c:when test="${not empty childResourceType}">
              						<c:param name="ctype" value="${childResourceType}:${summary.resourceType.id}" />
            					</c:when>
          						<c:otherwise>
            						<c:param name="ctype" value="${summary.resourceType.id}"/>
          						</c:otherwise>
        					</c:choose>
        				</c:otherwise>
      				</c:choose>
    			</c:url>
  			</c:when>
  			<c:otherwise>
    			<c:url var="gotoResourceLink" value="/resource/${summary.entityId.typeName}/monitor/Visibility.do">
      				<c:param name="mode" value="${mode}" />
      				<c:param name="eid" value="${summary.entityId.appdefKey}" />
    			</c:url>
    		</c:otherwise>
  		</c:choose>
		<li>
			<span class="checkboxColumn">
				<c:if test="${checkboxes}">
					<html:multibox property="child" value="${summary.resourceType.appdefTypeKey}" styleClass="childResource" />
				</c:if>
			</span>
			<c:choose>
   				<c:when test="${summary.summaryType == AUTOGROUP}">
      				<c:set var="icon" value=" autoGroupIcon" />
      			</c:when>
      			<c:when test="${summary.summaryType == CLUSTER}">
      				<c:set var="icon" value=" clusterIcon" />
  				</c:when>
			</c:choose>
			<span class="nameColumn<c:out value="${icon}" />">
				<a href="<c:out value="${gotoResourceLink}" />">
					<c:choose>
   						<c:when test="${summary.summaryType == AUTOGROUP}">
        					<c:out value="${summary.resourceType.name}"/>
      					</c:when>
      					<c:otherwise>
        					<c:out value="${summary.entityName}"/>
      					</c:otherwise>
    				</c:choose>
				</a>
			</span>
			<span class="availColumn">
				<tiles:insert page="/resource/common/monitor/visibility/AvailIcon.jsp">
   					<tiles:put name="availability" beanName="summary" beanProperty="availability" />
				</tiles:insert>
			</span>
			<span class="commentColumn">
				<c:choose>
      				<c:when test="${summary.summaryType == AUTOGROUP}">
	        			<c:set var="commentResourceName">
	        				<fmt:message key="resource.common.monitor.health.autoGroupType"><fmt:param value="${summary.resourceType.name}"/></fmt:message>
	        			</c:set>
	        			<c:set var="commentEID">
							<c:out value="${Resource.entityId}" />
						</c:set>
    	  			</c:when>
      				<c:when test="${summary.summaryType == CLUSTER}">
 	        			<c:set var="commentResourceName">
	        				<fmt:message key="resource.common.monitor.health.clusterGroupType"><fmt:param value="${summary.resourceType.name}"/></fmt:message>
	        			</c:set>
	        			<c:set var="commentEID">
							<c:out value="${summary.entityId.appdefKey}" />
						</c:set>
      				</c:when>
      				<c:otherwise>
	        			<c:set var="commentResourceName">
							<c:out value="${summary.resourceType.name}"/>
	        			</c:set>
	        			<c:set var="commentEID">
							<c:out value="${Resource.entityId}" />
						</c:set>
      				</c:otherwise>
    			</c:choose>
				<div class="resourceCommentIcon" 
					resourcename="<c:out value="${commentResourceName}" />"
				    eid="<c:out value="${commentEID}" />"
  					ctype="<c:out value="${summary.resourceType.appdefType}:${summary.resourceType.id}" />">&nbsp;</div>
			</span>
			<c:set var="resourceTypeNameId" value="${fn:replace(summary.resourceType.name, ' ', '_')}" />
  		</li>
    </c:forEach>
	<c:if test="${empty summaries}">
		<li style="padding-left:5%;">
			<tiles:insert definition=".resource.common.monitor.visibility.noHealths"/>
		</li>
  	</c:if>
</ul>
<div id="resourceInfoPopup" class="menu popup">
	<p>
		<span class="BoldText"><fmt:message key="${childResourcesTypeKey}"/></span><br/>
   		<span id="resourceInfoPopupNameField"></span>
	</p>
	<p>
	   	<span class="BoldText"><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></span><br/>
		<img id="resourceInfoPopupAvailabilityIcon" 
    			     src="/images/progress-running.gif"
    			     border="0" height="12" />         	
	</p>
</div>
<c:url var="stoplightUrl" value="/resource/AvailStoplight" />
<script>
	dojo11.addOnLoad(function() {
		var masterCheckbox = dojo11.byId("childResourcesAllCheckbox")
		
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
				var iconEl = dojo11.byId("resourceInfoPopupAvailabilityIcon");
				
				nameEl.innerHTML = el.attributes["resourcename"].value;
				iconEl.src = "<c:out value="${stoplightUrl}" />?eid=" + el.attributes["eid"].value + "&ctype=" + el.attributes["ctype"].value;

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