<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
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


<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="savedQueriesList"/>
<c:url var="selfAction" value="/dashboard/Admin.action">
	<c:param name="mode" value="savedQueries"/>
</c:url>
<jsu:importScript path="/js/dashboard_SummaryCounts.js" />
<jsu:script>
  	var help = '<hq:help/>';
</jsu:script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0"/></td>
    <td><img src='<s:url value="/images/spacer.gif"/>' width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PortletTitle" nowrap><fmt:message key="dash.home.SummaryCounts.Title"/></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif"/>' width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><s:a href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><img src='<s:url value="/images/title_pagehelp.gif"/>' width="20" height="20" alt="" border="0" hspace="10"/></s:a></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"></td>
    <td colspan='2'><img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='2'>
    <s:form action="executeSummaryCountsModifyPortlet.action">
<div id="narrowlist_false">
    <!-- Content Block Title: Display Settings -->
    <tiles:insertDefinition name=".header.tab">
      <tiles:putAttribute name="tabKey" value="dash.settings.DisplaySettings"/>
	  <tiles:putAttribute name="portletName" value=""/>
    </tiles:insertDefinition>
</div>
    <tiles:insertDefinition name=".ng.dashContent.admin.generalSettings">
      <tiles:putAttribute name="portletName" value="${portletName}" />
    </tiles:insertDefinition>
    <!-- Display Settings Content: the text is static, all boxes should be checked by default -->
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
      </tr>
       <tr valign="top">
        <td width="20%" class="BlockLabel"><fmt:message key="dash.settings.FormLabel.SummaryCounts"/></td>
        <td width="30%" class="BlockContent">                
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
			
              <td colspan="2" width="100%" class="FormLabel"><s:checkbox theme="simple"  value="%{#attr.filter.application}" name="application" class="applicationParent" styleClass="applicationParent" onclick="checkParent(this)" disabled="%{!#attr.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.AppShowTotal"/></td>
            </tr>
            <c:if test="${summary.appTypeMap != null}">
            <tr>
              <td><img  src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
              <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="applicationCheckAll" onclick="ToggleAll(this, 'application')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.AppCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="applicationCheckAll" onclick="ToggleAll(this, 'application')" /><b><fmt:message key="dash.home.DisplayCategory.AppCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <td><img src='<s:url  value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
              <td width="100%">
              <c:forEach var="type" items="${summary.appTypeMap}">
                <s:checkbox theme="simple"  name="applicationTypes" fieldValue="%{#attr.type.key}" value="%{#attr.type.key}" class="application"  styleClass="application" onclick="checkChild(this)" disabled="%{!#attr.modifyDashboard}"/><c:out value="${type.key}"/><br>
                <input type="checkbox" name="applicationTypes" onclick="checkChild(this)" value="<c:out value="${type.key}"/>" class="application" 
							 
						<c:forEach var="markedAppTypes" items="${filter.applicationTypes}">
							<c:if test="${markedAppTypes!='null' }">	
								<c:if test="${type.key == markedAppTypes }">
									<c:out value="checked='checked'"/>
								</c:if> 
							</c:if> 
						</c:forEach>
						>
						<c:out value="${type.key}"/></input><br/>
				</c:forEach>             
                &nbsp;
              </td>
            </tr>
            </c:if>
          </table>
          
          
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><s:checkbox theme="simple"  value="%{#attr.filter.platform}" name="platform" class="platformParent" styleClass="platformParent" onclick="checkParent(this)" disabled="%{!#attr.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.PlatformShowTotal"/></td>
            </tr>
            <c:if test="${summary.platformTypeMap != null}">
            <tr>
              <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
              <td width="100%">
              <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="platformCheckAll" onclick="ToggleAll(this, 'platform')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.PlatformCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="platformCheckAll" onclick="ToggleAll(this, 'platform')"/><b><fmt:message key="dash.home.DisplayCategory.PlatformCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <td><img  src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
              <td width="100%">
              <c:forEach var="type" items="${summary.platformTypeMap}">
                <input type="checkbox" name="platformTypes" onclick="checkChild(this)" value="<c:out value="${type.key}"/>" class="platform" 
							 
								<c:forEach var="markedPlatformTypes" items="${filter.platformTypes}">
									<c:if test="${markedPlatformTypes!='null' }">	
										<c:if test="${type.key == markedPlatformTypes }">
											<c:out value="checked='checked'"/>
										</c:if> 
									</c:if> 
								</c:forEach>
								>
								<c:out value="${type.key}"/></input><br/>
			  </c:forEach> 
 							
                &nbsp;
              </td>
            </tr>
            </c:if>
          </table>
          
          
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><s:checkbox theme="simple"  value="%{#attr.filter.server}" name="server" class="serverParent" styleClass="serverParent" onclick="checkParent(this)" disabled="%{!#attr.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.ServerShowTotal"/></td>
            </tr>
            <c:if test="${summary.serverTypeMap != null}">
            <tr>
              <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
              <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="serverCheckAll" onclick="ToggleAll(this, 'server')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.ServerCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="serverCheckAll" onclick="ToggleAll(this, 'server')"/><b><fmt:message key="dash.home.DisplayCategory.ServerCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
              <td width="100%">
                <c:forEach var="type" items="${summary.serverTypeMap}">
                <input type="checkbox" name="serverTypes" onclick="checkChild(this)" value="<c:out value="${type.key}"/>" class="server"
										
								<c:forEach var="markedServerTypes" items="${filter.serverTypes}">
									<c:if test="${markedServerTypes!='null' }">	
										<c:if test="${type.key == markedServerTypes }">
											<c:out value="checked='checked'"/>
										</c:if> 
									</c:if>
								</c:forEach>
							
								>
								<c:out value="${type.key}"/>
								</input>
								<br>
				</c:forEach>
                &nbsp;
				
              </td>
            </tr>
            </c:if>
          </table>
                      
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <td colspan="2" width="100%" class="FormLabel"><s:checkbox theme="simple"  value="%{#attr.filter.service}" name="service" class="serviceParent" styleClass="serviceParent" onclick="checkParent(this)" disabled="%{!#attr.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.ServiceShowTotal"/></td>
              </tr>
              <c:if test="${summary.serviceTypeMap!= null}">
              <tr>
                <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
                <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                  <input type="checkbox" class="serviceCheckAll" onclick="ToggleAll(this, 'service')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.ServiceCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                   <input type="checkbox" class="serviceCheckAll" onclick="ToggleAll(this, 'service')"/><b><fmt:message key="dash.home.DisplayCategory.ServiceCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
                </td>
              </tr>
              <tr>
                <td><img  src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
                <td width="100%">
				<c:forEach var="type" items="${summary.serviceTypeMap}">
                <input type="checkbox" name="serviceTypes" onclick="checkChild(this)" value="<c:out value="${type.key}"/>" class="service"
										
								<c:forEach var="markedServiceTypes" items="${filter.serviceTypes}">
									<c:if test="${markedServiceTypes!='null' }">	
										<c:if test="${type.key == markedServiceTypes }">
											<c:out value="checked='checked'"/>
										</c:if> 
									</c:if>
								</c:forEach>
							
								>
								<c:out value="${type.key}"/>
								</input>
								<br>
				</c:forEach>
                  &nbsp;
                </td>
              </tr>
              </c:if>
            </table>
            
                        
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <td colspan="2" width="100%" class="FormLabel">
				<s:checkbox theme="simple"  value="%{#attr.filter.cluster}" name="cluster" class="clusterParent" styleClass="clusterParent" onclick="checkParent(this)" disabled="%{!#attr.modifyDashboard}"/>
				<fmt:message key="dash.home.DisplayCategory.group.ClusterShowTotal"/></td>
              </tr>              
              <tr>
                <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="30" border="0"/></td>                
              </tr>                        
            </table>            
            
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <td colspan="2" width="100%" class="FormLabel">
				<s:checkbox theme="simple"  value="%{#attr.filter.groupMixed}" name="groupMixed" class="groupMixedParent" styleClass="groupMixedParent" onclick="checkParent(this)" disabled="%{!#attr.modifyDashboard}"/>
				<fmt:message key="dash.home.DisplayCategory.group.mixedGroups"/></td>
              </tr>              
              <tr>
                <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>
                <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="groupMixedCheckAll" onclick="ToggleAll(this, 'groupMixed')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.MixedGroupCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="groupMixedCheckAll" onclick="ToggleAll(this, 'groupMixed')"/><b><fmt:message key="dash.home.DisplayCategory.MixedGroupCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
                </td>
              </tr>
              <tr>
                <td><img src='<s:url value="/images/spacer.gif"/>' width="20" height="1" border="0"/></td>                
                <td width="100%">
				<s:checkbox theme="simple"  value="%{#attr.filter.groupGroups}" name="groupGroups" class="groupMixed" styleClass="groupMixed" onclick="checkChild(this)" disabled="%{!#attr.modifyDashboard}"/>
				<fmt:message key="dash.home.DisplayCategory.group.groups"/><br>
				<s:checkbox theme="simple"  value="%{#attr.filter.groupPlatServerService}" name="groupPlatServerService" class="groupMixed" styleClass="groupMixed" onclick="checkChild(this)" disabled="%{!#attr.modifyDashboard}"/>
				<fmt:message key="dash.home.DisplayCategory.group.plat.server.service"/><br>
				<s:checkbox theme="simple"  value="%{#attr.filter.groupApplication}" name="groupApplication" class="groupMixed" styleClass="groupMixed" onclick="checkChild(this)" disabled="%{!#attr.modifyDashboard}"/>
				<fmt:message key="dash.home.DisplayCategory.group.application"/><br>				
                </td>
              </tr>                        
            </table>
            
          </td>    
          <td width="50%" class="BlockLabel">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
      </tr>
    </table>
     <tiles:insertDefinition name=".form.buttons">
		  <c:if test='${sessionScope.modifyDashboard}'>
			<tiles:putAttribute name="cancelAction"  value="cancelSummaryCountsModifyPortlet" />
			<tiles:putAttribute name="resetAction"  value="resetSummaryCountsModifyPortlet" />
		  </c:if>
    </tiles:insertDefinition>
    </s:form>
    </td>
  </tr>
  <tr> 
    <td colspan="3"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
