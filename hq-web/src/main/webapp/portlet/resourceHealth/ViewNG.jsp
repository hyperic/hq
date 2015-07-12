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
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<tiles:importAttribute name="adminUrl" ignore="true"/>
<tiles:importAttribute name="portletName" ignore="true"/>

<c:set var="rssUrl" value="/rss/ViewResourceHealth.rss"/>

<div class="effectsPortlet">

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="dash.home.ResourceHealth"/>  
  <tiles:putAttribute name="adminUrl" value="${adminUrl}" />
  <tiles:putAttribute name="portletName" value="${portletName}" />
</tiles:insertDefinition>
  	<!-- JSON available at /dashboard/ViewResourceHealth.do -->
  	<jsu:script>
  		function requestFavoriteResources() {
			var avi="test";
			hqDojo.xhrGet({
				url: "<s:url value="JsonLoadFavorites.action" />",
				handleAs: "json",
				content: {
					hq: (new Date()).getTime()
				},
				load: showFavoriteResponseLocal,
				error: reportError
				});
		}
		
	function showFavoriteResponseLocal(response, args) {
        var faveText = response;
        var fList = faveText.favorites;
        var table = document.getElementById('favoriteTable');
        hqDojo.byId('modifiedFavoriteTime').innerHTML = 'Updated: ' + refreshTime();
        var maxResourceNameSize;
        
        if (table) {

            if (fList && fList.length > 0) {
                var tbody = table.getElementsByTagName('tbody')[0];

                for (var d = tbody.childNodes.length - 1; d > 1; d--) {
                    tbody.removeChild(tbody.childNodes[d]);
                }

                for (i = 0; i < fList.length; i++) {

                    var tr = document.createElement('tr');
                    var trTime = document.createElement('tr');
                    var td1 = document.createElement('td');
                    var td2 = document.createElement('td');
                    var td4 = document.createElement('td');
                    var td5 = document.createElement('td');
                    var td6 = document.createElement('td');
                    var urlColon = ":"
                    var resUrl = hqDojo.byId('viewResUrl').href;

                    tbody.appendChild(tr);

                    if (i % 2 == 0) {
                        tr.setAttribute((document.all ? 'className' : 'class'), "tableRowOdd");
                    } else {
                        tr.setAttribute((document.all ? 'className' : 'class'), "tableRowEven");
                    }

                    tr.appendChild(td1);
                    td1.setAttribute((document.all ? 'className' : 'class'), "resourceName");
                    td1.setAttribute("id", (fList[i].resourceName));

                    tr.appendChild(td2);
                    td2.setAttribute((document.all ? 'className' : 'class'), "resourceTypeName");
                    td2.setAttribute("id", (fList[i].resourceTypeName));

                    if (fList[i].resourceTypeName) {
                        td2.appendChild(document.createTextNode(fList[i].resourceTypeName));
                    } else {
                        // XXX: use common.value.notavail
                        td2.innerHTML = "N/A";
                    }

                    tr.appendChild(td4);
                    td4.setAttribute((document.all ? 'className' : 'class'), "availability");
                    td4.setAttribute("id", (fList[i].availability));

                    if (fList[i].availability) {
                        td4.appendChild(document.createTextNode(fList[i].availability));
                        switch (fList[i].availability) {
                            case "green":
                                td4.innerHTML = '<img src=<s:url  value="/images/icon_available_green.gif" /> />';
                                break;
                            case "red":
                                td4.innerHTML = '<img src=<s:url  value="/images/icon_available_red.gif" /> />';
                                break;
                            case "yellow":
                                td4.innerHTML = '<img src=<s:url  value="/images/icon_available_yellow.gif" /> />';
                                break;
                            case "orange":
                                td4.innerHTML = '<img src=<s:url  value="/images/icon_available_orange.gif" /> />';
                                break;
                            case "black":
                                td4.innerHTML = '<img src=<s:url  value="/images/icon_available_black.gif" /> />';
                                break;
                            default:
                                td4.innerHTML = '<img src=&quot;<s:url  value="/images/icon_available_error.gif" />&quot; />';
                        }

                    } else {
                        // XXX: use common.value.notavail
                        td4.innerHTML = "N/A";
                    }

                    tr.appendChild(td5);
                    td5.setAttribute((document.all ? 'className' : 'class'), "alerts");

                    if (fList[i].alerts) {
                        td5.appendChild(document.createTextNode(fList[i].alerts));
                    } else {
                        td5.innerHTML = "0";
                    }
                }

                // find the 'Resource Name' header cell and figure out it's displayed width.
                var maxResourceNameSize = table.rows[0].cells[0].offsetWidth;

                for (i = 0; i < fList.length; i++) {
                    
                    if (fList[i].resourceName && fList[i].resourceId && fList[i].resourceTypeId) {
						var resourceLink = getShortLink(fList[i].resourceName,maxResourceNameSize,unescape(resUrl).replace("eid=", "eid="+fList[i].resourceTypeId + urlColon + fList[i].resourceId));
                        table.rows[i+1].cells[0].innerHTML = resourceLink;
                    } else {
                        table.rows[i+1].cells[0].innerHTML = "&nbsp;";
                    }
                }
                
            } else {
            	hqDojo.style('noFaveResources', "display", "");
            }
       }
	}

	</jsu:script>
	<jsu:script onLoad="true">
		requestFavoriteResources();
	</jsu:script>
	
<s:a action="resourceHub" cssStyle="viewResUrl" name="viewResUrl" >
	<s:param name="eid" value="%{#attr.eid}"/>
	<c:out value="${platform.name}"/>&nbsp;
</s:a>


 	<table width="100%" border="0" cellspacing="0" cellpadding="0" id="favoriteTable" name="favoriteTable"  class="portletLRBorder">
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