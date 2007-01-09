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
onloads.push(requestViewEscalation);

 function requestViewEscalation() {
        var alertDefId = $('alertDefId').value;
        var urlPart1 = '<html:rewrite page="/escalation/jsonEscalationByAlertDefId/';
        var urlPart2 = '.do"/>';
        var url = urlPart1 + alertDefId + urlPart2;
        //var url = "../escalation/jsonEscalationByAlertDefId.do?id=" + alertDefId;
        new Ajax.Request(url, {method: 'get', onSuccess:showViewEscResponse, onFailure :reportError});
        }


function showViewEscResponse(originalRequest) {
    var tmp = eval('(' + originalRequest.responseText + ')');
    var creationTime = tmp.escalation.creationTime;
    var notifyAll = tmp.escalation.notifyAll
    var _version_ = tmp.escalation._version_;
    var modifiedTime = tmp.escalation.modifiedTime;
    var actions = tmp.escalation.actions;
    var allowPause = tmp.escalation.allowPause;
    var escName = tmp.escalation.name;
    var id = tmp.escalation.id;
    var maxWaitTime = (tmp.escalation.maxWaitTime / 60000) +
       " <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>";

    <c:if test="${not empty EscalationForm.escId}">
          $('viewEscalation').style.display = "";
    </c:if>
    <c:if test="${empty EscalationForm.escId}">
          $('viewEscalation').style.display = "none";
    </c:if>
  
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
      var actionsVersion = actions[i].action._version_;
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
  
      $('creationTime').value = creationTime;
      $('notifyAll').value = notifyAll;
      $('_version_').value = _version_;
      $('modifiedTime').value = modifiedTime;
      $('allowPause').value = allowPause;
      $('id').value = id;
  
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
      $('pauseTimeText').innerHTML = 'Allow user to pause escalation: ' + allowPause + "<br>";
      Sortable.create(escViewUL,{ghosting:true,constraint:false});
   }    
}
    
    function editEscalation (row) {
        var select1 = document.createElement("select");
        var idStr = row.parentNode.parentNode.id;
        var getId = idStr.split('_');
        var liID = getId[1];
        var usersEditDiv = ('usersEditDiv_'+ getId[1]);
        //alert(idStr);
        var editMaxWait = $('editWait_' + getId[1]);
        $('wait_' + getId[1]).style.display = "none";
        $('pauseTimeText').style.display="none";
        $('pauseTimeEdit').style.display = "";

        editMaxWait.appendChild(document.createTextNode('<fmt:message key="alert.config.escalation.then"/> '));
        editMaxWait.appendChild(select1);
        select1.setAttribute('id', 'waittime_' + liID);
        select1.name = "waittime_" + liID;
        addOption(select1, '0', '<fmt:message key="alert.config.escalation.end"/>');
        addOption(select1, '300000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="5"/></fmt:message>');
        addOption(select1, '600000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="10"/></fmt:message>');
        addOption(select1, '1200000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="20"/></fmt:message>');
        addOption(select1, '1800000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="30"/></fmt:message>');
        addOption(select1, '2400000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="45"/></fmt:message>');
        addOption(select1, '3000000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="60"/></fmt:message>');

        if($('usersList')) {
          usersEditDiv.innerHTML = $('usersList').innerHTML;
          var usersInputList = usersEditDiv.getElementsByTagName('input');
          for(i=0;i < usersInputList.length; i++) {
              var inputNamesArr = usersInputList[i];
              inputNamesArr.name = inputNamesArr.name + "_" + liID;
          }
        }


    }

    function onchange_handler(el) {
        //alert(el+", value="+ el.options[el.selectedIndex].value );
        var index= el.options[el.selectedIndex].value

         if (index == "Email" || index == "SMS" || index == "NoOp") {
            hideSyslogInput(el);
         }
         else {
            showSyslogInput(el);
         }

         if (index == "Syslog" || index == "NoOp") {
            hideWhoSelect(el);
         }
         else {
            showWhoSelect(el);
         }
      }

      function onchange_who(el) {
        //alert(el+", value="+ el.options[el.selectedIndex].value );
        var index= el.options[el.selectedIndex].value
        var idStr = el.id;
        var getId = idStr.split('_');
        var rolesDivIn = $('rolesDiv' + getId[1]);
        var usersDivIn = $('usersDiv' + getId[1]);
        var emailDivIn = $('emailinput' + getId[1]);

        if (index == "Roles") {
           emailDivIn.style.display = 'none';
           configureRoles(idStr);
         } else if (index == "Users") {
           emailDivIn.style.display = 'none';
           configureUsers(idStr);
            //configureUsers(nodeId);
         } else if (index == "Others") {
           emailDivIn.style.display = '';
           //configureOthers(nodeId);
        }
      }

    function showWhoSelect(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var whoSelector = $('who_' + getId[1]);
        whoSelector.style.display='';
    }

    function hideWhoSelect(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var whoSelector = $('who_' + getId[1]);
        whoSelector.style.display='none';
    }

    function showSyslogInput(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var syslogDivIn = $('sysloginput' + getId[1]);
        syslogDivIn.style.display='';
    }

    function hideSyslogInput(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var syslogDivIn = $('sysloginput' + getId[1]);
        syslogDivIn.style.display='none';
    }

    function hideDisplay() {
        $(emailinput).style.display='none';
        $(sysloginput).style.display='none';
    }

    function removeRow(obj) {
        var oLi = obj.parentNode.parentNode;
        var root = oLi.parentNode;
        root.removeChild(oLi);
    }

    function addOption(sel, val, txt, selected) {


        var o = document.createElement('option');
        var t = document.createTextNode(txt);

        o.setAttribute('value',val);

        if (selected) {
          o.setAttribute('selected', 'true');
        }
        sel.appendChild(o);
        o.appendChild(document.createTextNode(txt));
      
        /*
        var option = new Option(txt, val);
        alert(sel.options);
        sel.options[sel.options.length] = option;
        option.isSelected = selected;
        */
    }

    function showResponse(originalRequest) {
        $('escMsg').innerHTML ="Escalation Saved";
        $('example').style.display= '';
    }



    function initEsc () {
        // Set up the escalation dropdown
        var escJson = eval( '( { "escalations":<c:out value="${escalations}" escapeXml="false"/> })' );
        var escalationSel = $('escIdSel');
        var schemes = escJson.escalations;

          for (var i = 0; i < schemes.length; i++) {

            if (schemes[i].name == "")
                continue;

            <c:if test="${not empty EscalationForm.escId}">
            if (schemes[i].id == '<c:out value="${EscalationForm.escId}"/>')
                addOption(escalationSel , schemes[i].id, schemes[i].name, true);
              else
            </c:if>

            addOption(escalationSel , schemes[i].id, schemes[i].name,
                      schemes[i].name == document.EscalationSchemeForm.escId.value);
        }
        
        document.EscalationForm.escName.value = document.EscalationSchemeForm.escId.value;
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

    function configureOthers(el) {
            var idStr = el;
            var getId = idStr.split('_');
            var othersDivIn = $('othersDiv_' + getId[1]);

      Dialog.confirm(othersDivIn.innerHTML,
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                  // Do something
                    new Effect.Shake(Windows.focusedWindow.getId());
                    return true;
                  }});
    }

    function configureUsers(el) {
      var idStr = el;
      var getId = idStr.split('_');
      var usersDivIn = $('usersDiv' + getId[1]);

      Dialog.confirm('<div id="usersConfigWindow">' + usersDivIn.innerHTML +
                     '</div>',
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                    var usersInputList =
                      usersDivIn.getElementsByTagName('input');
                    var updatedInputList =
                      $('usersConfigWindow').getElementsByTagName('input');

                    for(i = 0; i < usersInputList.length; i++) {
                        if (updatedInputList[i].checked) {
                          usersInputList[i].setAttribute("checked", "true");
                        }
                    }
                    new Effect.Fade(Windows.focusedWindow.getId());
                    return true;
                  }});
    }

    function configureRoles(el) {
      var idStr = el;
      var getId = idStr.split('_');
      var rolesDivIn = $('rolesDiv' + getId[1]);

      Dialog.confirm('<div id="rolesConfigWindow">' + rolesDivIn.innerHTML +
                     '</div>',
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                    var rolesInputList =
                      rolesDivIn.getElementsByTagName('input');
                    var updatedInputList =
                      $('rolesConfigWindow').getElementsByTagName('input');

                    for(i = 0; i < rolesInputList.length; i++) {
                        if (updatedInputList[i].checked) {
                          rolesInputList[i].setAttribute("checked", "true");
                        }
                    }
                    new Effect.Fade(Windows.focusedWindow.getId());
                    return true;
                  }});
    }

    function schemeChange(sel) {
      document.EscalationSchemeForm.escId.value =
        sel.options[sel.selectedIndex].value;
      document.EscalationSchemeForm.submit();
    }

    function reportError(originalRequest) {
        alert('Error ' + originalRequest.status + ' -- ' + originalRequest.statusText);
    }

    function getGroupOrder() {
        var sectionID = $('rowOrder');
        var order = Sortable.serialize(sectionID);
		var alerttext = Sortable.serialize('rowOrder') + '\n';

		alert(alerttext);

	}

    function hideExample() {
            $('example').style.display= 'none';
    }

    //onloads.push(Behaviour.apply());

