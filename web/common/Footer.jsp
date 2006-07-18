<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<script src="<html:rewrite page="/js/"/>footer.js" type="text/javascript"></script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td rowspan="99" class="PageTitle">&nbsp;</td>
    <td colspan="2"><html:img page="/images/spacer.gif" width="1" height="60" alt="" border="0" styleId="footerSpacer"/></td>
  </tr>
  <tr> 
    <td rowspan="99" class="PageTitle" valign="top"><html:img page="/images/footer_corner.gif" width="8" height="8" alt="" border="0"/></td>
    <td width="100%"><html:img page="/images/spacer.gif" width="1" height="8" alt="" border="0"/></td>
  </tr>
  <tr class="PageTitle"> 
    <td>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>           
          <td class="FooterBold" nowrap>
          <tiles:insert definition=".footer.current.time"/>
          </td>
          <td class="FooterRegular" width="30">&nbsp;</td>
          <td class="FooterRegular" nowrap>
          <a href="javascript:about('<html:rewrite page="/common/"/>')"><fmt:message key="footer.HQ"/>
          <fmt:message key="footer.version"/> <c:out value="${HQVersion}"/></a> <c:out value="${HQBuild}"/></td>
          <td class="FooterRegular" width="30">&nbsp;</td>
          <td class="FooterSmall" nowrap><fmt:message key="footer.pageVersionPrefix"/><c:out value="${camTitle}"/><fmt:message key="footer.pageVersionSuffix"/>
          <td class="FooterRegular" width="100%" align="right"><fmt:message key="footer.Copyright"/></td>
          <td class="FooterBold" nowrap><a href="http://www.hyperic.com" target="_blank">www.hyperic.com</a></td>
          <td class="FooterBold" width="15">&nbsp;</td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<script language="JavaScript" type="text/javascript">
  setFoot();
</script>

