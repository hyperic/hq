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


<tiles:importAttribute name="tpClass"/>
<tiles:importAttribute name="ntClass"/>
<tiles:importAttribute name="showDuration" ignore="true"/>

    <tr>
      <td class="BlockLabel">
        <html:img page="/images/icon_required.gif" width="9"
        height="9" border="0"/><b><fmt:message
        key="alert.config.props.CB.Enable"/></b>
      </td>
      <td class="BlockContent">
        <html:radio property="whenEnabled"
        value="${enableEachTime}" onchange="javascript:checkEnable();"/>
        <fmt:message key="alert.config.props.CB.Content.EachTime"/>
      </td>
    </tr>

    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${ntClass}'/>">
      <html:radio property="whenEnabled" onchange="javascript:checkEnable();"
        value="${enableNumTimesInPeriod}"/>
        <fmt:message key="alert.config.props.CB.Content.NT1"/>
        &nbsp;<html:text property="numTimesNT" size="2"
        maxlength="3" onchange="javascript:checkEnableNT();"/>
        &nbsp;
        <fmt:message key="alert.config.props.CB.Content.NT2"/>
        &nbsp;<html:text property="howLongNT" size="2"
        maxlength="3" onchange="javascript:checkEnableNT();"/>&nbsp;
        <tiles:insert definition=".events.config.conditions.enablement.timeunits">
          <tiles:put name="property" value="howLongUnitsNT"/>
          <tiles:put name="enableFunc" value="checkEnableNT"/>
        </tiles:insert>
        <c:if test="${numTimesNTErrs or howLongNTErrs}">
        <br>-- <span class="ErrorFieldContent">
        <html:errors property="numTimesNT"/>
        <html:errors property="howLongNT"/>
        </span>
        </c:if>
      </td>
    </tr>

    <c:if test="${showDuration}">
    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${tpClass}'/>"><html:radio
        property="whenEnabled" value="${enableTimePeriod}"
        onchange="javascript:checkEnable();"/><fmt:message
        key="alert.config.props.CB.Content.TP1"/>&nbsp;<html:text
        property="meetTimeTP" size="2"
        onchange="javascript:checkEnableTP();"/>&nbsp;
        <tiles:insert definition=".events.config.conditions.enablement.timeunits">
          <tiles:put name="property" value="meetTimeUnitsTP"/>
          <tiles:put name="enableFunc" value="checkEnableTP"/>
        </tiles:insert>
        <fmt:message
        key="alert.config.props.CB.Content.TP2"/>&nbsp;<html:text
        property="howLongTP"size="2" maxlength="3"
        onchange="javascript:checkEnableTP();"/>&nbsp;
        <tiles:insert definition=".events.config.conditions.enablement.timeunits">
          <tiles:put name="property" value="howLongUnitsTP"/>
          <tiles:put name="enableFunc" value="checkEnableTP"/>
        </tiles:insert>
        <c:if test="${meetTimeTPErrs or howLongTPErrs}">
        <br>-- <span class="ErrorFieldContent">
        <html:errors property="meetTimeTP"/>
        <html:errors property="howLongTP"/>
        </span>
        </c:if>
      </td>
    </tr>
    </c:if>