</script>

<html:form action="/alerts/ConfigEscalation" method="GET">
  <input type="hidden" id="ad" name="ad" value='<c:out value="${alertDef.id}"/>' />
  <c:choose>
    <c:when test="${not empty gad}">
      <html:hidden property="mode" value="viewGroupDefinition"/>
      <input type="hidden" id="gad" name="gad" value='<c:out value="${alertDef.id}"/>' />
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
<c:choose>
  <c:when test="${not empty gad}">
    <input type="hidden" id="gad" name="gad" value='<c:out value="${gad}"/>' />
  </c:when>
  <c:otherwise>
    <input type="hidden" id="ad" name="ad" value='<c:out value="${alertDef.id}"/>' />
  </c:otherwise>
</c:choose>
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
             <c:when test="${not empty gad}">
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

<div id="usersList" style="display:none;">
<div class="ListHeader">Select Users</div>
<ul class="boxy">
  <c:forEach var="user" items="${AvailableUsers}" varStatus="status">
    <li class="ListRow"><input type="checkbox" name="users"
      value="<c:out value="${user.id}"/>"> <BLK><c:out
      value="${user.name}" /></BLK></input></li>
  </c:forEach>
</ul>
</div>

<c:if test="${not empty AvailableRoles}">
  <div id="rolesList" style="display: none;">
  <div class="ListHeader">Select Roles</div>
  <ul class="boxy">
    <c:forEach var="role" items="${AvailableRoles}" varStatus="status">
      <li class="ListRow"><input type="checkbox" name="roles"
        value="<c:out value="${role.id}"/>"> <BLK><c:out
        value="${role.name}" /></BLK></input></li>
    </c:forEach>
  </ul>
  </div>
