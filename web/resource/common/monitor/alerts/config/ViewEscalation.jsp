<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="struts-html-el" prefix="html"%>
<%@ taglib uri="struts-tiles" prefix="tiles"%>
<%@ taglib uri="jstl-fmt" prefix="fmt"%>
<%@ taglib uri="display" prefix="display"%>
<%@ taglib uri="jstl-c" prefix="c"%>

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
<tiles:importAttribute name="gad" ignore="true"/>
<tiles:importAttribute name="alertDef" ignore="true"/>

<script language="JavaScript" src='<html:rewrite page="/js/scriptaculous.js"/>'
  type="text/javascript"></script>
<script src='<html:rewrite page="/js/dashboard.js"/>' type="text/javascript"></script>
<script src='<html:rewrite page="/js/effects.js"/>' type="text/javascript"></script>

<script type="text/javascript">
    <c:if test="${not empty escalationJSON}">
    onloads.push(showViewEscResponse);

function showViewEscResponse() {
    var tmp = eval('( <c:out value="${escalationJSON}" escapeXml="false"/> )');
    var notifyAll = tmp.escalation.notifyAll
    var actions = tmp.escalation.actions;
    var allowPause = tmp.escalation.allowPause;
    var id = tmp.escalation.id;
    var maxWaitTime = (tmp.escalation.maxWaitTime / 60000) +
       " <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>";

    $('viewEscalation').style.display = "";
    document.EscalationSchemeForm.escId.value = id;
  
    var escViewUL = $('viewEscalationUL');

    for(var i=escViewUL.childNodes.length-1; i>1; i--) {
	 escViewUL.removeChild(escViewUL.childNodes[i]);
    }

    for (i = 0; i < actions.length; i++) {
      var actionConfig = actions[i].action.config;
      var configListType = actionConfig.listType;
      var configNames = actionConfig.names;
      var configSms = actionConfig.sms;
      var actionId = actions[i].action.id;
      var actionsClassName = actions[i].action.className;
      var actionWaitTime = (actions[i].waitTime / 60000) +
         " <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>";
  
      var num = actionId;
	  var liID = 'row'+num;
      var viewLi = document.createElement('li');
      var remDiv = document.createElement('div');
      var usersDiv = document.createElement('div');
      var usersTextDiv = document.createElement('div');
      var usersEditDiv = document.createElement('div');
      var rolesDiv = document.createElement('div');
      var othersDiv = document.createElement('div');
      var waitDiv = document.createElement('div');
      var editWaitDiv = document.createElement('div');
      var sysDiv = document.createElement('div');
      var escTable = document.createElement('table');
      var escTableBody = document.createElement('tbody');
      var escTr1 = document.createElement('tr');
      var escTr2 = document.createElement('tr');
      var td1 = document.createElement('td');
      var td2 = document.createElement('td');
      var td3 = document.createElement('td');
      var td4 = document.createElement('td');
      var td5 = document.createElement('td');
      var select1 = document.createElement("select");
      var select2 = document.createElement("select");
      var select3 = document.createElement("select");
      var anchor = document.createElement("a");
  
      var emailInfo = actionConfig.names;
      var roleInfo = " ";
      var metaInfo = " ";
      var productInfo = " ";
      var versionInfo = " ";
  
      escViewUL.appendChild(viewLi)
  
      //viewLi.setAttribute((document.all ? 'className' : 'class'), "lineitem");
      viewLi.setAttribute('id','row_'+ liID);
      $('row_'+ liID).style.margin = "0px";
      $('row_'+ liID).style.padding = "0px";
      
      viewLi.appendChild(escTable);
      escTable.setAttribute((document.all ? 'className' : 'class'), "escTbl");
      escTable.setAttribute('border', '0');
      escTable.setAttribute('cellspacing','3');
      escTable.appendChild(escTableBody);
  
      escTableBody.appendChild(escTr2);
      escTableBody.appendChild(escTr1);
  
      escTr1.appendChild(td1);
  
      //td1.setAttribute('colspan', '3');
      td1.appendChild(waitDiv);
      waitDiv.setAttribute('id','wait_' + liID);
      waitDiv.innerHTML = "Wait time before escalating: " + actionWaitTime + "<br>";
  
      td1.appendChild(editWaitDiv);
      editWaitDiv.setAttribute('id','editWait_' + liID);
  
      escTr2.appendChild(td2);
      td2.setAttribute('width', '20%');
      td2.setAttribute('valign', 'top');
  
      td2.appendChild(usersTextDiv);
  
      if (configListType == "1"){
          usersTextDiv.innerHTML = "<fmt:message key="monitoring.events.MiniTabs.Others"/>:  " + emailInfo + "<br>";
      } else if (configListType == "2") {
          var uids = emailInfo.split(',');
          var userNames = "";
          for (var b = 0; b < uids.length; b++) {
              <c:forEach var="user" items="${AvailableUsers}" varStatus="status">
                  if (uids[b] == '<c:out value="${user.id}"/>') {
                      userNames += '<c:out value="${user.name}" /> ';
                  }
              </c:forEach>
          }
          
          usersTextDiv.innerHTML = "<fmt:message key="monitoring.events.MiniTabs.Users"/>: " + userNames + "<br>";
      } else  if (configListType == "3") {
          var rids = emailInfo.split(',');
          var roleNames = "";
          for (var b = 0; b < rids.length; b++) {
              <c:forEach var="role" items="${AvailableRoles}" varStatus="status">
                  if (rids[b] == '<c:out value="${role.id}"/>') {
                      roleNames += '<c:out value="${role.name}" /> ';
                  }
              </c:forEach>
          }
          
          usersTextDiv.innerHTML = "<fmt:message key="monitoring.events.MiniTabs.Users"/>: " + roleNames + "<br>";
      }
  
      escTr2.appendChild(td3);
      td3.setAttribute('width', '20%');
      td3.setAttribute('valign', 'top');
      td3.style.paddingRight = "20px";
  
      switch(configListType) {
      case 1:
        td3.innerHTML = emailInfo + "<br>";
        break;
      case 2:
        td3.innerHTML = configNames + "<br>";
        break;
      case 3:
        td3.innerHTML = roleInfo + "<br>";
        break;
      }
  
      td3.style.paddingTop = "5px";
  
      escTr2.appendChild(td4);
      td5.setAttribute('width', '50%');
  
      td4.appendChild(usersEditDiv);
      usersEditDiv.style.display = 'none';
      usersEditDiv.setAttribute('class', 'escInput'+ liID);
      usersEditDiv.setAttribute('id', 'usersEditDiv_'+ liID);
      usersEditDiv.setAttribute('width', '40%');
      usersEditDiv.innerHTML = " ";
      if (allowPause) {
        $('acknowledged').innerHTML = '<fmt:message key="alert.config.escalation.allow.pause" /> ' + maxWaitTime;
      }
      else {
        $('acknowledged').innerHTML = '<fmt:message key="alert.config.escalation.allow.continue" />';
      }

      if (notifyAll) {
        $('changed').innerHTML = '<fmt:message key="alert.config.escalation.state.change.notify.all" />';
      }
      else {
        $('changed').innerHTML = '<fmt:message key="alert.config.escalation.state.change.notify.previous" />';
      }

      Sortable.create(escViewUL,{ghosting:true,constraint:false});
   }    
}
    </c:if>
    
    function addOption(sel, val, txt, selected) {
        var o = document.createElement('option');
        var t = document.createTextNode(txt);

        o.setAttribute('value',val);

        if (selected) {
          o.setAttribute('selected', 'true');
        }
        sel.appendChild(o);
        o.appendChild(document.createTextNode(txt));
    }

    function initEsc () {
        // Set up the escalation dropdown
        var escJson = eval( '( { "escalations":<c:out value="${escalations}" escapeXml="false"/> })' );
        var escalationSel = $('escIdSel');
        var schemes = escJson.escalations;

        for (var i = 0; i < schemes.length; i++) {

            if (schemes[i].name == "")
                continue;

            addOption(escalationSel , schemes[i].id, schemes[i].name,
                      schemes[i].id == document.EscalationSchemeForm.escId.value);
        }
   }

   onloads.push( initEsc );

    function configure(id) {
      var sel = $('who' + id);
      var selval = sel.options[sel.selectedIndex].value;

      if (selval == 'Users') {
        // Select checkboxes based on existing configs
        insertUsers(this.id);
      }
      else if (selval == 'Others') {
        // Set the inner text
        insertOthers(this.id);
      }
      else if (selval == 'Roles') {
        // Select checkboxes based on existing configs
        insertRoles(this.id);
      }
    }

    function schemeChange(sel) {
      if (sel.options[sel.selectedIndex].value != "") {
        document.EscalationSchemeForm.escId.value =
            sel.options[sel.selectedIndex].value;
        document.EscalationSchemeForm.submit();
      }
    }

    function hideExample() {
            $('example').style.display= 'none';
    }

