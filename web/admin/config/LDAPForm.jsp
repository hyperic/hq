<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<hq:constant classname="org.hyperic.hq.common.shared.HQConstants" symbol="JDBCJAASProvider" var="camProvider"/>
<hq:constant classname="org.hyperic.hq.common.shared.HQConstants" symbol="LDAPJAASProvider" var="ldapProvider"/>

<hq:config var="ldapAuth" prop="CAM_JAAS_PROVIDER" value="${ldapProvider}"/>
<hq:config var="camAuth" prop="CAM_JAAS_PROVIDER" value="${camProvider}"/>

<!--  LDAP CONFIG PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.settings.LDAPConfigPropTab"/>  
</tiles:insert>
<!--  /  -->

<!--  LDAP CONFIG PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockCheckboxLabel" align="left" colspan="4"><html:checkbox property="ldapEnabled" /><fmt:message key="admin.settings.UseLDAPAuth"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPUrlLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapUrl"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPSslLabel"/></td>
    <td width="30%" class="BlockContent"><html:checkbox property="ldapSsl"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPUsernameLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapUsername"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Password"/></td>
    <td width="30%" class="BlockContent"><html:password size="31" property="ldapPassword" redisplay="true"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPSearchBaseLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapSearchBase"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPSearchFilterLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapSearchFilter"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.LDAPLoginPropertyLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="ldapLoginProperty"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>

  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->
