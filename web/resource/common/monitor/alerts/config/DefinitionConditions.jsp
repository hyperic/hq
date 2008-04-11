<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
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

 <!--<input type="hidden" name="remove.x" id="remove.x"/>-->
<tiles:importAttribute name="formName"/>

<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
<tiles:put name="tabKey" value="alert.config.props.CondBox"/>
</tiles:insert>

<script  src="<html:rewrite page='/js/alertConfigFunctions.js'/>" type="text/javascript"></script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <logic:messagesPresent property="condition[0].trigger">
  <tr>
    <td colspan="2" class="ErrorField">
      <span class="ErrorFieldContent"><html:errors
      property="condition[0].trigger"/></span>
    </td>
  </tr>
  </logic:messagesPresent>
  <tiles:insert definition=".events.config.conditions.condition">
    <tiles:put name="formName"><c:out value="${formName}"/></tiles:put>
  </tiles:insert>

  <tiles:insert definition=".events.config.conditions.enablement"/>
</table>
