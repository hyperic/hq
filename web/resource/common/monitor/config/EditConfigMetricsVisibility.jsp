<%@ page language="java" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="hq" prefix="hq" %>
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


<%-- The 4 tables on the page have different sort order and column attributes.
  -- They share page number and page size request attributes (i.e, 15 on each list).
  --
  -- ps pagesize param
  -- pn pagenum param
  -- a [sc so] availibility params
  -- u [sc so] utilization params
  -- t [sc so] usage params
  -- p [sc so] performance params
  --%>
<c:set var="section" value="application"/>


<c:url var="selfPaAction" value="/resource/${section}/monitor/Config.do">
  <c:param name="mode" value="configure"/>
  <c:if test="${not empty param.aetid}">
    <c:param name="aetid" value="${param.aetid}"/>
  </c:if>
  <c:if test="${not empty Resource}">
    <c:param name="eid" value="${Resource.entityId}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.sct}">
    <c:param name="sct" value="${param.sct}"/>
  </c:if>
  <c:if test="${not empty param.sot}">
    <c:param name="sot" value="${param.sot}"/>
  </c:if>
  <c:if test="${not empty param.scu}">
    <c:param name="scu" value="${param.scu}"/>
  </c:if>
  <c:if test="${not empty param.sou}">
    <c:param name="sou" value="${param.sou}"/>
  </c:if>

  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<c:url var="selfPuAction" value="/resource/${section}/monitor/Config.do">
  <c:param name="mode" value="configure"/>
  <c:if test="${not empty param.aetid}">
    <c:param name="aetid" value="${param.aetid}"/>
  </c:if>
  <c:if test="${not empty Resource}">
    <c:param name="eid" value="${Resource.entityId}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.sct}">
    <c:param name="sct" value="${param.sct}"/>
  </c:if>
  <c:if test="${not empty param.sot}">
    <c:param name="sot" value="${param.sot}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>

  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<c:url var="selfPpAction" value="/resource/${section}/monitor/Config.do">
  <c:param name="mode" value="configure"/>
  <c:if test="${not empty param.aetid}">
    <c:param name="aetid" value="${param.aetid}"/>
  </c:if>
  <c:if test="${not empty Resource}">
    <c:param name="eid" value="${Resource.entityId}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>
  <c:if test="${not empty param.sct}">
    <c:param name="sct" value="${param.sct}"/>
  </c:if>
  <c:if test="${not empty param.sot}">
    <c:param name="sot" value="${param.sot}"/>
  </c:if>
  <c:if test="${not empty param.scu}">
    <c:param name="scu" value="${param.scu}"/>
  </c:if>
  <c:if test="${not empty param.sou}">
    <c:param name="sou" value="${param.sou}"/>
  </c:if>

  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<c:url var="selfPtAction" value="/resource/${section}/monitor/Config.do">
  <c:param name="mode" value="configure"/>
  <c:if test="${not empty param.aetid}">
    <c:param name="aetid" value="${param.aetid}"/>
  </c:if>
  <c:if test="${not empty Resource}">
    <c:param name="eid" value="${Resource.entityId}"/>
  </c:if>
  <c:if test="${not empty param.scp}">
    <c:param name="scp" value="${param.scp}"/>
  </c:if>
  <c:if test="${not empty param.sop}">
    <c:param name="sop" value="${param.sop}"/>
  </c:if>
  <c:if test="${not empty param.scu}">
    <c:param name="scu" value="${param.scu}"/>
  </c:if>
  <c:if test="${not empty param.sou}">
    <c:param name="sou" value="${param.sou}"/>
  </c:if>
  <c:if test="${not empty param.sca}">
    <c:param name="sca" value="${param.sca}"/>
  </c:if>
  <c:if test="${not empty param.soa}">
    <c:param name="soa" value="${param.soa}"/>
  </c:if>

  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>

