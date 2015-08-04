<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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


<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="resources"/>
<c:set var="listSize" value="${fn:length(availSummaryList)}" />

<c:url var="selfAction" value="availSummaryPortletControl.action">
 	<c:if test="${not portletIdentityToken}">
 		<c:param name="token" value="${portletIdentityToken}"/>
 	</c:if>
</c:url>

<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	var help = '<hq:help/>';

	/***********************************************/
	/* Disable "Enter" key in Form script- By Nurul Fadilah
	/* This notice must stay intact for use
	/* Visit http://www.dynamicdrive.com/ for full source code
	/***********************************************/
	
	function handleEnter (field, event) {
			var keyCode = event.keyCode ? event.keyCode : event.which ? event.which : event.charCode;
			if (keyCode == 13) {
				var i;
				for (i = 0; i < field.form.elements.length; i++)
					if (field == field.form.elements[i])
						break;
				//i = (i + 1) % field.form.elements.length;
				//field.form.elements[i].focus();
				return false;
			}
			else
			return true;
		}
</jsu:script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle">
    <td rowspan="99"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0" /></td>
    <td><img src='<s:url value="/images/spacer.gif"/>' width="15" height="1" alt="" border="0" /></td>
    <td width="35%" class="PageTitle" nowrap><fmt:message key="dash.home.AvailSummary.Title"/></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt="" border="0" /></td>
    <td width="32%"><img src='<s:url value="/images/spacer.gif"/>' width="202" height="32" alt="" border="0" /></td>
    <td width="1%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt="" border="0" /></td>
  </tr>
  <tr>
    <td valign="top" align="left" rowspan="99"></td>
    <td ><img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" alt="" border="0" /> </td>
  </tr>
  <tr valign="top">
    <td colspan='3'>
	  <s:form action="updateAvailSummaryModifyPortlet" >
	  <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.DisplaySettings"/>
		<tiles:putAttribute name="portletName" value=""/>
      </tiles:insertDefinition>
	  
	  <tiles:insertDefinition name=".ng.dashContent.admin.generalSettings">
        <tiles:putAttribute name="portletName" value="${portletName}"/>
      </tiles:insertDefinition>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0" /></td>
        </tr>
         <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="common.label.Description"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
			 <s:textfield theme="simple" name="title" maxlength="50" onkeypress="return handleEnter(this, event);" disabled="%{#!attr.modifyDashboard}" value="%{#attr.titleDescription}" />
          </td>
        </tr>
         <tr valign="top">
          <td class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.AvailSummaryRange"/></td>
          <td class="BlockContent" colspan="3" valign="center">
             <fmt:message key="dash.settings.availSummary.top"/>&nbsp;
			 <s:select theme="simple" name="numberToShow" disabled="%{#!attr.modifyDashboard}"  list="#{ '5':'5', '10':'10', '20':'20','30':'30' }" value="%{#attr.numberToShow}"  />&nbsp;<fmt:message key="dash.settings.availSummary.resources"/>
          </td>
        </tr>
        <tr>
          <td colspan="4" class="BlockContent"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0" /> </td>
        </tr>
        <tr>
          <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0" /> </td>
        </tr>
      </table>
      &nbsp;<br>

	  <tiles:insertDefinition name=".header.tab">
        <tiles:putAttribute name="tabKey" value="dash.settings.SelectedResources"/>
		<tiles:putAttribute name="portletName" value=""/>
      </tiles:insertDefinition>
	  
      <display:table cellspacing="0" cellpadding="0" width="100%" action="${selfAction}"
                     pageSize="${pageSize}" items="${availSummaryList}" var="resource"  >

        <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
          <display:checkboxdecorator name="ids" value="${resource.entityId.type}:${resource.id}" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
        </display:column>

        <display:column width="50%" property="name" sort="true" sortAttr="7"
                  defaultSort="true" title="dash.settings.ListHeader.Resource" />

        <display:column width="50%" property="description"
                        title="common.header.Description" />

      </display:table>
		
      <c:url var="availAddToListUrl" value="availSummaryAddResourcesPortletControl.action" >
          <c:param name="mode" value="availSummaryAddResources"/>
          <c:if test="${not empty portletIdentityToken}">
            <c:param name="key" value=".ng.dashContent.availsummary.resources${portletIdentityToken}"/>
            <c:param name="token" value="${portletIdentityToken}"/>
          </c:if>
          <c:if test="${empty portletIdentityToken}">
            <c:param name="key" value=".ng.dashContent.availsummary.resources"/>
          </c:if>
      </c:url>

	  
      <c:choose>
          <c:when test="${not sessionScope.modifyDashboard}">
          </c:when>
          <c:otherwise>
	  
			  <tiles:insertDefinition name=".ng.toolbar.addToList">
                      <tiles:putAttribute name="addToListUrl" value="${availAddToListUrl}"  />
                      <tiles:putAttribute name="listItems" value="${availSummaryList}"/>
                      <tiles:putAttribute name="listSize" value="${listSize}" />
                      <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
                      <tiles:putAttribute name="pageSizeAction" value="${selfAction}"/>
                      <tiles:putAttribute name="pageNumAction" value="${selfAction}"/>
                      <tiles:putAttribute name="defaultSortColumn" value="1"/>
					  <tiles:putAttribute name="showPagingControls" value="false"/>
               </tiles:insertDefinition>
          </c:otherwise>
      </c:choose>

	  <tiles:insertDefinition name=".form.buttons">
		  <c:if test='${sessionScope.modifyDashboard}'>
			<tiles:putAttribute name="cancelAction"  value="cancelAvailSummaryModifyPortlet" />
			<tiles:putAttribute name="resetAction"   value="resetAvailSummaryModifyPortlet" />
		  </c:if>
      </tiles:insertDefinition>
	  
      <s:hidden theme="simple" name="token"/>
      </s:form>

    </td>
  </tr>
  <tr>
    <td colspan="4"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="13" alt="" border="0" /></td>
  </tr>
</table>

