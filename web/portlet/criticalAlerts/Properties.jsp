<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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


<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="resources"/>
<c:url var="selfAction" value="/dashboard/Admin.do?mode=criticalAlerts"/>


<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
var help = '<hq:help/>';
</script>
<script type="text/javascript">
/***********************************************/
/* Disable "Enter" key in Form script- By Nurul Fadilah(nurul@REMOVETHISvolmedia.com)
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

</script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="35%" class="PageTitle" nowrap><fmt:message key="dash.home.RecentAlerts.Title"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td ><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='3'>
      <html:form action="/dashboard/ModifyCriticalAlerts" >

      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
      </tiles:insert>

      <tiles:insert definition=".dashContent.admin.generalSettings">
        <tiles:put name="portletName" beanName="portletName" />
      </tiles:insert>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
         <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="common.label.Description"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
            <c:choose>
              <c:when test="${not sessionScope.modifyDashboard}">
                <c:out value="${CriticalAlertsForm.title}"/>
              </c:when>
              <c:otherwise>
                <html:text property="title" maxlength="50" onkeypress="return handleEnter(this, event);"/>
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
         <tr valign="top">
          <td class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.AlertRange"/></td>
             <td class="BlockContent" colspan="3" valign="center">
                 <fmt:message key="dash.settings.criticalAlerts.last"/>
                 &nbsp;
                 <c:choose>
                     <c:when test="${not sessionScope.modifyDashboard}">
                         <c:out value="${CriticalAlertsForm.numberOfAlerts}"/>
                     </c:when>
                     <c:otherwise>
                         <html:select property="numberOfAlerts">
                             <html:option value="5"/>
                             <html:option value="10"/>
                             <html:option value="20"/>
                             <html:option value="30"/>
                         </html:select>
                     </c:otherwise>
                 </c:choose>
                 <c:choose>
                     <c:when test="${not sessionScope.modifyDashboard}">
						<c:if test="${CriticalAlertsForm.priority eq '3'}">
						  !!! - High
						</c:if>
						<c:if test="${CriticalAlertsForm.priority eq '2'}">
						  !! - Medium
						</c:if>
						<c:if test="${CriticalAlertsForm.priority eq '1'}">
						  ! - Low 
						</c:if>
						<c:if test="${CriticalAlertsForm.priority eq '0'}">
						  ALL
						</c:if>
                     </c:when>
                     <c:otherwise>
                         <html:select property="priority">
                             <html:option value="3">!!! - High</html:option>
                             <html:option value="2">!! - Medium</html:option>
                             <html:option value="1">! - Low</html:option>
                             <html:option value="0">ALL</html:option>
                         </html:select>
                     </c:otherwise>
                 </c:choose>
                 &nbsp;
                 <fmt:message key="dash.settings.criticalAlerts.withinThePast"/>
                 <c:choose>
                     <c:when test="${not sessionScope.modifyDashboard}">
						<c:if test="${CriticalAlertsForm.past eq 1800000}">30
						    <fmt:message key="admin.settings.Minutes"/>
						</c:if>
						<c:if test="${CriticalAlertsForm.past eq 3600000}">
						    <fmt:message key="admin.settings.Hour"/>
						</c:if>
						<c:if test="${CriticalAlertsForm.past eq 43200000}">12
						   <fmt:message key="admin.settings.Hours"/>
						</c:if>
						<c:if test="${CriticalAlertsForm.past eq 86400000}">
						   <fmt:message key="admin.settings.Day"/>
						</c:if>
						<c:if test="${CriticalAlertsForm.past eq 604800000}">
						   <fmt:message key="admin.settings.Week"/>
						</c:if>
						<c:if test="${CriticalAlertsForm.past eq 2419200000}">
						   <fmt:message key="admin.settings.Month"/>
						</c:if>
                     </c:when>
                     <c:otherwise>
                         <html:select property="past">
                             <html:option value="1800000">30
                                 <fmt:message key="admin.settings.Minutes"/>
                             </html:option>
                             <html:option value="3600000">
                                 <fmt:message key="admin.settings.Hour"/>
                             </html:option>
                             <html:option value="43200000">12
                                 <fmt:message key="admin.settings.Hours"/>
                             </html:option>
                             <html:option value="86400000">
                                 <fmt:message key="admin.settings.Day"/>
                             </html:option>
                             <html:option value="604800000">
                                 <fmt:message key="admin.settings.Week"/>
                             </html:option>
                             <html:option value="2419200000">
                                 <fmt:message key="admin.settings.Month"/>
                             </html:option>
                         </html:select>
                     </c:otherwise>
                 </c:choose>
                 &nbsp;
                 <fmt:message key="dash.settings.criticalAlerts.for"/>
                 &nbsp;
                 <c:choose>
                     <c:when test="${not sessionScope.modifyDashboard}">
                         <c:out value="${CriticalAlertsForm.selectedOrAll}"/>&nbsp;resources
                     </c:when>
                     <c:otherwise>
                         <html:select property="selectedOrAll">
                             <html:option value="selected">selected resources</html:option>
                             <html:option value="all">all resources</html:option>
                         </html:select>
                     </c:otherwise>
                 </c:choose>
                 &nbsp;
                 <fmt:message key="dash.settings.criticalAlerts.period"/>
             </td>
        </tr>
        <tr>
          <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
        <tr>
          <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
      &nbsp;<br>
      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.SelectedResources"/>
      </tiles:insert>

      <display:table cellspacing="0" cellpadding="0" width="100%" action="${selfAction}"
                     pageSize="${pageSize}" items="${criticalAlertsList}" var="resource"  >

        <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">          
          <display:checkboxdecorator name="ids" value="${resource.entityId.type}:${resource.id}" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
        </display:column>
      
        <display:column width="50%" property="name" sort="true" sortAttr="7"
                  defaultSort="true" title="dash.settings.ListHeader.Resource" /> 

        <display:column width="50%" property="description"
                        title="common.header.Description" /> 

      </display:table>

      <c:set var="addToListUrl" value="/dashboard/Admin.do?mode=criticalAlertsAddResources&key=.dashContent.criticalalerts.resources"/> 
      <c:if test="${not empty CriticalAlertsForm.token}">
        <c:set var="addToListUrl" value="/dashboard/Admin.do?mode=criticalAlertsAddResources&key=.dashContent.criticalalerts.resources${CriticalAlertsForm.token}&token=${CriticalAlertsForm.token}"/> 
      </c:if>
      <c:choose>
          <c:when test="${not sessionScope.modifyDashobard}">
           <html:image page="/images/tbb_addtolist_locked.gif" border="0" property="" /> 
          </c:when>
          <c:otherwise>
              <tiles:insert definition=".toolbar.addToList">
                  <tiles:put name="addToListUrl">
                      <c:out value="${addToListUrl}"/>
                  </tiles:put>
                  <tiles:put name="listItems" beanName="criticalAlertsList"/>
                  <tiles:put name="listSize" beanName="criticalAlertsList" beanProperty="totalSize"/>
                  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
                  <tiles:put name="pageSizeAction" beanName="selfAction"/>
                  <tiles:put name="pageNumAction" beanName="selfAction"/>
                  <tiles:put name="defaultSortColumn" value="1"/>
              </tiles:insert>
          </c:otherwise>
      </c:choose>
	  <tiles:insert definition=".form.buttons">
		 <c:if test='${not sessionScope.modifyDashboard}'>
		   <tiles:put name="noReset" value="true"/>
		   <tiles:put name="noCancel" value="true"/>
		 </c:if>
	  </tiles:insert>
      <html:hidden property="token"/>
      </html:form>
      
    </td>
  </tr>
  <tr> 
    <td colspan="4"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>

