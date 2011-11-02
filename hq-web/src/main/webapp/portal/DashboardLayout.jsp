<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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

<c:set var="selectedDashboardId" value="${sessionScope['.user.dashboard.selected.id']}" />
<jsu:importScript path="/js/dash.js" />
<jsu:importScript path="/js/scriptaculous.js" />
<jsu:importScript path="/js/requests.js" />
<jsu:importScript path="/js/dashboard.js" />
<jsu:importScript path="/js/effects.js" />
<jsu:script>
	if (top != self){
	    top.location.href = self.document.location;
	}
	
	autoLogout = false;
	
	function removePortlet(name, label) {
		var portletUrl = "<html:rewrite page="/app/dashboard/${selectedDashboardId}/portlets/{portletName}"/>";
		
	    hqDojo.xhrPost({
	        url: unescape(portletUrl).replace("{portletName}", name),
	        content: {
				_method: "DELETE"
	        },
	        load: function(){postRemovet(name,label)}
	    });
	}
	  
	function postRemovet(name, label){
	    new Effect.BlindUp(hqDojo.byId(name));
	    var wide = isWide(name);
	    if (!wide && !isNarrow(name)) {
	        return;
	    }
	    var portletOptions;
	    for (i = 0; i < document.forms.length; i++) {
	      if (document.forms[i].wide) {
	        if (wide == (document.forms[i].wide.value == 'true')) {
	            portletOptions = document.forms[i].portlet.options;
	            break;
	        }
	      }
	    }
	    if (portletOptions) {
	        // Make sure that we are not re-inserting
	        for (var i = 0; i < portletOptions.length; i++) {
	            if (portletOptions[i].value == name) {
	                return;
	            }
	        }
	        portletOptions[portletOptions.length] = new Option(label, name);
	
	        // Make sure div is visible
	        hqDojo.byId('addContentsPortlet' + wide).style.visibility='visible';
	    }
	}
</jsu:script>
<html:link action="/Resource" linkName="viewResUrl" styleId="viewResUrl" style="display:none;">
	<html:param name="eid" value="{eid}"/>
</html:link>

<%
  String divStart;
  String divEnd;
  String narrowWidth;
    
  String agent = request.getHeader("USER-AGENT");
  
  if (null != agent && -1 !=agent.indexOf("MSIE")) {
    divStart = ""; 
    divEnd = "";
    narrowWidth = "width='25%'";
  }
  else {
    divStart = "<div style=\"margin:0px;\">";
    divEnd = "</div>";
    narrowWidth = "width='100%'";
  }
%>
<c:choose>
	<c:when test="${portal.columns ne null}">
		<c:set var="headerColspan" value="${portal.columns + 3}" />
	</c:when>
	<c:otherwise>
		<c:set var="headerColspan" value="${5}" />
	</c:otherwise>
</c:choose>

