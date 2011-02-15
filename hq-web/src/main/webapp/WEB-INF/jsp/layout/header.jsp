<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<div id="headerLogo">
	<a href="<html:rewrite action="/Dashboard" />">
		<img alt="<fmt:message key="header.Dashboard" />" src="<spring:url value="/static/images/hqlogo.jpg"/>" border="0" />
	</a>
</div>
<div id="headerLinks">
	<sec:authorize access="hasRole('ROLE_USER')">
 		<c:if test="${not empty HQUpdateReport}">
 	 		<div id="update" class="dialog" style="display: none;">
 	 			<c:out value="${HQUpdateReport}" escapeXml="false"/>
 	 
 	 			<form name="updateForm" action="">
 	 				<div style="text-align:right;">
 	 					<input id="updateAcknowledgementButton" type="button" class="button42" value="<fmt:message key="header.Acknowledge"/>" />
 	 				</div>
 	 			</form>
 			</div>
			<a id="updateLink">
 				<img src="<spring:url value="/static/images/transmit2.gif" />" align="absMiddle" border="0" />                        
 			</a>
		</c:if>	
	</sec:authorize>
	<ul>
		<sec:authorize access="hasRole('ROLE_USER')">
			<li>
				<span><fmt:message key="header.Welcome"/></span>
				<a href="<html:rewrite action="/admin/user/UserAdmin" />?mode=view&u=${sessionScope.webUser.id}">
	            	${sessionScope.webUser.firstName}
				</a>
			</li>
			<li>
				<a id="signOutLink" href="<spring:url value="/j_spring_security_logout" />" title="<fmt:message key="header.SignOut" />">
					<fmt:message key="header.SignOut" />
				</a>
			</li>
		</sec:authorize>
		<li>
			<a id="screencastLink" href="http://www.hyperic.com/demo/screencasts.html" target="_blank" title="<fmt:message key="header.Screencasts" />">
				<fmt:message key="header.Screencasts" />
			</a>
		</li>
		<li>
			<a id="helpLink" href="http://support.hyperic.com/confluence/display/DOC/" target="_blank" title="<fmt:message key="header.Help" />">
				<fmt:message key="header.Help" />
			</a>
		</li>
	</ul>
</div>
<sec:authorize access="hasRole('ROLE_USER')">
	<div id="loading" class="ajaxLoading" style="">
       	<img src="<spring:url value="/static/images/ajax-loader.gif" />" border="0" width="16" height="16" />
    </div>
    <div id="headerAlerts" style="display: none;">
    	<span id="recentText"><fmt:message key="header.RecentAlerts"/></span>
       	<div id="recentAlerts"></div>
    </div>
    <div id="headerTabs">
		<ul>
			<li id="dashboardTab" class="tab">
				<a href="<html:rewrite action="/Dashboard" />">
					<fmt:message key="header.dashboard"/>
				</a>
			</li>
	        <li id="resourceTab" class="tab">
	        	<a href="<html:rewrite action="/ResourceHub" />">
	        		<fmt:message key="header.resources"/>
	        	</a>
	           	<ul>
	               	<li>
	               		<a href="<html:rewrite action="/ResourceHub" />">
	               			<fmt:message key="header.Browse"/>
	               		</a>
	               	</li>
	                	
	               	
	               	<li>
	               		<a><fmt:message key=".dashContent.recentResources"/></a>
			        </li>
				</ul>
			</li>
	        <li id="analyzeTab" class="tab">
				<a><fmt:message key="header.analyze"/></a>
	        </li>
		    <li id="adminTab" class="tab">
		    	<a href="<html:rewrite action="/Admin" />">
		    		<fmt:message key="header.admin"/>
		    	</a>
		   	</li>
		</ul>
	</div>
	<div id="headerSearch">
		<input type="text" id="searchBox" value=""/>
    </div>
	<div id="headerSearchResults" style="display:none;">
       	<div id="searchClose" class="cancelButton right"></div>
	        <div class="resultsGroup">
       		    <div class="category"><fmt:message key="header.Resources"/> (<span id="resourceResultsCount"></span>)</div>
	           	<ul id="resourceResults">
           		    <li></li>
		        </ul>
		    </div>
		    <div class="resultsGroup">
           		<div class="category"><fmt:message key="header.users"/> (<span id="usersResultsCount"></span>)</div>
		        <ul id="usersResults">
               		<li></li>
           		</ul>
       		</div>
    	</div>
    </div>
    <script src="<spring:url value="/js/lib/lib.js" />" type="text/javascript"></script>
    <script>
    	dojo.require("dijit.Dialog");
   	 	
    	var resourceURL = '<html:rewrite action="/Resource" />';
		var userURL = '<html:rewrite action="/admin/user/UserAdmin" />';
    	var searchWidget = new hyperic.widget.search({ search: '<spring:url value="/app/search" />' }, 3, { keyCode: 83, ctrl: true });
    	var refreshCount = 0;
    	var refreshAlerts = function() {
      		dojo.xhrGet({
	    	  	url: "<html:rewrite page="/common/RecentAlerts.jsp"/>",
	    	  	load: function(response, args) {
	    	        dojo.style("headerAlerts", "display" , "");
	    	        dojo.byId("recentAlerts").innerHTML = response;
	    	  	} 
	      	});
	    };
   	 	
	    dojo.ready(function() { 
	    	refreshAlerts();
        	activateHeaderTab();
        	searchWidget.create();
        
        	dojo.subscribe("refreshAlerts", function(data) {
        		refreshCount++;
        		refreshAlerts();
        	});
        	
        	setInterval(function() {
        		if (refreshCount < 30 || window.autoLogout === undefined) {
    	            dojo.publish("refreshAlerts");
    	        } else {
    	        	top.location.href = "<spring:url value="/j_spring_security_logout" />";
    	        }
        		
        		if (refreshCount > 30) refreshCount = 0;
        	}, 60*1000);
        	
        	//Connect the events for the box, cancel and search buttons
        	dojo.connect(searchWidget.searchBox, "onkeypress", searchWidget, "search");
        	
        	// What should the hot-keys do?
        	dojo.subscribe('enter', searchWidget, "search");
        	
        	// Render Search Tooltip
        	dojo.byId('headerSearch').title = "<fmt:message key="header.searchTip.mac" />";
        	
    		if (dojo.byId("updateLink")) {
    			new dijit.Dialog({
        	 	 		id: 'update_popup',
        	 			refocus: true,
        	 			autofocus: false,
        	 			opacity: 0,
        	 			title: "<fmt:message key="header.dialog.title.update" />"
        			}, 
        			dojo.byId('update')
        		);
        	 	
    			dojo.connect(dojo.byId("updateAcknowledgementButton"), "onclick", function(e) {
        			if (this.value == "<fmt:message key="header.Acknowledge"/>") {
            	        dojo.xhrPost({
                	 	 	url: "<html:rewrite action="/Dashboard" />",
                 		 	content: { 
                 		 		update: true 
                 		 	},
                 	 		load: function(data) {
                 	 			dojo.style("updateLink", "display", "none");
                 	 			dijit.byId("update_popup").hide();
                			}
               			});
                	}
        		});
        		dojo.connect(dojo.byId("updateLink"), "onclick", function(e) {
        			 dijit.byId("update_popup").show();
        		});
    		}
    	});
    </script>
</sec:authorize>