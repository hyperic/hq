<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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

<script language="JavaScript" src="<html:rewrite page="/js/scriptaculous.js"/>" type="text/javascript"></script>
<script language="JavaScript" src="<html:rewrite page='/js/requests.js'/>" type="text/javascript" id="requests"></script>
<script src="<html:rewrite page="/js/dashboard.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>

<script language="JavaScript" type="text/javascript">
  if (top != self)
    top.location.href = self.document.location;

  autoLogout = false;

  // Register the removePortlet method
  ajaxEngine.registerRequest( 'removePortlet',
                              '<html:rewrite page="/dashboard/RemovePortlet.do"/>' );

  ajaxEngine.registerRequest( 'movePortlet',
                              '<html:rewrite page="/dashboard/ReorderPortlets.do"/>' );

  function removePortlet(name, label) {
    ajaxEngine.sendRequest( 'removePortlet', 'portletName=' + name );
    new Effect.BlindUp($(name));

    var wide = isWide(name);
    if (!wide && !isNarrow(name)) {
        return;
    }

    var portletOptions;
    for (i = 0; i < document.forms.length; i++) {
      if (document.forms[i].wide) {
        if (wide == (document.forms[i].wide.value == 'true')) {
            portletOptions = document.forms[i].portlet.options;
            break;
        }
      }
    }

    if (portletOptions) {
        // Make sure that we are not re-inserting
        for (var i = 0; i < portletOptions.length; i++) {
            if (portletOptions[i].value == name) {
                return;
            }
        }
        portletOptions[portletOptions.length] = new Option(label, name);

        // Make sure div is visible
        $('addContentsPortlet' + wide).style.visibility='visible';
    }
  }

</script>
<script language="JavaScript" type="text/javascript">
    function refreshPortlets() {

        var problemPortlet = $('problemResourcesTable');
        var favoritePortlet = $('favoriteTable');

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

        for (i = 0; i < getRecentForm.length; i++) {

            if (/RemoveAlerts/.test(getRecentForm[i].action)) {
                for (i = 0; i < nodes.length; i++) {
                    if (/recentAlertsTable/.test(nodes[i].id)) {
                         //alert('in recent alerts table')
                        var alertTblId = nodes[i].id;
                        var getId = alertTblId.split('_');
                        var alertIdPart = getId[1];

                        if (alertIdPart) {
                            var alertIdToken = '_' + alertIdPart;

                            setInterval("requestRecentAlerts" + alertIdToken + "()", 30000);
                        } else {
                            setInterval("requestRecentAlerts()", 30000);
                        }

                    }
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

</script>
<html:link page="/Resource.do?eid=" linkName="viewResUrl" styleId="viewResUrl" style="visibility:hidden;"></html:link>

<%
  String divStart;
  String divEnd;
  String narrowWidth;
    
  String agent = request.getHeader("USER-AGENT");
  
  if (null != agent && -1 !=agent.indexOf("MSIE")) {
    divStart = "";
    divEnd = "";
    narrowWidth = "width='25%'";
  }
  else {
    divStart = "<div name=\"containerDiv\" style=\"margin:0px; width: 25%\">";
    divEnd = "</div>";
    narrowWidth = "width='100%'";
  }
%>

<c:set var="headerColspan" value="${portal.columns + 3}"/>

<div class="effectsContainer ">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td colspan="<c:out value="${headerColspan}"/>"><tiles:insert page="/portal/DashboardHeader.jsp"/></td>
  </tr>
  <tr>
    <td class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
      <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
<%-- Multi-columns Layout
  This layout render lists of tiles in multi-columns. Each column renders its tiles
  vertically stacked.  
--%>
<c:set var="narrow" value="true" scope="page" />
<c:set var="width"  value=""     scope="page" />
<c:set var="hr"     value=""     scope="page" />
<c:set var="showUpAndDown" value="true" scope="request"/>

<!-- Content Block -->
<c:forEach var="columnsList" items="${portal.portlets}" >  
  
  <c:choose>    
    <c:when test="${portal.columns eq 1}">    
      <c:set var="narrow" value="false" />
      <c:set var="hr" value="95%" />
      <c:set var="width" value="width='100%'" />
    </c:when>
  
    <c:when test="${narrow eq 'true'}">      
      <c:set var="hr" value="180" />
      <c:set var="width">
        <%= narrowWidth %>
      </c:set>
    </c:when>
    
    <c:otherwise>
      <c:set var="narrow" value="false" />
      <c:set var="hr" value="75%" />
      <c:set var="width" value="width='75%'" />
    </c:otherwise>
  </c:choose>

  <td valign="top" name="specialTd" <c:out value="${width}" escapeXml="false"/>>      
    <%= divStart %>

<ul id="<c:out value="narrowList_${narrow}"/>" class="boxy">
  <c:forEach var="portlet" items="${columnsList}">
  <c:set var="isFirstPortlet" value="${portlet.isFirst}" scope="request"/>
  <c:set var="isLastPortlet"  value="${portlet.isLast}"  scope="request"/>
  <li id="<c:out value="${portlet.fullUrl}"/>">
    <div class="DashboardPadding">
    <tiles:insert beanProperty="url" beanName="portlet" flush="true">
      <tiles:put name="portlet" beanName="portlet"/>
    </tiles:insert>
    </div>
  </li>
  </c:forEach>
</ul>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr><td valign="top" class="DashboardPadding">
  <c:choose>
  <c:when test="${narrow eq 'true'}">      
    <tiles:insert name=".dashContent.addContent.narrow" flush="true"/>
  </c:when>
  <c:otherwise>
    <tiles:insert name=".dashContent.addContent.wide" flush="true"/>
  </c:otherwise>
  </c:choose>
  </td></tr>
</table>
      <script type="text/javascript">
      <!--
        Sortable.create("<c:out value="narrowList_${narrow}"/>",
          {dropOnEmpty: true,
           format: /^(.*)$/,
           containment: ["<c:out value="narrowList_${narrow}"/>"],
           onUpdate: function() {
                //new Ajax.Request('<html:rewrite page="/dashboard/ReOrderPortlet.do"/>',{method: 'post', onSuccess: Sortable.serialize('<c:out value="narrowList_${narrow}"/>')}); },
                ajaxEngine.sendRequest( 'movePortlet', Sortable.serialize('<c:out value="narrowList_${narrow}"/>') ); },
           constraint: 'vertical'});
      -->
      </script>
      <c:choose >
        <c:when test="${narrow eq 'true'}">              
          <c:set var="narrow" value="false" />
        </c:when>
        <c:otherwise>              
          <c:set var="narrow" value="true" />
        </c:otherwise>
      </c:choose>

    <small name="footer"><br></small><html:img page="/images/spacer.gif" width="${hr}" height="1" border="0"/>
    <%= divEnd %>
    <script language="JavaScript" type="text/javascript">
      if (!isIE) {
        resizeToCorrectWidth();
      }
    </script>
  </td> 
  
</c:forEach>

  <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
</tr>
</table> 
</div>
<!-- /Content Block --> 

