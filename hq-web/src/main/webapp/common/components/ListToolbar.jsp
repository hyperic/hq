<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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


<tiles:importAttribute name="listNewUrl" ignore="true"/>
<tiles:importAttribute name="listNewParamName" ignore="true"/>
<tiles:importAttribute name="listNewParamValue" ignore="true"/>
<tiles:importAttribute name="widgetInstanceName" ignore="true"/>
<tiles:importAttribute name="portletToken" ignore="true"/>
<tiles:importAttribute name="noButtons" ignore="true"/>
<tiles:importAttribute name="newOnly" ignore="true"/>
<tiles:importAttribute name="deleteOnly" ignore="true"/>
<tiles:importAttribute name="alerts" ignore="true"/>
<tiles:importAttribute name="hideAlertDefinitionActions" ignore="true"/>
<tiles:importAttribute name="includeGroup" ignore="true"/>
<tiles:importAttribute name="listItems" ignore="true"/>
<tiles:importAttribute name="listSize" ignore="true"/>
<tiles:importAttribute name="pageSizeParam" ignore="true"/>
<tiles:importAttribute name="pageSizeAction"/>
<tiles:importAttribute name="pageNumParam" ignore="true"/>
<tiles:importAttribute name="pageNumAction" ignore="true"/>
<tiles:importAttribute name="defaultSortColumn"/>
<tiles:importAttribute name="goButtonLink" ignore="true"/>
<jsu:script>
  	var goButtonLink;
</jsu:script>
<c:choose>
  <c:when test="${not empty listNewParamName && not empty listNewParamValue}">
    <c:url var="listNewUrl" value="${listNewUrl}">
      <c:param name="${listNewParamName}" value="${listNewParamValue}"/>
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="listNewUrl" value="${listNewUrl}"/>
  </c:otherwise>
</c:choose>

<!-- LIST TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>  
  <c:if test="${empty noButtons}">
    <c:if test="${!deleteOnly}">
    <%-- this is for formatting nazis (you know who you are): there is a good reason for "bad" formatting of the next line --%>
    <%-- for example, it fixes https://intranet.covalent.net/bugz/show_bug.cgi?id=6780.  so, suffer in silence.  vitaliy.--%>
    <td width="40"><html:link href="${listNewUrl}"><html:img page="/images/tbb_new.gif" width="42" height="16" border="0"/></html:link></td>
    </c:if>
    <c:if test="${!newOnly}">
      <c:if test="${includeGroup}">
    <td width="40" align="left" id="<c:out value="${widgetInstanceName}"/>GroupButtonTd"><div id="<c:out value="${widgetInstanceName}"/>GroupButtonDiv"><html:img page="/images/tbb_group_gray.gif" border="0"  /></div></td>
      </c:if>
    <%-- this is for formatting nazis (you know who you are): there is a good reason for "bad" formatting of the next line --%>
    <td width="40" align="left" id="<c:out value="${widgetInstanceName}"/>DeleteButtonTd"><div id="<c:out value="${widgetInstanceName}"/>DeleteButtonDiv"><html:img page="/images/tbb_delete_gray.gif" border="0"  /></div></td>
      <c:if test="${not empty ResourceSummary && empty hideAlertDefinitionActions}">
 	 	<td align="left" id="<c:out value="${widgetInstanceName}"/>EnableAlertsButtonTd"><div id="<c:out value="${widgetInstanceName}"/>EnableAlertsButtonDiv"><html:img page="/images/tbb_enable_all_alerts_gray.gif" border="0"  /></div></td>
 	 	<td align="left" id="<c:out value="${widgetInstanceName}"/>DisableAlertsButtonTd"><div id="<c:out value="${widgetInstanceName}"/>DisableAlertsButtonDiv"><html:img page="/images/tbb_disable_all_alerts_gray.gif" border="0"  /></div></td>
      </c:if>
    </c:if>
  </c:if>
<c:if test="${not empty goButtonLink}">
<td width="50%">
  <div id="goButtonDiv">
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td class="BoldText" nowrap><fmt:message key="alert.config.list.SetActiveLabel"/></td>
        <td><html:img page="/images/spacer.gif" width="10" height="1" alt="" border="0"/></td>
        <td>
          <select name="active" id="active" size="1">
              <option value="1"><fmt:message key="alert.config.props.PB.ActiveYes"/></option>
              <option value="2"><fmt:message key="alert.config.props.PB.ActiveNo"/></option>
          </select>
        </td>
        <td><html:img page="/images/spacer.gif" width="10" height="1" alt="" border="0"/></td>
        <td width="100%"><html:link href="#" styleId="goButtonLink"><html:img page="/images/dash-button_go-arrow_gray.gif" width="23" height="17" alt="" border="0" styleId="goButtonImg"/></html:link></td>
        <jsu:script>
          	goButtonLink = "<c:out value="${goButtonLink}"/>";
          
          	hideDiv("goButtonDiv");
  
          	var checkboxesArr = document.getElementsByName("definitions");
          	var numCheckboxes = checkboxesArr.length;
          
          	if (numCheckboxes > 0) {
            	showDiv("goButtonDiv");
          	}
          </jsu:script>
      </tr>
    </table>
  </div>
</td>
</c:if>
<c:if test="${alerts}">
    <td align="left" id="<c:out value="${widgetInstanceName}${portletToken}"/>FixedButtonTd" style="white-space: nowrap">
		<div id="<c:out value="${widgetInstanceName}${portletToken}"/>FixedButtonDiv">
			<input type="button" id="<c:out value="${widgetInstanceName}${portletToken}"/>_FixButton" value="<fmt:message key="resource.common.alert.action.fixed.label"/>" class="CompactButtonInactive" disabled="disabled" onclick="MyAlertCenter.processButtonAction(this)" />
			&nbsp;&nbsp;
			<input type="button" id="<c:out value="${widgetInstanceName}${portletToken}"/>_AckButton" value="<fmt:message key="resource.common.alert.action.acknowledge.label"/>" class="CompactButtonInactive" disabled="disabled" onclick="MyAlertCenter.processButtonAction(this)" />
			<input type="hidden" name="buttonAction" value="" />
          	<input type="hidden" name="fixedNote" value="" />
          	<input type="hidden" name="ackNote" value="" />
			<input type="hidden" name="fixAll" value="false" />
          	<input type="hidden" name="pauseTime" value="" />
		</div>
	</td>
</c:if>
<c:choose>
  <c:when test="${not empty pageSizeAction}">
    <tiles:insert definition=".controls.paging">
      <tiles:put name="listItems" beanName="listItems"/>
      <tiles:put name="listSize" beanName="listSize"/>
      <tiles:put name="pageSizeParam" beanName="pageSizeParam"/>
      <tiles:put name="pageSizeAction" beanName="pageSizeAction"/>
      <tiles:put name="pageNumParam" beanName="pageNumParam"/>
      <tiles:put name="pageNumAction" beanName="pageNumAction"/>
      <tiles:put name="defaultSortColumn" beanName="defaultSortColumn"/>
    </tiles:insert>
  </c:when>
</c:choose>
</tr>
</table>
