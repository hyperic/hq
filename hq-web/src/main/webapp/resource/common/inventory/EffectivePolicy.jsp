<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="EFFECTIVE_POLICY" var="EFFECTIVE_POLICY" />
<c:set var="effectivePolicy" value="${requestScope[EFFECTIVE_POLICY]}"/>

<c:if test="${not empty effectivePolicy}">
<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.configProps.effectivePolicy"/>
</div>
<div id="panelContent">
&nbsp; ${effectivePolicy}
</div>
</div>
</c:if>


