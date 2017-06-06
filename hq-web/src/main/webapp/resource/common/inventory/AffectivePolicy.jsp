<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="AFFECTIVE_POLICY" var="AFFECTIVE_POLICY" />
<c:set var="affectivePolicy" value="${requestScope[AFFECTIVE_POLICY]}"/>

<c:if test="${not empty affectivePolicy}">
<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.configProps.affectivePolicy"/>
</div>
<div id="panelContent">
&nbsp; ${affectivePolicy}
</div>
</div>
</c:if>


