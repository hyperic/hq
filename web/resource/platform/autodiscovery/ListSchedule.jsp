<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="hq" prefix="hq" %>
<%@ taglib uri="display" prefix="display" %>
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


<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="ALL_SCHEDULES_ATTR" var="CONST_SCHEDULES" />

<c:set var="widgetInstanceName" value="listSchedules"/>

<c:url var="selfAction"    
        value="/resource/platform/AutoDiscovery.do?mode=view&rid=${Resource.id}&type=${Resource.entityId.type}" />
<c:set var="newScheduleAction" 
        value="/resource/platform/AutoDiscovery.do?mode=new&rid=${Resource.id}&type=${Resource.entityId.type}"/>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="sos" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
        
<c:url var="pnAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
        
<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="widgetInstanceName" value="listAutoDiscoverySchedule"/>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>

<c:set var="entityId" value="${Resource.entityId}"/>

<html:form action="/resource/platform/autodiscovery/RemoveSchedule">
<input type="hidden" name="rid" value="<c:out value='${Resource.id}'/>">
<input type="hidden" name="type" value="<c:out value='${Resource.entityId.type}'/>">

<!--  SCHEDULE TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.autodiscovery.AutoDiscoveryScheduleTab"/>
</tiles:insert>
<!--  /  -->

<!--  SCHEDULE CONTENTS -->
<div id="listDiv">
  <display:table items="${requestScope[CONST_SCHEDULES]}" cellspacing="0" cellpadding="0" width="100%" action="${selfAction}" var="aiSchedule" pageSize="${pageSize}" >
    <display:column width="1%" property="id" 
                    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\">"  
		    isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
      <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
    </display:column>
    <display:column width="20%" property="scanName" sort="true" sortAttr="5" 
                    title="resource.autodiscovery.autoDiscoverySchedule.AutoDiscoveryTH" 
                    href="/resource/platform/AutoDiscovery.do?mode=edit&rid=${Resource.id}&type=${entityId.type}&sid=${aiSchedule.id}"/>
   <display:column width="16%" property="nextFireTime" title="resource.server.ControlSchedule.ListHeader.NextFire"  
                   nowrap="true" sort="true" sortAttr="15" defaultSort="true">
      <display:datedecorator/>
   </display:column>
    <display:column width="30%" value="${aiSchedule.scheduleValue.scheduleString}"   
                    title="resource.autodiscovery.autoDiscoverySchedule.DateScheduledTH"/>
    <display:column width="33%" property="scanDesc" 
                    title="common.header.Description"/>
  </display:table>
  
</div>
<!--  /  -->

<c:set var="items" value="${requestScope[CONST_SCHEDULES]}"/>

<tiles:insert definition=".toolbar.list">
  <tiles:put name="listNewUrl" beanName="newScheduleAction"/>
  <tiles:put name="listItems" beanName="items"/>
  <tiles:put name="listSize" beanName="items" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="selfAction" />
  <tiles:put name="pageNumAction" beanName="selfAction"/>    
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="defaultSortColumn" value="15"/>
</tiles:insert>

</html:form>