<div class="effectsContainer">
	<table width="100%" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<td colspan="100%">
				<tiles:insert page="/portal/DashboardHeader.jsp" />
			</td>
		</tr>
		<tr>
			<!-- Role based config dashboard area -->
			<td colspan="100%" style="padding-left:16px; padding-right:15px;">
				<c:choose>
					<c:when test="${DashboardForm.dashboardSelectable}">
						<html:form method="post" action="/SetDashboard" styleId="DashboardForm">
							<div class="dashboard">
								<div style="display: table-cell; vertical-align: middle; float:left;">
									<span style="font-weight: bold; margin-right: 4px;">
										<fmt:message key="dash.home.SelectDashboard" />
									</span> 
									<html:select property="selectedDashboardId" value="${DashboardForm.selectedDashboardId}" onchange="changeDashboard('DashboardForm');" styleId="dashSelect">
										<html:optionsCollection property="dashboards" value="id" label="name"></html:optionsCollection>
									</html:select> 
									<html:hidden styleId="defaultDashboard" property="defaultDashboard" /> 
									
									<c:if test="${(DashboardForm.selectedDashboardId != DashboardForm.defaultDashboard) || empty DashboardForm.defaultDashboard}">
										<input id="makeDefaultBtn" type="button" class="button42" 
										       value="Make Default" 
										       onclick="selectDefaultDashboard('<html:rewrite page="/SetDefaultDashboard.do" />');" />
										<span id="makeDefaultUpdatingIcon" style="display:none;">
											<html:img page="/images/4.0/icons/ajax-loaders.gif"	alt="updating..." border="0" />
										</span>
										<span id="makeDefaultUserMessage" class="message" style="display:none;"><fmt:message key="dash.home.DashboardDefaultUpdatedMessage" /></span>
										<span id="makeDefaultUserError" class="message" style="display:none;"><fmt:message key="dash.home.DashboardDefaultUpdatedError" /></span>
									</c:if>
								</div>
								<div class="message" style="margin-top: 5px; display: inline; float:right;">
									<c:choose>
										<c:when test="${not sessionScope.modifyDashboard}">
											<span style="font-weight: bold;"><fmt:message key="note" /></span>&nbsp;
											<span><fmt:message key="dash.home.ReadOnlyMessage" /></span>
										</c:when>
										<c:when	test="${sessionScope.modifyDashboard and requestScope.roleDashboard}">
											<span style="font-weight: bold;"><fmt:message key="note" /></span>&nbsp;
											<span><fmt:message key="dash.home.ModifyRoleDashboardMessage" /></span>
										</c:when>
									</c:choose>
								</div>								
							</div>
						</html:form>
					</c:when>
					<c:otherwise>
						<div style="margin: 16px 0px;"></div>
					</c:otherwise>
				</c:choose>
			</td>
		</tr>
		<tr>
			<%-- Multi-columns Layout
	  		This layout render lists of tiles in multi-columns. Each column renders its tiles
	  		vertically stacked.  
			--%>
			<c:set var="narrow" value="true" scope="page" />
			<c:set var="width" value="" scope="page" />
			<c:set var="hr" value="" scope="page" />
			<c:set var="showUpAndDown" value="true" scope="request" />
	
			<!-- Content Block -->
			<c:forEach var="columnsList" items="${portal.portlets}" varStatus="loopStatus">
				<c:choose>
					<c:when test="${portal.columns eq 1}">
						<c:set var="narrow" value="false" />
						<c:set var="styleSpec" value="style='padding-left:16px;padding-right:15px;width:100%;'" />
					</c:when>
					<c:otherwise>
						<c:choose>
							<c:when test="${loopStatus.index == 0}">
								<c:set var="styleSpec" value="style='padding-left:16px;padding-right:5px;width:50%;'" />							
							</c:when>
							<c:otherwise>
								<c:set var="styleSpec" value="style='padding-left:5px;padding-right:15px;width:50%;'" />
							</c:otherwise>
						</c:choose>
					</c:otherwise>
				</c:choose>
	
				<td valign="top" name="specialTd" <c:out value="${styleSpec}" escapeXml="false"/>>
					<%=divStart%>
						<ul id="<c:out value="narrowList_${narrow}"/>" class="boxy">
							<c:forEach var="portlet" items="${columnsList}">
								<c:set var="isFirstPortlet" value="${portlet.isFirst}" scope="request" />
								<c:set var="isLastPortlet" value="${portlet.isLast}" scope="request" />
								<li id="<c:out value="${portlet.fullUrl}"/>">
									<tiles:insert beanProperty="url" beanName="portlet" flush="true">
										<tiles:put name="portlet" beanName="portlet" />
									</tiles:insert>
								</li>
							</c:forEach>
						</ul>
				
						<c:if test="${sessionScope.modifyDashboard}">
							<table width="100%" border="0" cellspacing="0" cellpadding="0">
								<tr>
									<td valign="top" class="DashboardPadding">
										<c:choose>
											<c:when test="${narrow eq 'true'}">
												<tiles:insert name=".dashContent.addContent.narrow" flush="true" />
											</c:when>
											<c:otherwise>
												<tiles:insert name=".dashContent.addContent.wide" flush="true" />
											</c:otherwise>
										</c:choose>
									</td>
								</tr>
							</table>
							<jsu:script>
						        // -----------
						        // XXX:
						        // This should be rewritten using dojo 1.1 dnd.move package
						        // http://docs.google.com/View?docid=d764479_11fcs7s397
						        // writing a new Sortable version using dojo 1.1 which will hopefully play better with IE
						        // -----------
					            Sortable.create("<c:out value="narrowList_${narrow}"/>", {
						            dropOnEmpty: true,
			               			format: /^(.*)$/,
			               			containment: ["<c:out value="narrowList_${narrow}"/>"],
			               			handle: 'widgetHandle',
			               			onUpdate: function() {
			                    		hqDojo.xhrPost({
			                        		url: "<html:rewrite action="/dashboard/ReorderPortlets"/>",
			                        		postData: Sortable.serialize('<c:out value="narrowList_${narrow}"/>'),
			                        		load: function(){ }
			                    		});
			                    	},
			               			constraint: 'vertical'
				               	});
				      		</jsu:script>
				      	</c:if> 
				      	<c:choose>
							<c:when test="${narrow eq 'true'}">
								<c:set var="narrow" value="false" />
							</c:when>
							<c:otherwise>
								<c:set var="narrow" value="true" />
							</c:otherwise>
						</c:choose> 
					<%=divEnd%> 
					<jsu:script>
						if (!Prototype.Browser.IE) {
		        			resizeToCorrectWidth();
		      			}
		      		</jsu:script>
		    	</td>
			</c:forEach>
		</tr>
		<tr>
			<td colspan="100%" style="padding-left:20px; padding-right:15px;">
				<div style=""></div>
			</td>
		</tr>
	</table>
</div>