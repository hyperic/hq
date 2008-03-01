<%@ page language="java" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>

<html:form action="/alerts/SetOpenNMSAction">
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine">
  <logic:messagesPresent>
  <tr>
    <td colspan="6" align="left" class="ErrorField"><html:errors/></td>
  </tr>
  </logic:messagesPresent>
  <tr>
    <td class="BlockLabel"><fmt:message key="alert.config.edit.opennms.server"/></td>
    <td class="BlockContent"><html:text property="server"/></td>
    <td class="BlockLabel"><fmt:message key="alert.config.edit.opennms.ip"/></td>
    <td class="BlockContent"><html:text property="ip"/></td>
    <td class="BlockLabel"><fmt:message key="alert.config.edit.opennms.port"/></td>
    <td class="BlockContent"><html:text property="port"/></td>
  </tr>
</table>
<html:hidden property="ad"/>
<html:hidden property="id"/>
<html:hidden property="aetid"/>
<html:hidden property="eid"/>
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent" style="border-bottom: 1px solid #D5D8DE;">
  <tr><td align="center"><html:image page="/images/tbb_set.gif" border="0" titleKey="FormButtons.ClickToOk" property="ok" style="padding-right: 5px;"/>
    <html:image page="/images/tbb_remove.gif" border="0" titleKey="FormButtons.ClickToDelete" property="delete" style="padding-left: 5px;"/></td></tr>
</table>
</html:form>
