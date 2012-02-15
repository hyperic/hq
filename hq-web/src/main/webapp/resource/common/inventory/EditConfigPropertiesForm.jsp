<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
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
<jsu:importScript path="/js/functions.js" />
<jsu:importScript path="/js/serviceInventory_ConfigProperties.js" />

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

<c:set var="entityId" value="${Resource.entityId}"/>
<% String resourceTitle = "";%>
<c:choose>
<c:when test="${PLATFORM == entityId.type}">
 <%resourceTitle =".page.title.resource.platform";%>
</c:when>    
<c:when test="${SERVER == entityId.type}">
 <%resourceTitle =".page.title.resource.server";%>
</c:when>    
<c:when test="${SERVICE == entityId.type}">
 <%resourceTitle =".page.title.resource.service";%>
</c:when>    
<c:when test="${APPLICATION == entityId.type}">
 <%resourceTitle =".page.title.resource.application";%>
</c:when>    
<c:when test="${GROUP == entityId.type}">
 <%resourceTitle =".page.title.resource.group";%>
</c:when> 
</c:choose>
<html:form onsubmit="selectAllOptions()" action="/resource/${Resource.entityId.typeName}/inventory/EditConfigProperties">
<logic:present name="todash" scope="request">
  <input type="hidden" name="todash" value="1"/>
</logic:present>
<tiles:insert definition="<%=resourceTitle%>">
    <tiles:put name="resource.server.inventory.EditConfigProperties"/>
    <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
</tiles:insert>
<c:if test="${monitorHelp != null}">
<table width="100%">
  <tr>
  <td class="ConfigPropHeaderHelp" colspan="4"><fmt:message key="resource.common.inventory.configProps.EnableMonitoring"/></td>
  </tr>
</table>
</c:if>
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.inventory.configProps.ConfigurationPropertiesTab"/>
</tiles:insert>
<html:hidden property="rid"/>
<html:hidden property="type"/>
<tiles:insert definition=".portlet.error">
</tiles:insert>
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
	<tr>
    <td class="ConfigPropHeader" colspan="4"><fmt:message key="resource.common.inventory.configProps.Shared"/></td>
</tr>
<tr>

<c:if test="${productConfigOptionsCount == 0}">
    <td width="100%" colspan="4"><fmt:message key="resource.common.inventory.configProps.Shared.zeroLength"/></td>
    <td></td>
</c:if>
<logic:iterate id="resourceConfigOption" indexId="ctr" name="org.apache.struts.taglib.html.BEAN"
    property="resourceConfigOptions" >
		<td width="25%" class="BlockLabel"><c:if test="${resourceConfigOption.optional == false}"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/></c:if><bean:write name="resourceConfigOption" property="shortOption"/><br><span class="CaptionText"><c:out value="${resourceConfigOption.description}"/></span></td>
<c:choose>
    <c:when test="${resourceConfigOption.isEnumeration == false && resourceConfigOption.isBoolean == false}">
 <td width="25%" class="BlockContent"><c:choose><c:when test="${resourceConfigOption.isSecret == true }"><input type="password" </c:when><c:otherwise><input type="text" </c:otherwise></c:choose>size="35" name="<c:out value='${resourceConfigOption.option}'/>" value="<c:out value='${resourceConfigOption.value}'/>"></td>
    </c:when>
    <c:when test="${resourceConfigOption.isBoolean == true}">
    <td width="25%" class="BlockContent">
    <input type="checkbox" name="<c:out value='${resourceConfigOption.option}'/>" value="true" <c:if test="${resourceConfigOption.value == true}">checked="checked"</c:if>/> 
    </td>
    </c:when>
    <c:otherwise>
    <td width="25%" class="BlockContent">
      <html:select property="${resourceConfigOption.option}" value="${resourceConfigOption.value}">
      <html:optionsCollection name="resourceConfigOption" property="enumValues"/>
      </html:select>
    </td>
    </c:otherwise>
</c:choose>
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
<logic:iterate id="monitorConfigOption" indexId="ctr" name="org.apache.struts.taglib.html.BEAN"
    property="monitorConfigOptions" >
		<td width="25%" class="BlockLabel"><c:if test="${monitorConfigOption.optional == false}"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/></c:if><bean:write name="monitorConfigOption" property="shortOption"/><br><span class="CaptionText"><c:out value="${monitorConfigOption.description}"/></span></td>
