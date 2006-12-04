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

<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-c" prefix="c" %>

<script type="text/javascript">
    function getGroupOrder() {
		var sections = $('testlist');
		var alerttext = '';
		var sectionID = $('testlist');
		var order = Sortable.serialize(sectionID);
		
		rowSeq = Sortable.sequence($('testlist'))+ '\n';
		return rowSeq;
		alert(rowSeq);
	}
	
	function addRow() {
		var ni = $('testlist');
        var numi = document.getElementById('theValue');
        var num = (document.getElementById('theValue').value -1)+ 2;
        
        numi.value = num;
        var liID = 'row_'+num;
        var escLi = document.createElement('li');
        var remDiv = document.createElement('div');
        var escTable = document.createElement('table');
        var escTr1 = document.createElement('tr');
        var escTr2 = document.createElement('tr');
        var td1 = document.createElement('td');
		var td2 = document.createElement('td');
		var td3 = document.createElement('td');
		var td4 = document.createElement('td');
		var select1 = document.createElement("select");
		var select2 = document.createElement("select");
		var anchor = document.createElement("a");
		
		ni.appendChild(escLi);
		escLi.setAttribute((document.all ? 'className' : 'class'), "lineitem");
        escLi.setAttribute('id', liID);
        
        escLi.appendChild(remDiv);
        remDiv.setAttribute((document.all ? 'className' : 'class'), "remove");
        
        
        //remDiv.innerHTML ="<a href=\"javascript:;\" onclick=\"removeEvent(\'" + escLi + "\');Behaviour.apply();\"><img src=\"images/tbb_delete.gif\" height=\"16\" width=\"46\" border=\"0\" >";
        
        
		escLi.appendChild(escTable);
		escTable.setAttribute((document.all ? 'className' : 'class'), "escTbl");
		
		escTable.appendChild(escTr1);
		escTr1.appendChild(td1);
		td1.setAttribute('width', '25%');

		td1.appendChild(document.createTextNode('Email: '));    
		
	       
		escTr1.appendChild(td2);
		td2.setAttribute('width', '25%');
		
		td2.appendChild(select1);
		select1.setAttribute('id', 'select1');
		select1.name = "select1_" + liID;
		addOption(select1, 'Role', 'role');
		addOption(select1, 'sysadmin1', 'sysadmin1');
		addOption(select1, 'sysadmin2', 'sysadmin2');
		addOption(select1, 'VP of Engineering', 'VP of Engineering');
		
		escTr1.appendChild(td3);
		td3.setAttribute('width', '50%');
		td3.appendChild(anchor);
		anchor.appendChild(document.createTextNode('Configure'));       
		
		escTable.appendChild(escTr2);
		escTr2.appendChild(td4);
		td4.setAttribute('colspan', '3');
		td4.appendChild(document.createTextNode('Then wait: '));
		
		td4.appendChild(select2);
		select2.setAttribute('id', 'select1');
		select2.name = "select2_" + liID;
		addOption(select2, '20', '20 minutes');
		addOption(select2, '10', '10 minutes');
		addOption(select2, '5', '5 minutes');
        
	}
	
	function removeElement(divNum) {
		var d = document.getElementById('testlist');
		var olddiv = document.getElementById(divNum);
	    d.removeChild(olddiv);
	}
	
	function addOption(sel, val, txt) {
		var o = document.createElement("OPTION");
		var t = document.createTextNode(txt);
		o.setAttribute("value",val);
		o.appendChild(t);
		sel.appendChild(o);
	}

	function showResponse (originalRequest) {
		var newData = eval("(" + originalRequest.responseText + ")");
		$('example').innerHTML = newData;
	}

	function init () {
		$('submit').onclick = function () {
		sendEscForm();
		}
	}

/* this is where we need to define the GET URL for the returned JSON (var url = 'sample5.html';) needs to be replaced. Right now it's just being written into a div at the bottom of the page */

	function sendEscForm () {
		var url = 'sample5.html';
		var pars = 'escForm=' + Form.serialize('escalationForm');
		var myAjax = new Ajax.Request( url, {method: 'get', parameters: pars, onComplete: showResponse} );
	}
</script>
<style type="text/css">
  #section #testlist { 
      list-style-type:none;
      margin:0;
      padding:0;
   }
   #section #testlist li {
    
     font:13px Verdana;
     margin:0;
     margin-left:20px;
     padding-left:20px;
     padding:4px;
     cursor:move;
   }
   
   .escTbl {
   width:100%;
    margin-top:10px;
   margin-bottom:10px;
   }
   
   #testlist li {
   list-style: none; 
   width:90%;
   border-bottom:1px solid gray;
   }
 
 .remove {
 float:right;
 margin-right:10px;
 }
