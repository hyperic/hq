<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitleBar"> 
    <td><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="34%"><c:out value="${attachment.description}"/></td>
    <td width="33%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="26" alt="" border="0"/></td>
  </tr>
  <tr>
  	<td rowspan="99" class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
</table>

<c:url var="attachUrl" context="/hqu/${attachment.plugin.name}" value="${attachment.path}"/>

<div style="border-left: 4px solid #CCCCCC;">
<iframe src="<c:out value="${attachUrl}"/>" frameborder="no"></iframe>
</div>
