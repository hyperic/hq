<%@ page language="java" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>

<c:set var="count" value="0"/>
<c:forEach var="timeTick" items="${timeIntervals}">
  <c:set var="eventsExist" value="${timeTick.eventsExist || eventsExist}"/>
</c:forEach>

<c:if test="${eventsExist}">

<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>

<script type="text/javascript">
  var eventsTime = 0;

  function initEventDetails() {
    ajaxEngine.registerRequest( 'getEventDetails', '<html:rewrite page="/resource/common/monitor/visibility/EventDetails.do"/>');
    ajaxEngine.registerAjaxElement('eventsSummary');
  }

  onloads.push( initEventDetails );

  function showEventsCallback() {
    var detail = dojo.byId('eventsSummary');
    if (detail.innerHTML == "") {
      setTimeout("showEventsCallback()", 500);
    }
    else {
      var div = dojo.byId('eventDetailTable');
      detail.innerHTML=unescape(detail.innerHTML);
      if (div.style.display == 'none')
        new Effect.Appear(div);
    }
  }

  function showEventsDetails(time, status) {
    eventsTime = time;
    var detail = dojo.byId('eventsSummary');
    detail.innerHTML = "";

    if (status != null)
      ajaxEngine.sendRequest( 'getEventDetails',
                              'eid=<c:out value="${eid}"/>',
                              'begin=' + time,
                              'status=' + status);
    else
      ajaxEngine.sendRequest( 'getEventDetails',
                              'eid=<c:out value="${eid}"/>',
                              'begin=' + time);
    showEventsCallback();
  }

  function hideEventDetail() {
    new Effect.Fade(dojo.byId('eventsSummary'));
  }
</script>

  <tr style="height: 12px; padding-top: 2px;">
    <td></td>
    <c:forEach var="timeTick" items="${timeIntervals}" varStatus="status">
      <c:set var="count" value="${status.count}"/>
    <td background="<html:rewrite page="/images/no_event.gif"/>" align="center" valign="middle">
      <c:if test="${timeTick.eventsExist}">
      <div class="eventBlock" onmouseover="this.style.backgroundColor='#0000ff'" onmouseout="this.style.backgroundColor='#003399'" onmousedown="overlay.delayTimePopup(<c:out value="${count - 1}"/>);showEventsDetails(<c:out value="${timeTick.time}"/>);overlay.moveOverlay(this)"></div>
      </c:if>
    </td>
    </c:forEach>
    <td align="right"><fmt:message key="resource.common.monitor.label.elc"/></td>
  </tr>
  <tr>
    <td colspan="<c:out value="${count + 2}"/>" style="height: 3px;"></td>
  </tr>

</c:if>
