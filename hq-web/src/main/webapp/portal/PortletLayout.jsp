<%@ page language="java"%>
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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
<link rel="stylesheet" href="<html:rewrite page="/static/js/dojo/1.5/dojo/resources/dojo.css"/>" type="text/css"/>
<link rel="stylesheet" href="<html:rewrite page="/static/js/dojo/1.5/dijit/themes/tundra/tundra.css"/>" type="text/css"/>
<link rel="shortcut icon" href="<html:rewrite page="/images/4.0/icons/favicon.ico"/>"/>
<link rel="stylesheet" href="<html:rewrite page="/css/win.css"/>" type="text/css"/>
<link rel="stylesheet" href="<html:rewrite page="/css/HQ_40.css"/>" type="text/css"/>
<script type="text/javascript">
	djConfig.parseOnLoad = true;
	djConfig.baseUrl = '/static/js/dojo/1.5/dojo/';
</script>
<jsu:importScript path="/static/js/dojo/1.5/dojo/dojo.js" />
<jsu:importScript path="/js/prototype.js" />
<jsu:importScript path="/js/popup.js" />
<jsu:importScript path="/js/requests.js" />
<jsu:importScript path="/js/diagram.js" />
<jsu:importScript path="/js/functions.js" />
<jsu:importScript path="/js/lib/lib.js" />
<jsu:importScript path="/js/lib/charts.js" />
<jsu:script>
    var imagePath = "/images/";
    hqDojo.require('dojo.date');
    var onloads = [];
	
    function initOnloads() {
       if (arguments.callee.done) return;
        
       arguments.callee.done = true;
        
       if(typeof(_timer)!="undefined") clearInterval(_timer);
        
       for ( var i = 0 ; i < onloads.length ; i++ )
           onloads[i]();
    };
	
    hqDojo.addOnLoad(function() {
        initOnloads();
    });
</jsu:script>
<html:link action="/Resource" linkName="viewResUrl" styleId="viewResUrl" style="display:none;">
	<html:param name="eid" value=""/>
</html:link>

<tiles:insert beanProperty="url" beanName="portlet" flush="true">
	<tiles:put name="portlet" beanName="portlet" />
</tiles:insert>
