<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<c:set var="rssUrl" value="/rss/ViewResourceHealth.rss"/>

<div class="effectsPortlet">
	<tiles:insert definition=".header.tab">
	  	<tiles:put name="tabKey" value="dash.home.ResourceHealth"/>
	  	<tiles:put name="adminUrl" beanName="adminUrl" />
	  	<tiles:put name="portletName" beanName="portletName" />
	  	<tiles:put name="rssBase" beanName="rssUrl" />
	</tiles:insert>

  	<!-- JSON available at /dashboard/ViewResourceHealth.do -->
  	<jsu:script>
  		function requestFavoriteResources() {
			hqDojo.xhrGet({
				url: "<html:rewrite action="/dashboard/ViewResourceHealth"/>",
				handleAs: "json",
				content: {
					hq: (new Date()).getTime()
				},
				load: function(response, args) {
				    showFavoriteResponse(response, args);
				    setTimeout("requestFavoriteResources()", portlets_reload_time);
				},
				error: function(response, args) {
				    reportError(response, args);
				    setTimeout("requestFavoriteResources()", portlets_reload_time);
				}
			});
		}
	</jsu:script>
	<jsu:script onLoad="true">
		requestFavoriteResources();
	</jsu:script>

 	<table width="100%" border="0" cellspacing="0" cellpadding="0" id="favoriteTable" class="portletLRBorder">
 		<tbody>
			<tr class="tableRowHeader">
				<th class="tableRowInactive">
					<fmt:message key="dash.home.TableHeader.ResourceName"/>
				</th>
				<th width="80px" class="tableRowInactive">
					<fmt:message key="dash.home.TableHeader.Type"/>
				</th>
				<th width="60px" align="center" class="tableRowInactive">
					<fmt:message key="resource.common.monitor.visibility.AvailabilityTH"/>
				</th>
				<th width="40px" align="center" class="tableRowInactive">
					<fmt:message key="dash.home.TableHeader.Alerts"/>
				</th>
			</tr>
	 	<!-- table rows are inserted here dynamically -->
		</tbody>
	</table>
	<table width="100%" cellpadding="0" cellspacing="0" border="0" id="noFaveResources" style="display:none;" class="portletLRBorder">
        <tbody>
	        <tr class="ListRow">
	            <td class="ListCell">
	                <c:url var="path" value="/images/4.0/icons/properties.gif"/>
	                <fmt:message key="dash.home.add.resources.to.display">
	                  	<fmt:param value="${path}"/>
	                </fmt:message>
	            </td>
	        </tr>
        </tbody>
  	</table>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
	    <tr>
    		<td id="modifiedFavoriteTime" class="modifiedDate">Updated: </td>
        </tr>
    </table>
</div>