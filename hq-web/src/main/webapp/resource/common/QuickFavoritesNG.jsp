<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
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


<tiles:importAttribute name="resource"/>

<hq:constant var="PLATFORM" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM"/>
<hq:constant var="SERVER"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER"/>
<hq:constant var="SERVICE" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE"/>
<hq:constant var="APPLICATION"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION"/>
<hq:constant var="GROUP" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP"/>
<hq:constant var="GROUP_DYNAMIC"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_DYNAMIC"/>

<c:if test="${resource.entityId.type == PLATFORM }">
	<c:set var="quickActionLink" value="QuickAddPlatformToDashboardFavorites.action" scope="request" />
</c:if>
<c:if test="${resource.entityId.type == SERVER }">
	<c:set var="quickActionLink" value="QuickAddServerToDashboardFavorites.action" scope="request" />
</c:if>
<c:if test="${resource.entityId.type == SERVICE }">
	<c:set var="quickActionLink" value="QuickAddServiceToDashboardFavorites.action" scope="request" />
</c:if>													
<c:if test="${resource.entityId.type == APPLICATION }">
	<c:set var="quickActionLink" value="QuickAddApplicationToDashboardFavorites.action" scope="request" />
</c:if>	
<c:if test="${resource.entityId.type == GROUP || resource.entityId.type == GROUP_DYNAMIC }">
	<c:set var="quickActionLink" value="QuickAddGroupToDashboardFavorites.action" scope="request" />
</c:if>	

<c:choose>
  	<c:when test="${not hasMultipleDashboard && isFavorite}">
		<c:url var="theUrl" value="${quickActionLink}" >
			<c:param name="eid" value="${resource.entityId.appdefKey}"/>
			<c:param name="mode" value="remove"/>
		</c:url>
    	<s:a action="%{#attr.theUrl}">
    		<fmt:message key="resource.common.quickFavorites.remove"/><img width="11" height="9" border="0" src='<s:url value="/images/title_arrow.gif"/>'/>
    	</s:a>
  	</c:when> 
  	<c:otherwise>
  		<c:choose>
  			<c:when test="${not hasMultipleDashboard}">
				<c:url var="theUrl" value="${quickActionLink}" >
					<c:param name="eid" value="${resource.entityId.appdefKey}"/>
  					<c:param name="mode" value="add"/>
				</c:url>
  				<s:a action="%{#attr.theUrl}">
    				<fmt:message key="resource.common.quickFavorites.add"/><img width="11" height="9" border="0" src='<s:url value="/images/title_arrow.gif"/>'/>
    			</s:a>
    		</c:when>
    		<c:otherwise>
				<a id="AddToFavorites_Link" href="#">
					<fmt:message key="resource.common.quickFavorites.addToMultipleDashboards"/><img width="11" height="9" border="0" src='<s:url value="/images/title_arrow.gif"/>'/>
				</a>
				
				<div id="AddToFavorites_Dialog" style="display:none;">
					<input id="AddToFavorites_EID" type="hidden" name="eid" value="<c:out value="${resource.entityId.appdefKey}"/>" />
					<input id="AddToFavorites_Mode" type="hidden" name="mode" value="add" />
					<div id="AddToFavorites_MessageDiv" style="display:none"></div>
					<div id="AddToFavorites_Div" style="width:400px;">
						<div id="AddToFavorites_DataDiv">
							<div style="height:240px; overflow-x:hidden; overflow-y:auto;">
								<table width="100%" cellpadding="0" cellspacing="0" border="0">
									<thead>
										<tr class="tableRowHeader">
											<th class="ListHeaderCheckbox" style="width:20px"><input type="checkbox" id="AddToFavorites_CheckAllBox" onclick="" /></td>
											<th class="ListHeader" style="width:99%"><fmt:message key="common.header.Name"/></td>
										</tr>
									</thead>
									<tbody id="AddToFavorites_TableBody">
										<c:forEach items="${editableDashboards}" var="dash" varStatus="iteration">
											<tr style="background-color:<c:choose><c:when test="${(iteration.count % 2) == 0}">#EDEDED</c:when><c:otherwise>#FFF</c:otherwise></c:choose>;" >
												<td style="padding: 3px; padding-left: 4px;">
													<input type="checkbox" id="AddToFavorites_CheckBox<c:out value='${iteration.count}' />" name="dashboardId" value="<c:out value='${dash.id}' />" />
												</td>
												<td style="padding: 3px;">
													<span><c:out value="${dash.name}" /></span>
												</td>
											</tr>
										</c:forEach>
									</tbody>
								</table>
							</div>
						</div>
						<div id="AddToFavorites_ButtonDiv" style="padding-top:5px">
							<span style="whitespace:nowrap">
								<input type="button" id="AddToFavorites_AddButton" 
								       value="<fmt:message key='common.label.Add' />" class="CompactButton" />
								&nbsp;
		  						<input type="button" id="AddToFavorites_CancelButton" 
		  						       value="<fmt:message key='common.label.Cancel' />" class="CompactButton" />
		  					</span>
		  					<span id="AddToFavorites_Progress" style="display:none">
		  						<img src='<s:url value="/images/4.0/icons/ajax-loader-gray.gif"/>'" align="absMiddle" />
		  					</span>
		  					<span id="AddToFavorites_SuccessMsg" style="display:none;" class="successDialogMsg">
		  						<fmt:message key="resource.common.DashboardUpdatedMessage" />
		  					</span>
		  					<span id="AddToFavorites_ErrorMsg" style="display:none;" class="failureDialogMsg">
		  						<fmt:message key="resource.common.DashboardUpdatedError" />
		  					</span>
						</div>
					</div>
				</div>
				<jsu:importScript path="/js/addtodashboard.js" />
				<jsu:script onLoad="true">
				<c:url var="genUrl" value="${quickActionLink}" >
					<c:param name="eid" value="${resource.entityId.appdefKey}"/>
  					<c:param name="mode" value="add"/>
				</c:url>
				    var config = {
				    	title : "<fmt:message key='resource.common.quickFavorites.addToMultipleDashboards' />",
				    	dialogId : "AddToFavorites_Dialog",
				    	callerId : "AddToFavorites_Link",
				    	url : "<s:url action="%{#attr.genUrl}" />",
				    	addButtonId : "AddToFavorites_AddButton",
				    	cancelButtonId : "AddToFavorites_CancelButton",
				    	progressId : "AddToFavorites_Progress",
		    			successMsgId : "AddToFavorites_SuccessMsg",
		    			failureMsgId : "AddToFavorites_ErrorMsg",
		    			checkboxAllId : "AddToFavorites_CheckAllBox",
				    	checkboxIdPrefix : "AddToFavorites_CheckBox"
				    };
					    
				    AddToDashboard.initDialog(config);
				</jsu:script>
    		</c:otherwise>
    	</c:choose>
  	</c:otherwise> 
</c:choose>