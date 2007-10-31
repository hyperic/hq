<script type="text/javascript">
    <%= accordionSidebar( refresh:60 ) %>
    plugin.accordion.itemClicked = function(item) {
        var url = '&nodeid=' + item.nodeid;
        //mixin with the table update
    }

    plugin.accordion.swapVis = function(elem) {
        plugin.accordion.disableSelection(elem);
        var sib = elem.nextSibling;
        if (dojo.html.getStyleProperty(sib, 'display') == 'none') {
            sib.style.display = 'block';
        } else {
            sib.style.display = 'none';
        }
        plugin.accordion.update({typeId: elem.getAttribute('nodeid')});
    }

    plugin.accordion.swapSelected = function(elem) {
        plugin.accordion.disableSelection(elem);
        if (selectedItem && typeof(selectedItem) == 'object') {
            selectedItem.style.padding = '3px';
            selectedItem.style.border = '';
            selectedItem.style.background = '';
        }
        selectedItem = elem;
        dojo.io.cookie.setCookie('selecteditemid', elem.getAttribute('nodeid'));
        plugin.accordion.setSelected(selectedItem);
        plugin.accordion.update({typeId: elem.getAttribute('nodeid')});
    }

    plugin.accordion.setSelected = function(elem) {
        elem.style.padding = '2px';
        elem.style.border = '1px dotted dimGray';
        elem.style.background = '#EFF0F2 none repeat scroll 0%';
    }

    plugin.accordion.disableSelection = function(element) {
        element.onselectstart = function() {
            return false;
        };
        element.unselectable = "on";
        element.style.MozUserSelect = "none";
    }

    plugin.accordion.openAll = function() {
        var tree = document.getElementById('resourceTree');
        var x = tree.getElementsByTagName('div');
        for (var i = 0; i < x.length; i++) {
            if (x[i].className == 'resourcetypelist') {
                x[i].style.display = '';
                // setselColor(x[i])
            }
        }
    }

    plugin.accordion.closeAll = function() {
        var tree = document.getElementById('resourceTree');
        var x = tree.getElementsByTagName('div');
        for (var i = 0; i < x.length; i++) {
            if (x[i].className == 'resourcetypelist') {
                x[i].style.display = 'none';
                //setunselColor(x[i])
            }
        }
    }

    plugin.accordion.setselColor = function(elem) {
        elem.style.backgroundColor = "#EEf3f3";
    }

    plugin.accordion.setunselColor = function(elem) {
        elem.style.backgroundColor = "#ffffff";
    }

    plugin.accordion.update = function(kwArgs) {
        SystemsDown_refreshTable(kwArgs);
    }

    function updateFilterCount(count, obj) {
        if (currentCountFilter) {
            if (currentCountFilter == obj) {
                return;
            }
            currentCountFilter.style.color = '#003399';
            currentCountFilter.style.cursor = 'pointer';
            currentCountFilter.style.fontWeight = 'bold';
        }
        dojo.io.cookie.setCookie('filtercount', obj.id);
        currentCountFilter = obj;
        currentCountFilter.style.color = '#000000';
        currentCountFilter.style.cursor = 'default';
        currentCountFilter.style.fontWeight = 'normal';
        plugin.accordion.update({numRows: count});
    }
</script>
<div class="downContainer">
    <div class="downList">
        <div class="leftbxblueborder">
            <div class="BlockTitle" style="text-align:left;">${l.ResType}</div>
                <div>
                    <div class="boxLinks left">
                        <a href="javascript:plugin.accordion.closeAll();">${l.CollAll}</a>
                    </div>
                    <div class="boxLinks right">
                        <a href="javascript:plugin.accordion.openAll();">${l.DispAll}</a>
                    </div>
                    <div style="clear:both;height:1px;"></div>
                </div>
                <div class="leftbxht">
                    <div class="tree" id="resourceTree" >
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="downTableContainer" id="downCont">
        <div id="systemsDownTable">
        <%def headerHTML = '<div style="float: right; margin-top:5px;padding-right:15px;">Show Most Recent:&nbsp;<span id="defaultCount" onclick="updateFilterCount(50, this)">50</span>&nbsp;|&nbsp;<span id="onehundred" onclick="updateFilterCount(100,this)" class="countLinksActive">100</span>&nbsp;|&nbsp;<span id="onethousand"  onclick="updateFilterCount(1000,this)" class="countLinksActive">1000</span></div>'%>

        <%= dojoTable(id:'SystemsDown', title:l.SystemsDownTitle,
            refresh:60, pageControls:false, url:urlFor(action:'data'),
            schema:systemsDownSchema, numRows:50, headerHTML: headerHTML)
        %>
        </div>
    </div>
</div>
