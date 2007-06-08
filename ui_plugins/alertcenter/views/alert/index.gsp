<script type="text/javascript">
    var alertColumns = [
        { field: "Date"},
        { field: "Time"},
        { field: "Alert" },
        { field: "Resource" },
        { field: "State" },
        { field: "Severity" },
        { field: "Group" },
    ];
    
    dojo.addOnLoad(function() {
        filteringTable = dojo.widget.createWidget("dojo:FilteringTable",
                                                  {valueField: "myId"},
                                                  dojo.byId("content"));

        for (var x = 0; x<alertColumns.length; x++) {
            filteringTable.columns.push(filteringTable.createMetaData(alertColumns[x]));
        }
        var bindArgs = {
            url: 'data.hqu',
            method: "get",
            mimetype: "text/json-comment-filtered",
            handle: function(type,data,evt) {
                AjaxReturn = data;
                filteringTable.store.setData(data);
            }
        };
        dojo.io.bind (bindArgs);
    });    
</script>

<div id="alert" style="width:98%;margin-left:auto;margin-right:auto;margin-top:20px;margin-bottom:20px;">
   <table id="content"></table>
</div> 
