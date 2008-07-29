<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<tiles:importAttribute name="resource" />

<table border="0"><tr><td class="LinkBox">
<html:link page="/resource/group/Inventory.do?mode=new"><fmt:message key="resource.hub.NewGroupLink"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
    <tiles:insert definition=".resource.common.quickFavorites">
      <tiles:put name="resource" beanName="resource"/>
    </tiles:insert>
    <br>
    <a href="#" onclick="javascript:maintenance_<c:out value='${resource.id}'/>.dialog.show();">Schedule Maintenance</a>
</table>

<div class="maintenance_dialog" id="maintenance<c:out value='${resource.id}'/>">
    <span id="existing_downtime_<c:out value='${resource.id}'/>"></span>
    <fieldset><legend>From:</legend>
        <label for="from_date">Date: </label>
        <input type="text" name="from_date" id="from_date"><br>
        <label for="from_time">Time: </label>
        <input type="text" name="from_time" id="from_time">
    </fieldset>
    <fieldset><legend>To:</legend>
        <label for="to_date">Date: </label>
        <input type="text" name="to_date" id="to_date"><br>
        <label for="to_time">Time: </label>
        <input type="text" name="to_time" id="to_time">
    </fieldset>
    <span id="clear_schedule_btn"></span>
    <span id="schedule_btn"></span>
</div>

<script type="text/javascript">
dojo11.require("dijit.dijit");
dojo11.require("dijit.form.Button");
dojo11.require("dijit.form.DateTextBox");
dojo11.require("dijit.form.tzDateTextBox");
dojo11.require("dijit.form.TimeTextBox");
dojo11.require("dijit.Dialog");

var maintenance_<c:out value="${resource.id}"/> = null;
dojo11.addOnLoad(function(){
    maintenance_<c:out value="${resource.id}"/> = new hyperic.maintenance_schedule(<c:out value="${resource.id}"/>);
});
</script>