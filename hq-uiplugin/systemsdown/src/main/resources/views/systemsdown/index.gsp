<script type="text/javascript">
    document.navTabCat = "Resource";
    
    <%= ajaxAccordionFilter( refresh:60, updateURL:urlFor(action:'summary')+"?q=all", id:"SystemsDownFilter", filterTargetId:"SystemsDown") %>
    plugin.accordion.update = function(kwArgs) {
        if(kwArgs.numRows)
            updateKWArgs.numRows = kwArgs.numRows;
        SystemsDown_refreshTable(updateKWArgs);
    }

    function updateFilterCount(count, obj) {
        updateKWArgs.numRows=count;
        if (currentCountFilter) {
            currentCountFilter.style.color = '#003399';
            currentCountFilter.style.cursor = 'pointer';
            currentCountFilter.style.fontWeight = 'bold';
        }else
            return;
        hqDojo.cookie('filtercount', obj.id);
        currentCountFilter = obj;
        currentCountFilter.style.color = '#000000';
        currentCountFilter.style.cursor = 'default';
        currentCountFilter.style.fontWeight = 'normal';
        plugin.accordion.update({numRows: count});
    }
    
    plugin.MessagePanel = function(){
        this.toggleVisibility = function(reason){
            var msgPanelObj = hqDojo.byId("messagePanel");
            if(reason == "NO_DATA_RETURNED"){
                if(msgPanelObj.style.display != "block"){
                    msgPanelObj.style.display = "block";
                    hqDojo.byId("messagePanelMessage").innerHTML = this.getLocalizedMessageForReason(reason);
                }
            }else
                msgPanelObj.style.display = "none";
            
        };
        this.getLocalizedMessageForReason = function(reason){
            return "${l.noDataAvailable}";
        };
        hqDojo.subscribe("XHRComplete", this, "toggleVisibility");
    }
    
    new plugin.MessagePanel();
    
    hqDojo.subscribe("XHRComplete", function() {
    	setFoot();
    });    
</script>
<div class="messagePanel messageInfo" style="display:none;" id="messagePanel"><div class="infoIcon"></div><span id="messagePanelMessage"></span></div>
<div class="downContainer" style="clear:both;">
    <div class="downList">
        <div class="leftbxblueborder">
            <div class="BlockTitle" style="text-align:left;">${l.ResType}</div>
                <div id="functionPanel">
                    <div class="boxLinks left">
                        <a href="javascript:plugin.accordion.closeAll();">${l.CollAll}</a>
                    </div>
                    <div class="boxLinks right">
                        <a href="javascript:plugin.accordion.openAll();">${l.DispAll}</a>
                    </div>
                    <div style="clear:both;height:1px;line-height:1px;"><!-- line-height hack for IE --></div>
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
        <%def headerHTML = '<div style="float: right; margin-top:5px;padding-right:15px;">Show Most Recent:&nbsp;<span id="50" onclick="updateFilterCount(50, this)" class="countLinksActive">50</span>&nbsp;|&nbsp;<span id="100" onclick="updateFilterCount(100, this)" class="countLinksActive">100</span>&nbsp;|&nbsp;<span id="1000"  onclick="updateFilterCount(1000,this)" class="countLinksActive">1000</span></div>'%>

        <%= dojoTable(id:'SystemsDown', title:l.SystemsDownTitle,
            refresh:60, pageControls:false, url:urlFor(action:'data'),
            schema:systemsDownSchema, numRows:25, pageSize:50, headerHTML: headerHTML)
        %>
        </div>
    </div>
</div>
