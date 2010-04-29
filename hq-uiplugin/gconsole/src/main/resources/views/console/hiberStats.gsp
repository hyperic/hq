<h2>Hibernate Operations Summary</h2>
<table>
  <thead>
    <tr>
      <th>Operation</th>
      <th>Calls</th>
    </tr>
  </thead>
  <tbody>
  <% stats.each { op, val -> %>
    <tr>
      <td>${op}</td>
      <td>${val.num}</td>
    </tr>
  <% } %>
  </tbody>
</table>

<h2>Hibernate Access Log</h2>
<table width='100%'>
  <thead>
    <tr>
      <th width='15%'>Date</th>
      <th width='5%'>Duration</th>
      <th width='10%'>Op</th>
      <th width='70%'>Message</th>
    </tr>
  </thead>
  <tbody>
    <% for (l in logs) { %>
      <tr>
        <td>${dateFormat.format(l.start)}</td>
        <td>${l.duration}</td>
        <td>${l.op}</td>
        <td>${l.msg}</td>
      </tr>
    <% } %>
  </tbody>
</table>