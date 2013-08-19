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
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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

<tiles:importAttribute name="resourceOwner" ignore="false"/>
<tiles:importAttribute name="groupType" ignore="true"/>

<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_PSS" var="CONST_ADHOC_PSS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_GRP" var="CONST_ADHOC_GRP" />
<hq:constant
        classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
        symbol="APPDEF_TYPE_GROUP_DYNAMIC" var="CONST_DYNAMIC_GRP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_APP" var="CONST_ADHOC_APP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_COMPAT_PS" var="CONST_COMPAT_PS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_COMPAT_SVC" var="CONST_COMPAT_SVC" />

<hq:pageSize var="pageSize"/>
<c:set var="selfAction"    
        value="/resource/group/Inventory.do?mode=view&eid=${Resource.entityId}&accord=1" />
<c:set var="widgetInstanceName" value="listGroups"/>

<c:set var="addToListUrl" 
        value="/resource/group/Inventory.do?mode=addResources&rid=${Resource.id}&type=${Resource.entityId.type}"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}"/>

<c:url var="tableAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
</c:url>

<html:form action="/resource/group/inventory/RemoveGroupResources">
<html:hidden property="eid" value="${Resource.entityId}"/>

<!--  RESOURCES, COMPATIBLE CONTENTS -->
<div id="listDiv">
  <display:table var="resourceItem" cellspacing="0" cellpadding="0" width="100%" action="${tableAction}"
                  orderValue="so" order="${param.so}" sortValue="sc" sort="${param.sc}" pageValue="pn"
                  page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" items="${AppdefEntries}" >
      <c:if test="${!(groupType == CONST_DYNAMIC_GRP)}">
        <display:column width="1%" property="entityId.appdefKey"
                        title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"
                isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
          <display:checkboxdecorator name="resources" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
        </display:column>
      </c:if>
    <display:column width="18%" property="name" sort="true" sortAttr="5"
                    defaultSort="true" title="common.header.Name"
                     href="/Resource.do?mode=currentHealth&eid=${resourceItem.entityId.type}:${resourceItem.id}"/>
    <display:column width="18%" property="appdefResourceTypeValue.name" title="resource.group.inventory.TypeTH" />
    <display:column width="44%" property="description" title="common.header.Description" />
    <display:column property="id" title="resource.common.monitor.visibility.AvailabilityTH" width="10%" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle">
      <display:availabilitydecorator resource="${resourceItem}"/>
    </display:column>
  </display:table>
  
</div>
<!--  /  -->
<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="showAddToListBtn"><c:out value="${((webUser.id == resourceOwner.id) || useroperations['modifyResourceGroup']) && !(groupType == CONST_DYNAMIC_GRP)}"/></tiles:put>
  <tiles:put name="showRemoveBtn"><c:out value="${((webUser.id == resourceOwner.id) || useroperations['modifyResourceGroup']) && !(groupType == CONST_DYNAMIC_GRP)}"/></tiles:put>
  <tiles:put name="addToListUrl" beanName="addToListUrl"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="pageSizeAction" beanName="psAction" />
  <tiles:put name="pageSizeParam" value="ps"/>
  <tiles:put name="pageNumAction" beanName="pnAction"/>
  <tiles:put name="pageNumParam" value="pn"/>
  <tiles:put name="listItems" beanName="AppdefEntries"/>
  <tiles:put name="listSize" beanName="AppdefEntries" beanProperty="totalSize"/>
  <tiles:put name="defaultSortColumn" value="5"/>
</tiles:insert>

</html:form>
