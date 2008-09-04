<%@ page language="java" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>

<tiles:importAttribute name="typeName"/>
<tiles:importAttribute name="aetid"/>

<td class="ListCell" align="center">
  <html:link styleClass="buttonGreen" href="/resource/${typeName}/monitor/Config.do?mode=configure&aetid=${aetid}"><span><fmt:message key="resource.common.button.editMetricTemplate"></span></html:link>
</td>