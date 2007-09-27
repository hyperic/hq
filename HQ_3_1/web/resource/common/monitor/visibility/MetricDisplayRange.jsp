<%@ taglib uri="struts-html-el" prefix="html" %>
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


<tiles:importAttribute name="form" ignore="true"/>

<c:if test="${empty form}">
  <c:set var="form" value="${MetricDisplayRangeForm}"/>
</c:if>

<script type="text/javascript">
var pageData = new Array();
</script>

<html:form action="/resource/common/monitor/visibility/MetricDisplayRange">

<tiles:insert definition=".page.title.resource.generic">
  <tiles:put name="titleKey" value="resource.common.monitor.visibility.MetricDisplayRangePageTitle"/>
</tiles:insert>

<tiles:insert definition=".resource.common.monitor.visibility.metricDisplayRangeForm"/>

<tiles:insert definition=".form.buttons"/>
<tiles:insert definition=".page.footer"/>

<c:if test="${not empty form.rid}">
<html:hidden property="rid"/>
</c:if>
<c:if test="${not empty form.type}">
<html:hidden property="type"/>
</c:if>
<c:forEach var="eid" items="${form.eid}">
<input type="hidden" name="eid" value="<c:out value="${eid}"/>">
</c:forEach>
<c:if test="${not empty form.ctype}">
<html:hidden property="ctype"/>
</c:if>
</html:form>
