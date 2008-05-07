<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
<%@ taglib uri="hq" prefix="hq" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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


<tiles:importAttribute name="titleKey" ignore="true"/>
<tiles:importAttribute name="titleName" ignore="true"/>
<tiles:importAttribute name="titleBgStyle" ignore="true"/>
<tiles:importAttribute name="titleImg" ignore="true"/>
<tiles:importAttribute name="subTitleName" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="linkUrl" ignore="true"/>
<tiles:importAttribute name="showSearch" ignore="true"/>

<hq:constant var="PLATFORM"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM"/>
<hq:constant var="SERVER"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER"/>
<hq:constant var="SERVICE" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE"/>
<hq:constant var="APPLICATION"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION"/>
<hq:constant var="GROUP" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP"/>
<hq:constant var="GROUP_COMPAT" 
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalAction"
    symbol="SELECTOR_GROUP_COMPAT"/>
<hq:constant var="GROUP_ADHOC" 
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubPortalAction"
    symbol="SELECTOR_GROUP_ADHOC"/>
<hq:constant var="CHART"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubForm"
	symbol="CHART_VIEW"/>
<hq:constant var="LIST"
    classname="org.hyperic.hq.ui.action.resource.hub.ResourceHubForm"
	symbol="LIST_VIEW"/>

<c:if test="${not empty resourceOwner}">
  <hq:owner var="ownerStr" owner="${resourceOwner}"/>
</c:if>
<c:if test="${showSearch}">
    <c:choose>
    <c:when test="${not empty ResourceHubForm.keywords}">
      <c:set var="initSearchVal" value="${ResourceHubForm.keywords}"/>
    </c:when>
    <c:otherwise>
      <fmt:message var="initSearchVal" key="resource.hub.search.KeywordSearchText"/>
    </c:otherwise>
  </c:choose>
</c:if>
 <script  type="text/javascript">
  var help = "<hq:help/>";
</script>
<table width="100%" cellspacing="0" cellpadding="0" style="border: 0px;">
  <tr>
    <td colspan="4">

<table width="100%" border="0" cellspacing="0" cellpadding="0" style="border: 0px; padding-bottom: 10px;">
  <tr class="PageTitleBar"> 
    <td width="5"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td width="15"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td colspan="2" nowrap>
<c:choose>
  <c:when test="${not empty titleKey}">
    <fmt:message key="${titleKey}">
      <c:if test="${not empty titleName}">
        <fmt:param value="${titleName}"/>
      </c:if>
      <c:if test="${not empty subTitleName}">
        <fmt:param value="${subTitleName}"/>
      </c:if>
    </fmt:message>
  </c:when>
  <c:otherwise>
    <c:out value="${titleName}" escapeXml="false"/>
      <c:if test="${not empty subTitleName}">
        <c:out value="${subTitleName}"/>
      </c:if>
  </c:otherwise>
</c:choose>
    </td>
<c:choose>
  <c:when test="${not empty titleBgStyle && not empty titleImg}">
    <td width="15%" style="padding-right: 20px;"><html:img page="/images/${titleImg}" width="202" height="26" alt="" border="0" style="float: right;"/></td>
  </c:when>
  <c:otherwise>
    <td width="10%" style="padding-right: 20px;">&nbsp;</td>
  </c:otherwise>
</c:choose>
  </tr>
  <tr>
    <td rowspan="99" class="PageTitle">&nbsp;</td>
    <td valign="top" align="left" rowspan="99">&nbsp;</td>
    <td colspan="3">&nbsp;</td>
  </tr>
<c:if test="${not empty resource || not empty linkUrl || not empty showSearch}">
  <tr valign="top"> 
  <c:choose>
    <c:when test="${not empty resource}">
    <td colspan="2">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <td class="PageTitleSmallText" valign="top">
      <b><fmt:message key="common.label.Description"/></b>
      <hq:shortenText maxlength="30" value="${resource.description}" styleClass="ListCellPopup5"/>
    </td>
    <td style="width: 5px;">&nbsp;</td>
    <td class="PageTitleSmallText" valign="top" colspan="2" nowrap>
      <b><fmt:message key="resource.common.inventory.props.OwnerLabel"/></b> <c:out value="${ownerStr}" escapeXml="false"/> 
      <c:if test="${not empty resource}">
        - <html:link page="/resource/${resource.entityId.typeName}/Inventory.do?mode=changeOwner&rid=${resource.id}&type=${resource.entityId.type}"><fmt:message key="resource.common.inventory.props.ChangeButton"/></html:link><br>
      </c:if>
          </td>
        </tr>
<logic:present name="cprops">
<c:set var="leftRight" value="1"/>
<logic:iterate id="cprop" name="cprops">
  <c:if test="${leftRight > 0}">
    <tr>
  </c:if>
      <td class="PageTitleSmallText" width="33%" valign="top"><b><c:out value="${cprop.key}"/></b><fmt:message key="common.label.Colon"/>
      <hq:shortenText maxlength="30" value="${cprop.value}" styleClass="ListCellPopup5"/></td>
  <c:choose>
  <c:when test="${leftRight < 0}">
    <c:set var="leftRight" value="${leftRight * -1}"/>
    <td>&nbsp;</td>
    </tr>
  </c:when>
  <c:otherwise>
    <c:set var="leftRight" value="${leftRight - 1}"/>
    <td>&nbsp;</td>
  </c:otherwise>
  </c:choose>
