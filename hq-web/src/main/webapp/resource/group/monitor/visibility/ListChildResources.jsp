<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

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


<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="appdefResourceType" ignore="true"/>
<tiles:importAttribute name="childResourcesTypeKey"/>
<tiles:importAttribute name="childResourcesHealthKey" ignore="true"/>
<tiles:importAttribute name="virtual" ignore="true"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:if test="${empty mode}">
  <c:set var="mode" value="currentHealth"/>
</c:if>

<script type="text/javascript">
  <c:set var="widgetInstanceName" value="childResources"/>
  <c:set var="listMembersName" value="groupMembers"/>

  initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
  widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');

<c:if test="${empty params.eids && not virtual}">
  function checkAllBoxes() {
    var check = true;
    for (var i = 0; i < document.forms[0].elements.length; i++) {
      if (document.forms[0].elements[i].name == 'eids' &&
        document.forms[0].elements[i].checked)
        check = false;
    }

    if (check) {
      for (var i = 0; i < document.forms[0].elements.length; i++) {
        if (document.forms[0].elements[i].name == 'eids' ||
            document.forms[0].elements[i].name == 'groupMembersAll')
            document.forms[0].elements[i].checked = true;
      }
    }
  }

  onloads.push( checkAllBoxes );
</c:if>
</script>

<c:if test="${empty childResourcesHealthKey}">
  <c:set var="childResourcesHealthKey" value="resource.service.monitor.visibility.MembersTab"/>
</c:if>

<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_AUTOGROUP" var="AUTOGROUP" />
<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_CLUSTER" var="CLUSTER" />
<hq:constant classname="org.hyperic.hq.bizapp.shared.uibeans.UIConstants"
             symbol="SUMMARY_TYPE_SINGLETON" var="SINGLETON" />

<c:set var="useAvailStoplightDimensions" value="false" />
<c:if test="${useAvailStoplightDimensions}">
  <c:set var="availStoplightDimensions" value=" width=\"106\" " />
</c:if>

<c:choose>
	<c:when test="${not empty summaries}">
    	<c:forEach var="summary" items="${summaries}" varStatus="status">
      		<div id="<c:out value="${summary.resourceName}"/>_menu" class="menu">
        		<ul>
          			<li>
          				<div class="BoldText"><fmt:message key="${childResourcesTypeKey}"/></div>
              			<c:out value="${summary.resourceTypeName}"/>
          			</li>
          			<li>
          				<div class="BoldText"><fmt:message key="resource.common.monitor.visibility.USAGETH"/></div>
            			<hq:metric metric="${summary.throughput}" unit="${summary.throughputUnits}"  defaultKey="common.value.notavail" />
          			</li>
          			<hr/>
          			<li>
            			<html:link page="/resource/${summary.resourceEntityTypeName}/monitor/Visibility.do?mode=${mode}&type=${summary.resourceTypeId}" paramId="rid" paramName="summary" paramProperty="resourceId">
              				<fmt:message key="resource.common.monitor.visibility.GoToResource"/>
            			</html:link>
          			</li>
        		</ul>
      		</div>

      		<c:set var="count" value="${status.count}"/>
    	</c:forEach>

    	<c:if test="${count > 5}">
      		<div class="scrollable">
    	</c:if>

		<c:if test="${not empty summaries}">
			<table width="100%" border="0" cellpadding="1" cellspacing="0" id="ResourceTable">
    			<tr>
      				<c:if test="${checkboxes}">
	      				<td class="ListHeaderCheckbox"><input type="checkbox" onclick="ToggleAllGroup(this, widgetProperties, '<c:out value="${listMembersName}"/>')" name="<c:out value="${listMembersName}"/>All"></td>
      				</c:if>
      				<td class="ListHeader" width="90%" align="left"><fmt:message key="${childResourcesHealthKey}"/></td>
      				<td class="ListHeaderInactive" align="center" nowrap><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></td>
      				<td class="ListHeaderInactive" width="10%">&nbsp;</td>
    			</tr>
    			<c:forEach var="summary" items="${summaries}">
    				<tr>
      					<c:if test="${checkboxes}">
      						<td class="ListCellCheckbox"><html:multibox property="eids" value="${summary.entityId}" styleClass="${listMembersName}" onchange="ToggleGroup(this, widgetProperties)"/></td>
      					</c:if>
      					<td class="ListCell" style="padding-top:10px;">
        					<html:link page="/resource/${summary.resourceEntityTypeName}/monitor/Visibility.do?mode=${mode}&eid=${summary.resourceTypeId}:${summary.resourceId}"><c:out value="${summary.resourceName}"/></html:link>
      					</td>
      					<td class="ListCellCheckbox">
    						<tiles:insert page="/resource/common/monitor/visibility/AvailIcon.jsp">
        						<tiles:put name="availability" beanName="summary" beanProperty="availability" />
    						</tiles:insert>
      					</td>
        				<td class="ListCellCheckbox resourceCommentIcon"
    	    				onmouseover="menuLayers.show('<c:out value="${summary.resourceName}" />_menu', event)" 
    	    				onmouseout="menuLayers.hide()">&nbsp;
						</td>
    				</tr>
    			</c:forEach>
    		</table>
    	</c:if>

    <c:if test="${count > 5}">
      </div>
    </c:if>

  </c:when>
  <c:when test="${not virtual}">
<table width="100%" border="0" cellpadding="1" cellspacing="0" id="ResourceTable">
  <tr>
    <td class="ListHeader" colspan="2" width="100%" align="left"><fmt:message key="resource.service.monitor.visibility.MembersTab"/></td>
    </td>
  </tr>
</table>
<tiles:insert definition=".resource.common.monitor.visibility.noHealths"/>
  </c:when>
</c:choose>

