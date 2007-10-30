<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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
<c:set var="widgetInstanceName" value="savedQueriesList"/>
<c:url var="selfAction" value="/dashboard/Admin.do?mode=savedQueries"/>

<script language="JavaScript" src="<html:rewrite page="/js/"/>dashboard_SummaryCounts.js" type="text/javascript"></script>
<script type="text/javascript">
  var help = '<hq:help/>';
</script>


<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle"> 
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="67%" class="PageTitle" nowrap><fmt:message key="dash.home.SummaryCounts.Title"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" width="20" height="20" alt="" border="0" hspace="10"/></html:link></td>
  </tr>
  <tr> 
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td colspan='2'><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top"> 
    <td colspan='2'>
    <html:form action="/dashboard/ModifySummaryCounts.do">

    <!-- Content Block Title: Display Settings -->
    <tiles:insert definition=".header.tab">
      <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
    </tiles:insert>

    <tiles:insert definition=".dashContent.admin.generalSettings">
      <tiles:put name="portletName" beanName="portletName" />
    </tiles:insert>
    <!-- Display Settings Content: the text is static, all boxes should be checked by default -->
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
      </tr>
       <tr valign="top">
        <td width="20%" class="BlockLabel"><fmt:message key="dash.settings.FormLabel.SummaryCounts"/></td>
        <td width="30%" class="BlockContent">                
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="application" styleClass="applicationParent" onclick="checkParent(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.AppShowTotal"/></td>
            </tr>
            <c:if test="${summary.appTypeMap != null}">
            <tr>
              <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
              <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="applicationCheckAll" onclick="ToggleAll(this, 'application')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.AppCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="applicationCheckAll" onclick="ToggleAll(this, 'application')" /><b><fmt:message key="dash.home.DisplayCategory.AppCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
              <td width="100%">
              <c:forEach var="type" items="${summary.appTypeMap}">
                <html:multibox property="applicationTypes" value="${type.key}" styleClass="application" onclick="checkChild(this)" disabled="${not sessionScope.modifyDashboard}"/><c:out value="${type.key}"/><br>
              </c:forEach>             
                &nbsp;
              </td>
            </tr>
            </c:if>
          </table>
          
          
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="platform" styleClass="platformParent" onclick="checkParent(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.PlatformShowTotal"/></td>
            </tr>
            <c:if test="${summary.platformTypeMap != null}">
            <tr>
              <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
              <td width="100%">
              <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="platformCheckAll" onclick="ToggleAll(this, 'platform')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.PlatformCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="platformCheckAll" onclick="ToggleAll(this, 'platform')"/><b><fmt:message key="dash.home.DisplayCategory.PlatformCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
              <td width="100%">
              <c:forEach var="type" items="${summary.platformTypeMap}">
                <html:multibox property="platformTypes" value="${type.key}" styleClass="platform" onclick="checkChild(this)" disabled="${not sessionScope.modifyDashboard}"/><c:out value="${type.key}"/><br>
              </c:forEach>           
                &nbsp;
              </td>
            </tr>
            </c:if>
          </table>
          
          
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tr>
              <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="server" styleClass="serverParent" onclick="checkParent(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.ServerShowTotal"/></td>
            </tr>
            <c:if test="${summary.serverTypeMap != null}">
            <tr>
              <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
              <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="serverCheckAll" onclick="ToggleAll(this, 'server')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.ServerCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="serverCheckAll" onclick="ToggleAll(this, 'server')"/><b><fmt:message key="dash.home.DisplayCategory.ServerCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
              <td width="100%">
                <c:forEach var="type" items="${summary.serverTypeMap}">
                  <html:multibox property="serverTypes" value="${type.key}" styleClass="server" onclick="checkChild(this)" disabled="${not sessionScope.modifyDashboard}"/><c:out value="${type.key}"/><br>            
                </c:forEach>
                &nbsp;
              </td>
            </tr>
            </c:if>
          </table>
                      
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="service" styleClass="serviceParent" onclick="checkParent(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.ServiceShowTotal"/></td>
              </tr>
              <c:if test="${summary.serviceTypeMap!= null}">
              <tr>
                <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
                <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                  <input type="checkbox" class="serviceCheckAll" onclick="ToggleAll(this, 'service')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.ServiceCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                   <input type="checkbox" class="serviceCheckAll" onclick="ToggleAll(this, 'service')"/><b><fmt:message key="dash.home.DisplayCategory.ServiceCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
                </td>
              </tr>
              <tr>
                <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
                <td width="100%">
                  <c:forEach var="type" items="${summary.serviceTypeMap}">
                    <html:multibox property="serviceTypes" value="${type.key}" styleClass="service" onclick="checkChild(this)" disabled="${not sessionScope.modifyDashboard}"/><c:out value="${type.key}"/><br>                
                  </c:forEach>
                  &nbsp;
                </td>
              </tr>
              </c:if>
            </table>
            
                        
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="cluster" styleClass="clusterParent" onclick="checkParent(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.group.ClusterShowTotal"/></td>
              </tr>              
              <tr>
                <td><html:img page="/images/spacer.gif" width="20" height="30" border="0"/></td>                
              </tr>                        
            </table>            
            
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <td colspan="2" width="100%" class="FormLabel"><html:checkbox property="groupMixed" styleClass="groupMixedParent" onclick="checkParent(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.group.mixedGroups"/></td>
              </tr>              
              <tr>
                <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
                <td width="100%">
                <c:choose>
                <c:when test="${not sessionScope.modifyDashboard}">
                    <input type="checkbox" class="groupMixedCheckAll" onclick="ToggleAll(this, 'groupMixed')" disabled="true"/><b><fmt:message key="dash.home.DisplayCategory.MixedGroupCheckAll"/></b><br>&nbsp;
                </c:when>
                <c:otherwise>
                    <input type="checkbox" class="groupMixedCheckAll" onclick="ToggleAll(this, 'groupMixed')"/><b><fmt:message key="dash.home.DisplayCategory.MixedGroupCheckAll"/></b><br>&nbsp;
                </c:otherwise>
                </c:choose>
                </td>
              </tr>
              <tr>
                <td><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>                
                <td width="100%">
                  <html:checkbox property="groupGroups" styleClass="groupMixed" onclick="checkChild(this)" disabled="${not sessionScope.modifyDashboard}" /><fmt:message key="dash.home.DisplayCategory.group.groups"/><br>
                  <html:checkbox property="groupPlatServerService" styleClass="groupMixed" onclick="checkChild(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.group.plat.server.service"/><br>
                  <html:checkbox property="groupApplication" styleClass="groupMixed" onclick="checkChild(this)" disabled="${not sessionScope.modifyDashboard}"/><fmt:message key="dash.home.DisplayCategory.group.application"/><br>
                </td>
              </tr>                        
            </table>
            
          </td>
          <td width="50%" class="BlockLabel">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
      </tr>
    </table>
    <tiles:insert definition=".form.buttons">
      <c:if test='${not sessionScope.modifyDashboard}'>
        <tiles:put name="noReset" value="true"/>
        <tiles:put name="noCancel" value="true"/>
      </c:if>
    </tiles:insert>
    </html:form>
    </td>
  </tr>
  <tr> 
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>
