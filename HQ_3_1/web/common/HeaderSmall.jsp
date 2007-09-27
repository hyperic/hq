<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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

<script language="JavaScript" type="text/javascript">
  var help = "<hq:help/>";
</script>

<script src="<html:rewrite page="/js/"/>diagram.js" type="text/javascript"></script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td width="22%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>
          <c:choose>
            <c:when test="${applicationScope.smallLogo}">           
              <td><html:img page="/customer/${applicationScope.smallLogoName}" width="225" height="22" alt="" border="0"/></td>          
            </c:when>
            <c:otherwise>
              <td><html:link page="/Dashboard.do"><html:img page="/images/logo_small.gif" width="119" height="22" alt="Click for Dashboard" border="0"/> </html:link></td>
            </c:otherwise>
          </c:choose>           
          <td width="100%" class="logoSm"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
          <td><html:img page="/images/logo_Image_small.gif" width="161" height="22" alt="" border="0"/></td>
        </tr>
      </table>
    </td>
    <td width="78%" class="MastheadBgBottom">
      <table width="250" border="0" cellspacing="4" cellpadding="0">
        <tr> 
          <td rowspan="99"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
          <td width="100%"><html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/toolbox_Help.gif" onmouseover="imageSwap(this, imagePath + 'toolbox_Help', '_on');" onmouseout="imageSwap(this, imagePath + 'toolbox_Help', '')" width="28" height="12" alt="" border="0"/></html:link></td>
        </tr>
      </table>
    </td>
  </tr>
  <tr> 
    <td colspan="3" class="MastheadBottomLine"><html:img page="/images/spacer.gif" width="1" height="2" alt="" border="0"/></td>
  </tr>
</table>
