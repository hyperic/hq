<%
  import java.text.DateFormat
  import org.hyperic.hq.measurement.MeasurementConstants
  import org.hyperic.hq.events.EventConstants
  import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl
  
  // This dateformat is localized
  DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, 
                                                 DateFormat.LONG)
  String alertTime = df.format(new Date(alert.timestamp))
  
  def previousIndicators = [:]
  if (resource.supportsMonitoring) {
    previousIndicators = resource.designatedMetrics.getLastDataPoints(MeasurementConstants.ACCEPTABLE_LIVE_MILLIS)
  }
%>

The ${resource.name} ${resource.resourceType.name} has generated the following alert -
  ${action.shortReason}
  
------------------------------------------

ALERT DETAIL
- Resource Name:  ${resource.name}
- Alert Name:  ${alertDef.name}
<% if (alertDef.description) { %>
- Alert Description:  ${alertDef.description}
<% } %>
- Condition Set:  ${action.longReason}
- Alert Severity:  ${EventConstants.getPriority(alertDef.priority)}
- Alert Date:  ${alertTime}
<% if (alertDef.performsEscalations()) { 
       def lastFix = EscalationManagerEJBImpl.one.getLastFix(alertDef)
       if (lastFix) {  %>
- Previous Fix: ${lastFix}
<%     } 
} %>

<% if (previousIndicators) { %>
- Previous Indicator Metrics: 
  <% for (i in previousIndicators) { %>
    ${i.key.template.name}: ${i.key.template.formatValue(i.value)}    
  <% } %>
<% } %>

------------------------------------------

For additional detail about this alert, go to ${alert.urlFor('alert')}

------------------------------------------

This message was delivered to you by Hyperic HQ.
To view the HQ Dashboard, go to http://localhost:7080/Dashboard.do

------------------------------------------
