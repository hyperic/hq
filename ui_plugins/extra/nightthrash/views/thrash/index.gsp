<script type="text/javascript">
function compressData() {
  dojo.xhrGet({
    url: '<%= urlFor(action:"compressData") %>',
    handleAs: "text/json-comment-filtered",
    load: function(responseObj, ioArgs) {
      dojo.byId('compressResult').innerHTML = responseObj.compressResult;
    }
  });
}

function dbAnalyze() {
  dojo.xhrGet({
    url: '<%= urlFor(action:"dbAnalyze") %>',
    handleAs: "text/json-comment-filtered",
    load: function(responseObj, ioArgs) {
      dojo.byId('analyzeResult').innerHTML = responseObj.analyzeResult;
    }
  });
}

function dbMaint() {
  dojo.xhrGet({
    url: '<%= urlFor(action:"dbMaint") %>',
    handleAs: "text/json-comment-filtered",
    load: function(responseObj, ioArgs) {
      dojo.byId('maintResult').innerHTML = responseObj.maintResult;
    }
  });
}

function purgeEventLogs() {
  dojo.xhrGet({
    url: '<%= urlFor(action:"purgeEventLogs") %>',
    handleAs: "text/json-comment-filtered",
    load: function(responseObj, ioArgs) {
      dojo.byId('purgeResult').innerHTML = responseObj.purgeResult;
    }
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
