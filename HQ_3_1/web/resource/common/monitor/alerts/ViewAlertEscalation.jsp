<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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

<c:if test="${not empty escalation}">
<!-- Content Block Title: Notification -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="monitoring.events.MiniTabs.Escalation"/>
</tiles:insert>

<script language="JavaScript" type="text/javascript">
  var isButtonClicked = false;
  
  function checkSubmit() {
    if (isButtonClicked) {
      alert('<fmt:message key="error.PreviousRequestEtc"/>');
      return false;
    }
  }
</script>

<table cellpadding="0" cellspacing="0" border="0" width="100%" class="TableBottomLine" style="margin-bottom: 10px;">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="common.header.EscalationName"/></td>
    <td class="BlockContent" colspan="2"><c:out value="${escalation.name}"/></td>
  </tr>
  <tr>
    <td colspan="3"  class="BlockContent">
      <tiles:insert page="/resource/common/monitor/alerts/config/ViewEscalation.jsp">
        <tiles:put name="chooseScheme" value="false"/>
      </tiles:insert>
    </td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel" style="padding-left: 4px;"><fmt:message key="common.label.EscalationActionLogs"/></td>
    <td class="BlockContent" colspan="2">&nbsp;</td>
  </tr>
  <c:forEach var="log" varStatus="status" items="${alert.escalationLogs}">
  <tr>
    <td width="20%" class="BlockLabel" style="padding-bottom:3px;">
      <span style="color:#333333;"><hq:dateFormatter value="${log.timeStamp}"/> - </span>
    </td>
    <td colspan="3"  class="BlockContent" style="padding-left: 4px;padding-bottom:3px;">
      <c:out value="${log.detail}"/>
    </td>
  </tr>
  </c:forEach>
  <tr>
    <td rowspan="2" width="20%" class="BlockLabel" style="border-top: solid #D5D8DE 1px; border-bottom: solid #D5D8DE 1px;">&nbsp;</td>
    <td nowrap="true" class="BlockContent" style="padding: 10px; border-top: solid #D5D8DE 1px;">
  <c:if test="${escalation.pauseAllowed && alert.acknowledgeable}">
      <input type=checkbox name="pause" value="true"/>
      <fmt:message key="alert.escalation.pause"/>
      <select name="pauseTime">
        <option value="300000">5</option>
        <c:if test="${escalation.maxPauseTime >= 600000}">
        <option value="600000">10</option>
        <c:if test="${escalation.maxPauseTime >= 900000}">
        <option value="900000">15</option>
        <c:if test="${escalation.maxPauseTime >= 1200000}">
        <option value="1200000">20</option>
        <c:if test="${escalation.maxPauseTime >= 1800000}">
        <option value="1800000">30</option>
        <c:if test="${escalation.maxPauseTime >= 3600000}">
        <option value="3600000">60</option>
        </c:if>
        </c:if>
        </c:if>
        </c:if>
        </c:if>
      </select>
      <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>
  </c:if>&nbsp;
    </td>
    <td rowspan="2" width="60%" class="BlockLabel" style="border-top: solid #D5D8DE 1px;">
        <div style="text-align:left;">
<tiles:insert page="/common/components/ActionButton.jsp">
  <tiles:put name="labelKey" value="resource.common.alert.action.acknowledge.label"/>
  <tiles:put name="buttonHref" value="javascript:document.forms[0].submit();"/>
  <tiles:put name="buttonClick">$('mode').setAttribute('value', '<fmt:message key="resource.common.alert.action.acknowledge.label"/>')</tiles:put>
  <c:choose>
    <c:when test="${alert.acknowledgeable}">
      <tiles:put name="disabled" value="false"/>
    </c:when>
    <c:otherwise>
      <tiles:put name="disabled" value="true"/>
    </c:otherwise>
  </c:choose>
  <tiles:put name="icon"><html:img page="/images/icon_ack.gif" alt="Click to Acknowledge" align="middle"/></tiles:put>
</tiles:insert>
            </div>
    </td>
  </tr>
</table>
</c:if>
