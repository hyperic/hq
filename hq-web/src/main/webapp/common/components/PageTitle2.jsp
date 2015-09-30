<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
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


<tiles:importAttribute name="titleKey" ignore="true"/>
<tiles:importAttribute name="titleName" ignore="true"/>
<tiles:importAttribute name="titleBgStyle" ignore="true"/>
<tiles:importAttribute name="titleImg" ignore="true"/>
<tiles:importAttribute name="subTitleName" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="linkUrl" ignore="true"/>
<tiles:importAttribute name="showSearch" ignore="true"/>
<tiles:importAttribute name="eid" ignore="true" />
<tiles:importAttribute name="ctype" ignore="true" />
<tiles:importAttribute name="any" ignore="true" />
<tiles:importAttribute name="own" ignore="true" />
<tiles:importAttribute name="unavail" ignore="true" />
<tiles:importAttribute name="ignoreBreadcrumb" ignore="true" />
<tiles:importAttribute name="noTitle" ignore="true" />

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
<hq:constant var="GROUP_COMPAT" 
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalAction"
    symbol="SELECTOR_GROUP_COMPAT"/>
<hq:constant var="GROUP_ADHOC" 
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalAction"
    symbol="SELECTOR_GROUP_ADHOC"/>
<hq:constant var="CHART"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubForm"
	symbol="CHART_VIEW"/>
<hq:constant var="LIST"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubForm"
	symbol="LIST_VIEW"/>

<c:if test="${not empty resourceOwner}">
  	<hq:owner var="ownerStr" owner="${resourceOwner}"/>
</c:if>

<c:if test="${showSearch}">
    <c:choose>
	    <c:when test="${not empty keywords}">
    		<c:set var="initSearchVal" value="${keywords}"/>
    	</c:when>
    	<c:otherwise>
      		<fmt:message var="initSearchVal" key="resource.hub.search.KeywordSearchText"/>
    	</c:otherwise>
  	</c:choose>
</c:if>
<c:set var="any" value="${any}"/>
<c:set var="own" value="${own}"/>
<c:set var="unavail" value="${unavail}"/>
<jsu:script> 
	var help = "<hq:help/>";
</jsu:script>

<c:if test="${empty  ignoreBreadcrumb}">
	<c:set var="ignoreBreadcrumb" value="true"/>
</c:if>
<c:if test="${not empty eid and not ignoreBreadcrumb}">
<div id="breadcrumbContainer" style="padding: 0px 0px 0pt;">
	<hq:breadcrumb resourceId="${eid}" 
				   ctype="${ctype}"
				   baseBrowseUrl="/resourceHub.action" 
				   baseResourceUrl="/resourceAction.action" />
</div>
</c:if>

