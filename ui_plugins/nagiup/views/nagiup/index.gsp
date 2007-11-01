<script type="text/javascript">

var _hqu_Nagiup_sortField;
var _hqu_Nagiup_pageNum = 0;
var _hqu_Nagiup_lastPage = false;
var _hqu_Nagiup_sortOrder;
var _hqu_Nagiup_urlXtra = [];
var _hqu_Nagiup_ajaxCountVar = 0;

dojo.addOnLoad(function() {
    _hqu_Nagiup_table =
    dojo.widget.createWidget("dojo:FilteringTable",
    {alternateRows:false,
        valueField: "id"},
            dojo.byId("Nagiup"));
    _hqu_Nagiup_table.createSorter = function(a) {
        return
        null;
    };
    Nagiup_refreshTable();
});

// Allows the caller to specify a callback which will return
// additional query parameters (in the form of a map)
function Nagiup_addUrlXtraCallback(fn) {
    _hqu_Nagiup_urlXtra.push(fn);
}

function _hqu_Nagiup_makeQueryStr(kwArgs) {
    var res = '?pageNum=' + _hqu_Nagiup_pageNum;
    if (kwArgs && kwArgs.numRows)
        res += '&pageSize=' + kwArgs.numRows;
    else
        res += '&pageSize=15';
    if (kwArgs && kwArgs.typeId)
        res += '&typeId=' + kwArgs.typeId;

    if (_hqu_Nagiup_sortField)
        res += '&sortField=' + _hqu_Nagiup_sortField;
    if (_hqu_Nagiup_sortOrder != null)
        res += '&sortOrder=' + _hqu_Nagiup_sortOrder;

    var callbacks = _hqu_Nagiup_urlXtra;
    for (var i = 0; i < callbacks.length; i++) {
        var cb = callbacks[i];

        var cbmap = cb("Nagiup");
        for (var v in cbmap) {
            if (v == 'extend') continue;
            res += '&' + v + '=' + cbmap[v];
        }
    }
    return res;
}

function _hqu_Nagiup_setSortField(el) {
    if (el.getAttribute('sortable') == 'false')
        return;

    var curSortIdx =
            _hqu_Nagiup_table.sortInformation[0].index;
    _hqu_Nagiup_sortOrder =
    _hqu_Nagiup_table.sortInformation[0].direction;

    if (curSortIdx == el.getAttribute('colIdx')) {
        _hqu_Nagiup_sortOrder = ~_hqu_Nagiup_sortOrder & 1;
    } else {
        _hqu_Nagiup_sortOrder = 0;
    }
    _hqu_Nagiup_sortField = el.getAttribute('field');
    _hqu_Nagiup_table.sortInformation[0] =
    {index:el.getAttribute('colidx'),

        direction:_hqu_Nagiup_sortOrder};
    _hqu_Nagiup_pageNum = 0;
    Nagiup_refreshTable();
}

function Nagiup_refreshTable(kwArgs) {
    var queryStr = _hqu_Nagiup_makeQueryStr(kwArgs);
    _hqu_Nagiup_ajaxCountVar++;
    if (_hqu_Nagiup_ajaxCountVar > 0) {
        dojo.byId("_hqu_Nagiup_loadMsg").style.visibility =
        'visible';
    }
    dojo.io.bind({
        url: 'http://localhost:7080/hqu/nagiup/nagiup/data.hqu' + queryStr,
        method: "get",
        mimetype: "text/json-comment-filtered",
        load: function(type, data, evt) {
            AjaxReturn = data;
            _hqu_Nagiup_sortField = data.sortField;
            _hqu_Nagiup_sortOrder = data.sortOrder;
            var sortColIdx = 0;
            var thead =
                    dojo.byId("Nagiup").getElementsByTagName("thead")[0];
            var ths = thead.getElementsByTagName('th')
            for (j = 0; j < ths.length; j++) {
                if (ths[j].getAttribute('field') ==
                    _hqu_Nagiup_sortField) {
                    sortColIdx = j;
                    break;
                }
            }

            _hqu_Nagiup_table.sortInformation =
            [{index:sortColIdx,

                direction:_hqu_Nagiup_sortOrder}]
            _hqu_Nagiup_table.store.setData(data.data);
            _hqu_Nagiup_pageNum = data.pageNum;
            _hqu_Nagiup_lastPage = data.lastPage;
            _hqu_Nagiup_setupPager();
            _hqu_Nagiup_highlightRow(data.data);
            _hqu_Nagiup_ajaxCountVar--;
            if (_hqu_Nagiup_ajaxCountVar == 0) {

                dojo.byId("_hqu_Nagiup_loadMsg").style.visibility = 'hidden';
            }
            if (data.data == '') {
                dojo.byId("_hqu_Nagiup_noValues").style.display =
                '';
                dojo.byId("_hqu_Nagiup_noValues").innerHTML =
                "There isn't any information currently";
            }
        }
    });
}