</logic:iterate>
<c:if test="${leftRight < 0}">
    <td colspan="3">&nbsp;</td>
  </tr>
</c:if>
</logic:present>
      <c:if test="${empty ResourceType}">
        <tr><td colspan="3">
        <tiles:insert definition=".resource.common.navmap">
          <tiles:put name="resource" beanName="resource"/>
        </tiles:insert>
        </td></tr>
      </c:if>
      </table>
    </td>
    </c:when>
    <c:when test="${showSearch}">
    <td style="vertical-align: middle;">
<c:choose>
  <c:when test="${ResourceHubForm.ff == PLATFORM}">
    <c:set var="allTypesKey" value="resource.hub.filter.AllPlatformTypes"/>
    <c:set var="section" value="platform"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == SERVER}">
    <c:set var="allTypesKey" value="resource.hub.filter.AllServerTypes"/>
    <c:set var="section" value="server"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == SERVICE}">
    <c:set var="allTypesKey" value="resource.hub.filter.AllServiceTypes"/>
    <c:set var="section" value="service"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == APPLICATION}">
    <c:set var="section" value="application"/>
  </c:when>
  <c:when test="${ResourceHubForm.ff == GROUP}">
    <c:set var="allTypesKey" value="resource.hub.filter.AllGroupTypes"/>
    <c:set var="section" value="group"/>
  </c:when>
</c:choose>

      <!--  SEARCH TOOLBAR CONTENTS -->
    <!--  SEARCH TOOLBAR CONTENTS -->
      <div style="position: absolute; width: 50px;font-size: 11px; font-weight: 700; ">
      <fmt:message key="resource.hub.Search"/>
      </div>
      <div style="width: 600px; padding-left: 50px;">
      <div>
      <html:text property="keywords" size="15" maxlength="40" onfocus="ClearText(this)" value="${initSearchVal}"/>
    <c:choose>
    <c:when test="${empty allTypesKey}">
      <html:hidden property="ft" value=""/>&nbsp;
    </c:when>
    <c:otherwise>
      <span style="padding-left: 4px;">
      <html:select property="ft" styleClass="FilterFormText" size="1">
        <html:option value="" key="${allTypesKey}"/>
        <html:optionsCollection property="types"/>
      </html:select>
      </span>
      <c:if test="${not empty AvailableResGrps}">
      <span style="padding-left: 4px;">
        <html:select property="fg" styleClass="FilterFormText">
          <html:option value="" key="resource.hub.filter.AllGroupOption"/>
          <html:optionsCollection name="AvailableResGrps"/>
        </html:select>
      </span>
      </c:if>
    </c:otherwise>
    </c:choose>
    </div>
    <div style="padding: 4px;">
      <html:checkbox property="unavail" value="true"/>
      <fmt:message key="resource.hub.legend.unavailable"/>
    <span style="padding: 6px;">
      <html:checkbox property="own" value="true"/>
      <fmt:message key="resource.hub.search.label.Owned">
        <fmt:param><c:out value="${sessionScope.webUser.name}"/></fmt:param>
      </fmt:message>
    </span>
    <span style="background-color: #D5D8DE; padding: 6px;">
    <fmt:message key="resource.hub.search.label.Match"/>
    <html:radio property="any" value="true"/> <fmt:message key="any"/>
    <html:radio property="any" value="false"/> <fmt:message key="all"/>
    <html:image page="/images/4.0/icons/accept.png" property="ok" style="padding-left: 6px; vertical-align: text-bottom;"/>
    </span>
    </div>
    </div>

      <!--  /  -->
    </td>
    </c:when>
    <c:otherwise>
    <td class="PageTitleSmallText" colspan="2">&nbsp;</td>
    </c:otherwise>
  </c:choose>

  <td style="padding: 4px;">
    <c:if test="${not empty linkUrl}">
      <span class="LinkBox" onclick="toggleMenu('toolMenu');" id="toolMenuSpan"><fmt:message key="resource.toolsmenu.text"/><html:img page="/images/arrow_dropdown.gif" styleId="toolMenuSpan" border="0"/></a></span>
    <div style="clear: both"></div>
    <div id="toolMenu" style="display: none; position: absolute; margin-top: 2px; margin-left: -2px;z-index:5">
<tiles:insert attribute="linkUrl">
  <c:if test="${not empty resource}">
    <tiles:put name="resource" beanName="resource"/>
  </c:if>
</tiles:insert>
      </div>
    </c:if>
    </td>
  </tr>
</c:if>
</table>

    </td>
  </tr>
  <tr>
    <td class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="20" height="1" alt="" border="0"/></td>
    <td width="100%">
