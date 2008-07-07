<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="/common/Error.jsp" %>
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
  
  Copyright (C) [2004, 2005, 2006, 2007, 2008], Hyperic, Inc.
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
<script type="text/javascript">
    dojo.require("dijit.Menu");
    dojo.require("dojo.parser");
    dojo.require("dijit.Tooltip");
    
    function getUpdateStatus(opt) {
        if (opt == "<fmt:message key="header.Acknowledge"/>") {
            var pars = "update=true";
            var updateUrl = 'Dashboard.do?';
            var url = updateUrl + pars;
            new Ajax.Request(url, {method: 'post'});
            dojo.byId('hb').innerHTML = '<html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/>'
        }
        menuLayers.hide();
    }
    var resourceURL = '/Resource.do';
    var searchWidget = new hyperic.widget.search({search:'/search.shtml'}, 3, {keyCode: 83, ctrl: true});
    dojo.require("dojo.fx");
    dojo.connect(window, "onload",function(){ 
        activateHeaderTab();
        searchWidget.create();
        //Connect the events for the box, cancel and search buttons
        dojo.connect(searchWidget.searchBox, "onkeypress", searchWidget, "search");
        dojo.connect(searchWidget.nodeCancel, "onclick", searchWidget, "toggleSearchBox");
        dojo.connect(searchWidget.nodeSearchButton, "onclick", searchWidget,  "toggleSearchBox");
        // What should the hot-keys do?
        dojo.subscribe('enter', searchWidget, "search");
        dojo.subscribe('search', searchWidget, "toggleSearchBox");
        dojo.subscribe('escape', searchWidget, "toggleSearchBox");
    });
    
      <!--
      var refreshCount = 0;
                                                                                    
      function refreshAlerts() {
        refreshCount++;

        new Ajax.Request('<html:rewrite page="/common/RecentAlerts.jsp"/>',
                         {method: 'get', onSuccess:showRecentAlertResponse});
      }

      function showRecentAlertResponse(originalRequest) {
        if (originalRequest.responseText.indexOf('recentAlertsText') > 0) {
          $('recentAlerts').innerHTML = originalRequest.responseText;
        }
        else {
          refreshCount = 31;
        }

        if (refreshCount < 30) {
          setTimeout( "refreshAlerts()", 60*1000 );
        } else if (autoLogout) {
          top.location.href = "<html:rewrite action="/Logout"/>";
        }
      }

      onloads.push( refreshAlerts );
      //-->
      </script>
    <div id="header">
    <div id="headerLogo" title="Home" onclick="/Dashboard.do">&nbsp;</div>
    <div id="navTabContainer">
        <c:set var="pageURL" value="${requestURL}"/>
        <ul>
            <li id="dashTab" class=""><a href="/Dashboard.do"><span><fmt:message key="header.dashboard"/></span></a></li>
            <li id="resTab" class=""><a><span id="resource" onclick="hyperic.widget.menu.onclick(this)"><fmt:message key="header.resources"/></span></a></li>
            <li id="analyzeTab" class=""><a><span id="analyze" onclick="hyperic.widget.menu.onclick(this)"><fmt:message key="header.analyze"/></span></a></li>
            <li id="setTab" class=""><a href="/Settings.html"><span><fmt:message key="header.settings"/></span></a></li>
            <li id="adminTab" class=""><a href="/Admin.do"><span><fmt:message key="header.admin"/></span></a></li>
        </ul>
    </div>
	<div dojoType="dijit.Menu" id="resource_1" popupDelay="500" style="display: none;">
	    <div dojoType="dijit.MenuItem" iconClass="resourceIcon" onClick="document.location='/ResourceHub.do'"><fmt:message key="header.browse"/></div>
	    <tiles:insert definition=".header.optional.tabs">
	        <tiles:put name="location" value="resources"/>
	    </tiles:insert>
	    <div dojoType="dijit.PopupMenuItem" iconClass="favoriteIcon" id="submenu2">
	        <span><fmt:message key="header.favorite"/></span>
	        <div dojoType="dijit.Menu">
	            <div dojoType="dijit.MenuItem" onClick="">Placeholder</div>
	        </div>
	    </div>
	    <div dojoType="dijit.PopupMenuItem" iconClass="recentIcon" id="submenu3">
	        <span><fmt:message key="header.recent"/></span>
	        <div dojoType="dijit.Menu">
	            <tiles:insert definition=".toolbar.recentResources"/>
	        </div>
	    </div>
	</div>
	<div dojoType="dijit.Menu" id="analyze_1" popupDelay="500" contextMenuForWindow="false" style="display: none;">
	    <div dojoType="dijit.MenuItem" iconClass="reportIcon" onClick="/document.location='reporting/ReportCenter.do'"><fmt:message key="reporting.reporting.ReportCenterTitle"/></div>
	    <tiles:insert definition=".header.optional.tabs">
	        <tiles:put name="location" value="tracking"/>
	    </tiles:insert>
	</div>
    <div id="headerAlerts">
      <div class="headAlertWrapper">
        <div class="recentText">
          <fmt:message key="header.RecentAlerts"/>
        </div>
        <div id="recentAlerts"></div>
      </div>
    </div>
    <div id="headerLinks">
        <fmt:message key="header.Welcome"/>
         <c:choose>
            <c:when test="${useroperations['viewSubject']}">
                <html:link page="/admin/user/UserAdmin.do?mode=view&u=${sessionScope.webUser.id}">
                    <c:out value="${sessionScope.webUser.firstName}"/>
                </html:link>
            </c:when>
            <c:otherwise>
                <c:out value="${sessionScope.webUser.firstName}"/>
            </c:otherwise>
        </c:choose>
        <span><a href="<html:rewrite page='/Logout.do'/>"><fmt:message key="header.SignOut"/></a></span>
        <span><html:link href="javascript:void(0)" onclick="tutorialWin=window.open('http://www.hyperic.com/demo/screencasts.html','tutorials','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');tutorialWin.focus();return false;"><fmt:message key="header.Screencasts"/></html:link></span>
        <span><a href="javascript:void(0)" onclick="helpWin=window.open('<hq:help/>','help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');helpWin.focus();return false;"><fmt:message key="header.Help"/></a></span>
    </div>
    <div id="headerSearch"><fmt:message key="header.Search"/></div>
        <div id="headerSearchBox" style="display:none">
            <div style="float:left;margin:3px 0px 3px 5px;">
            <input type="text" id="searchBox" value=""/>
        </div>
    </div>
    <div id="headerSearchResults" style="display:none">
        <a class="all left" href="/resource/Hub.html"><fmt:message key="header.SearchShowAll"/></a>
        <div id="searchClose" class="cancelButton right"></div>
        <div class="clear">&nbsp;</div>
        <div class="resultsGroup">
            <div class="category"><fmt:message key="header.Resources"/> (<span id="resourceResultsCount"></span>)</div>
            <ul id="resourceResults">
                <li></li>
            </ul>
        </div>
        <div class="resultsGroup">
            <div class="category"><fmt:message key="header.Alerts"/> (0)</div>
            <ul>
                <li></li>
            </ul>
        </div>
        <div class="resultsGroup">
            <div class="category"><fmt:message key="header.Events"/> (0)</div>
            <ul>
                <li></li>
            </ul>
        </div>
    </div>
     
</div>
<div id="headerBottom">&nbsp;</div>

<c:if test="${not empty HQUpdateReport}">
<div id="update" class="menu" style="z-index:15000000;border:1px solid black;padding-top:15px;padding-bottom:15px;font-weight:bold;font-size:12px;">
<c:out value="${HQUpdateReport}" escapeXml="false"/>
    <form name="updateForm" action="">
        <div style="text-align:center;padding-left:15px;padding-right:15px;"><input type="button" value="<fmt:message key="header.RemindLater"/>" onclick="getUpdateStatus(this.value);"><span style="padding-left:15px;"><input type="button" value="<fmt:message key="header.Acknowledge"/>" onclick="getUpdateStatus(this.value);"></span>
        </div>
    </form>
</div>
</c:if>
