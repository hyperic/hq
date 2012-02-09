<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".

  Copyright (C) [2004-2010], Hyperic, Inc.
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

<jsu:importScript path="/js/scriptaculous.js" />
<jsu:importScript path="/js/dashboard.js" />
<jsu:importScript path="/js/effects.js" />
<jsu:importScript path="/js/window.js" />
<jsu:script>
	document.toggleSubmit = function(e){
    	if(e && e.keyCode == 13){
        	saveNewEscalation();
        	e.stopPropagation ();
        	e.preventDefault();
    	}
	}

	function showViewEscResponse(originalRequest) {
	    var tmp = eval('(' + originalRequest.responseText + ')');
	    var creationTime = tmp.escalation.creationTime;
	    var notifyAll = tmp.escalation.notifyAll;
	    var _version_ = tmp.escalation._version_;
	    var modifiedTime = tmp.escalation.modifiedTime;
	    var actions = tmp.escalation.actions;
	    var allowPause = tmp.escalation.allowPause;
	    var escName = tmp.escalation.name;
	    var id = tmp.escalation.id;
	    var maxWaitTime = formatWaitTime(null, tmp.escalation.maxWaitTime, '<fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>',  '<fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>');
	
	    <c:if test="${not empty EscalationForm.escId}">
	    	hqDojo.byId('viewEscalation').style.display = "";
	        hqDojo.byId('createEscTable').style.display = "none";
	    </c:if>
	    <c:if test="${empty EscalationForm.escId}">
	        hqDojo.byId('viewEscalation').style.display = "none";
	        hqDojo.byId('createEscTable').style.display = "";
	    </c:if>
	  
	    var escViewUL = hqDojo.byId('viewEscalationUL');
	
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
	      	var actionWaitTime = formatWaitTime(null, actions[i].waitTime, '<fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>',  '<fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>');
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
	  
	      hqDojo.byId('creationTime').value = creationTime;
	      hqDojo.byId('notifyAll').value = notifyAll;
	      hqDojo.byId('_version_').value = _version_;
	      hqDojo.byId('modifiedTime').value = modifiedTime;
	      hqDojo.byId('allowPause').value = allowPause;
	      hqDojo.byId('id').value = id;
	  
	      escViewUL.appendChild(viewLi)
	  
	      //viewLi.setAttribute((document.all ? 'className' : 'class'), "lineitem");
	      viewLi.setAttribute('id','row_'+ liID);
	      hqDojo.byId('row_'+ liID).style.margin = "0px";
	      hqDojo.byId('row_'+ liID).style.padding = "0px";
	      
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
	      hqDojo.byId('pauseTimeText').innerHTML = 'Allow user to pause escalation: ' + allowPause + "<br>";
	   }    
	}
	
	function editEscalation (row) {
	    var select1 = document.createElement("select");
	    var idStr = row.parentNode.parentNode.id;
	    var getId = idStr.split('_');
	    var liID = getId[1];
	    var usersEditDiv = ('usersEditDiv_'+ getId[1]);
	    //alert(idStr);
	    var editMaxWait = hqDojo.byId('editWait_' + getId[1]);
	    hqDojo.byId('wait_' + getId[1]).style.display = "none";
	    hqDojo.byId('pauseTimeText').style.display="none";
	    hqDojo.byId('pauseTimeEdit').style.display = "";
	
	    editMaxWait.appendChild(document.createTextNode('<fmt:message key="alert.config.escalation.then"/> '));
	    editMaxWait.appendChild(select1);
	    select1.setAttribute('id', 'waittime_' + liID);
	    select1.name = "waittime_" + liID;
	    addOption(select1, '0', '<fmt:message key="alert.config.escalation.end"/>');
	    addOption(select1, '300000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="5"/></fmt:message>');
	    addOption(select1, '600000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="10"/></fmt:message>');
	    addOption(select1, '1200000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="20"/></fmt:message>');
	    addOption(select1, '1800000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="30"/></fmt:message>');
	    addOption(select1, '2700000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="45"/></fmt:message>');
	    addOption(select1, '3600000', '<fmt:message key="alert.config.escalation.wait"><fmt:param value="60"/></fmt:message>');
	
	    if(hqDojo.byId('usersList')) {
	      	usersEditDiv.innerHTML = hqDojo.byId('usersList').innerHTML;
	      	
	      	var usersInputList = usersEditDiv.getElementsByTagName('input');
	      
	      	for(i=0;i < usersInputList.length; i++) {
	          	var inputNamesArr = usersInputList[i];
	          
	          	inputNamesArr.name = inputNamesArr.name + "_" + liID;
	      	}
	    }
	}

	function addRow() {
	    var ni = hqDojo.byId('rowOrder');
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
	    var snmpDiv = document.createElement('div');
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
	
	    ni.appendChild(escLi);
	    escLi.setAttribute((document.all ? 'className' : 'class'), "lineitem");
	    escLi.setAttribute('id','row_'+ liID);
	    //escLi.innerHTML = "here";
	
	    escLi.appendChild(remDiv);
	    remDiv.setAttribute((document.all ? 'className' : 'class'), "remove");
	    remDiv.innerHTML ='<a href="#" onclick="removeRow(this);"><html:img page="/images/tbb_delete.gif" height="16" width="46" border="0"  alt="" /></a>';
	
	escLi.appendChild(escTable);
	escTable.setAttribute((document.all ? 'className' : 'class'), "escTbl");
	escTable.setAttribute('border', '0');
	escTable.appendChild(escTableBody);
	
	escTableBody.appendChild(escTr2);
	escTableBody.appendChild(escTr1);
	
	escTr1.appendChild(td1);
	
	td1.setAttribute('colspan', '3');
	td1.appendChild(document.createTextNode('<fmt:message key="alert.config.escalation.then"/> '));
	
	td1.appendChild(select1);
	select1.setAttribute('id', 'waittime_' + liID);
	
	select1.setAttribute('name', 'waittime_' + liID);
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
	addOption(select1, '2700000', '<fmt:message key="alert.config.escalation.wait">
	                              <fmt:param value="45"/>
	                            </fmt:message>');
	addOption(select1, '3600000', '<fmt:message key="alert.config.escalation.wait">
	                              <fmt:param value="60"/>
	                            </fmt:message>');
	
	escTr2.appendChild(td2);
	td2.setAttribute('valign', 'top');
	td2.setAttribute('class', 'td2');
	
	
	
	td2.appendChild(select2);
	select2.setAttribute('id', 'Email_' + liID);
	
	
	select2.onchange = function(){onchange_handler(this);}
	select2.setAttribute('name', 'action_' + liID);
	addOption(select2, 'Email', 'Email');
	addOption(select2, 'SMS', 'SMS');
	addOption(select2, 'Syslog', 'Sys Log');
	<c:if test="${snmpEnabled}">
	addOption(select2, 'SNMP', '<fmt:message key="alert.config.escalation.action.snmp.notification"/>');
	</c:if>
	
	addOption(select2, 'NoOp', 'Suppress Alerts');
	
	escTr2.appendChild(td3);
	td3.setAttribute('class', 'td3');
	td3.setAttribute('valign', 'top');
	
	td3.appendChild(select3);
	
	select3.setAttribute('name', 'who_' + liID);
	select3.setAttribute('id', 'who_' + liID);
	select3.onchange = function(){onchange_who(this);}
	addOption(select3, 'Select', '<fmt:message key="alert.config.escalation.notify.who"/>');
	<c:if test="${not empty AvailableRoles}">
	addOption(select3, 'Roles', '<fmt:message key="monitoring.events.MiniTabs.Roles"/>')
	</c:if>
	addOption(select3, 'Users', '<fmt:message key="monitoring.events.MiniTabs.Users"/>');
	addOption(select3, 'Others', '<fmt:message key="monitoring.events.MiniTabs.Others"/>');
	
	escTr2.appendChild(td4);
	td5.setAttribute('width', '50%');
	
	td4.appendChild(emailDiv);
	emailDiv.setAttribute('class', 'emailDiv');
	emailDiv.setAttribute('id', 'emailinput' + liID);
	hqDojo.byId('emailinput'+ liID).style.display = 'none';
	emailDiv.setAttribute('class', 'escInput');
	emailDiv.setAttribute('width', '40%');
	emailDiv.innerHTML = "email addresses (comma separated):<br><textarea rows=3 cols=35 id=emailinput_" + liID + " name=emailinput_" + liID + "></textarea>";
	
	td4.appendChild(sysDiv);
	sysDiv.setAttribute('class', 'escInput'+ liID);
	sysDiv.setAttribute('id', 'sysloginput'+ liID);
	hqDojo.byId('sysloginput'+ liID).style.display = 'none';
	sysDiv.setAttribute('width', '40%');
	sysDiv.innerHTML = "meta: <input type=text name=meta_" + liID + " size=40><br>" + "product: <input type=text name=product_" + liID + " size=40><br>" + "version: <input type=text name=version_" + liID + " size=40><br>";
	  
	        td4.appendChild(usersDiv);
	        usersDiv.setAttribute('id', 'usersDiv' + liID);
	        hqDojo.byId('usersDiv'+ liID).style.display = 'none';
	
	        if(hqDojo.byId('usersList')) {
	          usersDiv.innerHTML = hqDojo.byId('usersList').innerHTML;
	          var usersInputList = usersDiv.getElementsByTagName('input');
	          for(i=0;i < usersInputList.length; i++) {
	          var inputNamesArr = usersInputList[i];
	          inputNamesArr.name = inputNamesArr.name + "_" + liID;
	      }
	    }
	
	    td4.appendChild(rolesDiv);
	    rolesDiv.setAttribute('id', 'rolesDiv' + liID);
	    hqDojo.byId('rolesDiv'+ liID).style.display = 'none';
	
	    if(hqDojo.byId('rolesList')) {
	      rolesDiv.innerHTML = hqDojo.byId('rolesList').innerHTML;
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
	
	      if (index != "Syslog") {
	      hideSyslogInput(el);
	      } else {
	      showSyslogInput(el);
	      }
	
	      if (index == "SNMP") {
	      showSnmpInput(el);
	      } else {
	      hideSnmpInput(el);
	      }
	
	     if (index == "Syslog" || index == "NoOp" || index == "SNMP") {
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
	    var rolesDivIn = hqDojo.byId('rolesDiv' + getId[1]);
	    var usersDivIn = hqDojo.byId('usersDiv' + getId[1]);
	    var emailDivIn = hqDojo.byId('emailinput' + getId[1]);
	
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
        var whoSelector = hqDojo.byId('who_' + getId[1]);
        whoSelector.style.display='';
    }

    function hideWhoSelect(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var whoSelector = hqDojo.byId('who_' + getId[1]);
        whoSelector.style.display='none';
    }

    function showSyslogInput(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var syslogDivIn = hqDojo.byId('sysloginput' + getId[1]);
        syslogDivIn.style.display='';
    }

    function hideSyslogInput(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var syslogDivIn = hqDojo.byId('sysloginput' + getId[1]);
        syslogDivIn.style.display='none';
    }

    function showSnmpInput(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var snmpDivIn = hqDojo.byId('snmpinput' + getId[1]);
        snmpDivIn.style.display='';
    }

    function hideSnmpInput(el) {
        var idStr = el.id;
        var getId = idStr.split('_');
        var snmpDivIn = hqDojo.byId('snmpinput' + getId[1]);
        snmpDivIn.style.display='none';
    }

    function hideDisplay() {
        hqDojo.style(emailinput, "display", "none");
        hqDojo.style(sysloginput, "display", "none");
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
	  var escJson = eval( '(' + originalRequest.responseText + ')' );
	  if(escJson.error){
	      var escmsg = hqDojo.byId('escMsg');
	      escmsg.innerHTML = escJson.error;
	      var example = hqDojo.byId('example');
	      example.style.display= '';
	  }else{
	      document.EscalationSchemeForm.escId.value = escJson.escalation.id;
	      document.EscalationSchemeForm.submit();
	  }
    }

    function saveNewEscalation() {
        var escName = hqDojo.byId('escName').value;
        if (escName == "") {
            alert('<fmt:message key="alert.config.error.escalation.name.required"/>');
            return false;
        }

        var escDesc = hqDojo.byId('escDesc').value;
        if (escName.match(/['"]/) || escDesc.match(/['"]/)) {
            alert('<fmt:message key="error.input.badquotes"/>');
            return false;
        }

        sendEscForm();
        return false;
      }
        
      function initEsc () {
        // Set up the escalation dropdown
        var escJson = eval( '( { "escalations":<c:out value="${escalations}" escapeXml="false"/> })' );
        var escalationSel = hqDojo.byId('escIdSel');
        var schemes = escJson.escalations;

          for (var i = 0; i < schemes.length; i++) {

            if (schemes[i].name == "")
                continue;

            <c:if test="${not empty EscalationForm.escId}">
            if (schemes[i].name == '<c:out value="${EscalationForm.escId}"/>')
                addOption(escalationSel , schemes[i].name, schemes[i].name, true);
              else
            </c:if>

            addOption(escalationSel , schemes[i].name, schemes[i].name,
                      schemes[i].name == document.EscalationSchemeForm.escId.value);
        }
        
        document.EscalationForm.escName.value = document.EscalationSchemeForm.escId.value;
      <c:if test="${empty param.escId}">
        addRow();
      </c:if>
    }

   function sendEditEscForm() {
        var id;
        var gadId;
        var eId;
        var aetId;
        var escFormSerial = Form.serialize('viewEscalation');
        var url = '<html:rewrite action="/escalation/updateEscalation"/>';
        if (hqDojo.byId('gad')) {
           gadId = hqDojo.byId('gad').value;
        } else {
            gadId = '';
        }
        if (hqDojo.byId('ad')){
            adId = hqDojo.byId('ad').value;
        } else {
            adId = '';
        }
        if (hqDojo.byId('eid')){
            eID = hqDojo.byId('eid').value;
        } else {
           eId = '';
        }
        if (hqDojo.byId('aetid')) {
            aetId = hqDojo.byId('aetid').value;
        } else {
            aetId = '';
        }

        var pars = { 
        	"rowOrder": rowOrder,
        	"escForm": escFormSerial,
        	"ad": adId,
        	"gad": gadId,
        	"eid": eId,
        	"aetid": aetId
        };
        
        new Ajax.Request( url, {method: 'post', parameters: pars, onComplete: showResponse, onFailure :reportError} );

    }

    function sendEscForm() {
        var escFormSerial = Form.serialize('EscalationForm');
        var url = '<html:rewrite action="/escalation/saveEscalation"/>';
        if (hqDojo.byId('gad')) {
           gadId = hqDojo.byId('gad').value;
        } else {
            gadId = '';
        }
        if (hqDojo.byId('ad')){
            adId = hqDojo.byId('ad').value;
        } else {
            adId = '';
        }
        if (hqDojo.byId('eid')){
            eID = hqDojo.byId('eid').value;
        } else {
           eId = '';
        }
        if (hqDojo.byId('aetid')) {
            aetId = hqDojo.byId('aetid').value;
        } else {
            aetId = '';
        }

        var pars = "rowOrder=0&" + "escForm=" + escFormSerial + "&ad=" + adId + "&gad=" + gadId + "&eid=" + eId + "aetid=" + aetId;
        new Ajax.Request( url, {method: 'post', parameters: pars, onComplete: showResponse, onFailure: reportError} );
    }

    function configure(id) {
      var sel = hqDojo.byId('who' + id);
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
            var othersDivIn = hqDojo.byId('othersDiv_' + getId[1]);

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
      var usersDivIn = hqDojo.byId('usersDiv' + getId[1]);

      Dialog.confirm('<div id="usersConfigWindow">' + usersDivIn.innerHTML +
                     '</div>',
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                    var usersInputList =
                      usersDivIn.getElementsByTagName('input');
                    var updatedInputList =
                      hqDojo.byId('usersConfigWindow').getElementsByTagName('input');

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
      var rolesDivIn = hqDojo.byId('rolesDiv' + getId[1]);

      Dialog.confirm('<div id="rolesConfigWindow">' + rolesDivIn.innerHTML +
                     '</div>',
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                    var rolesInputList =
                      rolesDivIn.getElementsByTagName('input');
                    var updatedInputList =
                      hqDojo.byId('rolesConfigWindow').getElementsByTagName('input');

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
        var sectionID = hqDojo.byId('rowOrder');
        var order = Sortable.serialize(sectionID);
		var alerttext = Sortable.serialize('rowOrder') + '\n';

		alert(alerttext);

	}

    function hideExample() {
            hqDojo.byId('example').style.display= 'none';
    }


    function textCounter( field, countfield, maxlimit ) {
       if ( field.value.length > maxlimit )
       {
         field.value = field.value.substring( 0, maxlimit );
         hqDojo.byId('example').style.display= '';
         //hqDojo.byId('example').setAttribute((document.all ? 'className' : 'class'), "ErrorBlock");
         hqDojo.byId('escMsg').innerHTML ='<fmt:message key="error.Error.Tab"/> ' + '<fmt:message key="alert.config.error.250Char"/>';
        return false;
       } else {
         hqDojo.byId('escMsg').innerHTML ='';
         hqDojo.byId('example').style.display= 'none';
         return true;
        }
     }
</jsu:script>
<<jsu:script onLoad="true">
    hqDojo.connect(document, 'onkeypress', document.toggleSubmit);
    hqDojo.connect(document.forms[0], 'onkeypress', document.toggleSubmit);
    hqDojo.connect(document.forms[1], 'onkeypress', document.toggleSubmit);
</jsu:script>

<html:form action="/alerts/ConfigEscalation" method="GET">
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
  <c:if test="${not empty alertDef}">
    <c:choose>
      <c:when test="${not empty Resource}">
        <html:hidden property="eid" value="${Resource.entityId}" />
      </c:when>
      <c:otherwise>
        <html:hidden property="aetid" value="${ResourceType.appdefTypeKey}" />
      </c:otherwise>
    </c:choose>
  </c:if>
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

<div id="example" style="display:none;">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <td class="ErrorBlock" width="9">
    <html:img page="/images/tt_error.gif" height="9" width="9" border="0" alt=""/>
  </td>
  <td class="ErrorBlock" width="100%">
    <div id="escMsg"></div>
  </td>
</table>
</div>

<div style="margin-bottom:10px;border:1px solid #D5D8DE;padding: 3px;" class="ListHeaderInactive">
       <fmt:message key="inform.config.escalation.scheme.newescalation.escalationexplanation" />
         <c:if test="${not useroperations['createEscalation']}">
           <fmt:message key="inform.config.escalation.scheme.select" />
         </c:if>
 </div>

<c:if test="${useroperations['createEscalation']}">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tbody>
    <tr>
      <td class="BlockTitle" colspan="2">
          Step 1 - <fmt:message key="common.label.CreateNew" />
          <c:choose>
            <c:when test="${not empty param.aname}">
              <c:set var="escapedAlertDefName">
              	<c:out value="${param.aname}" />
              </c:set>
              <fmt:message key="alert.config.escalation.scheme.for">
                <fmt:param value="${escapedAlertDefName}"/>
              </fmt:message>
              <c:choose>
                <c:when test="${not empty param.ad}">
                  <input type="hidden" id="ad" name="ad" value='<c:out value="${param.ad}"/>' />
                </c:when>
                <c:when test="${not empty param.gad}">
                  <input type="hidden" id="gad" name="gad"
                  value='<c:out value="${param.gad}"/>' />
                </c:when>
              </c:choose>
            </c:when>
            <c:otherwise>
              <fmt:message key="alert.config.escalation.scheme" />
            </c:otherwise>
          </c:choose>
      </td>
    </tr>
    <tr class="ListRow">
      <td class="BlockLabel" style="padding:3px;" width="20%">
          <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="common.label.Name" />
      </td>
      <td style="padding:3px;" align="left" width="80%">
          <input type="text" size="23" name="name" id="escName" />
      </td>
    </tr>
    <tr class="ListRow">
      <td class="BlockLabel" valign="top" style="padding:3px;" width="20%">
          <fmt:message key="common.label.Description" />
      </td>
      <td style="padding:3px;" align="left" width="80%">
          <textarea name="description" id="escDesc" onkeypress="textCounter(this,this.form.counter,250);" onblur="textCounter(this,this.form.counter,250);"></textarea>
      </td>
    </tr>
  </tbody>
</table>
<table width="100%" cellpadding="3" cellspacing="0" border="0" id="createEscTable">
  <tbody>
    <tr>
      <td class="BlockTitle"><fmt:message key="alert.config.escalation.acknowledged"/></td>
    </tr>
    <tr class="ListRow">
      <td style="padding-left:15px;padding-bottom:10px;">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tbody>
          <tr>
            <td style="padding-top:2px;padding-bottom:2px;">
				<span id="AlertEscalationOption"><input type="radio" name="allowPause" value="true" />
				<fmt:message key="alert.config.escalation.allow.pause" />&nbsp;</span>
			</td>
          </tr>
          <tr>
            <td style="padding-top:2px;padding-bottom:2px;"><input
              type="radio" name="allowPause" value="false" checked="true" /> <fmt:message
              key="alert.config.escalation.allow.continue" /></td>
          </tr>
        </tbody>
      </table>
      </td>
    </tr>
    <tr>
      <td class="BlockTitle"><fmt:message
        key="alert.config.escalation.state.change" /><br>
      </td>
    </tr>
    <tr class="ListRow">
      <td style="padding-left:15px;padding-bottom:10px;">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tbody>
          <tr>
            <td style="padding-top:2px;padding-bottom:2px;"><input
              type="radio" name="notifyAll" value="false" checked="true" /> <fmt:message
              key="alert.config.escalation.state.change.notify.previous" /></td>
          </tr>
          <tr>
            <td style="padding-top:2px;padding-bottom:2px;"><input
              type="radio" name="notifyAll" value="true" /> <fmt:message
              key="alert.config.escalation.state.change.notify.all" /></td>
          </tr>
        </tbody>
      </table>
      </td>
    </tr>
    <tr>
      <td class="BlockTitle"><fmt:message key="alert.config.escalation.state.ended" /><br>
      </td>
    </tr>
    <tr class="ListRow">
      <td style="padding-left:15px;padding-bottom:10px;">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tbody>
          <tr>
            <td style="padding-top:2px;padding-bottom:2px;"><input
              type="radio" name="repeat" value="false" checked="true" /> <fmt:message
              key="alert.config.escalation.state.ended.stop" /></td>
          </tr>
          <tr>
            <td style="padding-top:2px;padding-bottom:2px;"><input
              type="radio" name="repeat" value="true" /> <fmt:message
              key="alert.config.escalation.state.ended.repeat" /></td>
          </tr>
        </tbody>
      </table>
      </td>
    </tr>
    <tr class="ToolbarContent"><!-- SET TOOLBAR -->
      <td>
        <tiles:insert page="/common/components/ActionButton.jsp">
          <tiles:put name="labelKey" value="common.label.Next"/>
          <tiles:put name="buttonClick" value="return saveNewEscalation();"/>
        </tiles:insert>
      </td>
    </tr>
  </tbody>
</table>

<div id="usersList" style="display:none;">
<div class="ListHeader">Select Users</div>
<ul class="boxy">
  <c:forEach var="user" items="${AvailableUsers}" varStatus="status">
    <li class="ListRow"><input type="checkbox" name="users"
      value="<c:out value="${user.id}"/>"> <c:out
      value="${user.name}" /></input></li>
  </c:forEach>
</ul>
</div>

<c:if test="${not empty AvailableRoles}">
  <div id="rolesList" style="display: none;">
  <div class="ListHeader">Select Roles</div>
  <ul class="boxy">
    <c:forEach var="role" items="${AvailableRoles}" varStatus="status">
      <li class="ListRow"><input type="checkbox" name="roles"
        value="<c:out value="${role.id}"/>"> <c:out
        value="${role.name}" /></input></li>
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
      <td class="BlockTitle"><fmt:message key="alert.config.escalation.acknowledged"/></td>
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
              key="alert.config.escalation.allow.pause" /> </div>
            </td>
          </tr>
        </tbody>
      </table>
      </td>
    </tr>
  </tbody>
</table>
<jsu:script>
  var escalationSpan = hqDojo.byId("AlertEscalationOption");
  if (escalationSpan != null) {
	  escalationSpan.appendChild(hyperic.form.createEscalationPauseOptions({id: "maxWaitTime", name: "maxWaitTime"}));
  }

  var pauseTimeEditDiv = hqDojo.byId("pauseTimeEdit");
  if (pauseTimeEditDiv != null) {
	  pauseTimeEditDiv.appendChild(hyperic.form.createEscalationPauseOptions({id: "maxWaitTime_<c:out value="${alertDef.id}"/>", name: "maxWaitTime"}));
  }
</jsu:script>
<br>
<br>
<input type="hidden" value="" id="creationTime"> <input type="hidden"
  value="" id="_version_"> <input type="hidden" value="" id="notifyAll">
<input type="hidden" value="" id="modifiedTime"> <input type="hidden"
  value="" id="allowPause"> <input type="repeat" value="" id="repeat">
  <input type="hidden" value="" id="escName">
<input type="hidden" value="" id="id"></form>
</c:if>

