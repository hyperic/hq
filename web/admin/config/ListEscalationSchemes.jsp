<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
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

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>

<table id="escalations" width="100%" cellpadding="0" cellspacing="0"><tr class="ListRow"><td colspan="2" class="ListHeaderInactiveSorted">Escalation Scheme</td></tr></table>

<script langugage="text/Javascript">
  var escJson = eval( '( { "escalations":<c:out value="${escalations}" escapeXml="false"/> })' );

  var schemes = escJson.escalations;

  if (schemes.length == 0) {
    var tr = document.createElement("tr");
    tr.setAttribute('class', 'ListRow');

    var td = document.createElement("td");
    td.setAttribute('class', 'ListCell');
    td.setAttribute('colspan', '2');
    td.innerHTML = '<fmt:message key="admin.config.message.noEscalations"/>';
    tr.appendChild(td);

    $('escalations').appendChild(tr);
  }

  for (var i = 0; i < schemes.length; i++) {
    var tr = document.createElement("tr");
    if ((i % 2) == 0) {
      tr.setAttribute('class', 'tableRowEven');
    }
    else {
      tr.setAttribute('class', 'tableRowOdd');
    }

    var td = document.createElement("td");
    td.setAttribute('class', 'ListCell');
    td.innerHTML = schemes[i].name;
    tr.appendChild(td);

    td = document.createElement("td");
    td.setAttribute('class', 'ListCell');
    td.setAttribute('style', 'text-align: right');
    td.innerHTML = '[-' + schemes[i].id + '-]';
    tr.appendChild(td);

    $('escalations').appendChild(tr);
  }
</script>

<br/>
<br/>
<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
