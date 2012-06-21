<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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

<!-- Content Block Title: Properties -->
<tiles:insert definition=".header.tab">
	<tiles:put name="tabKey" value="alert.current.detail.props.Title" />
</tiles:insert>

<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0"	class="TableBottomLine">
	<tr valign="top">
		<td width="20%" class="BlockLabel">
			<fmt:message key="common.label.Name" />
		</td>
		<td width="30%" class="BlockContent">
			<c:choose>
				<c:when test="${not empty Resource and not alertDef.deleted}">
					<html:link action="/alerts/Config" titleKey="alert.config.props.PB.ViewDef">
						<html:param name="mode" value="viewDefinition"/>
						<html:param name="eid" value="${Resource.entityId.appdefKey}"/>
						<html:param name="ad" value="${alertDef.id}"/>
                        <c:out value="${alertDef.name}" escapeXml="true"/>
					</html:link>
				</c:when>
				<c:otherwise>
					<c:out value="${alertDef.name}" />
				</c:otherwise>
			</c:choose>
		</td>
		<td width="20%" class="BlockLabel">
			<fmt:message key="alert.config.props.PB.Priority" />
		</td>
		<td width="30%" class="BlockContent">
			<fmt:message key="${'alert.config.props.PB.Priority.'}${alertDef.priority}" />
		</td>
	</tr>
	<tr valign="top">
		<td class="BlockLabel">
			<c:if test="${not empty Resource}">
				<fmt:message key="common.label.Resource" />
			</c:if>
		</td>
		<td class="BlockContent">
			<c:if test="${not empty Resource}">
				<html:link action="/Resource" 
				           paramId="eid" 
				           paramName="Resource"
						   paramProperty="entityId">
                    <c:out value="${Resource.name}" escapeXml="true"/>
				</html:link>
			</c:if>
		</td>
		<td class="BlockLabel">
			<fmt:message key="alert.current.detail.props.AlertDate" />
		</td>
		<td class="BlockContent">
			<hq:dateFormatter time="false" value="${alert.ctime}" />
		</td>
	</tr>
	<tr valign="top">
		<td width="20%" class="BlockLabel">
			<c:if test="${not empty alertDef.description}">
				<fmt:message key="common.label.Description" />
			</c:if>
		</td>
		<td width="30%" class="BlockContent">
			<c:out value="${alertDef.description}" />
		</td>
		<td width="20%" class="BlockLabel">
			<fmt:message key="alert.config.props.PB.AlertStatus" />
		</td>
		<td width="30%" class="BlockContent">
			<!-- For now, the alert is fixed, or not fixed -->
			<c:choose>
				<c:when test="${alert.fixed}">
					<html:img page="/images/icon_fixed.gif" 
					          width="12" 
					          height="12"
						      border="0" />
					<fmt:message key="resource.common.alert.action.fixed.label" />
				</c:when>
				<c:otherwise>
					<html:img page="/images/icon_available_red.gif" 
					          width="12"
						      height="12" 
						      border="0" />
					<fmt:message key="resource.common.alert.action.notfixed.label" />
				</c:otherwise>
			</c:choose>
		</td>
	</tr>
</table>
