<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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


<tiles:insertDefinition name=".events.config.view.conditions">
  	<tiles:putAttribute name="showValues" value="false"/>
</tiles:insertDefinition>


<c:if test="${not canEditConditions}">
	<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ErrorField">
  		<tr>
    		<td><fmt:message key="alert.config.props.CB.CanNotEditConditions"/></td>
  		</tr>
	</table>
</c:if>

<c:if test="${canModify && canEditConditions && not alertDef.deleted}">
	<c:if test="${not empty Resource || not empty ResourceType}">
		<c:choose>
  			<c:when test="${not empty Resource}">
    			<c:url var="editUrl" value="editConditionsAlertsConfigPortal.action">
    				<c:param name="mode" value="editConditions" />
			    	<c:param name="eid" value="${Resource.entityId.appdefKey}" />
			    	<c:param name="ad" value="${alertDef.id}" />
			    </c:url>
			</c:when>
			<c:otherwise>
			  	<c:url var="editUrl" value="editConditionsAlertsConfigPortal.action">
			    	<c:param name="mode" value="editConditions" />
			    	<c:param name="aetid" value="${ResourceType.appdefTypeKey}" />
			    	<c:param name="ad" value="${alertDef.id}" />
			    </c:url>
			</c:otherwise>
		</c:choose>
		<tiles:insertDefinition name=".toolbar.edit">
			<tiles:putAttribute name="editUrl" value="${editUrl}" />
		</tiles:insertDefinition>
	</c:if>
</c:if>
<br/>