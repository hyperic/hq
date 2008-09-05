<%@ page language="java" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>

<tiles:importAttribute name="typeName"/>
<tiles:importAttribute name="aetid"/>

<td class="ListCell" align="center"><html:link page="/resource/${typeName}/monitor/Config.do?mode=configure&aetid=${aetid}"><html:img page="/images/tbb_editMetricTemplate.gif" width="136" height="16" border="0"/></html:link></td>
