<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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
<link rel="stylesheet" href="<html:rewrite page="/js/dojo/1.1.2/dojo/resources/dojo.css"/>" type="text/css"/>
<link rel="stylesheet" href="<html:rewrite page="/js/dojo/1.1.2/dijit/themes/tundra/tundra.css"/>" type="text/css"/>
<link rel="shortcut icon" href="<html:rewrite page="/images/4.0/icons/favicon.ico"/>"/>
<link rel="stylesheet" href="<html:rewrite page="/css/win.css"/>" type="text/css"/>
<link rel="stylesheet" href="<html:rewrite page="/css/HQ_40.css"/>" type="text/css"/>

<script type="text/javascript">
djConfig = { isDebug: false, locale: 'en-us' }
</script>
<script type="text/javascript" src="<html:rewrite page='/js/dojo/0.4.4/dojo.js'/>"></script> 
<script type="text/javascript">
djConfig.parseOnLoad = true;
djConfig.baseUrl = '/js/dojo/1.1.2/dojo/';
djConfig.scopeMap = [
        ["dojo", "dojo11"],
        ["dijit", "dijit11"],
        ["dojox", "dojox11"]
    ];
</script>
<script src="<html:rewrite page='/js/dojo/1.1.2/dojo/dojo.js'/>" type="text/javascript"></script>
<script type="text/javascript">
    var imagePath = "<html:rewrite page="/images/"/>";
    dojo11.require('dojo.date');
    dojo.require('dojo.event.*');
	dojo.require('dojo.widget.*');
</script>
<script src="<html:rewrite page='/js/prototype.js'/>" type="text/javascript"></script>
<script src="<html:rewrite page='/js/popup.js'/>" type="text/javascript"></script>
<script src="<html:rewrite page='/js/requests.js'/>" type="text/javascript"></script>
<script src="<html:rewrite page='/js/diagram.js'/>" type="text/javascript"></script>
<script src="<html:rewrite page='/js/functions.js'/>" type="text/javascript"></script>
<script src="<html:rewrite page='/js/lib/lib.js'/>" type="text/javascript"></script>
<script src="<html:rewrite page='/js/lib/charts.js'/>" type="text/javascript"></script>
<script type="text/javascript">
var onloads = [];
	function initOnloads() {
    	if (arguments.callee.done) return;
        
        arguments.callee.done = true;
        
        if(typeof(_timer)!="undefined") clearInterval(_timer);
        
        for ( var i = 0 ; i < onloads.length ; i++ )
        	onloads[i]();
    };

	function refreshPortlets() {
	
	    var problemPortlet = dojo.byId('problemResourcesTable');
	    var favoritePortlet = dojo.byId('favoriteTable');
	
	    var nodes = document.getElementsByTagName('table');
	    var getRecentForm = document.getElementsByTagName('form')
	
	    for (i = 0; i < nodes.length; i++) {
	        if (/metricTable/.test(nodes[i].id)) {
	            //alert('in metric table')
	            var metricTblId = nodes[i].id;
	            var getId = metricTblId.split('_');
	            var metricIdPart = getId[1];
	
	            if (metricIdPart) {
	                var metricIdToken = '_' + metricIdPart;
	
	                setInterval("requestMetricsResponse" + metricIdToken + "()", 30000);
	            } else {
	                setInterval("requestMetricsResponse()", 30000);
	            }
	        }
	    }
	
	
	    for (i = 0; i < nodes.length; i++) {
	        if (/availTable/.test(nodes[i].id)) {
	            // alert('in avail table')
	            var availTblId = nodes[i].id;
	            var getId = availTblId.split('_');
	            var availIdPart = getId[1];
	
	            if (availIdPart) {
	                var availIdToken = '_' + availIdPart;
	
	                setInterval("requestAvailSummary" + availIdToken + "()", 30000);
	            } else {
	                setInterval("requestAvailSummary()", 30000);
	            }
	        }
	    }
	
	    if (problemPortlet) {
	        setInterval("requestProblemResponse()", 30000);
	    }
	
	    if (favoritePortlet) {
	        setInterval("requestFavoriteResources()", 30000);
	    }
	}
	
	onloads.push(refreshPortlets);

	dojo11.addOnLoad(function() {
		initOnloads();
	});
</script>
<html:link page="/Resource.do?eid=" linkName="viewResUrl" styleId="viewResUrl" style="display:none;"></html:link>

<tiles:insert beanProperty="url" beanName="portlet" flush="true">
	<tiles:put name="portlet" beanName="portlet" />
</tiles:insert>