<!--  COLLECT METRICS TAB -->
<c:set var="emptyMsg"><fmt:message key="resource.common.monitor.visibility.EmptyMetricsEtc"/></c:set>
<hq:constant
        classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants"
        symbol="APPDEF_TYPE_GROUP"
        var="APPDEF_TYPE_GROUP"/>

<c:set var="entityTypeId" value="${param.aetid}"/>

<c:choose>
<c:when test="${Resource.entityId.type == APPDEF_TYPE_GROUP ||
                not empty ChildResourceType}">
<!-- AUTOGROUP METRICS LIST -->
<!--  AVAILABILITY CONTENTS 3 -->
<display:table items="${availabilityMetrics}" var="grpavailmetric" 
         action="${selfPaAction}" orderValue="soa" order="${param.soa}" sortValue="sca" 
         sort="${param.sca}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember1')\" name=\"listMember1All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember1"/>
 </display:column>
 <display:column value="${grpavailmetric.name}" title="resource.common.monitor.visibility.AvailabilityTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <display:column property="activeMembers" title="resource.common.monitor.visibility.config.MembersCollectingTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:groupmetricdecorator active="${grpavailmetric.activeMembers}" total="${grpavailmetric.totalMembers}"/>
 </display:column> <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:datedecorator active="${grpavailmetric.activeMembers}" isElapsedTime="true" isGroup="true"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  USAGE CONTENTS -->
<display:table items="${throughputMetrics}" var="grpthroughmetric" 
         action="${selfPtAction}" orderValue="sot" order="${param.sot}" sortValue="sct" 
         sort="${param.sct}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember2')\" name=\"listMember2All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember2"/>
 </display:column>
 <display:column value="${grpthroughmetric.name}" title="resource.common.monitor.visibility.UsageTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <display:column property="activeMembers" title="resource.common.monitor.visibility.config.MembersCollectingTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:groupmetricdecorator active="${grpthroughmetric.activeMembers}" total="${grpthroughmetric.totalMembers}"/>
 </display:column>
 <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:datedecorator active="${grpthroughmetric.activeMembers}" isElapsedTime="true" isGroup="true"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  PERFORMANCE CONTENTS -->
<display:table items="${performanceMetrics}" var="grpperfmetric" 
         action="${selfPpAction}" orderValue="sop" order="${param.sop}" sortValue="scp" 
         sort="${param.scp}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember3')\" name=\"listMember3All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember3"/>
 </display:column>
 <display:column value="${grpperfmetric.name}" title="resource.common.monitor.visibility.PerformanceTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <display:column property="activeMembers" title="resource.common.monitor.visibility.config.MembersCollectingTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:groupmetricdecorator active="${grpperfmetric.activeMembers}" total="${grpperfmetric.totalMembers}"/>
 </display:column><display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:datedecorator active="${grpperfmetric.activeMembers}" isElapsedTime="true" isGroup="true"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  UTILIZATION CONTENTS -->
<display:table items="${utilizationMetrics}" var="grputilmetric" 
         action="${selfPuAction}" orderValue="sou" order="${param.sou}" sortValue="scu" 
         sort="${param.scu}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember4')\" name=\"listMember4All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember4"/>
 </display:column>
 <display:column value="${grputilmetric.name}" title="resource.common.monitor.visibility.UtilizationTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <display:column property="activeMembers" title="resource.common.monitor.visibility.config.MembersCollectingTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:groupmetricdecorator active="${grputilmetric.activeMembers}" total="${grputilmetric.totalMembers}"/>
 </display:column> <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactive">
  <display:datedecorator active="${grputilmetric.activeMembers}" isElapsedTime="true" isGroup="true"/>
 </display:column>
</display:table>
<!--  /  -->