</style>
 
<body bgcolor="#FFFFFF" style="margin:15px;" onload="init();">
  <table width="100%" cellpadding="3" cellspacing="0" border="0">
    <tbody>
      <tr>
        <td colspan="2" id="section" width="100%">
          <form name="escalationForm" id="escalationForm">
            <input type="hidden" value="0" id="theValue">
            <ul id="testlist">
              <li id="testlist_row_1" class="lineitem">
                <div class="remove">
                  <a href="#" style="text-decoration:none;"><html:img page="/images/tbb_delete.gif" height="16" width="46" border="0" /></a>
                </div>
                <table cellpadding="0" cellspacing="0" border="0" width="100%">
                  <tr>
                    <td><select name="action">
                      <option selected value="Email">
                        Email
                      </option>
                      <option value="SMTP">
                        SMTP trap
                      </option>
                      <option value="SMS">
                        SMS
                      </option>
                    </select></td>
                    <td style="padding-right:20px;"><select name="who">
                      <option value="Sys Admin 1">
                        Sys Admin 1
                      </option>
                      <option value="Sys Admin 2">
                        Sys Admin 2
                      </option>
                      <option value="Sys Admin Manager">
                        Sys Admin Manager
                      </option>
                      <option value="VP of Engineering">
                        VP of Engineering
                      </option>
                      <option selected value="Role">
                        Role
                      </option>
                    </select></td>
                    <td width="45%"><a href="">Configure...</a></td>
                  </tr>
                  <tr>
                    <td colspan="3" style="padding-top:5px;padding-bottom:5px;padding-left:30px;">Then wait <select name="time">
                      <option value="10">
                        5 minutes
                      </option>
                      <option value="10">
                        10 minutes
                      </option>
                      <option selected value="20">
                        20 minutes
                      </option>
                    </select></td>
                  </tr>
                </table>
              </li>
            </ul><br>
            <a href="#" onclick="addRow();" style="text-decoration:none;"><html:img page="/images/tbb_addtolist.gif" height="16" width="85" border="0" /></a><br>
            <br>
          </form>
        </td>
      </tr>
      <tr>
        <td style="padding-left:5px;padding-top:10px;padding-bottom:5px;border-top:1px solid #d9d9d9;" colspan="2">If the alert is acknowledged:<br></td>
      </tr>
      <tr>
        <td style="padding-left:15px;padding-bottom:10px;" colspan="2">
          <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type="radio" name="pause" value="pauseTime"> Allow user to pause escalation for <select name="time">
                <option value="10 minutes">
                  5 minutes
                </option>
                <option value="10 minutes">
                  10 minutes
                </option>
                <option selected value="20 minutes">
                  20 minutes
                </option>
              </select> min<br></td>
            </tr>
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type="radio" name="pause" value="pauseTime"> Allow user to pause escalation for <select name="time">
                <option value="10">
                  5 minutes
                </option>
                <option value="10">
                  10 minutes
                </option>
                <option selected value="20">
                  20 minutes
                </option>
              </select> min</td>
            </tr>
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type="radio" name="pause" value="pauseTime"> Continue escalation without pausing</td>
            </tr>
          </table>
        </td>
      </tr>
      <tr>
        <td style="padding-left:5px;padding-top:10px;border-top:1px solid #d9d9d9;padding-bottom:5px;" colspan="2">If the alert is fixed:<br></td>
      </tr>
      <tr>
        <td style="padding-left:15px;padding-bottom:10px;" colspan="2">
          <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type="radio" name="pause" value="pauseTime"> Notify only previously notified users of the fix</td>
            </tr>
            <tr>
              <td style="padding-top:2px;padding-bottom:2px;"><input type="radio" name="pause" value="pauseTime"> Notify entire escalation chain of the fix</td>
            </tr>
          </table>
        </td>
      </tr>
      <tr>
        <td align="right"><input type="button" onclick="submitEscForm();" id="submit" value="submit">&nbsp;&nbsp; &nbsp;<input name="Cancel" id="cancel" type="button" value="Cancel"><!--<input type="button" onClick="getGroupOrder();Behaviour.apply()" value="Debug: Show serialized list order">--></td>
      </tr>
      <tr>
        <td>
          <br>
          <div id="example"></div>
        </td>
      </tr>
    </tbody>
  </table>
</body>
</html>