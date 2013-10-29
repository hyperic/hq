<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
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
	    <c:when test="${not empty ResourceHubForm.keywords}">
    		<c:set var="initSearchVal" value="${ResourceHubForm.keywords}"/>
    	</c:when>
    	<c:otherwise>
      		<fmt:message var="initSearchVal" key="resource.hub.search.KeywordSearchText"/>
    	</c:otherwise>
  	</c:choose>
</c:if>
<jsu:script> 
	var help = "<hq:help/>";
</jsu:script>
<c:if test="${not empty eid}">
	<div id="breadcrumbContainer">
		<hq:breadcrumb resourceId="${eid}" 
					   ctype="${ctype}"
		               baseBrowseUrl="/ResourceHub.do" 
		               baseResourceUrl="/Resource.do" />
	</div>
</c:if>

<table width="100%" cellspacing="0" cellpadding="0" style="border: 0px;clear:both;">
	<tr>
    	<td colspan="4">
			<table width="100%" border="0" cellspacing="0" cellpadding="0" style="border: 0px; margin-bottom: 10px;">
				<c:if test="${not empty titleName or not empty titleKey}">
					<tr class="PageTitleBar">
						<td colspan="100%" style="padding: 0pt 25px 10px;">
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
    							<td colspan="2" style="padding: 5px 25px 0pt;">
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
      												<html:link action="/resource/${resource.entityId.typeName}/Inventory">
      													<html:param name="mode" value="changeOwner"/>
      													<html:param name="rid" value="${resource.id}"/>
      													<html:param name="type" value="${resource.entityId.type}"/>
      												  	<fmt:message key="resource.common.inventory.props.ChangeButton"/>
      												</html:link>
      												<br>
      											</c:if>
          									</td>
        								</tr>
										<logic:present name="cprops">
											<c:set var="leftRight" value="1"/>
											<logic:iterate id="cprop" name="cprops">
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
											</logic:iterate>
											<c:if test="${leftRight < 0}">
    											<td colspan="3">&nbsp;</td>
  											</tr>
											</c:if>
										</logic:present>
      									<c:if test="${not empty pluginLinkInfo}">
											<tr>
												<td colspan="100%" style="padding-top: 10px;">
													<a href="<html:rewrite action="/mastheadAttach"><html:param name="typeId" value="${pluginLinkInfo.pluginId}"/><html:param name="sn" value="${pluginLinkInfo.selectedId}"/></html:rewrite>">View in HQ vSphere</a>
												</td>
											</tr>
										</c:if>					
										<c:if test="${empty ResourceType}">
        									<tr>
        										<td colspan="3" style="padding-top: 10px;">
        											<tiles:insert definition=".resource.common.navmap">
          												<tiles:put name="resource" beanName="resource"/>
        											</tiles:insert>
        											
        											<!-- TOOLS -->
													<c:if test="${not empty linkUrl}">
                                                        <c:if test="${(resource.entityId.type == GROUP) && (resource.groupType == GROUP_DYNAMIC)}">
                                                            <c:set var="dontShowTools" value="none"/>
                                                        </c:if>
                                                        <div class="toolsMenuStacked LinkBox" style="display:${dontShowTools};">
	        												<span onclick="toggleMenu('toolMenu');" id="toolMenuSpan">
	        													<fmt:message key="resource.toolsmenu.text"/>
	        													<img src="<html:rewrite page="/images/arrow_dropdown.gif" />" border="0" alt="" id="toolMenuArrow">
	        												</span>
															<div style="clear: both"></div>
	        												<div id="toolMenu" style="display: none; position: absolute; margin-top: 2px; margin-left: -7px; z-index:5">
	            												<tiles:insert attribute="linkUrl">
	                												<c:if test="${not empty resource}">
	                    												<tiles:put name="resource" beanName="resource"/>
	                												</c:if>
	            												</tiles:insert>
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
    							<td style="vertical-align: middle; padding: 0 25px 10px;">
									<c:choose>
  										<c:when test="${ResourceHubForm.ff == PLATFORM}">
    										<c:set var="allTypesKey" value="resource.hub.filter.AllPlatformTypes"/>
    										<c:set var="section" value="platform"/>
  										</c:when>
  										<c:when test="${ResourceHubForm.ff == SERVER}">
											<c:set var="allTypesKey" value="resource.hub.filter.AllServerTypes"/>
											<c:set var="section" value="server"/>
  										</c:when>
  										<c:when test="${ResourceHubForm.ff == SERVICE}">
											<c:set var="allTypesKey" value="resource.hub.filter.AllServiceTypes"/>
											<c:set var="section" value="service"/>
  										</c:when>
  										<c:when test="${ResourceHubForm.ff == APPLICATION}">
    										<c:set var="section" value="application"/>
  										</c:when>
  										<c:when test="${ResourceHubForm.ff == GROUP}">
    										<c:set var="allTypesKey" value="resource.hub.filter.AllGroupTypes"/>
    										<c:set var="section" value="group"/>
  										</c:when>
									</c:choose>
									<!-- TOOLS -->
									<c:if test="${not empty linkUrl}">
									    <div class="toolsMenu">
									        <span class="LinkBox" onclick="toggleMenu('toolMenu');" id="toolMenuSpan">
									        	<fmt:message key="resource.toolsmenu.text"/>
									        	<img src="<html:rewrite page="/images/arrow_dropdown.gif" />" border="0" alt="" id="toolMenuArrow">
									        </span>
									        <div style="clear: both"></div>
									        <div id="toolMenu" style="display: none; position: absolute; margin-top: 2px; margin-left: -2px;z-index:5">
									            <tiles:insert attribute="linkUrl">
									                <c:if test="${not empty resource}">
									                    <tiles:put name="resource" beanName="resource"/>
									                </c:if>
									            </tiles:insert>
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
            								<html:text property="keywords" size="15" maxlength="40" onfocus="this.value='';" value="${initSearchVal}"/>
								            <c:choose>
								                <c:when test="${empty allTypesKey}">
								                    <html:hidden property="ft" value=""/>&nbsp;
								                </c:when>
								                <c:otherwise>
								                    <span style="padding-left: 4px;">
								                        <html:select property="ft" styleClass="FilterFormText" size="1">
								                            <html:option value="" key="${allTypesKey}"/>
								                            <html:optionsCollection property="types"/>
								                        </html:select>
								                    </span>
								                    <c:if test="${not empty AvailableResGrps}">
								                        <span style="padding-left: 4px;">
								                            <html:select property="fg" styleClass="FilterFormText">
								                                <html:option value="" key="resource.hub.filter.AllGroupOption"/>
								                                <html:optionsCollection name="AvailableResGrps"/>
								                            </html:select>
								                        </span>
								                    </c:if>
								                </c:otherwise>
								            </c:choose>
          									<c:if test="${ResourceHubForm.ff != GROUP}">
            									<html:checkbox styleId="unavail" property="unavail" value="true"/>
            									<label for="unavail"><fmt:message key="resource.hub.legend.unavailable"/></label>
          									</c:if>
            								<html:checkbox styleId="own" property="own" value="true"/>
            								<label for="own">
								                <fmt:message key="resource.hub.search.label.Owned">
								                    <fmt:param>
								                    </fmt:param>
								                </fmt:message>
								                <!-- fmt:message caches previous value some times, so take it out of tag -->
								                <c:out value="${sessionScope.webUser.firstName}"/>
								            </label>
								            <span><fmt:message key="resource.hub.search.label.Match"/></span>
								            <html:radio styleId="anyRadio" property="any" value="true"/>
								            <label for="anyRadio"><fmt:message key="any"/></label>
								            <html:radio styleId="allRadio" property="any" value="false"/>
								            <label for="allRadio"><fmt:message key="all"/></label>&nbsp;
								            <html:image page="/images/4.0/icons/accept.png" property="ok" style="padding-left: 6px; vertical-align: text-bottom;"/>
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
  	<tr>
    	<td style="padding-left:25px;">
