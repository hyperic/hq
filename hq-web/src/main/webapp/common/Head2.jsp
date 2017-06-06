<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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
<link rel="stylesheet" href="<s:url value="/static/js/dojo/1.5/dojo/resources/dojo.css"/>" type="text/css"/>
<link rel="stylesheet" href="<s:url value="/static/js/dojo/1.5/dijit/themes/tundra/tundra.css"/>" type="text/css"/>
<link rel="shortcut icon" href="<s:url value="/images/4.0/icons/favicon.ico"/>"/>
<link rel="stylesheet" href="<s:url value="/css/win.css"/>" type="text/css"/>
<link rel="stylesheet" href="<s:url value="/css/HQ_40.css"/>" type="text/css"/>
<link rel="stylesheet" href="<s:url value="/hqu/public/hqu.css"/>" type="text/css"/>

<style>
	div.wwctrl label{
		margin-left: 5px;
		top: -3px;
		position: relative
	}
	div.errorMessage{
		font-size: 10px;
		font-style: italic;
	}
	ul.errorMessage, ul.actionMessage{
		list-style-type: none;
		padding: 0px;
		margin: 0px;
	}
	ul.errorMessage > li span:before {
		background: url('/images/tt_error.gif') CENTER CENTER NO-REPEAT;
		content:url('/images/tt_empty.png'); /* 21x21 transparent pixels */
		width:11px;
		height:11px;
		padding-right: 10px;
	}
	ul.actionMessage > li span:before {
		background: url('/images/tt_check.gif') CENTER CENTER NO-REPEAT;
		content:url('/images/tt_empty.png'); /* 21x21 transparent pixels */
		width:9px;
		height:9px;
		padding-right: 10px;
	}
	.wwgrpError{ 
		background-color: #FFFD99;
		padding: 3px;
	}
</style>
<script>
	var djConfig = {};
	djConfig.parseOnLoad = true;
	djConfig.baseUrl = '/static/js/dojo/1.5/dojo/';
	djConfig.scopeMap = [ [ "dojo", "hqDojo" ], [ "dijit", "hqDijit" ], [ "dojox", "hqDojox" ] ];

</script>	
<!--[if IE]>
<script type="text/javascript">
	// since dojo has trouble when it comes to using relative urls + ssl, we
	// use this workaorund to provide absolute urls.
	function qualifyURL(url) {
		var a = document.createElement('img');
	    a.src = url;
	    return a.src;
	}
		
	djConfig.modulePaths = {
	    "dojo": qualifyURL("/static/js/dojo/1.5/dojo"),
	    "dijit":  qualifyURL("/static/js/dojo/1.5/dijit"),
	    "dojox":  qualifyURL("/static/js/dojo/1.5/dojox")
  	};
</script>
<![endif]-->
<script type="text/javascript" src="<s:url value="/static/js/dojo/1.5/dojo/dojo.js" />"></script>

<jsu:importScript path="/js/prototype.js" />
<jsu:importScript path="/js/popup.js" />
<jsu:importScript path="/js/diagram.js" />
<jsu:importScript path="/js/functions.js" />
<jsu:importScript path="/js/lib/lib.js" />
<jsu:importScript path="/js/lib/charts.js" />
<c:set var="maxLongValue">
	<%= Long.MAX_VALUE %>
</c:set>
<jsu:script>
	var imagePath = "/images/";
	hqDojo.require('dojo.date');

	hyperic.data.escalation = {};
	hyperic.data.escalation.pauseSelect = document.createElement("select");
	hyperic.data.escalation.pauseSelect.options[0] = new Option("5 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>", "300000");
	hyperic.data.escalation.pauseSelect.options[1] = new Option("10 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>", "600000");
	hyperic.data.escalation.pauseSelect.options[2] = new Option("20 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>", "1200000");
	hyperic.data.escalation.pauseSelect.options[3] = new Option("30 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>", "1800000");
	hyperic.data.escalation.pauseSelect.options[4] = new Option("45 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>", "2700000");
	hyperic.data.escalation.pauseSelect.options[5] = new Option("60 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.1"/>", "3600000");
	hyperic.data.escalation.pauseSelect.options[6] = new Option("2 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>", "7200000");
	hyperic.data.escalation.pauseSelect.options[7] = new Option("4 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>", "14400000");
	hyperic.data.escalation.pauseSelect.options[8] = new Option("8 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>", "28800000");
	hyperic.data.escalation.pauseSelect.options[9] = new Option("12 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>", "43200000");
	hyperic.data.escalation.pauseSelect.options[10] = new Option("24 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>", "86400000");
	hyperic.data.escalation.pauseSelect.options[11] = new Option("48 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>", "172800000");
	hyperic.data.escalation.pauseSelect.options[12] = new Option("72 <fmt:message key="alert.config.props.CB.Enable.TimeUnit.2"/>", "259200000");
	hyperic.data.escalation.pauseSelect.options[13] = new Option("<fmt:message key="alert.config.props.CB.Enable.UntilFixed"/>", "${maxLongValue}");
</jsu:script>