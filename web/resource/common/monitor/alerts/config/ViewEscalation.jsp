<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-c" prefix="c" %>

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
<script language="JavaScript" src="<html:rewrite page="/js/scriptaculous.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/dashboard.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>

<script type="text/javascript">
    function getGroupOrder() {
		var sections = $('order');
		var alerttext = '';
		var sectionID = $('order');
		var order = Sortable.serialize(sectionID);
		
		rowSeq = Sortable.sequence($('order'))+ '\n';
		return rowSeq;
		alert(rowSeq);
	}
	
	function addRow() {
		var ni = $('order');
        var numi = document.getElementById('theValue');
        var num = (document.getElementById('theValue').value -1)+ 2;
        
        numi.value = num;
        var liID = 'row'+num;
        var escLi = document.createElement('li');
        var remDiv = document.createElement('div');
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
        escLi.setAttribute('id', 'row' + liID);
        
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

		td1.setAttribute('colspan', '4');
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
       	addOption(select2, 'Syslog', 'Sys Log');
       	
		escTr2.appendChild(td3);
		td3.setAttribute('width', '20%');
		td3.setAttribute('valign', 'top');
        td3.style.paddingRight = "20px";
        td3.id = "whoSelect";
		
        td3.appendChild(select3);
        select3.name = "who_" + liID;
        <c:if test="${not empty AvailableRoles}">
		addOption(select3, 'Roles', '<fmt:message key="monitoring.events.MiniTabs.Roles"/>')
        </c:if>
		addOption(select3, 'Users', '<fmt:message key="monitoring.events.MiniTabs.CAMusers"/>');
		addOption(select3, 'Others', '<fmt:message key="monitoring.events.MiniTabs.OR"/>');
		
		function onchange_handler(el) {
        //alert(el+", value="+ el.options[el.selectedIndex].value );
        var index= el.options[el.selectedIndex].value

         if (index == "Email") {
			 // If the current option is mtg - show time; show attend
			showEmailInput();
			hideSyslogInput();
            $('whoSelect').style.display = '';
           }
		   else if (index == "Syslog") {
			hideEmailInput();
			showSyslogInput();
            $('whoSelect').style.display = 'none';
           }
        } 
		
		escTr2.appendChild(td4);
		td5.setAttribute('width', '50%');
		
		td4.appendChild(emailDiv);
		//emailDiv.style.display = 'none';
		emailDiv.setAttribute('id', 'emailinput');
		emailDiv.setAttribute('class', 'escInput');
		emailDiv.setAttribute('width', '40%');
		emailDiv.innerHTML = "email addresses (comma separated):<br><textarea rows=3 cols=35 id=emailinput_" + liID + "></textarea>";
		
		td4.appendChild(sysDiv);
		sysDiv.style.display = 'none';
		sysDiv.setAttribute('class', 'escInput');
		sysDiv.setAttribute('id', 'sysloginput');
		sysDiv.setAttribute('width', '40%');
		sysDiv.innerHTML = "meta: <input type=text name=meta_" + liID + " size=40><br>" + "product: <input type=text name=product_" + liID + "size=40><br>" + "version: <input type=text name=version_" + liID + "size=40><br>";
	}
	    
     function onchange_staticRow(el) {
    	 var index= el.options[el.selectedIndex].value
        
        if (index == "Email") {
			$('emailinput0').style.display='';
			$('syslog0').style.display = 'none';
            $('who').style.display = '';
           } else if (index == "Syslog") {
			$('emailinput0').style.display='none';
			$('syslog0').style.display='';
           $('who').style.display = 'none';
           }
        } 


	function showEmailInput() { 
	$(emailinput).style.display='';
	}
	
	function hideEmailInput() { 
	$(emailinput).style.display='none';
	}
	
	function showSyslogInput() {
	$(sysloginput).style.display='';
	}
	
	function hideSyslogInput() {
	$(sysloginput).style.display='none';
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
		$('example').innerHTML = "<span style=font-weight:bold;>Escalation Saved: " + Form.serialize('EscalationForm') + "</span>";
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
            sendEscForm();
            return false;
		}
	}

    onloads.push( initEsc );
    
 /*   
	function submitForm() {
	//var sort = Sortable.create('order',{tag:'li',only:'section',handle:'handle'});
	var url = '<html:rewrite action="/escalation/saveEscalation"/>';
	text = Sortable.serialize('EscalationForm');
	alert(text);
	var params = 'postText=' + text;
	ajaxEngine.sendRequest(example, {method:'post',parameters: pars, onComplete: showResponse});
	}
*/

	function sendEscForm() {
		var adId = $('ad').value;
		var escFormSerial = Form.serialize('EscalationForm');
	    var url = '<html:rewrite action="/escalation/saveEscalation"/>';
		var pars = "escForm=" + escFormSerial + "&ad=" + adId;
		new Ajax.Request( url, {method: 'post', parameters: pars, onComplete: showResponse} );
	}

    function configure(id) {
      var sel = $('who' + id);
      var selval = sel.options[sel.selectedIndex].value;

      if (selval == 'Users') {
        // Select checkboxes based on existing configs
        configureUsers();
      }
      else if (selval == 'Others') {
        // Set the inner text
        configureOthers();
      }
      else if (selval == 'Roles') {
        // Select checkboxes based on existing configs
        configureRoles();
      }
    }

    function configureOthers() {
      Dialog.confirm('<textarea name=emailAddresses cols=80 rows=3></textarea>',
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                    // Do something
                    new Effect.Shake(Windows.focusedWindow.getId());
                    return true;
                  }});
    }

    function configureUsers() {
      Dialog.confirm($('usersList').innerHTML,
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                    // Do something
                    new Effect.Shake(Windows.focusedWindow.getId());
                    return true;
                  }});
    }

    function configureRoles() {
      Dialog.confirm($('rolesList').innerHTML,
                  {windowParameters: {className:'dialog', width:305, height:200,
                   resize:false, draggable:false},
                  okLabel: "OK", cancelLabel: "Cancel",
                  ok:function(win) {
                    // Do something
                    new Effect.Shake(Windows.focusedWindow.getId());
                    return true;
                  }});
    }

    function schemeChange(sel) {
      document.EscalationSchemeForm.escId.value =
        sel.options[sel.selectedIndex].value;
      document.EscalationSchemeForm.submit();
    }
    
 /*   
    function renameFormInput() {
    var EscalationForm = document.forms[1];
    var formEls = EscalationForm.elements;
    var formVals = formEls.values;
    alert(formVals);
    for (var i = 0; i < formVals.length; i++) {
    	alert(formVals);
    }
}
*/

