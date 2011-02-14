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
    <div id="headerAlerts" style="display:none;">
    	<div class="headAlertWrapper">
       		<div class="recentText">
      			<fmt:message key="header.RecentAlerts"/>
       		</div>
       		<div id="recentAlerts"></div>
		</div>
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
		<div id="headerSearchResults" style="display:none">
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
    <script>
    	dojo.require("dojo.fx");
    	dojo.require("dijit.Dialog");
   	 	
    	var hyperic = {};
    	
    	hyperic.widget = {};
    	hyperic.widget.search = function(/*Object*/ urls, /*number*/ minStrLenth, /*Object*/ keyCode){
	    	this.opened     = false;
	        this.minStrLen  = minStrLenth; 
	        this.resourceURL= urls.resource;
	        this.searchURL  = urls.search;
	        this.keyCode    = keyCode;
	        this.listeners  = [];
	        /**
	         * Connect all the events up and grab the nodes that we are going to need
	         */
	        this.create = function(){
	            this.searchBox          = dojo.byId('searchBox');
	            this.searchContainer    = dojo.byId('headerSearchBox');
	            this.nodeSearchResults  = dojo.byId('headerSearchResults');
	            this.nodeCancel         = dojo.byId('searchClose');
	            this.nodeSearchButton   = dojo.byId("headerSearch");
	            //Set up the key listeners for the search feature
	            this.listeners.push( new hyperic.utils.addKeyListener(window.document, this.keyCode, 'search') );
	            this.listeners.push( new hyperic.utils.addKeyListener(this.searchContainer, { keyCode: 13 }, 'enter') );
	            this.listeners.push( new hyperic.utils.addKeyListener(dojo.byId('header'), { keyCode: 27 }, 'escape') );
	        };
	        this.search = function(e){
	            var string = e.target.value;
	            if(this.searchBox.value.length >= this.minStrLen){
	                this.searchStarted();
	                dojo.xhrGet({
	                    url: this.searchURL+'?q='+string, 
	                    handleAs: "json",
	                    headers: { 
	                    	"Content-Type": "application/json" 
	                    },
	                    timeout: 5000, 
	                    load: function(response, args) {
	                    	var resURL = resourceURL + "?eid=";
	        	            var usrURL = userURL + "?mode=view&u=";
	        	            var template = "<li class='type'><a href='link' title='fullname'>text<\/a><\/li>";
	        	            var count = 0;
	        	            var res = "";
	        	            var relink = new RegExp("link", "g");
	        	            var retext = new RegExp("text", "g");
	        	            var refulltext = new RegExp("fullname", "g");
	        	            var retype = new RegExp("type", "g");
	        	            var resources = response.resources;
	        	            
	        	            for (var i = 0; i < resources.length; i++) {
	        	            	var length = resources[i].name.length;
	        	                var fullname = resources[i].name;
	        	                
	        	                if (length >= 37){
	        	                	resources[i].name = resources[i].name.substring(0,4) + "..." + resources[i].name.substring(length-28, length);
	        	                }
	        	                
	        	                res += template.replace(relink, resURL+resources[i].id)
	        	                	.replace(retext, resources[i].name)
	        	                    .replace(retype, resources[i].resType)
	        	                    .replace(refulltext, fullname);
	        	                    
	        	                count++;
	        	            }
	        	            
	        	            dojo.byId("resourceResults").innerHTML = res;
	        	            dojo.byId("resourceResultsCount").innerHTML = count;

	        	            count = 0;
	        	            res = "";
	        	             
	        	            var users = response.users;
	        	             
	        	            for (var i = 0; i < users.length; i++) {
	        	                var fullname = users[i].name;
	        	             
	        	                res += template.replace(relink, usrURL+users[i].id)
	        	                    .replace(retype, "user")
	        	                    .replace(retext, users[i].name);
	        	            
	        	                count++;
	        	            }
	        	             
	        	            dojo.byId("usersResults").innerHTML = res;
	        	            dojo.byId("usersResultsCount").innerHTML = count;
	        	            dojo.byId('headerSearchResults').style.display = '';
	        	            dojo.byId('searchBox').className = "";
	                    },
	                    error: this.error
	                });
	                
	               
	            }else{
	                this.searchEnded();
	                this.nodeSearchResults.style.display = 'none';
	            }
	        };
	        this.error = function(){
	            this.searchEnded();
	            alert("foo");
	        };
	        this.loadResults = function(response){
	            this.searchEnded();
	            
	        };
	        this.toggleSearchBox = function() {
	            if(this.opened) {
	                this.nodeSearchResults.style.display = 'none';
	                dojo.fx.wipeOut({node:this.searchContainer, duration: 400}).play();
	                this.opened = false;
	                this.searchEnded();
	                this.searchBox.value = '';
	            }
	            else {
	                window.scrollTo(0,0);
	                dojo.fx.wipeIn({node:this.searchContainer, duration: 400}).play();
	                this.opened = true;
	                this.searchBox.focus();
	            }
	        };
	        this.searchStarted = function(){
	            this.searchBox.className = "searchActive";
	        };
	        this.searchEnded = function(){
	            this.searchBox.className = "";
	        };

	        return this;
	    };
	    
	    hyperic.utils = {};
	    hyperic.utils.addKeyListener = function(/*Node*/node, /*Object*/ keyComb, /*String*/topic){
	    	this.node = node;
	        this.keyComb = keyComb;
	        this.topic = topic;
	        this.canceled = false;
	        this.keyListener = function(e){
	            if (e &&                                  // event exists and,                
	            	e.keyCode == this.keyComb.keyCode &&  // event keyCode matches expected keyCode and,
	            	!this.canceled &&                     // listener isn't cancelled and,
	            	(!(this.keyComb.ctrl ||               // No modifier key required or,
	            	   this.keyComb.alt  || 
	            	   this.keyComb.shift) ||
	                ((this.keyComb.ctrl && e.ctrlKey) ||  // event modifier matches expected modifier
	                 (this.keyComb.alt && e.altKey) ||
		             (this.keyComb.shift && e.shiftKey)))) {
	                this.publish(e);                      // publish event
	            }
	        };
	        this.publish = function(e){
                e.preventDefault();
                e.stopPropagation();
	            
	            dojo.publish(this.topic, [e]);
	        };
	        this.cancel = function(){
	            this.canceled = true;
	            
	            dojo.disconnect(node, "onkeyup", this, "keyListener");
	        };

	        dojo.connect(node, "onkeyup", this, "keyListener");

	        return this;
	    };
	    
	    /**
	     * @deprecated used only for the struts header
	     */
	    var activateHeaderTab = function(){
	        var l = document.location;
	        if (document.navTabCat) {
	            //This is a plugin
	            if (document.navTabCat == "Resource") {
	                dojo.addClass("resTab", "activeTab");
	            } else if(document.navTabCat == "Admin") {
	                dojo.addClass("adminTab", "activeTab");
	            }
	            return;
	        }
	        l = l+""; // force string cast
	        if ( l.indexOf("Dash")!=-1 || 
	             l.indexOf("dash")!=-1 ) {
	        	dojo.addClass("dashTab", "active");
	        } else if( l.indexOf("Resou")!=-1 ||
	                   l.indexOf("resource")!=-1 || 
	                   l.indexOf("alerts/")!=-1 || 
	                   l.indexOf("TabBodyAttach.do")!=-1 ) {
	        	dojo.addClass("resTab", "active");
	        } else if( l.indexOf("rep")!=-1 || 
	                   l.indexOf("Rep")!=-1 || 
	                   l.indexOf("masth")!=-1 ) {
	        	dojo.addClass("analyzeTab", "active");
	        } else if( l.indexOf("admin.do")!=-1 || 
	                   l.indexOf("Admin.do")!=-1 ) {
	        	dojo.addClass("adminTab", "active");
	        }
	    };

    	var resourceURL = '<html:rewrite action="/Resource" />';
		var userURL = '<html:rewrite action="/admin/user/UserAdmin" />';
    	var searchWidget = new hyperic.widget.search({ search: '<spring:url value="/app/search" />' }, 3, { keyCode: 83, ctrl: true });
    	var refreshCount = 0;
    	var refreshAlerts = function() {
      		refreshCount++;

	      	dojo.xhrGet({
	    	  	url: "<html:rewrite page="/common/RecentAlerts.jsp"/>",
	    	  	load: function(response, args) {
	    	        if (response.indexOf('recentAlertsText') > 0) {
	    	            dojo.style("headerAlerts", "display" , "");
	    	            dojo.byId("recentAlerts").innerHTML = response;
	    	        } else {
	    	            refreshCount = 31;
	    	        }
	
	    	        if (refreshCount < 30) {
	    	            setTimeout("refreshAlerts", 60*1000);
	    	        } else if (autoLogout) {
	    	        	top.location.href = "<spring:url value="/j_spring_security_logout" />";
	    	        }
	    	  	} 
	      	});
	    };
   	 	
	    dojo.ready(function() { 
	    	// refreshAlerts();
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