<c:choose>
    <c:when test="${monitorConfigOption.isEnumeration == false && monitorConfigOption.isBoolean == false}">
 <td width="25%" class="BlockContent"><c:choose><c:when test="${monitorConfigOption.isSecret == true }"><input type="password" </c:when><c:otherwise><input type="text" </c:otherwise></c:choose> size="35" name="<c:out value='${monitorConfigOption.option}'/>" value="<c:out value='${monitorConfigOption.value}'/>"></td>
    </c:when>
    <c:when test="${monitorConfigOption.isBoolean == true}">
    <td width="25%" class="BlockContent">
    <input type="checkbox" name="<c:out value='${monitorConfigOption.option}'/>" value="true" <c:if test="${monitorConfigOption.value == true}">checked="checked"</c:if>/> 
    </td>
    </c:when>
    <c:otherwise>
    <td width="25%" class="BlockContent">
      <html:select property="${monitorConfigOption.option}" value="${monitorConfigOption.value}">
      <html:optionsCollection name="monitorConfigOption" property="enumValues"/>
      </html:select>
    </td>
    </c:otherwise>
</c:choose>
    <c:choose> 
    <c:when test="${(ctr+1) % 2 ==0}">
    </tr>
    <tr>
    </c:when>
    </c:choose>
</logic:iterate>
<c:if test="${entityId.type == 3}">
</tr><tr>
<td colspan="4" class="BlockBottomLine"><html:img page="images/spacer.gif" width="1" height="1" border="0"/></td>
<td></td>
</tr><tr>
<c:if test="${rtSupported}">
<td width="25%" class="BlockLabel">
<fmt:message key="resource.common.inventory.configProps.serviceResponseTime"/>
</td>
<td width="25%" class="BlockContent">
<html:radio property="serviceRTEnabled" value="true"/>
<fmt:message key="Yes"/>
<html:radio property="serviceRTEnabled" value="false"/>
<fmt:message key="No"/>
</td>
</tr>
<c:if test="${euRtSupported}">
<tr>
<td width="25%" class="BlockLabel">
<fmt:message key="resource.common.inventory.configProps.euResponseTime"/>
</td>
<td width="25%" class="BlockContent">
<html:radio property="euRTEnabled" value="true"/>
<fmt:message key="Yes"/>
<html:radio property="euRTEnabled" value="false"/>
<fmt:message key="No"/>
</td>
</tr>
</c:if>
</c:if>
<logic:iterate id="rtConfigOption" indexId="ctr" name="org.apache.struts.taglib.html.BEAN"
    property="rtConfigOptions"  >
		<td width="25%" class="BlockLabel"><c:if test="${rtConfigOption.optional == false}"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/></c:if><bean:write name="rtConfigOption" property="shortOption"/><br><span class="CaptionText"><c:out value="${rtConfigOption.description}"/></span></td>
<c:choose>
    <c:when test="${rtConfigOption.isArray == false && rtConfigOption.isEnumeration == false && rtConfigOption.isBoolean == false}">
<td width="25%" class="BlockContent"><c:choose><c:when test="${rtConfigOption.isSecret == true }"><input type="password" </c:when><c:otherwise><input type="text" </c:otherwise></c:choose>size="35" name="<c:out value='${rtConfigOption.option}'/>" value="<c:out value='${rtConfigOption.value}'/>"></td>
    </c:when>
    <c:when test="${rtConfigOption.isArray == true}">
    <td class="BlockContent" >

 <table width="100%" cellspacing="0" cellpadding="0" border="0">
 <tr>
 <td width="100%">
      <html:select multiple="yes" styleClass="multiSelect" size="6" property="${rtConfigOption.option}" value="${rtConfigOption.value}" onchange="replaceButtons(this, '${rtConfigOption.option}')" onclick="replaceButtons(this, '${rtConfigOption.option}')">
      <html:optionsCollection name="rtConfigOption" property="enumValues"/>
      </html:select>
      <td>&nbsp;</td>
      <td width=100% id="<c:out value="${rtConfigOption.option}"/>Nav">
    <div id="<c:out value="${rtConfigOption.option}"/>Up"><html:img page="/images/dash_movecontent_up-off.gif" width="20" height="20" alt="" border="0"/></div>
    <html:img page="/images/spacer.gif" width="1" height="10" border="0"/>
    <div id="<c:out value="${rtConfigOption.option}"/>Down"><html:img page="/images/dash_movecontent_dn-off.gif" width="20" height="20" alt="" border="0"/></div>
    <html:img page="/images/spacer.gif" width="1" height="20" border="0"/>
    <div id="<c:out value="${rtConfigOption.option}"/>Delete"><html:img page="/images/dash_movecontent_del-off.gif" width="20" height="20" alt="" border="0"/></div></td>
    </tr>
    <tr><td colspan="3">&nbsp;</td></tr>
    <tr>
    <td colspan="2">
    <input type="text" size="25" id="<c:out value="${rtConfigOption.option}"/>Content"/></td>
    <td><html:link href="#" onclick="addItem('${rtConfigOption.option}')"><html:img page="/images/dash_movecontent_add-on.gif" width="20" border="0"/></html:link>
    </td>
    </tr>
    </table>

    </td>
  
    </c:when>