sections = ['section'];

	function createNewSection(name) {
		var name = $F('sectionName');
		if (name != '') {
			var newDiv = Builder.node('div', {id: 'group' + (sections.length + 1), className: 'section', style: 'display:none;' }, [
				Builder.node('h3', {className: 'handle'}, name)
			]);

			sections.push(newDiv.id);
			$('page').appendChild(newDiv);
			Effect.Appear(newDiv.id);
			destroyLineItemSortables();
			createLineItemSortables();
			createGroupSortable();
		}
	}

	function createLineItemSortables() {
		for(var i = 0; i < sections.length; i++) {
			Sortable.create(sections[i],{tag:'li',dropOnEmpty: true, containment: sections,only:'lineitem'});
		}
	}

	function destroyLineItemSortables() {
		for(var i = 0; i < sections.length; i++) {
			Sortable.destroy(sections[i]);
		}
	}

	function createGroupSortable() {
		Sortable.create('order',{tag:'li',only:'section',handle:'handle'});
	}

</script>

<html:form action="/alerts/ConfigEscalation">
  <html:hidden property="mode"/>
  <c:choose>
    <c:when test="${not empty param.ad}">
  	  <input type=hidden id="ad" name="ad" value="<c:out value="${param.ad}"/>"/>
  	</c:when>
  	<c:when test="${not empty param.gad}">
  	  <input type=hidden id="gad" name="gad" value="<c:out value="${param.gad}"/>"/>
  	</c:when>
  </c:choose>
  <c:choose>
    <c:when test="${not empty Resource}">
      <html:hidden property="eid" value="${Resource.entityId}"/>
    </c:when>
    <c:otherwise>
      <html:hidden property="aetid" value="${ResourceType.appdefTypeKey}"/>
    </c:otherwise>
  </c:choose>
  <html:hidden property="escId"/>
</html:form>
 
<form action="<html:rewrite action="/escalation/saveEscalation"/>" name="EscalationForm" id="EscalationForm">
<input type="hidden" value="0" id="pid">
<input type="hidden" value="0" id="pversion">

<input type="hidden" value="0" id="if the escalation is new or not">

  <input type="hidden" value="0" id="theValue">
  <c:choose>
    <c:when test="${not empty Resource}">
      <html:hidden property="eid" value="${Resource.entityId}"/>
    </c:when>
    <c:otherwise>
      <html:hidden property="aetid" value="${ResourceType.appdefTypeKey}"/>
    </c:otherwise>
  </c:choose>
  <c:choose>
    <c:when test="${not empty param.ad}">
  	  <input type=hidden id="ad" name="ad" value="<c:out value="${param.ad}"/>"/>
  	</c:when>
  	<c:when test="${not empty param.gad}">
  	  <input type=hidden id="gad" name="gad" value="<c:out value="${param.gad}"/>"/>
  	</c:when>
  </c:choose>
