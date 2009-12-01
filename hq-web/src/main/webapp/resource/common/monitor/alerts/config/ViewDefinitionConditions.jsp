<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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


<tiles:importAttribute name="showValues" ignore="true"/>

<!-- Content Block Title: Condition -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.CondBox"/>
</tiles:insert>

<!-- Condition Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine ">
  <c:forEach var="cond" items="${alertDefConditions}">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <c:if test="${! cond.first}">
      <c:choose>
      <c:when test="${cond.required}">
      <fmt:message key="alert.config.props.CB.And"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="alert.config.props.CB.Or"/>
      </c:otherwise>
      </c:choose>
      </c:if>
      <fmt:message key="alert.config.props.CB.IfCondition"/>
    </td>
    <td width="80%" class="BlockContent">
      <c:out value="${cond.conditionText}"/>
    </td>
  </tr>
  <c:if test="${showValues}">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <fmt:message key="alert.config.props.CB.ActualValue"/>
    </td>
    <td width="80%" class="BlockContent">
      <c:choose>
      <c:when test="${not empty cond.actualValue}">
      <c:out value="${cond.actualValue}"/>
      </c:when>
      <c:otherwise>
      <fmt:message key="alert.config.props.CB.NoActualValue"/>
      </c:otherwise>
      </c:choose>
    </td>
  </tr>
  </c:if>
  </c:forEach>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
<c:if test="${not empty primaryAlert}">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <fmt:message key="alert.config.props.CB.Recovery"/>
    </td>
    <td width="80%" class="BlockContent">
      <fmt:message key="alert.config.props.CB.RecoveryFor"/>
	  <c:out value="${primaryAlert.name}"/>
     </td>
  </tr>
</c:if>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.CB.Enable"/></td>
    <td width="80%" class="BlockContent">
      <c:set var="howLongUnits"><fmt:message key="${'alert.config.props.CB.Enable.TimeUnit.'}${enableActionsHowLongUnits}"/></c:set>
      <c:set var="howManyUnits"><fmt:message key="${'alert.config.props.CB.Enable.TimeUnit.'}${enableActionsHowManyUnits}"/></c:set>
      <fmt:message key="${enableActionsResource}">
        <fmt:param value="${enableActionsHowLong}"/>
        <fmt:param value="${howLongUnits}"/>
        <fmt:param value="${enableActionsHowMany}"/>
        <fmt:param value="${howManyUnits}"/>
      </fmt:message>
    </td>
  </tr>
<c:if test="${alertDef.willRecover}">
  <tr valign="top">
    <td class="BlockLabel">&nbsp;</td>
    <td class="BlockContent">
        <fmt:message key="alert.config.props.CB.Content.UntilReenabled"/>
    </td>
  </tr>
</c:if>
</table>
