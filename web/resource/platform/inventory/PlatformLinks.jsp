<%@ page language="java" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="NetworkServer" ignore="true"/>
<tiles:importAttribute name="FileServer" ignore="true"/>
<tiles:importAttribute name="WindowsServer" ignore="true"/>
<tiles:importAttribute name="ProcessServer" ignore="true"/>

<c:if test="${not empty resource}">

<hq:userResourcePermissions debug="false" resource="${Resource}"/>

<table border="0"><tr><td class="LinkBox">
    <c:if test="${canModify}">
            <html:link page="/resource/platform/Inventory.do?mode=editConfig&eid=${resource.entityId}"><fmt:message key="resource.platform.inventory.link.Configure"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
    </c:if>

    <c:choose>
        <c:when test="${canCreateChild}" >
            <html:link page="/resource/server/Inventory.do?mode=new&eid=${resource.entityId}"><fmt:message key="resource.platform.inventory.NewServerLink"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
            <html:link page="/resource/service/Inventory.do?mode=new&eid=${resource.entityId}"><fmt:message key="resource.platform.inventory.NewServiceLink"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
        </c:when>
        <c:otherwise>
            <fmt:message key="resource.platform.inventory.NewServerLink"/><html:img page="/images/tbb_new_locked.gif" alt="" border="0"/><br>
            <fmt:message key="resource.platform.inventory.NewServiceLink"/><html:img page="/images/tbb_new_locked.gif" alt="" border="0"/><br>
        </c:otherwise>
    </c:choose>
    <c:choose>
        <c:when test="${canModify && canCreateChild}" >            
            <html:link page="/resource/platform/AutoDiscovery.do?mode=new&rid=${resource.id}&type=${resource.entityId.type}"><fmt:message key="resource.platform.inventory.NewDiscoveryLink"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
        </c:when>
        <c:otherwise>
            <fmt:message key="resource.platform.inventory.NewDiscoveryLink"/><html:img page="/images/tbb_new_locked.gif" alt="" border="0"/><br>
        </c:otherwise>
    </c:choose>
    <html:link page="/alerts/EnableAlerts.do?alertState=enabled&eid=${resource.entityId.type}:${resource.id}">
        <fmt:message key="resource.platform.alerts.EnableAllAlerts"/></html:link>
        <html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/><br>
    <html:link page="/alerts/EnableAlerts.do?alertState=disabled&eid=${resource.entityId.type}:${resource.id}">
        <fmt:message key="resource.platform.alerts.DisableAllAlerts"/></html:link>
        <html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/><br>
    <tiles:insert definition=".resource.common.quickFavorites">
      <tiles:put name="resource" beanName="resource"/>
    </tiles:insert>
    <br>
    <a href="javascript:clone_platform.dialog.show();">Clone Platform</a>
</td></tr></table>

<div id ="clone_resource_dialog" style="display:none;">
    <form action="" id="clone_resource_dialog_form" onsubmit="javascript: return false;">
        <fieldset style="width: 450px; text-align: center;">
            <legend>Choose clone target resources</legend>
            <label for="cln_search" style="width: 150px">Search resources:</label>
            <input type="text" id="cln_search" name="cln_search" value="[ Resources ]">
            <div style="width: 180px; float: left; text-align: right">
                <label for="available_clone_targets" style="width: inherit">Available clone targets</label>
                <select name="available_clone_targets" id="available_clone_targets" size="10" style="width: 120px" multiple="multiple"></select>
            </div>
            <div style="width: 66px; height: 100px; float: left; text-align: center; padding-top: 3em">
                <button id="add_clone_btn">&rArr;</button><br />
                <button id="remove_clone_btn">&lArr;</button>
            </div>
            <div style="width: 180px; float: left; text-align: left">
                <label for="selected_clone_targets" style="width: inherit">Selected clone targets</label>
                <select name="selected_clone_targets" id="selected_clone_targets" size="10" style="width: 120px" multiple="multiple"></select>
            </div>
        </fieldset>
        <div style="text-align: right;">
            <span id="clone_cancel_btn"></span>
            <span id="clone_btn"></span>
        </div>
    </form>
</div>
<script type="text/javascript">

    dojo11.require("dijit.dijit");
    dojo11.require("dijit.form.Button");
    dojo11.require("dijit.form.DateTextBox");
    dojo11.require("dijit.form.TimeTextBox");
    dojo11.require("dijit.Dialog");

    clone_platform = new hyperic.clone_resource_dialog('<c:out value="${resource.id}"/>');
</script>
</c:if>