function _hqu_Nagiup_highlightRow(el) {
    for (i = 0; i < el.length; i++) {
        var id = el[i].id;
        var body = document.getElementById("Nagiup");
        var trs = body.getElementsByTagName('tr');
        var styleClassVal = el[i].styleClass;
        if (id && (styleClassVal && styleClassVal != '')) {
            for (b = 0; b < trs.length; b++) {
                var vals = trs[b].getAttribute("value");
                if (id == vals) {
                    var rowTDs =
                            trs[b].getElementsByTagName('td');
                    for (k = 0; k < rowTDs.length; k++) {

                        rowTDs[k].setAttribute((document.all ? 'className' : 'class'),
                                styleClassVal);
                    }
                }
            }
        }
    }
}

function _hqu_Nagiup_setupPager() {
    var leftClazz = "noprevious";
    var pageNumDisplay = dojo.byId("_hqu_Nagiup_pageNumbers");
    pageNumDisplay.innerHTML = "Page " +
                               (_hqu_Nagiup_pageNum + 1);

    if (_hqu_Nagiup_pageNum != 0) {
        leftClazz = 'previousLeft';
    }

    dojo.byId("_hqu_Nagiup_pageLeft").setAttribute((document.all ?
                                                    'className' : 'class'), leftClazz);

    var rightClazz = "nonext";
    if (_hqu_Nagiup_lastPage == false) {
        rightClazz = "nextRight";
    }

    dojo.byId("_hqu_Nagiup_pageRight").setAttribute((document.all ?
                                                     'className' : 'class'), rightClazz);
}

function _hqu_Nagiup_nextPage() {
    if (_hqu_Nagiup_lastPage == false) {
        _hqu_Nagiup_pageNum++;
        Nagiup_refreshTable();
    }
}

function _hqu_Nagiup_previousPage() {
    if (_hqu_Nagiup_pageNum != 0) {
        _hqu_Nagiup_pageNum--;
        Nagiup_refreshTable();
    }
}

function _hqu_Nagiup_autoRefresh() {
    setTimeout("_hqu_Nagiup_autoRefresh()", 60000);
    Nagiup_refreshTable();
}

setTimeout("_hqu_Nagiup_autoRefresh()", 60000);
</script>
<style>
.nagContainer{margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;}
.nagTableContainer{border: 1px solid rgb(96, 165, 234); overflow-x: hidden; overflow-y: auto; float: right;display:inline; width: 99%; height: 442px;}
 #nagCont table {
     font-family: Lucida Grande, Verdana;
     font-size: 11px;
     width: 100%;
     border: 1px solid #ccc;
     border-collapse: collapse;
     cursor: default;
 }

 #nagCont  table th {
     font-weight: bold;
     padding: 3px;
     padding-left: 5px;
     border-right: 1px solid #7bafff;
     border-top: 1px solid #7bafff;
 }

 #nagCont table td {
     padding: 3px;
     font-weight: normal;
     padding-left: 5px;
 }

 #nagCont table thead td, table thead th {
     background-image: url( "images/ft-head.gif" );
     background-repeat: no-repeat;
     background-position: top right;
 }

 #nagCont table thead td.selectedUp, table thead th.selectedUp {
     background-image: url( "images/ft-headup.gif" );
 }

 #nagCont  table thead td.selectedDown, table thead th.selectedDown {
     background-image: url( "images/ft-headdown.gif" );
 }

 #nagCont table tbody tr td {
     border-bottom: 1px solid #ddd;
 }

 #nagCont table tbody tr.alt td {
     background: #eef4fc;
 }

 #nagCont  table tbody tr.selected td {

 }

 #nagCont table tbody tr:hover td {
     background: #a6c2e7;
 }

 #nagCont table tbody tr.selected:hover td {

 }

</style>


<div class="nagContainer">
    <div class="nagTableContainer" id="nagCont">
        <div id="nagTable">
        <%def headerHTML = '<div style="float: right; margin-top:5px;padding-right:15px;"></div>'%>
        <%= dojoTable(id:'Nagiup', title:'nagiup',
              refresh:60, url:urlFor(action:'data'), pageControls:false,
              schema:nagiupSchema, numRows:15) %>

        </div>
    </div>
</div>