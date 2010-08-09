<link type="text/css" href="/${urlFor(asset:'css')}/ui.all.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/jquery.auto-complete.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/jquery.hyperic.treecontrol.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/jquery.hyperic.healthcontrol.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/tablesorter/blue/style.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/hq-gemfire.css" rel="stylesheet"/>
<!--[if IE]><script language="javascript" type="text/javascript" src="/${urlFor(asset:'js')}/excanvas.min.js"></script><![endif]-->
<script src="/${urlFor(asset:'js')}/jquery-1.4.2.min.js" type="text/javascript"></script>
<script type="text/javascript">
        jQuery.noConflict();
</script>
<script src="/${urlFor(asset:'js')}/jquery-ui-1.8.2.custom.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.auto-complete.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.flot.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.timers-1.2.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.ba-bbq.1.2.1.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.hyperic.treecontrol.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.hyperic.healthcontrol.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/date.format.js" type="text/javascript"></script>

<script type="text/javascript">
jQuery(document).ready(function() {
  jQuery("#tree-inventory").treecontrol({
    url: "/hqu/gemfire/gemfire/inventory.hqu?eid=${eid}",
    refreshInterval:10000,
    selectedCallback: function(item) {
    setSelectedResource(item.id);
    }
  });
  jQuery('#data').load('/hqu/gemfire/gemfire/membersList.hqu?eid=${eid}');
});

function setSelectedResource(id) {
  jQuery('#data').load('/hqu/gemfire/gemfire/member.hqu?eid=${eid}&mid='+id);
}

</script>
<div id="gemfirePlugin">
  <table>
    <tr>
      <td width="350px">
        <div id="tree-inventory"></div>
      </td>
      <td>
        <div id="data"></div>
      </td>
    </tr>
  </table>
</div>
