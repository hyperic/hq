<script type="text/javascript">
    getDojo();
</script>
<div id="auditTable">
  <%= dojoTable(id:'Audits', title:l.AuditTitle,
                refresh:60, url:urlFor(action:'data'),
                schema:auditSchema, numRows:15) %>
</div>
