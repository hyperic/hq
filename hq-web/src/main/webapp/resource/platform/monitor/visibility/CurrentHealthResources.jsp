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
      <tiles:insert definition=".resource.platform.monitor.visibility.listchildresources">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="internal" value="true"/>
        <tiles:put name="childResourceType" value="3"/>
        <tiles:put name="childResourcesHealthKey" value="resource.common.monitor.visibility.PlatformServicesHealthTab"/>
        <tiles:put name="childResourcesTypeKey" value="resource.server.monitor.visibility.ServiceTypeTH"/>
        <tiles:put name="checkboxes" beanName="showProblems"/>
      </tiles:insert>
    </td>
  </tr>
  <tr>
    <td>
      <tiles:insert definition=".resource.platform.monitor.visibility.listchildresources">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="internal" beanName="false"/>
        <tiles:put name="childResourceType" value="2"/>
        <tiles:put name="childResourcesHealthKey" value="resource.platform.monitor.visibility.DeployedServersHealthTab"/>
        <tiles:put name="childResourcesTypeKey" value="resource.platform.monitor.visibility.ServerTypeTH"/>
        <tiles:put name="checkboxes" beanName="showProblems"/>
      </tiles:insert>
    </td>
  </tr>
  <tr>
    <td>
      <tiles:insert definition=".resource.common.monitor.visibility.listvirtualresources">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="internal" value="false"/>
        <tiles:put name="childResourceType" value="3"/>
        <tiles:put name="virtual" value="true"/>
        <tiles:put name="childResourcesHealthKey" value="resource.common.monitor.visibility.VirtualPlatformsHealthTab"/>
        <tiles:put name="childResourcesTypeKey" value="resource.group.monitor.visibility.PlatformTypeTH"/>
        <tiles:put name="checkboxes" beanName="showProblems"/>
      </tiles:insert>
    </td>
  </tr>
  <c:if test="${showProblems}">
  <tr>
    <td>
      <tiles:insert definition=".resource.common.monitor.visibility.problemmetrics"/>
    </td>
  </tr>
  </c:if>
  <c:if test="${showOptions}">
  <tr>
    <td>
      <tiles:insert page="/resource/common/monitor/visibility/MetricsDisplayOptions.jsp"/>
    </td>
  </tr>
  </c:if>
</table>

