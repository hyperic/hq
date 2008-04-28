<%= dojoInclude(["dijit.InlineEditBox",
                 "dijit.form.TextBox" ]) %>
<link rel=stylesheet href="/hqu/public/hqu.css" type="text/css">

<script type="text/javascript">
function saveValue(key, newVal, oldVal) {
  dojo.xhrGet({
    url: '<%= urlFor(action:"setProp") %>',
    handleAs: "json-comment-filtered",
    content: {key: key, newVal: newVal, oldVal: oldVal},
    load: function(responseObj, ioArgs) {
    },
    error: function(responseObj, ioArgs) {
      alert('error! ' + responseObj);
    }
  });
}
</script>

<div>
  <table>
  <thead>
    <tr>
      <td>Key</td>
      <td>Value</td>
      <td>Default Value</td>
      <td>Read Only</td>
    </tr>
  </thead>
  <tbody>
  <% for (p in props) { %>
    <tr>
      <td>${h p.key}</td>
      <td>
        <span id="editable_${p.id}" dojoType="dijit.InlineEditBox"
            title="Click to edit" onSave="saveValue('${p.key}', arguments[0], arguments[1])">
            ${h p.value}
        </span>
      </td>
      <td>${h p.defaultValue}</td>
      <td>${h p.readOnly}</td>
    </tr>
  <% } %>
  </tbody>
  </table>
<div>
