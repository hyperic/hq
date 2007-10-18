<script type="text/javascript">
    var selectedItem;
    var url = "";
    var plugin={};plugin.accordion={};plugin.ajax={};
    
    plugin.ajax.getData = function(type, data, evt) {
        if (data) {
            var domTree = document.getElementById('resourceTree');
            var tree = "";
            var unique;
            for (var x = 0; x < data.length; x++) {
                var parent = data[x]['parent'];
                var children = data[x]['children'];
                var innerChildren = "";
                var markExpanded = false;
                for (var i = 0; i < children.length; i++) {
                    if(selectedItem){
                        var ddd = selectedItem.resourceId;
                        if(selectedItem.resourceId == children[i]['id']){
                            unique = dojo.dom.getUniqueId();
                            innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['url'], children[i]['id'], unique);
                            markExpanded = true;
                        } else {
                            innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['url'], children[i]['id']);
                        }
                    }else{
                        innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['url'], children[i]['id']);
                    }
                }
                tree +=  plugin.accordion.createParent(data[x]['parent'], data[x]['id'], children.length, innerChildren, markExpanded);
            }
            domTree.innerHTML = tree;
            if(unique) {
                setSelected(dojo.byId(unique));
            }
        }
    }

    plugin.accordion.createParent = function(name, id, length, innerChildren, markExpanded) {
        var ret = '<div class="topCat" onclick="swapVis(this);" resourceId="'+id+'">'+name+" ("+length+")</div>"
            + '<div class="resourcetypelist"';
        if(markExpanded) {
            ret += '>' ;
        } else {
            ret += 'style="display:none">' ;
        }
        ret += innerChildren;
        ret += "</div>";
        return ret;
    };

    plugin.accordion.createChild = function(name, id, domId) {
        if(domId){
            return '<div class="listItem" onclick="swapSelected(this);itemClicked(this);" id="' + domId + '" resourceId="' + id + '">' + name + '</div>';
        }else
        return '<div class="listItem" onclick="swapSelected(this);itemClicked(this);" resourceId="' + id + '">' + name + '</div>';
    };
    
    function itemClicked(item) {
        var url = '&resourceId='+item.resourceId;
        //mixin with the table update
    }
    
    function swapVis(elem) {
        var sib = elem.nextSibling;
        if(dojo.html.getStyleProperty(sib, 'display')=='none'){
            sib.style.display='block';
        } else {
            sib.style.display='none';
        }
    }
    
    function swapSelected(elem) {
        if(selectedItem){
            selectedItem.style.padding = '3px';
            selectedItem.style.border = '';
            selectedItem.style.background = '';
        }
        selectedItem = elem;
        setSelected(selectedItem);
    }
    
    function setSelected(elem){
        elem.style.padding = '2px';
        elem.style.border = '1px dotted dimGray';
        elem.style.background = '#EFF0F2 none repeat scroll 0%';
    }
    
    function disableSelection(element) {
        element.onselectstart = function() {
            return false;
        };
        element.unselectable = "on";
        element.style.MozUserSelect = "none";
        //element.style.cursor = "default";
    }

    function openAll() {
        var tree = document.getElementById('resourceTree');
        var x = tree.getElementsByTagName('div');
        for (var i = 0; i < x.length; i++) {
            if (x[i].className == 'resourcetypelist') {
                x[i].style.display = '';
               // setselColor(x[i])
            }
        }
    }

    function closeAll() {
        var tree = document.getElementById('resourceTree');
        var x = tree.getElementsByTagName('div');
        for (var i = 0; i < x.length; i++) {
            if (x[i].className == 'resourcetypelist') {
                x[i].style.display = 'none';
                //setunselColor(x[i])
            }
        }
    }

    function setselColor(elem) {
        elem.style.backgroundColor = "#EEf3f3";
    }

    function setunselColor(elem) {
        elem.style.backgroundColor = "#ffffff";
    }

    plugin.ajax.bindMixin = {
       load: plugin.ajax.getData,
       method: "get",
       mimetype: "text/json-comment-filtered"
    };

    plugin.ajax.bind = function (url){
       plugin.ajax.bindMixin.url = url;
       dojo.io.bind(plugin.ajax.bindMixin);
    }

    dojo.addOnLoad(function(){
        plugin.ajax.bind("/hqu/systemsdown/systemsdown/summary.hqu?q=all");
    });
    
    setInterval(function(){
        plugin.ajax.bind("/hqu/systemsdown/systemsdown/summary.hqu?q=all");
    }, 120000);
</script>

<div class="downContainer">
    <div class="downList">
        <div class="leftbxblueborder">
            <div class="BlockTitle" style="text-align:left;">${l.ResType}</div>
                <div>
                    <div class="boxLinks left">
                        <a href="javascript:closeAll();">${l.CollAll}</a>
                    </div>
                    <div class="boxLinks right">
                        <a href="javascript:openAll();">${l.DispAll}</a>
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
        <%= dojoTable(id:'SystemsDown', title:l.SystemsDownTitle,
            refresh:60, pageControls:false, url:urlFor(action:'data'),
            schema:systemsDownSchema, numRows:100) 
        %>
        </div>
    </div>
</div>
