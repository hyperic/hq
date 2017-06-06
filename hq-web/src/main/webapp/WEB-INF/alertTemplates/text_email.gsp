<%
  import java.text.DateFormat
  import org.hyperic.hq.hqu.rendit.util.HQUtil
  import org.hyperic.hq.measurement.MeasurementConstants
  import org.hyperic.hq.events.EventConstants
  import org.hyperic.hq.escalation.shared.EscalationManager
  import org.hyperic.hq.context.Bootstrap
  
  DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, 
                                                 DateFormat.LONG)
  String alertTime = df.format(new Date(alert.timestamp))
  
  def indicatorStr = new StringBuffer()
  if (resource.supportsMonitoring) {
      indicatorStr << "\nLast Indicator Metrics Collected: \n"
      def window = MeasurementConstants.ACCEPTABLE_LIVE_MILLIS
      for (i in resource.designatedMetrics.getLastDataPoints(window)) {
          if (i.value != null) {
                indicatorStr << "    [${df.format(new Date(i.value.timestamp))}] ${i.key.template.name} = ${i.key.template.formatValue(i.value)}\n"
          }
      }
  }

  def lastFixStr = ""
  if (alertDef.performsEscalations()) {
       def lastFix = Bootstrap.getBean(EscalationManager.class).getLastFix(alertDef)
       if (lastFix) 
           lastFixStr = "\n- Previous Resolution:            ${lastFix}"
  }

  def addAuxLogs(prefix, logs, buf) {
      for (l in logs) {
          buf << "$prefix${l.description}\n"
          if (l.URL) {
              buf << "$prefix  - ${HQUtil.baseURL}${l.URL}\n"
          }
          addAuxLogs(prefix + "  ", l.children, buf)
      }
  }

  def auxLogInfo = new StringBuffer()
  if (action.auxLogs) {
      auxLogInfo << "\n\nAdditional Information:\n"
      addAuxLogs("  ", action.auxLogs, auxLogInfo)
  }
  
%>
${resource.name} has generated the following alert:
    ${action.shortReason}
  
------------------------------------------

ALERT DETAIL
- Resource Name:     ${resource.name}
- Alert Name:        ${alertDef.name}<%
if (alertDef.description) { %>
- Alert Description: ${alertDef.description} <% } %>
- Alert Date / Time: ${alertTime}
- Triggering Condition(s): ${action.longReason}
- Alert Severity:    ${EventConstants.getPriority(alertDef.priority)}${lastFixStr}
${auxLogInfo}${indicatorStr}

------------------------------------------

For additional detail about this resource, go to <%= HQUtil.baseURL + resource.urlFor('currentHealth') %>

For additional detail about this alert, go to <%= HQUtil.baseURL + alert.urlFor('alert') %>

------------------------------------------

This message was delivered to you by Hyperic HQ.
To view the HQ Dashboard, go to ${HQUtil.baseURL}/Dashboard.action

------------------------------------------
