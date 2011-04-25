<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
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


<%-- this tile is designed to be included from resource/common/monitor/config/ConfigMetrics
  -- if the resource is a compatible group
  --
  -- @author csherr
  --%>

<tiles:importAttribute name="section" ignore="true"/>
<tiles:importAttribute name="Resource"/>

<!--  GROUP AVAILABILITY TITLE -->
<c:set var="tmpTitle"> - <fmt:message key="resource.group.monitor.visibility.config.GroupAvailSubTab"/></c:set>

<tiles:insert definition=".header.tab">
 <tiles:put name="tabKey" value="resource.group.monitor.visibility.config.GroupAvailTab"/>
 <tiles:put name="subTitle" beanName="tmpTitle"/>
</tiles:insert>
<!--  /  -->

<!--  GROUP AVAILABILITY CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr>
  <td width="20%" class="BlockLabel"><fmt:message key="resource.group.monitor.visibility.config.ShowGroupAvailLabel"/></td>
  <td width="30%" class="BlockContent"><fmt:message key="resource.group.monitor.visibility.config.When"/> XXX <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
  <td width="20%" class="BlockLabel"><fmt:message key="resource.group.monitor.visibility.config.ShowGroupUnavailLabel"/></td>
  <td width="30%" class="BlockContent"><fmt:message key="resource.group.monitor.visibility.config.When"/> XXX <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
 </tr>
 <tr>
  <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
 </tr>
</table>

<!-- EDIT TOOLBAR -->
<c:url var="editUrl" value="/resource/group/monitor/Config.do">
	<c:param name="mode" value="edit"/>
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${Resource.entityId.type}"/>
</c:url>
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" beanName="editUrl"/>
</tiles:insert>