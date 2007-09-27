<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-bean" prefix="bean" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<hq:resourceTypeName var="section" typeId="${param.type}"/>

<c:url var="selfAction" value="/resource/${section}/monitor/Config.do">
 <c:param name="mode" value="configure"/>
 <c:param name="rid" value="${Resource.id}"/>
 <c:param name="type" value="${Resource.entityId.type}"/>
</c:url>

<html:form action="/resource/group/monitor/config/EditAvailability">
<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
 <tr>
  <td colspan="4">
   <!--  PAGE TITLE -->
   <tiles:insert definition=".page.title.resource.group">
    <tiles:put name="titleKey" value="common.title.Edit"/>
    <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
   </tiles:insert>
  </td>
 </tr>
 <tr>
  <td class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
  <td><html:img page="/images/spacer.gif" width="75" height="1" alt="" border="0"/></td>
  <td width="100%">
    <!--  GROUP AVAILABILITY TITLE -->
    <tiles:insert definition=".header.tab">
     <tiles:put name="tabKey" value="resource.group.monitor.visibility.config.GroupAvailTab"/>
    </tiles:insert>
    <!--  /  -->

    <!--  GROUP AVAILABILITY CONTENTS -->
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
     <tr>
      <td width="20%" class="BlockLabel" nowrap="true"><fmt:message key="resource.group.monitor.visibility.config.ShowGroupAvailLabel"/></td>
      <logic:messagesPresent property="availabilityThreshold">
       <td width="30%" class="BlockContent" nowrap="true"><fmt:message key="resource.group.monitor.visibility.config.When"/> <html:text size="2" property="availabilityThreshold"/>% <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
      </logic:messagesPresent>
      <logic:messagesNotPresent property="availabilityThreshold">
       <td width="30%" class="BlockContent" nowrap="true"><fmt:message key="resource.group.monitor.visibility.config.When"/> <html:text size="2" property="availabilityThreshold"/>% <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
      </logic:messagesNotPresent>
      <td width="20%" class="BlockLabel" nowrap="true"><fmt:message key="resource.group.monitor.visibility.config.ShowGroupUnavailLabel"/></td>
      <logic:messagesPresent property="unavailabilityThreshold">
       <td width="30%" class="BlockContent" nowrap="true"><fmt:message key="resource.group.monitor.visibility.config.When"/> <html:text size="2" property="unavailabilityThreshold"/>% <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
      </logic:messagesPresent>
      <logic:messagesNotPresent property="unavailabilityThreshold">
       <td width="30%" class="BlockContent" nowrap="true"><fmt:message key="resource.group.monitor.visibility.config.When"/> <html:text size="2" property="unavailabilityThreshold"/>% <fmt:message key="resource.group.monitor.visibility.config.OfGroup"/></td>
      </logic:messagesNotPresent>
     </tr>
     <tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
     </tr>
    </table>
    <!--  /  -->

      &nbsp;<br>
          
    <!-- OK/RESET/CANCEL -->
    <tiles:insert definition=".form.buttons"/>
   </td>
   <td><html:img page="/images/spacer.gif" width="80" height="1" alt="" border="0"/></td>
  </tr>
</table>
</html:form>
