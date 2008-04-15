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


<tiles:importAttribute name="titleKey" ignore="true"/>
<tiles:importAttribute name="titleName" ignore="true"/>
<tiles:importAttribute name="titleBgStyle" ignore="true"/>
<tiles:importAttribute name="titleImg" ignore="true"/>
<tiles:importAttribute name="subTitleName" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="linkUrl" ignore="true"/>
<tiles:importAttribute name="showSearch" ignore="true"/>

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
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="4">

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitleBar"> 
    <td width="5"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td width="15"><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="85%" colspan="2" nowrap>
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
    <td width="14%"><html:img page="/images/${titleImg}" width="202" height="26" alt="" border="0" style="float: right;"/></td>
    <td><html:img page="/images/spacer.gif" width="20" height="20" alt="" border="0"/></td>
  </c:when>
  <c:otherwise>
    <td width="33%"><html:img page="/images/spacer.gif" width="1" height="26" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="20" height="20" alt="" border="0"/></td>
  </c:otherwise>
</c:choose>
  </tr>
  <tr>
    <td width="5" rowspan="99" class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td colspan="4"><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
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
    <td>
      <!--  SEARCH TOOLBAR CONTENTS -->
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td nowrap class="SearchBold"><fmt:message key="resource.hub.search.label.Search"/></td>
          <td class="SearchRegular"><html:text property="keywords" onfocus="ClearText(this)" value="${initSearchVal}" size="30" maxlength="40"/></td>
          <td class="SearchRegular" width="100%"><html:image page="/images/4.0/icons/accept.png" property="ok"/></td>
        </tr>
      </table>
      <!--  /  -->
    </td>
    </c:when>
    <c:otherwise>
    <td class="PageTitleSmallText">&nbsp;</td>
    <td>&nbsp;</td>
    </c:otherwise>
  </c:choose>

  <td colspan="2" style="padding-top:4px;padding-bottom:4px;">
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
  <tr> 
    <td colspan="4"><html:img page="/images/spacer.gif" height="10" alt="" border="0"/></td>
  </tr>
</table>

    </td>
  </tr>
  <tr>
    <td class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="20" height="1" alt="" border="0"/></td>
    <td width="100%">
