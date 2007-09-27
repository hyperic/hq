<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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


<html:form action="/resource/server/inventory/AddServerGroups" method="POST">

<tiles:insert definition=".page.title.resource.server">
  <tiles:put name="titleKey" value="resource.application.inventory.AddToGroupsTitle"/>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>

<tiles:insert definition=".resource.common.inventory.addResourceGroups">
  <tiles:put name="resource" beanName="Resource"/> 
  <tiles:put name="availableGroups" beanName="AvailableResGrps"/>
  <tiles:put name="availableGroupsCount" beanName="NumAvailableResGrps"/>
  <tiles:put name="pendingGroups" beanName="PendingResGrps"/>
  <tiles:put name="pendingGroupsCount" beanName="NumPendingResGrps"/>
</tiles:insert>

<tiles:insert definition=".form.buttons">
  <tiles:put name="addToList" value="true"/>
</tiles:insert>

<tiles:insert definition=".page.footer"/>

<html:hidden property="rid"/>
<html:hidden property="type"/>
</html:form>
