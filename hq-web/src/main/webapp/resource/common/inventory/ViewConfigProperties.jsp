<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

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


<!--  CONFIGURATION PROPERTIES TITLE -->
<tiles:importAttribute name="resource"/>
<tiles:importAttribute name="resourceType"/>
<tiles:importAttribute name="productConfigOptions" />
<tiles:importAttribute name="productConfigOptionsCount" ignore="true" />
<tiles:importAttribute name="monitorConfigOptions" ignore="true"/>
<tiles:importAttribute name="monitorConfigOptionsCount" ignore="true" />
<tiles:importAttribute name="rtConfigOptions" ignore="true"/>
<tiles:importAttribute name="controlConfigOptions" ignore="true"/>
<tiles:importAttribute name="controlConfigOptionsCount" ignore="true" />

<c:url var="editAction" value="/resource/${resource.entityId.typeName}/Inventory.do">
	<c:param name="mode" value="editConfig"/>
	<c:param name="rid" value="${resource.id}"/>
	<c:param name="type" value="${resourceType}"/>
</c:url>

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="PLATFORM" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="SERVER" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE" var="SERVICE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION" var="APPLICATION" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP" var="GROUP" />

<!--  /  -->
<!--  CONFIGURATION PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
    <logic:messagesPresent property="configServer">
    <tr><td class="ErrorField" colspan="4"><i><html:errors property="configServer"/></i></td></tr>
    </logic:messagesPresent>
    <logic:messagesPresent property="noAgent">
    <tr><td class="ErrorField" colspan="4"><i><html:errors property="noAgent"/></i></td></tr>
    </logic:messagesPresent>
	<tr>
    <td class="ConfigPropHeader" colspan="4"><fmt:message key="resource.common.inventory.configProps.Shared"/></td>
  </tr>
  <tr>
        <c:if test="${productConfigOptionsCount == 0}">
        <td width="100%" colspan="4"><i><fmt:message key="resource.common.inventory.configProps.Shared.zeroLength"/></i></td>
        <td></td>
        </c:if>
<logic:iterate id="productConfigOption" indexId="ctr" name="org.hyperic.hq.ui.beans.ConfigValues"
    collection="${productConfigOptions}" >
		<td width="25%" class="BlockLabel"><c:out value="${productConfigOption.shortOption}"/></td>
		<td width="25%" class="BlockContent"><c:out value='${productConfigOption.value}'/></td>
    <c:choose> 
    <c:when test="${(ctr+1) % 2 ==0}">
    </tr>
    <tr>
    </c:when>
    </c:choose>
</logic:iterate>
</tr>
<tr>
    <td class="ConfigPropHeader" colspan="4"><fmt:message key="resource.common.inventory.configProps.Monitoring"/></td>
  </tr>
  <tr>
<c:if test="${monitorConfigOptionsCount == 0 && serverBasedAutoInventory != 1}">
        <td width="100%" colspan="4"><i><fmt:message key="resource.common.inventory.configProps.Monitoring.zeroLength"/></i></td>
        <td></td>
        </c:if>
<logic:iterate id="monitorConfigOption" indexId="ctr" name="org.hyperic.hq.ui.beans.ConfigValues"
    collection="${monitorConfigOptions}" >
		<td width="25%" class="BlockLabel"><c:out value="${monitorConfigOption.option}"/></td>
		<td width="25%" class="BlockContent"><c:out value='${monitorConfigOption.value}'/></td>
    <c:choose> 
    <c:when test="${(ctr+1) % 2 ==0}">
    </tr>
    <tr>
    </c:when>
    </c:choose>