</script>

<html:form action="/alerts/ConfigEscalation">
  <input type="hidden" id="ad" name="ad" value='<c:out value="${alertDef.id}"/>' />
  <c:choose>
    <c:when test="${gad}">
      <html:hidden property="mode" value="viewGroupDefinition"/>
    </c:when>
    <c:otherwise>
      <html:hidden property="mode" />
    </c:otherwise>
  </c:choose>
  <c:choose>
    <c:when test="${not empty Resource}">
      <html:hidden property="eid" value="${Resource.entityId}" />
    </c:when>
    <c:otherwise>
      <html:hidden property="aetid" value="${ResourceType.appdefTypeKey}" />
    </c:otherwise>
  </c:choose>
  <html:hidden property="escId" />
</html:form>

<form action='<html:rewrite action="/escalation/saveEscalation"/>'
  name="EscalationForm" id="EscalationForm" onchange="hideExample();"><input
  type="hidden" value="0" id="pid"> <input type="hidden" value="0"
  id="pversion"> <input type="hidden" value="0"
  id="if the escalation is new or not"> <input type="hidden" value="0"
  id="theValue"> <c:choose>
  <c:when test="${not empty Resource}">
    <html:hidden property="eid" value="${Resource.entityId}" />
  </c:when>
  <c:otherwise>
    <html:hidden property="aetid" value="${ResourceType.appdefTypeKey}" />
  </c:otherwise>
