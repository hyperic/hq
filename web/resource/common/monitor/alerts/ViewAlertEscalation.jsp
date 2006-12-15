<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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

<table cellpadding="0" border="0" width="100%" class="BlockContent">
  <tr>
    <td rowspan="2" width="20%">&nbsp;</td>
    <td nowrap="true">
      <input type=checkbox name="pause" value="true"/>
      <fmt:message key="alert.escalation.pause"/>
      <select name="pauseTime">
        <option value="300000">5</option>
        <option value="600000">10</option>
        <option value="900000">15</option>
        <option value="1200000">20</option>
        <option value="1800000">30</option>
        <option value="3600000">60</option>
      </select>
      <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>
    </td>
    <td rowspan="2" width="60%">&nbsp;</td>
  </tr>
  <tr>
    <td style="padding: 10px;" align="middle">
<tiles:insert page="/common/components/ActionButton.jsp">
  <tiles:put name="labelKey" value="resource.common.alert.action.acknowledge.label"/>
  <tiles:put name="buttonHref" value="javascript:document.forms[0].submit();"/>
  <tiles:put name="buttonClick">$('mode').setAttribute('value', '<fmt:message key="resource.common.alert.action.acknowledge.label"/>')</tiles:put>
  <tiles:put name="disabled" beanName="alert" beanProperty="fixed"/>
</tiles:insert>
    </td>
  </tr>
</table>
<br/>

