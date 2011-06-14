<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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


<c:set var="chbtWidget" value="currentHealthByType"/>
<jsu:script>
	initializeWidgetProperties('<c:out value="${chbtWidget}"/>');
	chbtWidgetProps = getWidgetProperties('<c:out value="${chbtWidget}"/>');
	chbtWidgetProps['subGroup'] = 'chbtListMember';
</jsu:script>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="memberTypeLabel" ignore="true"/>
<tiles:importAttribute name="selfAction"/>

<hq:constant classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" symbol="APPDEF_TYPE_SERVER" var="SERVER_TYPE"/>
<hq:constant classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" symbol="APPDEF_TYPE_SERVICE" var="SERVICE_TYPE"/>

<c:choose>
  <c:when test="${not empty ChildResourceType && ChildResourceType.appdefType == SERVER_TYPE}">
    <fmt:message var="ChildTH" key="resource.common.monitor.visibility.ServerTH"/>
  </c:when>
  <c:when test="${not empty ChildResourceType && ChildResourceType.appdefType == SERVICE_TYPE}">
    <fmt:message var="ChildTH" key="resource.common.monitor.visibility.ServiceTH"/>
  </c:when>
  <c:when test="${not empty memberTypeLabel}">
    <c:set var="ChildTH" value="${memberTypeLabel}"/>
  </c:when>
</c:choose>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
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

<c:set var="tmpImg"><html:img page="/images/icon_alert.gif" width="11" height="11" alt="" border="0"/></c:set>

<c:choose>
  <c:when test="${not empty summaries}">
   <div id="chbtListDiv">
    <display:table items="${summaries}" var="summary" action="${psAction}" width="100%" cellspacing="0" cellpadding="0" 
                   orderValue="so" order="${param.so}" sortValue="sc" sort="${param.sc}" pageValue="pn" 
                   page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" >
     <display:column width="1%" property="resourceId" title="<input type=\"checkbox\" onclick=\"ToggleAllCompare(this, chbtWidgetProps)\" name=\"listToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
       <display:checkboxdecorator name="r" onclick="ToggleSelectionCompare(this, chbtWidgetProps);" styleClass="chbtListMember"/>
     </display:column>
     <display:column width="50%" property="resourceName" title="${ChildTH}" isLocalizedTitle="false" sort="true" sortAttr="5" defaultSort="true" styleClass="ListCell"
                     href="/resource/${summary.resourceEntityTypeName}/monitor/Visibility.do?mode=currentHealth&type=${summary.resourceTypeId}" paramId="rid" paramProperty="resourceId" nowrap="true"/>
     <display:column property="resourceId" width="8%" title="resource.common.monitor.visibility.AVAILTH" styleClass="ListCellCheckboxLeftLine" align="center">
      <display:availabilitydecorator resourceId="${summary.resourceId}" resourceTypeId="${summary.resourceTypeId}"/>
     </display:column>
     <display:column width="4%" value="${tmpImg}" title="&nbsp;"
                     isLocalizedTitle="false" styleClass="ListCellCheckboxLeftLine"
                     href="/alerts/Alerts.do?mode=list&type=${summary.resourceTypeId}" paramId="rid" paramProperty="resourceId" nowrap="true"/>
    </display:table>

<!--  /  -->

<tiles:insert definition=".resource.common.monitor.visibility.metricsToolbar">
  <tiles:put name="widgetInstanceName" beanName="chbtWidget"/>
  <tiles:put name="useCompareButton" value="true"/>
  <tiles:put name="usePager" value="true"/>
  <tiles:put name="listItems" beanName="summaries"/>
  <tiles:put name="listSize" beanName="summaries" beanProperty="totalSize"/>
  <tiles:put name="pageSizeAction" beanName="psAction"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>
</tiles:insert>

</div>

  </c:when>
  <c:otherwise>
<tiles:insert definition=".resource.common.monitor.visibility.noHealths"/>
  </c:otherwise>
</c:choose>

<input type="hidden" id="privateChildResource">
<jsu:script>
  	testCheckboxes("ToggleButtonsCompare", '<c:out value="${chbtWidget}"/>', "privateChildResource", "chbtListMember");
</jsu:script>