</c:when>
<c:when test="${not empty entityTypeId}">
<!-- RESOURCE TYPE METRIC LIST -->
<!--  AVAILABILITY CONTENTS 1 -->
<display:table items="${availabilityMetrics}" var="availtemplate" 
         action="${selfPaAction}" orderValue="soa" order="${param.soa}"
         sortValue="sca" sort="${param.sca}" pageValue="pn" page="${param.pn}"
         pageSizeValue="ps" pageSize="${param.ps}" styleId="fromTable"
         width="100%" cellpadding="0" cellspacing="0" border="0"
         emptyMsg="${emptyMsg}" nowrapHeader="true">
<%-- 
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember1')\" name=\"listMember1All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember1"/>
 </display:column>
--%>
 <display:column property="name" title="resource.common.monitor.visibility.AvailabilityTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" /> --%>
 <display:column property="defaultInterval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>

<%--
 <display:column width="1%" property="id" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="defaultOn[${availtemplate.id}]" id="defaultOn[${availtemplate.id}]" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember1"/>
 </display:column>
--%>

 <display:column property="defaultOn" title="resource.common.monitor.visibility.config.DefaultOnTH" align="left" nowrap="true" width="15%"  headerStyleClass="ListHeaderInactiveSorted">

<%--
  <display:labeldecorator forElement="defaultOn[${availtemplate.id}]" value="Collect ${availtemplate.name} Data" />
	--%>

	<display:checkboxdecorator name="defaultOn[${availtemplate.id}]" elementId="defaultOn[${availtemplate.id}]" label=" Collect ${availtemplate.name} Data" styleClass="listMember1"/>
		<%-- <display:booleandecorator flagKey="yesno"/> --%>
		<%-- <label><input type="checkbox" onclick="ToggleRemoveGo(this, widgetProperties, 'listMember1')" name="listMember1"><display:booleandecorator flagKey="yesno"/></label> --%>
		<%-- <display:checkboxdecorator name="defaultOn" styleClass="listMember1"/> --%>
 </display:column>
 <display:column property="designate" title="resource.common.monitor.visibility.config.IndicatorTH" align="left" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
	<display:checkboxdecorator name="designate[${availtemplate.id}]"  elementId="designate[${availtemplate.id}]" label=" Use ${availtemplate.name} as Indicator" styleClass="listMember2"/>
<%--
<display:booleandecorator flagKey="yesno"/>
--%>
 </display:column>
</display:table>
<!--  /  -->

<!--  USAGE CONTENTS -->
<display:table items="${throughputMetrics}" var="throughtemplate" 
         action="${selfPtAction}" orderValue="sot" order="${param.sot}" sortValue="sct" 
         sort="${param.sct}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember2')\" name=\"listMember2All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember2"/>
 </display:column>
 <display:column value="${throughtemplate.name}" title="resource.common.monitor.visibility.UsageTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" /> --%>
 <display:column property="defaultInterval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>
 <display:column property="defaultOn" title="resource.common.monitor.visibility.config.DefaultOnTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
    <display:booleandecorator flagKey="yesno"/>
 </display:column>
 <display:column property="designate" title="resource.common.monitor.visibility.config.IndicatorTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
    <display:booleandecorator flagKey="yesno"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  PERFORMANCE CONTENTS -->
<display:table items="${performanceMetrics}" var="perftemplate" 
         action="${selfPpAction}" orderValue="sop" order="${param.sop}" sortValue="scp" 
         sort="${param.scp}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember3')\" name=\"listMember3All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember3"/>
 </display:column>
 <display:column value="${perftemplate.name}" title="resource.common.monitor.visibility.PerformanceTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" />--%>
 <display:column property="defaultInterval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>
 <display:column property="defaultOn" title="resource.common.monitor.visibility.config.DefaultOnTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
    <display:booleandecorator flagKey="yesno"/>
 </display:column>
 <display:column property="designate" title="resource.common.monitor.visibility.config.IndicatorTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
    <display:booleandecorator flagKey="yesno"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  UTILIZATION CONTENTS -->