</logic:iterate>
<c:if test="${resourceType == SERVICE && rtSupported}">
</tr><tr>
<td colspan="4" class="BlockBottomLine"><html:img page="images/spacer.gif" width="1" height="1" border="0"/></td>
<td></td>
</tr><tr>
<c:if test="${rtSupported}">
<td width="25%" class="BlockLabel">
<fmt:message key="resource.common.inventory.configProps.serviceResponseTime"/>
</td>
<td width="25%" class="BlockContent">
<c:choose>
<c:when test="${serviceRT}">
<fmt:message key="Yes"/>
</c:when>
<c:otherwise>
<fmt:message key="No"/>
</c:otherwise>
</c:choose>
</td>
</tr>
<c:if test="${euRtSupported}">
<tr>
<td width="25%" class="BlockLabel">
<fmt:message key="resource.common.inventory.configProps.euResponseTime"/>
</td>
<td width="25%" class="BlockContent">
<c:choose>
<c:when test="${euRT == true}">
<fmt:message key="Yes"/>
</c:when>
<c:otherwise>
<fmt:message key="No"/>
</c:otherwise>
</c:choose>
</td>
</tr>
</c:if>
<c:if test="${not empty rtConfigOptions}">
<tr>
<logic:iterate id="rtConfigOption" indexId="ctr" name="org.hyperic.hq.ui.beans.ConfigValues"
    collection="${rtConfigOptions}" >
		<td width="25%" class="BlockLabel"><c:out value="${rtConfigOption.option}"/></td>
		<td width="25%" class="BlockContent"><c:out value='${rtConfigOption.value}'/></td>
    <c:choose> 
    <c:when test="${(ctr+1) % 2 ==0}">
    </tr>
    <tr>
    </c:when>
    </c:choose>
</logic:iterate>
</tr>
</c:if>
</c:if>
</c:if>
<c:if test="${serverBasedAutoInventory == 1  && resourceType == SERVER}">
<c:if test="${resource.wasAutodiscovered == false}">
<tr>
<td nowrap colspan="4" class="BlockCheckboxLabel">
<fmt:message key="resource.common.inventory.configProps.Monitoring.EnableAutoInventoryStatusLabel">
  <fmt:param value="${autodiscoveryMessageServiceList}"/>
</fmt:message>
<c:choose>
  <c:when test="${serverBasedAutoInventoryValue == true}">
    <fmt:message key="ON"/>
  </c:when>
  <c:otherwise>
    <fmt:message key="OFF"/>
  </c:otherwise>
</c:choose>
</td>
</tr>
</c:if>
</c:if>
<c:if test="${controlConfigOptionsCount != null}">
<tr>
    <td class="ConfigPropHeader" colspan="4"><fmt:message key="resource.common.inventory.configProps.Control"/></td>
  </tr>
	<tr>
        <c:if test="${controlConfigOptionsCount == 0}">
        <td width="100%" colspan="4"><i><fmt:message key="resource.common.inventory.configProps.Control.zeroLength"/></i></td>
        <td></td>
        </c:if>
<logic:iterate id="controlConfigOption" indexId="ctr" name="org.hyperic.hq.ui.beans.ConfigValues"
    collection="${controlConfigOptions}" >
		<td width="25%" class="BlockLabel"><c:out value="${controlConfigOption.option}"/></td>
		<td width="25%" class="BlockContent"><c:out value='${controlConfigOption.value}'/></td>
    <c:choose> 
    <c:when test="${(ctr+1) % 2 ==0}">
    </tr>
    <tr>
    </c:when>
    </c:choose>
</logic:iterate>
	</tr>
</c:if>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

<!-- EDIT TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
<c:if test="${(resourceType == SERVER && useroperations['modifyServer']) || (resourceType == SERVICE && useroperations['modifyService']) || ( resourceType == PLATFORM && useroperations['modifyPlatform']) }">
    <c:choose>
        <c:when test="${resourceType == SERVER || resourceType == SERVICE}">
            <c:choose>
                <c:when test="${editConfig == true}">
                  <tr>
                    <td><html:link href="${editAction}"><html:img page="/images/tbb_edit.gif" width="41" height="16" border="0"/></html:link></td>
                  </tr>
                </c:when>
                <c:otherwise>
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
          <tr>
            <td><html:link href="${editAction}"><html:img page="/images/tbb_edit.gif" width="41" height="16" border="0"/></html:link></td>
          </tr>
        </c:otherwise>
    </c:choose>
</c:if>
</table>
<!--  /  -->
