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
<script language="JavaScript" src='<html:rewrite page="/js/scriptaculous.js"/>'
  type="text/javascript"></script>
<script src='<html:rewrite page="/js/dashboard.js"/>' type="text/javascript"></script>
<script src='<html:rewrite page="/js/effects.js"/>' type="text/javascript"></script>



<script type="text/javascript">
onloads.push(showViewEscResponse);

 /*
 function requestViewEscalation() {
        //var alertDefId = $('alertDefId').value;
        //var url = "/escalation/jsonEscalationByAlertDefId/" + alertDefId;
        //new Ajax.Request(url, {method: 'get', onSuccess:showViewEscResponse, onFailure :reportError});

        new Ajax.Request("<html:rewrite page="/js/sampleEsc.html"/>", {method: 'get', onSuccess:showViewEscResponse, onFailure :reportError});
		}
		*/

function showViewEscResponse() {

        var escViewUL = $('viewEscalationUL');

        var num = $('alertDefId').value;
		var liID = 'row'+num;
        var viewLi = document.createElement('li');
        var remDiv = document.createElement('div');
        var usersDiv = document.createElement('div');
        var rolesDiv = document.createElement('div');
        var othersDiv = document.createElement('div');
        var emailDiv = document.createElement('div');
        var sysDiv = document.createElement('div');
        var escTable = document.createElement('table');
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
        var waitTimeInfo = " ";
        var emailInfo = " ";
        var roleInfo = " ";
        var metaInfo = " ";
        var productInfo = " ";
        var versionInfo = " ";

        escViewUL.appendChild(viewLi)
       
        viewLi.setAttribute((document.all ? 'className' : 'class'), "lineitem");
        viewLi.setAttribute('id','row_'+ liID);

        viewLi.appendChild(remDiv);
        remDiv.setAttribute((document.all ? 'className' : 'class'), "remove");
        remDiv.style.paddingTop = "10px;"
        remDiv.innerHTML ='<a href="#" onclick="removeRow(this);"><html:img page="/images/tbb_delete.gif" height="16" width="46" border="0"  alt="" /></a>';

        viewLi.appendChild(escTable);
        escTable.setAttribute((document.all ? 'className' : 'class'), "escTbl");
        escTable.setAttribute('border', '0');

        // Put the "wait" select after other options
        escTable.appendChild(escTr2);
        escTable.appendChild(escTr1);

        escTr1.appendChild(td1);

        td1.setAttribute('colspan', '3');
        td1.innerHTML = "waittime:" + waitTimeInfo + "<br>";

        escTr2.appendChild(td2);
        td2.setAttribute('width', '20%');
        td2.setAttribute('valign', 'top');
        td2.style.paddingBottom = "10px";

        td2.innerHTML = "Notify via:" + emailInfo + "<br>";
        td2.style.paddingLeft = "0px";
        td2.style.paddingTop = "5px";
        td2.style.paddingBottom = "10px";

        escTr2.appendChild(td3);
        td3.setAttribute('width', '20%');
        td3.setAttribute('valign', 'top');
        td3.style.paddingRight = "20px";

        td3.innerHTML = "Roles:" + roleInfo + "<br>";
        td3.style.paddingTop = "5px";

        escTr2.appendChild(td4);
        td5.setAttribute('width', '50%');


        td4.appendChild(sysDiv);
        sysDiv.style.display = 'none';
        sysDiv.setAttribute('class', 'escInput'+ liID);
        sysDiv.setAttribute('id', 'sysloginput'+ liID);
        sysDiv.setAttribute('width', '40%');
        sysDiv.innerHTML = "meta:"  + metaInfo + "<br>" + "product:" + productInfo + "<br>" + "version:" + versionInfo + "<br>";

        td4.appendChild(usersDiv);
        usersDiv.setAttribute('id', 'usersDiv' + liID);
        usersDiv.style.display = 'none';
        usersDiv.style.border = '0px';
        if($('usersList')) {
          usersDiv.innerHTML = $('usersList').innerHTML;
        }

        td4.appendChild(rolesDiv);
        rolesDiv.setAttribute('id', 'rolesDiv' + liID);
        rolesDiv.style.display = 'none';
        rolesDiv.style.border = '0px';
        if($('rolesList')) {
          rolesDiv.innerHTML = $('rolesList').innerHTML;
        }

        Sortable.create(escViewUL,{ghosting:true,constraint:false});


}

    function addRow() {
        var ni = $('rowOrder');
        var numi = document.getElementById('theValue');
        var num = (document.getElementById('theValue').value -1)+ 2;
        
        numi.value = num;
        var liID = 'row'+num;
        var escLi = document.createElement('li');
        var remDiv = document.createElement('div');
        var usersDiv = document.createElement('div');
        var rolesDiv = document.createElement('div');
        var othersDiv = document.createElement('div');
        var emailDiv = document.createElement('div');
        var sysDiv = document.createElement('div');
        var escTable = document.createElement('table');
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

        ni.appendChild(escLi);
        escLi.setAttribute((document.all ? 'className' : 'class'), "lineitem");
        escLi.setAttribute('id','row_'+ liID);

        escLi.appendChild(remDiv);
        remDiv.setAttribute((document.all ? 'className' : 'class'), "remove");
        remDiv.style.paddingTop = "10px;"
        remDiv.innerHTML ='<a href="#" onclick="removeRow(this);"><html:img page="/images/tbb_delete.gif" height="16" width="46" border="0"  alt="" /></a>';

        escLi.appendChild(escTable);
        escTable.setAttribute((document.all ? 'className' : 'class'), "escTbl");
        escTable.setAttribute('border', '0');
        
        // Put the "wait" select after other options
        escTable.appendChild(escTr2);
        escTable.appendChild(escTr1);

        escTr1.appendChild(td1);

        td1.setAttribute('colspan', '3');
        td1.appendChild(document.createTextNode('<fmt:message key="alert.config.escalation.then"/> '));
        td1.appendChild(select1);
        select1.setAttribute('id', 'waittime_' + liID);
        select1.name = "waittime_" + liID;
        addOption(select1, '0', '<fmt:message key="alert.config.escalation.end"/>');
        addOption(select1, '300000', '<fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="5"/>
                                    </fmt:message>');
        addOption(select1, '600000', '<fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="10"/>
                                    </fmt:message>');
        addOption(select1, '1200000', '<fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="20"/>
                                    </fmt:message>');
        addOption(select1, '1800000', '<fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="30"/>
                                    </fmt:message>');
        addOption(select1, '2400000', '<fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="45"/>
                                    </fmt:message>');
        addOption(select1, '3000000', '<fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="60"/>
                                    </fmt:message>');

        escTr2.appendChild(td2);
        td2.setAttribute('width', '20%');
        td2.setAttribute('valign', 'top');
        td2.style.paddingBottom = "10px";
        
        td2.appendChild(select2);
        select2.setAttribute('id', 'Email_' + liID);
        td2.style.paddingLeft = "20px";
        td2.style.paddingTop = "5px";
        td2.style.paddingBottom = "10px";
        select2.onchange = function(){onchange_handler(this);}
        select2.name = "action_" + liID;
        addOption(select2, 'Email', 'Email');
        addOption(select2, 'SMS', 'SMS');
        addOption(select2, 'Syslog', 'Sys Log');
        addOption(select2, 'NoOp', 'Suppress Alerts');

        escTr2.appendChild(td3);
        td3.setAttribute('width', '20%');
        td3.setAttribute('valign', 'top');
        td3.style.paddingRight = "20px";
        
        td3.appendChild(select3);
        td3.style.paddingTop = "5px";
        select3.name = "who_" + liID;
        select3.id = "who_" + liID;
        select3.onchange = function(){onchange_who(this);}
        addOption(select3, 'Select', '<fmt:message key="alert.config.escalation.notify.who"/>');
        <c:if test="${not empty AvailableRoles}">
        addOption(select3, 'Roles', '<fmt:message key="monitoring.events.MiniTabs.Roles"/>')
        </c:if>
        addOption(select3, 'Users', '<fmt:message key="monitoring.events.MiniTabs.CAMusers"/>');
        addOption(select3, 'Others', '<fmt:message key="monitoring.events.MiniTabs.OR"/>');
        
        escTr2.appendChild(td4);
        td5.setAttribute('width', '50%');
        
        td4.appendChild(emailDiv);
        emailDiv.style.display = 'none';
        emailDiv.setAttribute('id', 'emailinput' + liID);
        emailDiv.setAttribute('class', 'escInput');
        emailDiv.setAttribute('width', '40%');
        emailDiv.innerHTML = "email addresses (comma separated):<br><textarea rows=3 cols=35 id=emailinput_" + liID + " name=emailinput_" + liID + "></textarea>";
        
        td4.appendChild(sysDiv);
        sysDiv.style.display = 'none';
        sysDiv.setAttribute('class', 'escInput'+ liID);
        sysDiv.setAttribute('id', 'sysloginput'+ liID);
        sysDiv.setAttribute('width', '40%');
        sysDiv.innerHTML = "meta: <input type=text name=meta_" + liID + " size=40><br>" + "product: <input type=text name=product_" + liID + " size=40><br>" + "version: <input type=text name=version_" + liID + " size=40><br>";

        td4.appendChild(usersDiv);
        usersDiv.setAttribute('id', 'usersDiv' + liID);
        usersDiv.style.display = 'none';
        usersDiv.style.border = '0px';
        if($('usersList')) {
          usersDiv.innerHTML = $('usersList').innerHTML;
          var usersInputList = usersDiv.getElementsByTagName('input');
          for(i=0;i < usersInputList.length; i++) {
              var inputNamesArr = usersInputList[i];
              inputNamesArr.name = inputNamesArr.name + "_" + liID;
          }
        }

        td4.appendChild(rolesDiv);
        rolesDiv.setAttribute('id', 'rolesDiv' + liID);
        rolesDiv.style.display = 'none';
        rolesDiv.style.border = '0px';
        if($('rolesList')) {
          rolesDiv.innerHTML = $('rolesList').innerHTML;
          var rolesInputList = rolesDiv.getElementsByTagName('input');
           for(i=0;i < rolesInputList.length; i++) {
                  var inputRolesArr = rolesInputList[i];
                  inputRolesArr.name =  inputRolesArr.name + "_" + liID;
              }
          }

        Sortable.create('rowOrder',{ghosting:true,constraint:false});

      }
      /*
      function rowIDs(liID){
         var rows = liID.id;

        for(i=0;i < rows.length; i++) {
              var rowIdList = rows[i].id;
              var rowSplit = rowIdList.split('_');
              var rowListID = rowSplit[1];
            alert(rowListID)
            }
        }
       */
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
        var o = document.createElement("OPTION");
        var t = document.createTextNode(txt);
        o.setAttribute("value",val);

        if (selected) {
          o.setAttribute("selected", "true");
        }

        o.appendChild(t);
        sel.appendChild(o);
    }

    function showResponse(originalRequest) {
        $('example').innerHTML =
           "<span style=font-weight:bold;>Escalation Saved: " +
            Form.serialize('EscalationForm') + "</span>";
    }

    function initEsc () {
        // Set up the escalation dropdown
        var escJson = eval( '( { "escalations":<c:out value="${escalations}" escapeXml="false"/> })' );

        var escalationSel = $('escId');
        var schemes = escJson.escalations;
        for (var i = 0; i < schemes.length; i++) {
            <c:if test="${not empty param.escId}">
              if (schemes[i].name == '<c:out value="${param.escId}"/>')
                addOption(escalationSel , schemes[i].name, schemes[i].name, true);
              else
            </c:if>
            addOption(escalationSel , schemes[i].name, schemes[i].name);
        }

        document.EscalationForm.escName.value = document.EscalationSchemeForm.escId.value;
    
        $('submit').onclick = function () {
            if ($('escName').value == "") {
                alert('<fmt:message key="alert.config.error.escalation.name.required"/>');
                return false;
            }
            sendEscForm();
            return false;
        }

    
      <c:if test="${empty param.escId}">
        addRow();
      </c:if>
    }

    onloads.push( initEsc );


    function sendEscForm() {
        var adId;
        var gadId;
        var eId;
        var aetId;
        var rowOrder = Sortable.serialize('rowOrder');
        var escFormSerial = Form.serialize('EscalationForm');
        var url = '<html:rewrite action="/escalation/saveEscalation"/>';
        if ($('gad')) {
           gadId == $('gad').value;
        } else {
            gadId == '';
        }
        if ($('ad')){
            adId == $('ad').value;
        } else {
            adId == '';
        }
        if ($('eid')){
            eID = $('eid').value;
        } else {
           eId == '';
        }
        if ($('aetid')) {
            aetId = $('aetid').value;
        } else {
            aetId == '';
        }

        var pars = "rowOrder=" + rowOrder + "escForm=" + escFormSerial + "&ad=" + adId + "&gad=" + gadId + "&eid=" + eId + "aetid=" + aetId;
        new Ajax.Request( url, {method: 'post', parameters: pars, onComplete: showResponse, onFailure :reportError} );
        $('example2').innerHTML = rowOrder;
    }

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

    //onloads.push(Behaviour.apply());

</script>

<html:form action="/alerts/ConfigEscalation">
  <html:hidden property="mode" />
  <c:choose>
    <c:when test="${not empty param.ad}">
      <input type="hidden" id="ad" name="ad"
        value='<c:out value="${param.ad}"/>' />
    </c:when>
    <c:when test="${not empty param.gad}">
      <input type="hidden" id="gad" name="gad"
        value='<c:out value="${param.gad}"/>' />
    </c:when>
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

<tiles:insert definition=".portlet.confirm" />
<tiles:insert definition=".portlet.error" />

<form action='<html:rewrite action="/escalation/saveEscalation"/>'
  name="EscalationForm" id="EscalationForm"><input type="hidden"
  value="0" id="pid"> <input type="hidden" value="0" id="pversion">

<input type="hidden" value="0" id="if the escalation is new or not"> <input
  type="hidden" value="0" id="theValue"> <c:choose>
  <c:when test="${not empty Resource}">
    <html:hidden property="eid" value="${Resource.entityId}" />
  </c:when>
  <c:otherwise>
    <html:hidden property="aetid" value="${ResourceType.appdefTypeKey}" />
  </c:otherwise>
</c:choose> <c:choose>
  <c:when test="${not empty param.ad}">
    <input type="hidden" id="ad" name="ad" value='<c:out value="${param.ad}"/>' />
  </c:when>
  <c:when test="${not empty param.gad}">
    <input type="hidden" id="gad" name="gad"
      value='<c:out value="${param.gad}"/>' />
  </c:when>
</c:choose> <input type="hidden" id="ffff" name="ggg"
  value='<c:out value="${escalation.id}"/>' />
<table width="100%" cellpadding="3" cellspacing="0" border="0">
  <tbody>
    <tr class="tableRowHeader">
      <td align="right"><fmt:message key="alert.config.escalation.scheme"/>
        <select id="escId" name="escId" onchange="schemeChange(this)">
          <option value=""><fmt:message key="common.label.CreateNew" /></option>
        </select>
        <fmt:message key="common.label.Name" />
        <input type="text" size="25" name="escName" id="escName" /></td>
    </tr>
    <tr class="tableRowAction">
      <td class="section" width="100%">

      <ul id="rowOrder"></ul>
      <table width="100%" cellpadding="5" cellspacing="0" border="0"
        class="ToolbarContent">
        <tr>
          <td width="40"><a href="#" onclick="addRow();"
            style="text-decoration:none;"><html:img
            page="/images/tbb_addtolist.gif" height="16" width="85" border="0" /></a></td>
        </tr>
      </table>
      <br>
      </td>
    </tr>
    <tr>
      <td class="tableRowHeader">If the alert is acknowledged:</td>
    </tr>
    <tr class="ListRow">
      <td style="padding-left:15px;padding-bottom:10px;">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td style="padding-top:2px;padding-bottom:2px;">
            <input type="radio" name="allowPause" value="true" />
            <fmt:message key="alert.config.escalation.allow.pause"/>
            <select id="maxWaitTime" name="maxwaittime">
            <option value="300000">5 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/></option>
            <option value="600000">10 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/></option>
            <option value="1200000">20 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/></option>
            <option value="1800000">30 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/></option>
          </select></td>
        </tr>
        <tr>
          <td style="padding-top:2px;padding-bottom:2px;">
            <input type="radio" name="allowPause" value="false" checked="true"/>
            <fmt:message key="alert.config.escalation.allow.continue"/>

            </td>
        </tr>
      </table>
      </td>
    </tr>
    <tr>
      <td class="tableRowHeader">
        <fmt:message key="alert.config.escalation.state.change"/><br>
      </td>
    </tr>
    <tr class="ListRow">
      <td style="padding-left:15px;padding-bottom:10px;">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td style="padding-top:2px;padding-bottom:2px;">
            <input type="radio" name="notification" value="0" checked="true"/> 
            <fmt:message key="alert.config.escalation.state.change.notify.previous"/>
          </td>
        </tr>
        <tr>
          <td style="padding-top:2px;padding-bottom:2px;">
            <input type="radio" name="notification" value="1" />
            <fmt:message key="alert.config.escalation.state.change.notify.all"/>
          </td>
        </tr>
      </table>
      </td>
    </tr>
  </tbody>
</table>

<br>
<br>
<input type="button" value="Submit" onclick="sendEscForm();" id="submit"></input> <br><br>


<div id="usersList" style="display: none;">
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
</c:if>

</form>

 <form name="viewEscalation" id="viewEscalation">

      <input type="hidden" id="alertDefId" name="alertDefId" value='<c:out value="${param.ad}"/>' />
   
    
     <table width="100%" cellpadding="3" cellspacing="0" border="0">
        <tbody>
         <tr class="tableRowAction">
            <td class="section" width="100%">
            <ul id="viewEscalationUL">

            </ul>
            </td>
        </tr>
        </tbody>
     </table>

 </form>

<div id="example" style="padding:10px;width:725px;overflow:auto;"></div>
<div id="example2" style="padding:10px;width:725px;overflow:auto;"></div>


