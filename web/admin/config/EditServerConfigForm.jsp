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


<tiles:importAttribute name="showRt" ignore="true"/>

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="MINUTES_LABEL" var="CONST_MINUTES" />
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="HOURS_LABEL" var="CONST_HOURS" />
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="DAYS_LABEL" var="CONST_DAYS" />

  <logic:messagesPresent>
  <table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorField"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <td colspan="3" align="left" class="ErrorField"><html:errors/></td>
  </tr>
  </table>
  </logic:messagesPresent>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockContent"><fmt:message key="admin.settings.RestartNote"/></td>
  </tr>
</table>

<!--  EMAIL CONFIG TITLE -->
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.settings.EmailConfigTab"/>  
</tiles:insert>
<!--  /  -->

<!--  EMAIL CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  <tr>
    <td width="30%" class="BlockLabel"><fmt:message key="admin.settings.BaseURL"/></td>
    <td width="40%" class="BlockContent"><html:text size="31" property="baseUrl" /></td>
    <td width="30%" class="BlockContent" colspan="2"></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.SenderEmailAddressLabel"/></td>
    <td class="BlockContent"><html:text size="31" property="senderEmail" /></td>
    <td class="BlockContent" colspan="2"></td>
  </tr>
</table>
<!--  /  -->

<!--  UPDATE CONFIG TITLE -->
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.settings.UpdateConfigTab"/>  
</tiles:insert>
<!--  /  -->

<!--  UPDATE CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  <tr>
    <td width="30%" class="BlockLabel"><fmt:message key="admin.settings.AnnouncementType"/></td>
    <td width="40%" class="BlockContent">
    <html:radio property="updateMode" value="0"/><fmt:message key="admin.settings.All"/>
    <html:radio property="updateMode" value="1"/><fmt:message key="admin.settings.Major"/>
    <html:radio property="updateMode" value="2"/><fmt:message key="common.label.None"/>
    </td>
    <td width="30%" class="BlockContent" colspan="2"></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.Help"/></td>
    <td class="BlockContent" colspan="3">
    <html:radio property="externDocs" value="true"/><fmt:message key="admin.settings.Help.External"/>
    <html:radio property="externDocs" value="false"/><fmt:message key="admin.settings.Help.Internal"/>
    </td>
  </tr>
<!--  /  -->

<!--  MAINTENANCE WINDOW CONFIG TITLE -->
  <tr>
    <td colspan="4" class="BlockHeader">
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.settings.MaintenanceConfigTab"/>  
</tiles:insert>
    </td>
  </tr>
<!--  /  -->

<!--  MAINTENANCE WINDOW CONFIG CONTENTS -->
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.AlertsEnabled"/></td>
    <td class="BlockContent" colspan="3">
    <html:radio property="alertsAllowed" value="true"/><fmt:message key="yesno.true"/>
    <html:radio property="alertsAllowed" value="false"/><fmt:message key="yesno.false"/>
    </td>
  </tr>
<!--  /  -->

<!--  DATA MANAGER CONFIG TITLE -->
  <tr>
    <td colspan="4" class="BlockHeader">
<tiles:insert definition=".header.tab">  
  <tiles:put name="tabKey" value="admin.settings.DataMangerConfigTab"/>  
</tiles:insert>
    </td>
  </tr>
<!--  /  -->

<!--  DATA MANAGER CONFIG CONTENTS -->
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.DataMaintInterval"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="maintIntervalVal">
          <td class="ErrorField">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="ErrorField" width="100%"><fmt:message key="admin.settings.Hours"/>
            <html:hidden property="maintInterval" value="${CONST_HOURS}"/>
          </td>
</logic:messagesPresent>          
<logic:messagesNotPresent property="maintIntervalVal">
          <td class="BlockContent">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="BlockContent" width="100%"><fmt:message key="admin.settings.Hours"/>
            <html:hidden property="maintInterval" value="${CONST_HOURS}"/>
          </td>
</logic:messagesNotPresent>          
        </tr>
<logic:messagesPresent property="maintIntervalVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="maintIntervalVal"/></span>            
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="maintIntervalVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"/>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.PurgeOlderThanLabel"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="deleteUnitsVal">
          <td class="ErrorField">
            <html:text size="2" property="deleteUnitsVal" />
          </td>
          <td class="ErrorField" width="100%"><fmt:message key="admin.settings.Days"/>
            <html:hidden property="deleteUnits" value="${CONST_DAYS}"/>
          </td>
</logic:messagesPresent>          
<logic:messagesNotPresent property="deleteUnitsVal">
          <td class="BlockContent">
            <html:text size="2" property="deleteUnitsVal" />
          </td>
          <td class="BlockContent" width="100%"><fmt:message key="admin.settings.Days"/>
            <html:hidden property="deleteUnits" value="${CONST_DAYS}"/>
          </td>
</logic:messagesNotPresent>          
        </tr>
<logic:messagesPresent property="deleteUnitsVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="deleteUnitsVal"/></span>            
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="deleteUnitsVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"/>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.Reindex"/></td>
    <td class="BlockLabel" align="left">
      <div style="float:left;">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td align="left"><html:radio property="reindex" value="true"/><fmt:message key="yesno.true"/></td>
          <td align="left"><html:radio property="reindex" value="false"/><fmt:message key="yesno.false"/></td>
        </tr>
      </table>
      </div>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.AlertPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="alertPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="alertPurgeVal" />
          </td>
          <td class="ErrorField" width="100%"><fmt:message key="admin.settings.Days"/>
            <html:hidden property="alertPurge" value="${CONST_DAYS}"/>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="alertPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="alertPurgeVal" />
          </td>
          <td class="BlockContent" width="100%"><fmt:message key="admin.settings.Days"/>
            <html:hidden property="alertPurge" value="${CONST_DAYS}"/>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="alertPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="alertPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="alertPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.EventLogPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="elPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="ErrorField" width="100%"><fmt:message key="admin.settings.Days"/>
            <html:hidden property="elPurge" value="${CONST_DAYS}"/>
          </td>
</logic:messagesPresent>          
<logic:messagesNotPresent property="elPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="elPurgeVal" />
          </td>
          <td class="BlockContent" width="100%"><fmt:message key="admin.settings.Days"/>
            <html:hidden property="elPurge" value="${CONST_DAYS}"/>
          </td>
</logic:messagesNotPresent>          
        </tr>
<logic:messagesPresent property="elPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="elPurgeVal"/></span>            
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="elPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel" colspan="2" width="30%"></td>
  </tr>
</table>

