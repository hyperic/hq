<%@ page language="java" %>
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


<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="showProblems" ignore="true"/>
<tiles:importAttribute name="showOptions" ignore="true"/>

<c:if test="${showProblems}">
	<jsu:importScript path="/js/listWidget.js" />
</c:if>

<tiles:insert page="/resource/common/monitor/visibility/ResourcesTab.jsp"/>

<table width="285" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
      <tiles:insert definition=".resource.group.monitor.visibility.listchildresources">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="internal" value="false"/>
        <tiles:put name="childResourcesTypeKey" value="resource.common.inventory.security.ResourceTypeLabel"/>
        <c:if test="${mode == 'currentHealth'}">
          <tiles:put name="checkboxes" value="true"/>
        </c:if>
      </tiles:insert>
    </td>
  </tr>
  <c:if test="${not empty HostHealthSummaries}">
  <tr><td>
    <c:set var="tabKey" value="resource.common.monitor.visibility.Host${hostType}sTab"/>
    <c:set var="hostResourcesHealthKey" value="resource.common.monitor.visibility.${hostType}TH"/>
    <tiles:insert definition=".resource.group.monitor.visibility.listhostresources">
      <tiles:put name="tabKey" beanName="tabKey"/>
      <tiles:put name="hostResourcesHealthKey" beanName="hostResourcesHealthKey"/>
    </tiles:insert>
  </td></tr>
  </c:if>
  <tr>
    <td>
  <c:choose>
    <c:when test="${showProblems}">
      <tiles:insert definition=".resource.common.monitor.visibility.problemmetrics">
        <tiles:put name="hideTools" value="false"/>
      </tiles:insert>
    </c:when>
    <c:when test="${showOptions}">
      <tiles:insert page="/resource/common/monitor/visibility/MetricsDisplayOptions.jsp"/>
    </c:when>
  </c:choose>
    </td>
  </tr>
</table>
