<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%--
  NOTE: This copyright does *not* cover user programs that use Hyperic
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2011], VMware, Inc.
  This file is part of Hyperic.
  
  Hyperic is free software; you can redistribute it and/or modify
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

<tiles:importAttribute name="resource" ignore="true" />

<c:if test="${not empty resource}">

<div id="maintenance<c:out value='${resource.id}'/>">
    <div id="maintenance_status_<c:out value='${resource.id}'/>" style="display: none"></div>
    <div id="existing_downtime_<c:out value='${resource.id}'/>"></div>
	<div style="width: 300px">    
	<fieldset>
		<legend><fmt:message key="resource.group.MaintenanceWindow.From"/>:</legend>
        <label for="from_date" style="text-align: right; vertical-align: top"><fmt:message key="resource.group.MaintenanceWindow.Date"/>:&nbsp;</label>
        <input type="text" name="from_date" id="from_date" /><br />
        <label for="from_time" style="text-align: right; vertical-align: top; white-space: nowrap" id="maintenance_from_time_timezone"></label>
        <input type="text" name="from_time" id="from_time" />
    </fieldset>
    <fieldset>
		<legend><fmt:message key="resource.group.MaintenanceWindow.To"/>:</legend>
        <label for="to_date" style="text-align: right; vertical-align: top"><fmt:message key="resource.group.MaintenanceWindow.Date"/>:&nbsp;</label>
        <input type="text" name="to_date" id="to_date" /><br />
        <label for="to_time" style="text-align: right; vertical-align: top; white-space: nowrap" id="maintenance_to_time_timezone"></label>
        <input type="text" name="to_time" id="to_time" />
    </fieldset>
	</div>
	<div style="text-align:right;">
    	<span id="clear_schedule_btn"></span>
    	<span id="schedule_btn"></span>
		<span style="vertical-align:middle;padding-left:5px" class="maintenanceLink">
			<a href="#" id="maintenance_cancel_link_<c:out value='${resource.id}'/>"><fmt:message key="resource.group.MaintenanceWindow.Cancel"/></a>
		</span>
	</div>
</div>
<jsu:script>
	hqDojo.require("dijit.dijit");
	hqDojo.require("dijit.form.Button");
	hqDojo.require("dijit.form.DateTextBox");
	hqDojo.require("dijit.form.TimeTextBox");
	hqDojo.require("dijit.Dialog");
	hqDojo.require("dojo.date");
	
	hyperic.data.maintenance_schedule = {};
	hyperic.data.maintenance_schedule.label = {
		schedule : "<fmt:message key="resource.group.MaintenanceWindow.Schedule"/>",
		reschedule : "<fmt:message key="resource.group.MaintenanceWindow.Reschedule"/>",
		cancel : "<fmt:message key="resource.group.MaintenanceWindow.Cancel"/>",
		clear : "<fmt:message key="resource.group.MaintenanceWindow.Clear"/>",
		time : "<fmt:message key="resource.group.MaintenanceWindow.Time"/>"
	};
	hyperic.data.maintenance_schedule.message = {
		success : "<fmt:message key="resource.group.MaintenanceWindow.confirm.Updated"/>",
		currentSchedule : "<fmt:message key="resource.group.MaintenanceWindow.status.CurrentSchedule"/>",
		noSchedule : "<fmt:message key="resource.group.MaintenanceWindow.status.NoSchedule"/>",
		runningSchedule : "<fmt:message key="resource.group.MaintenanceWindow.status.RunningSchedule"/>"
	};
	hyperic.data.maintenance_schedule.error = {
		serverError : "<fmt:message key="resource.group.MaintenanceWindow.error.ServerError"/>",
		datePattern : "<fmt:message key="resource.group.MaintenanceWindow.error.DatePattern"/>",
		startDateRange : "<fmt:message key="resource.group.MaintenanceWindow.error.StartDateRange"/>",
		endDateRange : "<fmt:message key="resource.group.MaintenanceWindow.error.EndDateRange"/>",
		startTimeRange : "<fmt:message key="resource.group.MaintenanceWindow.error.StartTimeRange"/>",
		endTimeRange : "<fmt:message key="resource.group.MaintenanceWindow.error.EndTimeRange"/>",
		timePattern : "<fmt:message key="resource.group.MaintenanceWindow.error.TimePattern"/>"
	};
	
	var maintenance_<c:out value="${resource.id}"/> = null;
	
	// create Tools menu item after "Add to Group"
	var eeAddToGroupMenuElement = document.getElementById("AddToGroupMenuLink");
	
	if (eeAddToGroupMenuElement) {
		var downtimeLinkBreak = document.createElement("br");
		var downtimeLinkImg = document.createElement("img");
		downtimeLinkImg.border = '0';
		downtimeLinkImg.width = '11';
		downtimeLinkImg.height = '9';
		downtimeLinkImg.src = '/images/title_arrow.gif';
		
		var downtimeLink = document.createElement("a");
		var downtimeLinkText = document.createTextNode('<fmt:message key="resource.group.MaintenanceWindow.Title"/>');
		downtimeLink.setAttribute('href', '#');
		downtimeLink.onclick = function() {
			maintenance_<c:out value='${resource.id}'/>.getSchedule();
		};
		downtimeLink.appendChild(downtimeLinkText);
		downtimeLink.appendChild(downtimeLinkImg);
		
		var eeToolsMenuElement = eeAddToGroupMenuElement.parentNode;
		eeToolsMenuElement.insertBefore(downtimeLinkBreak, eeAddToGroupMenuElement.nextSibling);
		eeToolsMenuElement.insertBefore(downtimeLink, downtimeLinkBreak.nextSibling);
	}
</jsu:script>
<jsu:script onLoad="true">
    maintenance_<c:out value="${resource.id}"/> = new hyperic.maintenance_schedule({
        "title": "<fmt:message key="resource.group.MaintenanceWindow.Title"/>", 
        "appdefentityId": "<c:out value="${resource.entityId}"/>", 
        "resourceName": "<c:out value="${resource.name}" escapeXml="true"/>",
        "url": "/app/resource/<c:out value='${resource.entityId}' />/downtime"
    });
</jsu:script>
</c:if>