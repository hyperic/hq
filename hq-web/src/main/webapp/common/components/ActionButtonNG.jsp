<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
 
<tiles:importAttribute name="labelKey" />
<tiles:importAttribute name="buttonHref" ignore="true" /> <%-- This attribute has been deprecated --%>
<tiles:importAttribute name="buttonClick" />
<tiles:importAttribute name="icon" ignore="true" />
<tiles:importAttribute name="disabled" ignore="true" />
<tiles:importAttribute name="hidden" ignore="true" />

<div style="white-space:nowrap; text-align: left;">
	<c:if test="${not hidden}">
		<c:choose>
			<c:when test="${disabled}">
				<span class="InactiveText"><fmt:message key="${labelKey}" /></span>
			</c:when>
			<c:otherwise>
				<input type="button" id="button" class="button42"  
					   value="<fmt:message key="${labelKey}"/>" 
					   onclick="<c:out value="${buttonClick}" escapeXml="false"/>" />
				
				<c:if test="${not empty buttonHref}">
					<!-- the buttonHref attribute has been deprecated, use buttonClick attribute instead -->
				</c:if>
			</c:otherwise>
		</c:choose> 
		<c:if test="${not empty icon}">
			<c:if test="${disabled}">
				<span style="filter: alpha(opacity = 50); opacity: 0.5;">
			</c:if>
			<span style="padding-left: 3px;">
				<c:out value="${icon}" escapeXml="false" />
			</span>
			<c:if test="${disabled}">
				</span>
			</c:if>
		</c:if>
	</c:if>
</div>