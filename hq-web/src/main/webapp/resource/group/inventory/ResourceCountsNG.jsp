<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

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


<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_PSS" var="CONST_ADHOC_PSS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_GRP" var="CONST_ADHOC_GRP" />

<tiles:importAttribute name="resourceCount"/>
<tiles:importAttribute name="resourceTypeMap" ignore="true"/>

<!--  /  -->

<!--  RESOURCE COUNTS CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.group.inventory.TotalLabel"/></td>
		<td width="30%" class="BlockContent"><c:out value="${resourceCount}"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
  <tr valign="top">
		<td width="20%" class="BlockLabel">&nbsp;
              <c:if test="${Resource.groupType == CONST_ADHOC_PSS ||
                            Resource.groupType == CONST_ADHOC_GRP}"> 
                  <fmt:message key="resource.group.inventory.ResourcesByTypeLabel"/>
              </c:if>
        </td>
    <td width="30%" class="BlockContentNoPadding" colspan="3">
      <table width="66%" cellpadding="0" cellspacing="0" border="0" class="BlockContent">
        <tr valign="top">
<c:forEach var="entry" varStatus="status" items="${resourceTypeMap}">
          <td width="50%"><c:out value="${entry.key}"/> (<c:out value="${entry.value}"/>)</td>
  <c:choose>
    <c:when test="${status.count % 2 == 0}">
        </tr>
        <tr>
    </c:when>
    <c:otherwise>
      <c:if test="${status.last}">
        <c:forEach begin="${(status.count % 2) + 1}" end="2">
          <td width="50%">&nbsp;</td>
        </c:forEach>
      </c:if>
    </c:otherwise>
  </c:choose>
</c:forEach>
			    </tr>
			</table> 
        </td>
	</tr>
	<tr>
      <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->