<input type="hidden" id="ffff" name="ggg" value="<c:out value="${escalation.id}"/>" />
    <table width="100%" cellpadding="3" cellspacing="0" border="0">
    <tbody>
      <tr class="tableRowHeader">
        <td align="right">
          Escalation Scheme:
          <select id="escId" name="escId" onchange="schemeChange(this)">
            <option value="0"><fmt:message key="common.label.CreateNew"/></option>
          </select>
          
          <fmt:message key="common.label.Name"/>
          <input type=text size="25" name="escName" id="escName"/>
        </td>
      </tr>
        <tr class="tableRowAction">
        <td id="section" width="100%">

            <ul id="order">
              <li id="order_row1" class="lineitem">
				<div id="remove" class="remove" style="padding-top:10px;">
                  <a href="#" style="text-decoration:none;"><html:img page="/images/tbb_delete.gif" height="16" width="46" border="0"/></a>
                </div>

                <table cellpadding="3" cellspacing="0" border="0" width="100%">
                    <tr>
                        <td width="20%" valign="top" style="padding-bottom:10px;padding-left:20px;padding-top:5px;">
                            <select name="action_row0" onchange="onchange_staticRow(this);">
                                <option selected value="Email">
                                    Email 
                                </option>
                                <option value="Syslog">
                                    Sys log 
                                </option>
                            </select>
                        </td>
                        <td width="20%" style="padding-right:20px;" valign="top" id="who">
                            <select id="who_row0" name="who_row0">
                                <c:if test="${not empty AvailableRoles}">
                                    <option value="Roles">
                                        <fmt:message key="monitoring.events.MiniTabs.Roles" />
                                    </option>
                                </c:if>
                                <option value="Users">
                                    <fmt:message key="monitoring.events.MiniTabs.CAMusers" />
                                </option>
                                <option value="Others">
                                    <fmt:message key="monitoring.events.MiniTabs.OR" />
                                </option>
                            </select>
                        </td>
                        <td width="60%" valign="top">
                            <div id="emailinput0">
                                email addresses (comma separated): 
                                <br>
                <textarea rows="3" cols="35" name="users_row0" id="emailinput_0"></textarea>
                            </div>
                            <div id="syslog0" style="display:none;">
                                meta: 
                                <input type="text" name="meta_0" value="" size="40">
                                <br>
                                product: 
                                <input type="text" name="product_0" value="" size="40">
                                <br>
                                version: 
                                <input type="text" name="version_0" value="" size="40">
                                <br>
                            </div>
                    </tr>
                    <tr>
                        <td colspan="4" style="padding-top:5px;padding-bottom:5px;">
                            <fmt:message key="alert.config.escalation.then"/>
                            <select name="time_row0">
                                <option value="0">
                                    <fmt:message key="alert.config.escalation.end"/>
                                </option>
                                <option value="300000">
                                    <fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="5"/>
                                    </fmt:message>
                                </option>
                                <option value="600000">
                                    <fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="10"/>
                                    </fmt:message>
                                </option>
                                <option value="1200000">
                                    <fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="20"/>
                                    </fmt:message>
                                </option>
                                <option value="1800000">
                                    <fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="30"/>
                                    </fmt:message>
                                </option>
                                <option value="2400000">
                                    <fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="45"/>
                                    </fmt:message>
                                </option>
                                <option value="3000000">
                                    <fmt:message key="alert.config.escalation.wait">
                                      <fmt:param value="60"/>
                                    </fmt:message>
                                </option>
                            </select>
                        </td>
                    </tr>
                </table>
                </li>
            </ul>
            <table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
              <tr>
                <td width="40"><a href="#" onclick="addRow();" style="text-decoration:none;"><html:img page="/images/tbb_addtolist.gif" height="16" width="85" border="0"/></a></td>
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
              <td style="padding-top:2px;padding-bottom:2px;"><input type=radio name="allowPause" value="true"/> Allow user to pause escalation for

              <select id="maxWaitTime" name="maxwaittime">

                <option value="300000">
                  5 minutes
                </option>
                <option value="600000">
                  10 
                </option>
                <option value="1200000">
                  20 
                </option>
                <option value="1800000">
                  30 
                </option>
              </select></td>
            </tr>
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type=radio name="allowPause" value="false"/> Continue escalation without pausing</td>
            </tr>
          </table>
        </td>
      </tr>
      <tr>
        <td  class="tableRowHeader">If the alert state has changed:<br></td>
      </tr>
      <tr class="ListRow">
        <td style="padding-left:15px;padding-bottom:10px;">
          <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type=radio name="notification" value="0"/> Notify previously notified users of the change</td>
            </tr>
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type=radio name="notification" value="1"/> Notify entire escalation chain of the change</td>
            </tr>
          </table>
        </td>
      </tr>
    </tbody>
  </table>

<br><br>
 <input type=button value="Submit" onclick="sendEscForm();" id="submit"></input>


</form>

<div id="example" style="padding:10px;"></div>

<div id="usersList" style="display: none;">
  <c:forEach var="user" items="${AvailableUsers}" varStatus="status">
  <table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr class="ListRow">
    <td class="ListCell">
      <input type=checkbox name=u<c:out value="${user.id}"/>><c:out value="${user.name}"/></input>
    </td>
  </tr>
  </table>
  </c:forEach>
</div>

<c:if test="${not empty AvailableRoles}">
<div id="rolesList" style="display: none;">
  <c:forEach var="role" items="${AvailableRoles}" varStatus="status">
  
  <table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr class="ListRow">
    <td class="ListCell">
      <input type=checkbox name=r<c:out value="${role.id}"/>><c:out value="${role.name}"/></input>
    </td>
  </tr>
  </table>
  </c:forEach>
</div>
</c:if>
