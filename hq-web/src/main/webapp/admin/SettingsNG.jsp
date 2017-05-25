<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
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

<tiles:insertDefinition name=".header.tab">
	<tiles:putAttribute name="tabKey" value="admin.home.CompanyWideSettingsTab" />
	<tiles:putAttribute name="icon"><img src="/images/application_edit.gif" alt=""/></tiles:putAttribute>
</tiles:insertDefinition>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine" style="margin-bottom: 24px;">
<c:if test="${useroperations['administerCAM']}">
	<tr>
		<td width="20%" class="BlockLabel"><fmt:message key="admin.home.Settings"/></td>
		<td class="BlockContent">
			<s:a action="editSettings" >
				<fmt:message key="admin.home.ServerConfig"/>
			</s:a>
			<span class="italicInfo"> <fmt:message key="inform.config.admin.HQServerConfig"/></span>
		</td>
	</tr>
</c:if>
	<tr>
		<td  width="20%" class="BlockLabel"></td>
		<td class="BlockContent">
			<s:a action="monitorConfig" >
				<fmt:message key="admin.home.ResourceTemplates"/>
			</s:a>
			<tiles:importAttribute name="defaultMsg" ignore="true"/>
			<span class="italicInfo">
			<c:choose>
				<c:when test="${not empty defaultMsg}">
					<fmt:message key="${defaultMsg}"/>
				</c:when>
				<c:otherwise>
					<fmt:message key="inform.config.admin.MonitoringDefaults"/>
				</c:otherwise>
			</c:choose>
			</span>
		</td>
	</tr>
	<tr>
		<td class="BlockLabel"></td>
		<td class="BlockContent">
			<s:a action="escalateConfig">
				<fmt:message key="admin.home.EscalationSchemes"/>
			</s:a> <span class="italicInfo"><fmt:message key="inform.config.admin.EscalationScheme"/></span></td>
	</tr>
<c:if test="${useroperations['administerCAM']}">
	<tr>
		<td class="BlockLabel"></td>
		<td class="BlockContent"><a href='<spring:url value="PluginManager.action" />'><fmt:message key="admin.managers.plugin.title"/></a> <span class="italicInfo"><fmt:message key="admin.managers.plugin.description"/></span></td>
	</tr>
        <tr>
		<td class="BlockLabel"></td>
		<td class="BlockContent"><a href='<spring:url value="MigrationManager.action" />'><fmt:message key="admin.managers.plugin.title"/></a> <span class="italicInfo"><fmt:message key="admin.managers.plugin.description"/></span></td>
	</tr>
        <tr>
                <td class="BlockLabel"></td>
                <td class="BlockContent"><a href='<spring:url value="AgentReportManager.action" />'><fmt:message key="admin.managers.plugin.title"/></a> <span class="italicInfo"><fmt:message key="admin.managers.plugin.description"/></span></td>
        </tr> 
</c:if>
</table>
<!--  /  -->
