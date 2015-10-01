<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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


<tiles:importAttribute name="tpClass"/>
<tiles:importAttribute name="ntClass"/>
<tiles:importAttribute name="showDuration" ignore="true"/>

    <tr>
      <td class="BlockLabel">
        <img  src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/><b><fmt:message
        key="alert.config.props.CB.Enable"/></b>
      </td>
      <td class="BlockContent">
		<s:radio  theme="simple" name="whenEnabled" list="#{#attr.enableEachTime:''}" value="%{#attr.defForm.whenEnabled}" onchange="javascript:checkEnable();"/>
        <fmt:message key="alert.config.props.CB.Content.EachTime"/>
      </td>
    </tr>

    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${ntClass}'/>">
	  <s:radio  theme="simple" name="whenEnabled" list="#{#attr.enableNumTimesInPeriod:''}" value="%{#attr.defForm.whenEnabled}" onchange="javascript:checkEnable();"/>
      <fmt:message key="alert.config.props.CB.Content.NT1"/>
        &nbsp;<s:textfield theme="simple" name="numTimesNT" value="%{#attr.defForm.numTimesNT}" size="2"
        maxlength="3" onchange="javascript:checkEnableNT();"/>
        &nbsp;
        <fmt:message key="alert.config.props.CB.Content.NT2"/>
        &nbsp;<s:textfield  theme="simple" name="howLongNT" value="%{#attr.defForm.howLongNT}" size="2"
        maxlength="3" onchange="javascript:checkEnableNT();"/>&nbsp;
        <tiles:insertDefinition name=".events.config.conditions.enablement.timeunits">
          <tiles:putAttribute name="property" value="howLongUnitsNT"/>
          <tiles:putAttribute name="enableFunc" value="checkEnableNT"/>
        </tiles:insertDefinition>
        <c:if test="${numTimesNTErrs or howLongNTErrs}">
        <br>-- <span class="ErrorFieldContent">
        <s:fielderror fieldName="numTimesNT"/>
        <s:fielderror fieldName="howLongNT"/>
        </span>
        </c:if>
      </td>
    </tr>

    <c:if test="${showDuration}">
    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${tpClass}'/>">
	  <s:radio  theme="simple" name="whenEnabled" list="#{#attr.enableTimePeriod:''}" value="%{#attr.defForm.whenEnabled}" onchange="javascript:checkEnable();"/>
	  <fmt:message
        key="alert.config.props.CB.Content.TP1"/>&nbsp;<s:textfield theme="simple"
        name="meetTimeTP" value="%{#attr.defForm.meetTimeTP}" size="2"
        onchange="javascript:checkEnableTP();"/>&nbsp;
        <tiles:insertDefinition name=".events.config.conditions.enablement.timeunits">
          <tiles:putAttribute name="property" value="meetTimeUnitsTP"/>
          <tiles:putAttribute name="enableFunc" value="checkEnableTP"/>
        </tiles:insertDefinition>
        <fmt:message
        key="alert.config.props.CB.Content.TP2"/>&nbsp;<s:textfield theme="simple"
        name="howLongTP" value="%{#attr.defForm.howLongTP}" size="2" maxlength="3"
        onchange="javascript:checkEnableTP();"/>&nbsp;
        <tiles:insertDefinition name=".events.config.conditions.enablement.timeunits">
          <tiles:putAttribute name="property" value="howLongUnitsTP"/>
          <tiles:putAttribute name="enableFunc" value="checkEnableTP"/>
        </tiles:insertDefinition>
        <c:if test="${meetTimeTPErrs or howLongTPErrs}">
        <br>-- <span class="ErrorFieldContent">
        <s:fielderror fieldName="meetTimeTP"/>
        <s:fielderror fieldName="howLongTP"/>
        </span>
        </c:if>
      </td>
    </tr>
    </c:if>