<display:table items="${utilizationMetrics}" var="utiltemplate" 
         action="${selfPuAction}" orderValue="sou" order="${param.sou}" sortValue="scu" 
         sort="${param.scu}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember4')\" name=\"listMember4All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember4"/>
 </display:column>
 <display:column value="${utiltemplate.name}" title="resource.common.monitor.visibility.UtilizationTH" sort="true" sortAttr="2" defaultSort="true" width="70%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" />--%>
 <display:column property="defaultInterval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>
 <display:column property="defaultOn" title="resource.common.monitor.visibility.config.DefaultOnTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
    <display:booleandecorator flagKey="yesno"/>
 </display:column>
 <display:column property="designate" title="resource.common.monitor.visibility.config.IndicatorTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
    <display:booleandecorator flagKey="yesno"/>
 </display:column>
</display:table>

</c:when>
<c:otherwise>
<!-- SINGLE RESOURCE METRIC LIST -->
<!--  AVAILABILITY CONTENTS 2 -->
<display:table items="${availabilityMetrics}" var="availmetric" 
         action="${selfPaAction}" orderValue="soa" order="${param.soa}" sortValue="sca" 
         sort="${param.sca}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember1')\" name=\"listMember1All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember1"/>
 </display:column>
 <display:column value="${availmetric.name}" title="resource.common.monitor.visibility.AvailabilityTH" sort="true" sortAttr="2" defaultSort="true" width="85%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" /> --%>
 <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  USAGE CONTENTS -->
<display:table items="${throughputMetrics}" var="throughmetric" 
         action="${selfPtAction}" orderValue="sot" order="${param.sot}" sortValue="sct" 
         sort="${param.sct}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember2')\" name=\"listMember2All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember2"/>
 </display:column>
 <display:column value="${throughmetric.name}" title="resource.common.monitor.visibility.UsageTH" sort="true" sortAttr="2" defaultSort="true" width="85%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" /> --%>
 <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  PERFORMANCE CONTENTS -->
<display:table items="${performanceMetrics}" var="perfmetric" 
         action="${selfPpAction}" orderValue="sop" order="${param.sop}" sortValue="scp" 
         sort="${param.scp}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember3')\" name=\"listMember3All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember3"/>
 </display:column>
 <display:column value="${perfmetric.name}" title="resource.common.monitor.visibility.PerformanceTH" sort="true" sortAttr="2" defaultSort="true" width="85%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" />--%>
 <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>
</display:table>
<!--  /  -->

<!--  UTILIZATION CONTENTS -->
<display:table items="${utilizationMetrics}" var="utilmetric" 
         action="${selfPuAction}" orderValue="sou" order="${param.sou}" sortValue="scu" 
         sort="${param.scu}" pageValue="pn" page="${param.pn}" pageSizeValue="ps" pageSize="${param.ps}" 
         styleId="fromTable" width="100%" cellpadding="0" cellspacing="0" border="0" emptyMsg="${emptyMsg}" nowrapHeader="true">
 <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllRemoveGo(this, widgetProperties, 'listMember4')\" name=\"listMember4All\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderInactiveSorted">
  <display:checkboxdecorator name="mids" onclick="ToggleRemoveGo(this, widgetProperties)" styleClass="listMember4"/>
 </display:column>
 <display:column value="${utilmetric.name}" title="resource.common.monitor.visibility.UtilizationTH" sort="true" sortAttr="2" defaultSort="true" width="85%" headerStyleClass="ListHeaderInactiveSorted" />
 <%-- <display:column property="instanceId" title="common.header.Description" width="60%" headerStyleClass="ListHeaderInactiveSorted" />--%>
 <display:column property="interval" title="resource.common.monitor.visibility.config.CollectionIntervalTH" align="center" nowrap="true" width="15%" headerStyleClass="ListHeaderInactiveSorted">
  <display:datedecorator isElapsedTime="true"/>
 </display:column>
</display:table>
</c:otherwise>
</c:choose>