</c:choose>
    <c:choose> 
    <c:when test="${(ctr+1) % 2 ==0}">
    </tr>
    <tr valign="top">
    </c:when>
    </c:choose>
</logic:iterate>
</tr>
</c:if>
<c:if test="${serverBasedAutoInventory == 1 && entityId.type ==2}">
<c:if test="${Resource.wasAutodiscovered == false}">
<tr>
<td colspan="4" nowrap class="BlockCheckboxLabel">
<html:checkbox property="serverBasedAutoInventory" value="true"/>&nbsp;
<fmt:message key="resource.common.inventory.configProps.Monitoring.EnableAutoInventoryLabel">
  <fmt:param value="${autodiscoveryMessageServiceList}"/>
</fmt:message>
</td>
</tr>
</c:if>
</c:if>
        </tr>
<c:if test="${controlConfigOptionsCount != null && controlConfigOptionsCount != 0}">
<c:if test="${entityId.type != 1}">
  <tr valign="top">
    <td class="ConfigPropHeader" colspan="4"><fmt:message key="resource.common.inventory.configProps.Control"/></td>
  </tr>
	<tr valign="top">
<c:if test="${controlConfigOptionsCount == 0}">
    <td width="100%" colspan="4"><i><fmt:message key="resource.common.inventory.configProps.Control.zeroLength"/></i></td>
    <td></td>
</c:if>
<logic:iterate id="controlConfigOption" indexId="ctr" name="org.apache.struts.taglib.html.BEAN"
    property="controlConfigOptions" >
		<td width="25%" class="BlockLabel"><c:if test="${controlConfigOption.optional == false}"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/></c:if><bean:write name="controlConfigOption" property="shortOption"/><br><span class="CaptionText"><c:out value="${controlConfigOption.description}"/></span></td>
<c:choose>
    <c:when test="${controlConfigOption.isEnumeration == false && controlConfigOption.isBoolean == false }">
 <td width="25%" class="BlockContent"><c:choose><c:when test="${controlConfigOption.isSecret == true }"><input type="password" </c:when><c:otherwise><input type="text" </c:otherwise></c:choose>size="35" name="<c:out value='${controlConfigOption.option}'/>" value="<c:out value='${controlConfigOption.value}'/>"></td>
    </c:when>
    <c:when test="${controlConfigOption.isBoolean == true}">
    <td width="25%" class="BlockContent">
    <input type="checkbox" name="<c:out value='${controlConfigOption.option}'/>" value="true" <c:if test="${controlConfigOption.value == true}">checked="checked"</c:if>/> 
    </td>
    </c:when>
    <c:otherwise>
    <td width="25%" class="BlockContent">
      <html:select property="${controlConfigOption.option}" value="${controlConfigOption.value}">
      <html:optionsCollection name="controlConfigOption" property="enumValues"/>
      </html:select>
    </td>
    </c:otherwise>
</c:choose>
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
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<tiles:insert definition=".form.buttons"/>
<tiles:insert definition=".page.footer">
<c:if test="${monitorHelp != null}">
  <a name="setup"> </a><c:out value="${monitorHelp}" escapeXml="false"/>
</c:if>
</tiles:insert>
</html:form>
