<%@ page language="java" %>
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


<tiles:importAttribute name="entityId"/>
<c:set var="selfAction" value="/resource/service/monitor/Visibility.do?mode=resourceMetrics&eid=${entityId.type}:${Resource.id}"/>

<tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
  <tiles:put name="selectedIndex" value="1"/>
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  <tiles:put name="entityType" value="service"/>
  <c:if test="${perfSupported}">
    <tiles:put name="tabListName" value="perf"/>
  </c:if>
</tiles:insert>

<table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
<html:form action="/resource/service/monitor/visibility/ServiceMetrics">

<tiles:insert definition=".resource.common.monitor.visibility.metricsDisplay">
  <tiles:put name="summaries" beanName="MetricSummaries"/>
  <tiles:put name="buttonMode" value="baselines"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
  <tiles:put name="useCurrent" value="true"/>
</tiles:insert>

<html:hidden property="h"/>
<html:hidden property="rid"/>
<html:hidden property="type"/>
</html:form>
    </td>
  </tr>
</table>