</c:choose>
<input type="hidden" id="ad" name="ad" value='<c:out value="${alertDef.id}"/>' />
<input type="hidden" id="ffff" name="ggg" value='<c:out value="${escalation.id}"/>' />

<div id="example" style="display:none;">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <td class="ConfirmationBlock">
    <html:img page="/images/tt_check.gif" height="9" width="9" border="0" alt=""/>
  </td>
  <td class="ConfirmationBlock" width="100%">
    <div id="escMsg"></div>
  </td>
</table>
</div>

<table width="100%" cellpadding="4" cellspacing="0" border="0">
  <tbody>
    <tr class="tableRowHeader">
      <td><fmt:message key="alert.config.escalation.scheme" /> <select
        id="escIdSel" name="escId" onchange="schemeChange(this)">
        <option value=""><fmt:message key="resource.common.inventory.props.SelectOption" /></option>
      </select>
      </td>
      <td align="right">
         <c:url var="adminUrl" value="/admin/config/Config.do?mode=escalate">
           <c:param name="aname" value="${alertDef.name}"/>
           <c:choose>
             <c:when test="${gad}">
               <c:param name="gad" value="${alertDef.id}"/>
             </c:when>
             <c:otherwise>
               <c:param name="ad" value="${alertDef.id}"/>
             </c:otherwise>
           </c:choose>
         </c:url>
         <fmt:message key="admin.config.message.to.create">
           <fmt:param value="${adminUrl}"/>
         </fmt:message>
      </td>
    </tr>
  </tbody>
</table>

</form>

<table width="100%" cellpadding="0" cellspacing="0" border="0" id="viewEscalation">
  <tbody>
    <tr>
      <td class="BlockLabel" width="20%"nowrap="true"><fmt:message key="alert.config.escalation.acknowledged"/></td>
      <td id="acknowledged" class="BlockContent"></td>
    </tr>
    <tr>
      <td class="BlockLabel" nowrap="true"><fmt:message key="alert.config.escalation.state.change"/></td>
      <td id="changed" class="BlockContent"></td>
    </tr>
    <tr>
      <td class="BlockLabel" style="text-align: left;" nowrap="true"><fmt:message key="alert.config.escalation.Begin"/></td>
      <td class="BlockContent">&nbsp;</td>
    </tr>
    <tr>
      <td width="100%" colspan="2">
      <ul id="viewEscalationUL">
      </ul>
      </td>
    </tr>
    <tr>
      <td class="BlockLabel" style="text-align: left;" nowrap="true"><fmt:message key="alert.config.escalation.TheEnd"/></td>
      <td class="BlockContent">&nbsp;</td>
    </tr>
  </tbody>
</table>

