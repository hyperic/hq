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
  
  def lastFix  = ""
  if (alertDef.performsEscalations()) {
       lastFix = Bootstrap.getBean(EscalationManager.class).getLastFix(alertDef)
  }
  
  def addAuxLogs(prefix, logs, buf) {
      for (l in logs) {
          if (l.URL) {
              buf << "$prefix - <a href=\"${HQUtil.baseURL}${l.URL}\">${l.description}</a><br>\n"
          } else {
              buf << "$prefix - ${l.description}<br>\n"
          }
          addAuxLogs(prefix + "&nbsp;&nbsp;", l.children, buf)
      }
  }

  def auxLogInfo = new StringBuffer()
  if (action.auxLogs) {
      auxLogInfo << "Additional Information:<br>"
      addAuxLogs("&nbsp;&nbsp;", action.auxLogs, auxLogInfo)
  }
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<title>Hyperic HQ - Alert Email</title>
</head>
<body style="font-family: arial, sans-serif;font-size:13px;">
<table border="0" width="95%" cellspacing="0" 
       style="margin-left:auto;margin-right:auto;border:1px solid #999999" >
  <tr>
    <td style="background-color:#ff6819;padding-top:5px;padding-bottom:5px;padding-left:10px;color:white;font-size:16px;border-top:4px solid #a5a5a5;border-bottom:4px solid #a5a5a5;">
      <b>Hyperic HQ - Alert Notification</b>
    </td>
  </tr>
  <tr>
    <td style="padding-left:10px;padding-top:10px;padding-bottom:10px;font-size:14px;color:#333399;border-bottom:4px solid #A5D9EE;">
      ${resource.name} has generated the following alert - ${action.shortReason}
    </td>
  </tr>
  <tr>
    <td style="padding-top:5px;padding-left:10px;">
      <b>ALERT DETAIL:</b>
    </td>
  </tr>
  <tr>
    <td style="padding-top:5px;padding-bottom:10px;padding-left:10px;border-bottom:4px solid #A5D9EE;">
      <table width="100%" style="border: none; padding: 0px;">
      <tr>
      <td width="10%" nowrap>Resource Name:</td><td> <b>${resource.name}</b></td>
      </tr>
      <tr>
      <td nowrap>Alert Name:</td><td> <b>${alertDef.name}</b></td>
      </tr>
      <tr>
      <% if (alertDef.description) { %>
      <td nowrap>Alert Description:</td><td> <b>${alertDef.description}</b></td>
      </tr>
      <% } %>
      <tr>
      <td nowrap>Alert Date / Time:</td><td> <b>${alertTime}</b></td>
      </tr>
      <tr>
      <td nowrap>Triggering Condition(s):</td><td> ${action.longReason}</td>
      </tr>
      <tr>
      <td nowrap>Alert Severity:</td><td> <b>${EventConstants.getPriority(alertDef.priority)}</b></td>
      </tr>
      <% if (lastFix) { %>
      <tr>
           <td nowrap>Previous Resolution:</td><td> <b>${lastFix}</b></td>
      </tr>
      <% } %>
      <% if (resource.supportsMonitoring) { %>
      <tr>
           <td valign="top" nowrap>Last Indicator Metrics Collected:</td>
           <td>
           <% for (i in resource.designatedMetrics.getLastDataPoints(MeasurementConstants.ACCEPTABLE_LIVE_MILLIS)) { %>
               <% if (i.value != null) { %>
             &nbsp;&nbsp;&nbsp;[${df.format(new Date(i.value.timestamp))}] ${i.key.template.name} = <b>${i.key.template.formatValue(i.value)}</b><br>
               <% } %>
           <% } %>
           </td>
      </tr>
      <% } %>
      </table>
      ${auxLogInfo}
    </td>
  </tr>
  <tr>
    <td style="padding-left:10px;padding-top:10px;padding-bottom:10px;border-bottom:4px solid #A5D9EE;">
      For additional detail about this resource, go to <a href="${HQUtil.baseURL + resource.urlFor('currentHealth')}">${HQUtil.baseURL + resource.urlFor('currentHealth')}</a><br><br>
      For additional detail about this alert, go to <a href="${HQUtil.baseURL + alert.urlFor('alert')}">${HQUtil.baseURL + alert.urlFor('alert')}</a>
    </td>
  </tr>
  <tr>
    <td style="padding-left:10px;padding-top:10px;padding-bottom:10px;border-bottom:4px solid #A5D9EE;">
      This message was delivered to you by Hyperic HQ.
      To view the HQ Dashboard, go to <a href="${HQUtil.baseURL}/Dashboard.action">${HQUtil.baseURL}/Dashboard.action</a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="http://www.hyperic.com/images/app/bg-footer.gif" alt="Hyperic footer image">
    </td>
  </tr>
  </table>
</body>
</html>