</c:if></form>

<form name="viewEscalation" id="viewEscalation" style="display:none;"><input
  type="hidden" id="alertDefId" name="alertDefId"
  value='<c:out value="${alertDef.id}"/>' />
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tbody>
    <tr>
      <td width="100%">

      <ul id="viewEscalationUL" style="margin-left:0px;">

      </ul>
      </td>
    </tr>

    <tr>
      <td class="tableRowHeader">If the alert is acknowledged:</td>
    </tr>
    <tr class="ListRow">
      <td style="padding-left:15px;padding-bottom:10px;">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tbody>
          <tr>
            <td style="padding-top:10px;padding-bottom:2px;">
            <div id="pauseTimeText"></div>
            <div id="pauseTimeEdit" style="display:none;"><input
              type="radio" name="allowPause" value="true" /> <fmt:message
              key="alert.config.escalation.allow.pause" /> <select
              id="maxWaitTime_<c:out value="${alertDef.id}"/>"
              name="maxwaittime">
              <option value="300000">5 <fmt:message
                key="alert.config.props.CB.Enable.TimeUnit.1" /></option>
              <option value="600000">10 <fmt:message
                key="alert.config.props.CB.Enable.TimeUnit.1" /></option>
              <option value="1200000">20 <fmt:message
                key="alert.config.props.CB.Enable.TimeUnit.1" /></option>
              <option value="1800000">30 <fmt:message
                key="alert.config.props.CB.Enable.TimeUnit.1" /></option>
            </select></div>
            </td>
          </tr>
        </tbody>
      </table>
      </td>
    </tr>
  </tbody>
</table>

<br>
<br>
<input type="hidden" value="" id="creationTime"> <input type="hidden"
  value="" id="_version_"> <input type="hidden" value="" id="notifyAll">
<input type="hidden" value="" id="modifiedTime"> <input type="hidden"
  value="" id="allowPause"> <input type="hidden" value="" id="escName">
<input type="hidden" value="" id="id"></form>

