<script type="text/javascript">
function compressData() {
  dojo.io.bind({
    url: '<%= urlFor(action:"compressData") %>',
    method: "post",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      dojo.byId('compressResult').innerHTML = data.compressResult;
    },
  });
}

function dbAnalyze() {
  dojo.io.bind({
    url: '<%= urlFor(action:"dbAnalyze") %>',
    method: "post",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      dojo.byId('analyzeResult').innerHTML = data.analyzeResult;
    },
  });
}

function dbMaint() {
  dojo.io.bind({
    url: '<%= urlFor(action:"dbMaint") %>',
    method: "post",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      dojo.byId('maintResult').innerHTML = data.maintResult;
    },
  });
}

function purgeEventLogs() {
  dojo.io.bind({
    url: '<%= urlFor(action:"purgeEventLogs") %>',
    method: "post",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      dojo.byId('purgeResult').innerHTML = data.purgeResult;
    },
  });
}

</script>

<div>
<button onclick='dbAnalyze()'>DB Analyze</button>
<div id='analyzeResult'></div>
</div>

<div>
<button onclick='compressData()'>Data Compression</button>
<div id='compressResult'></div>
</div>

<div>
<button onclick='dbMaint()'>DB Maintenance</button>
<div id='maintResult'></div>
</div>

<div>
<button onclick='purgeEventLogs()'>Purge Event Logs</button>
<div id='purgeResult'></div>
</div>