<table width="100%" cellspacing="0" cellpadding="0" style="border: 0px;clear:both;">
	<tr>
    	<td colspan="4">
			<table width="100%" border="0" cellspacing="0" cellpadding="0" style="border: 0px; margin-bottom: 10px;">
			
				<c:if test="${not empty titleName or not empty titleKey}">
					<tr class="PageTitleBar">
								<c:choose>
									<c:when test="${disregardGenericTitle}">
										<td colspan="100%" style="padding: 0pt 0px 10px;"> 
									</c:when>
									<c:otherwise>
										<td colspan="100%" style="padding: 0pt 25px 10px;"> 
									</c:otherwise>
								</c:choose>	
							
							<c:choose>
								<c:when test="${not empty titleKey}">
									<c:set var="escapedTitleName">
										<c:out value="${titleName}" />
									</c:set>
									<c:set var="escapedSubTitleName">
										<c:out value="${subTitleName}" />
									</c:set>
									<fmt:message key="${titleKey}">
										<fmt:param value="${escapedTitleName}" />
										<fmt:param value="${escapedSubTitleName}" />
									</fmt:message>
								</c:when>
								<c:otherwise>
									<c:out value="${titleName}" escapeXml="false" />									
									<c:if test="${not empty subTitleName}">
										<span class="resourceSubTitle"> 
											<c:out value="${subTitleName}" /> 
										</span>
									</c:if>
								</c:otherwise>
							</c:choose>
						</td>
					</tr>
				</c:if>
			
				<c:if test="${not empty resource || not empty linkUrl || not empty showSearch}">
  					<tr valign="top"> 
  						<c:choose>
    						<c:when test="${not empty resource}">
								<c:choose>
									<c:when test="${disregardGenericTitle}">
											<td colspan="2" style="padding: 5px 0px 0pt;">
									</c:when>
									<c:otherwise>
											<td colspan="2" style="padding: 5px 25px 0pt;">
									</c:otherwise>
								</c:choose>							
      								<table width="100%" border="0" cellspacing="0" cellpadding="0">
        								<tr> 
          									<td class="PageTitleSmallText" valign="top">
      											<b><fmt:message key="common.label.Description"/></b>
      											<hq:shortenText maxlength="30" value="${resource.description}" styleClass="ListCellPopup5"/>
    										</td>
    										<td style="width: 5px;">&nbsp;</td>
    										<td class="PageTitleSmallText" valign="top" colspan="2" nowrap>
      											<b><fmt:message key="resource.common.inventory.props.OwnerLabel"/></b> <c:out value="${ownerStr}" escapeXml="false"/>

                                                <c:if test="${not empty resource &&
      											    ((resource.entityId.type != GROUP) || 
      											    (resource.groupType != GROUP_DYNAMIC))}">
      												-
													<c:if test="${resource.entityId.type == PLATFORM }">
														<c:set var="changeOwnerAction" value="changeOwnerInventoryPlatformVisibility" scope="request" />
													</c:if>
													<c:if test="${resource.entityId.type == SERVER }">
														<c:set var="changeOwnerAction" value="changeOwnerInventoryServerVisibility" scope="request" />
													</c:if>
													<c:if test="${resource.entityId.type == SERVICE }">
														<c:set var="changeOwnerAction" value="changeOwnerInventoryServiceVisibility" scope="request" />
													</c:if>													
													<c:if test="${resource.entityId.type == APPLICATION }">
														<c:set var="changeOwnerAction" value="changeOwnerInventoryApplicationVisibility" scope="request" />
													</c:if>	
													<c:if test="${resource.entityId.type == GROUP || resource.entityId.type == GROUP_DYNAMIC }">
														<c:set var="changeOwnerAction" value="changeOwnerInventoryGroupVisibility" scope="request" />
													</c:if>														
      												<s:a action="%{#attr.changeOwnerAction}">
      													<s:param name="mode" value="changeOwner"/>
      													<s:param name="rid" value="%{#attr.resource.entityId.id}"/>
      													<s:param name="type" value="%{#attr.resource.entityId.type}"/>
      												  	<fmt:message key="resource.common.inventory.props.ChangeButton"/>
      												</s:a>
      												<br>
      											</c:if>
          									</td>
        								</tr>
										<c:if test="${not empty cprops}">
											<c:set var="leftRight" value="1"/>
											<c:forEach var="cprop" items="${cprops}">
											
  												<c:if test="${leftRight > 0}">
    												<tr>
  												</c:if>
      											
      											<td class="PageTitleSmallText" width="33%" valign="top">
      												<b><c:out value="${cprop.key}"/></b>
      												<fmt:message key="common.label.Colon"/>
 													<c:choose>
 	 	 	 											<c:when test="${cprop.key == 'VM Instance'}">
 	 	 	 												<c:out value="${cprop.value}" escapeXml="false" />
 	 	 	 											</c:when>
 	 	 	 											<c:otherwise>
 	 	 	 												<hq:shortenText maxlength="30" value="${cprop.value}" styleClass="ListCellPopup5"/>
 	 	 	 											</c:otherwise>
 	 	 	 										</c:choose>      												
      											</td>
  												
  												<c:choose>
  													<c:when test="${leftRight < 0}">
    													<c:set var="leftRight" value="${leftRight * -1}"/>
    													<td>&nbsp;</td>
    												</tr>
  													</c:when>
  													<c:otherwise>
    													<c:set var="leftRight" value="${leftRight - 1}"/>
    													<td>&nbsp;</td>
  													</c:otherwise>
  												</c:choose>
											</c:forEach>
											<c:if test="${leftRight < 0}">
    											<td colspan="3">&nbsp;</td>
  											</tr>
											</c:if>
										</c:if>
      									<c:if test="${not empty pluginLinkInfo}">
											<tr>
												<td colspan="100%" style="padding-top: 10px;">
													<a href="<s:action name="/mastheadAttach" executeResult="true" ><s:param name="typeId" value="%{#request.pluginLinkInfo.pluginId}"/><s:param name="sn" value="%{#request.pluginLinkInfo.selectedId}"/></s:action>">View in HQ vSphere</a>
												</td>
											</tr>
										</c:if>					
										<c:if test="${empty ResourceType}">
        									<tr>
        										<td colspan="3" style="padding-top: 10px;">
        											<tiles:insertDefinition name=".resource.common.navmap">
          												<tiles:putAttribute name="resource" value="${sessionScope.resource}"/>
        											</tiles:insertDefinition>
        											
        											<!-- TOOLS -->
													<c:if test="${not empty linkUrl}">
                                                        <c:if test="${(resource.entityId.type == GROUP) && (resource.groupType == GROUP_DYNAMIC)}">
                                                            <c:set var="dontShowTools" value="none"/>
                                                        </c:if>
                                                        <div class="toolsMenuStacked LinkBox" style="display:${dontShowTools};">
	        												<span onclick="toggleMenu('toolMenu');" id="toolMenuSpan">
	        													<fmt:message key="resource.toolsmenu.text"/>
	        													<img src='<s:url value="/images/arrow_dropdown.gif" />' border="0" alt="" id="toolMenuArrow">
	        												</span>
															<div style="clear: both"></div>
	        												<div id="toolMenu" style="display: none; position: absolute; margin-top: 2px; margin-left: -7px; z-index:5">
	            												<tiles:insertAttribute name="linkUrl">
	                												<c:if test="${not empty resource}">
	                    												<tiles:putAttribute name="resource" value="${resource}"/>
	                												</c:if>
	            												</tiles:insertAttribute>
	        												</div>
	    												</div>
	    												<div style="clear:both;"></div>
													</c:if>
													<c:if test="${empty linkUrl}">
														<div style="clear:both;"></div>
													</c:if>
													<!-- END TOOLS -->
        										</td>
        									</tr>
      									</c:if>
      								</table>
    							</td>
    						</c:when>
    						<c:when test="${showSearch}">
								<c:choose>
									<c:when test="${disregardGenericTitle}">
										<td style="vertical-align: middle; padding: 0 0px 10px;">
									</c:when>
									<c:otherwise>
										<td style="vertical-align: middle; padding: 0 25px 10px;">
									</c:otherwise>
								</c:choose>	
    							
									<c:choose>
  										<c:when test="${ff == PLATFORM}">
											<s:set var="allTypesKey" value="getText('resource.hub.filter.AllPlatformTypes')"/>
    										<c:set var="allTypesKey" value="resource.hub.filter.AllPlatformTypes"/>
    										<c:set var="section" value="platform"/>
  										</c:when>
  										<c:when test="${ff == SERVER}">
										<s:set var="allTypesKey" value="getText('resource.hub.filter.AllServerTypes')"/>
											<c:set var="allTypesKey" value="resource.hub.filter.AllServerTypes"/>
											<c:set var="section" value="server"/>
  										</c:when>
  										<c:when test="${ff == SERVICE}">
										<s:set var="allTypesKey" value="getText('resource.hub.filter.AllServiceTypes')"/>
											<c:set var="allTypesKey" value="resource.hub.filter.AllServiceTypes"/>
											<c:set var="section" value="service"/>
  										</c:when>
  										<c:when test="${ff == APPLICATION}">
    										<c:set var="section" value="application"/>
  										</c:when>
  										<c:when test="${ff == GROUP}">
											<s:set var="allTypesKey" value="getText('resource.hub.filter.AllGroupTypes')"/>
    										<c:set var="allTypesKey" value="resource.hub.filter.AllGroupTypes"/>
    										<c:set var="section" value="group"/>
  										</c:when>
									</c:choose>
									<!-- TOOLS -->
									<c:if test="${not empty linkUrl}">
									    <div class="toolsMenu" >
									        <span class="LinkBox" onclick="toggleMenu('toolMenu');" id="toolMenuSpan"  >
									        	<fmt:message key="resource.toolsmenu.text"/>
									        	<img src='<s:url value="/images/arrow_dropdown.gif" />' border="0" alt="" id="toolMenuArrow">
									        </span>
									        <div style="clear: both"></div>
									        <div id="toolMenu" style="display: none; position: absolute; margin-top: 2px; margin-left: -2px;z-index:5">
									            <tiles:insertAttribute name="linkUrl">
									                <c:if test="${not empty resource}">
									                    <tiles:putAttribute name="resource" value="${sessionScope.resource}"/>
									                </c:if>
									            </tiles:insertAttribute>
									        </div>
									    </div>
									</c:if>
									<!-- END TOOLS -->
									<!-- FILTERBOXZ CONTENTS -->
									<div class="filterBox">
									    <div class="filterBoxTitle">
									        <fmt:message key="resource.hub.Search"/>
									    </div>
									    <div class="filterBoxFields">
            								<s:textfield theme="simple" name="keywords" size="15" maxlength="40" onfocus="this.value='';" value="%{#attr.initSearchVal}"/>
											<c:choose>
								                <c:when test="${empty allTypesKey}">
								                    <s:hidden theme="simple" name="ft" value=""/>&nbsp;
								                </c:when>
								                <c:otherwise>
								                    <span style="padding-left: 4px;">
  													  <s:select theme="simple"  name="ft"   list="types" cssClass="FilterFormText" headerKey="" headerValue="%{#allTypesKey}"/>
								                    </span>
								                    <c:if test="${not empty AvailableResGrps}">
								                        <span style="padding-left: 4px;">
														<s:select theme="simple"  name="fg"  value="%{#attr.resource.hub.filter.AllGroupOption}" list="%{#attr.AvailableResGrps}" cssClass="FilterFormText" size="1" headerValue="%{getText('resource.hub.filter.AllGroupOption')}" headerKey=""/>
								                            
								                        </span>
								                    </c:if>
								                </c:otherwise>
								            </c:choose>
          									<c:if test="${ResourceHubForm.ff != GROUP}">
            									<s:checkbox theme="simple" class="unavail" name="unavail" value="%{#attr.unavail}"/>
            									<label for="unavail"><fmt:message key="resource.hub.legend.unavailable"/></label>
          									</c:if>
            								<s:checkbox theme="simple" class="own" name="own" value="%{#attr.own}"/>
            								<label for="own">
								                <fmt:message key="resource.hub.search.label.Owned">
								                    <fmt:param>
								                    </fmt:param>
								                </fmt:message>
								                <!-- fmt:message caches previous value some times, so take it out of tag -->
								                <c:out value="${sessionScope.webUser.firstName}"/>
								            </label>
								            <span><fmt:message key="resource.hub.search.label.Match"/></span>
											<s:radio theme="simple" list="#{'true':getText('any') + '&nbsp;', 'false':getText('all')}" name="any" value="%{#attr.any}"></s:radio>
											
											
								            <input type="image" src='<s:url value="/images/4.0/icons/accept.png"/>' property="ok" style="padding-left: 6px; vertical-align: text-bottom;"/>
    									</div>

    									</div>
									</div>
									<!-- END SEARCHBOX  -->
    							</td>
    						</c:when>
    						<c:otherwise>
    							<td class="PageTitleSmallText" colspan="2">&nbsp;</td>
							</c:otherwise>
						</c:choose>
    					<td>&nbsp;</td>
  					</tr>
				</c:if>
			</table>
	</td>
 </tr>
</table>
