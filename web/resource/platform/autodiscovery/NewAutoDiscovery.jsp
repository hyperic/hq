<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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


<tiles:insert definition=".page.title.resource.platform">
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
</tiles:insert>

<c:set var="selfAction" 
        value="/resource/platform/AutoDiscovery.do?mode=new"/>

<c:set var="actionUrl" 
        value="/resource/platform/autodiscovery/NewAutoDiscovery"/>
        
<html:form action="${actionUrl}">
<html:hidden property="rid"/>
<html:hidden property="type"/>
    
<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<tiles:insert page="/resource/platform/autodiscovery/HeaderMessage.jsp"/>

<!-- FORM BUTTONS -->
<tiles:insert definition=".form.buttons"/>
<br>

<!-- SCAN PROPERTIES -->
<tiles:insert page="/resource/platform/autodiscovery/ScanPropertiesForm.jsp">
    <tiles:put name="isNewScan" value="true"/>
    <tiles:put name="selfAction" beanName="selfAction"/>
    <tiles:put name="actionUrl" beanName="actionUrl"/>
</tiles:insert>
&nbsp;<br>
	
<!-- FORM BUTTONS -->
<tiles:insert definition=".form.buttons"/>

</html:form>

<!-- FOOTER -->
<tiles:insert definition=".page.footer"/>


