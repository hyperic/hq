<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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
<tiles:importAttribute name="showDuration" ignore="true"/>

<!-- Content Block Title -->
<tiles:insertDefinition name=".header.tab">
<tiles:putAttribute name="tabKey" value="alert.config.props.CondBox"/>
</tiles:insertDefinition>
<jsu:importScript path="/js/alertConfigFunctions.js" />

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <s:if test="%{fieldErrors.containsKey('conditions[0].trigger')}">
  <tr>
    <td colspan="2" class="ErrorField">
      <span class="ErrorFieldContent"><s:fielderror fieldName="conditions[0].trigger"/></span>
    </td>
  </tr>
  </s:if>
  <tiles:insertDefinition name=".events.config.conditions.condition">
    <tiles:putAttribute name="formName"><c:out value="${formName}"/></tiles:putAttribute>
  </tiles:insertDefinition>

  <tiles:insertDefinition name=".events.config.conditions.enablement">
    <tiles:putAttribute name="showDuration" value="${showDuration}"/>
  </tiles:insertDefinition>
</table>
