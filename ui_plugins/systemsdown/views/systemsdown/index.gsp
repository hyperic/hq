<script type="text/javascript">
    <%= ajaxAccordionFilter( refresh:60, updateURL:urlFor(action:'summary')+"?q=all", id:"SystemsDownFilter", filterTargetId:"SystemsDown") %>
    plugin.accordion.update = function(kwArgs) {
        SystemsDown_refreshTable();
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
    
    plugin.MessagePanel = function(){
        this.toggleVisibility = function(reason){
            var msgPanelObj = dojo.byId("messagePanel");
            if(reason == "NO_DATA_RETURNED"){
                if(msgPanelObj.style.display != "block"){
                    msgPanelObj.style.display = "block";
                    dojo.byId("messagePanelMessage").innerHTML = this.getLocalizedMessageForReason(reason);
                }
            }else
                msgPanelObj.style.display = "none";
            
        };
        this.getLocalizedMessageForReason = function(reason){
            return "${l.noDataAvailable}";
        }
        dojo.event.topic.subscribe("XHRComplete", this, "toggleVisibility");
    }
    
    new plugin.MessagePanel();
    
    
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
        <%def headerHTML = '<div style="float: right; margin-top:5px;padding-right:15px;">Show Most Recent:&nbsp;<span id="defaultCount" onclick="updateFilterCount(50, this)">50</span>&nbsp;|&nbsp;<span id="onehundred" onclick="updateFilterCount(100,this)" class="countLinksActive">100</span>&nbsp;|&nbsp;<span id="onethousand"  onclick="updateFilterCount(1000,this)" class="countLinksActive">1000</span></div>'%>

        <%= dojoTable(id:'SystemsDown', title:l.SystemsDownTitle,
            refresh:60, pageControls:false, url:urlFor(action:'data'),
            schema:systemsDownSchema, numRows:50, headerHTML: headerHTML)
        %>
        </div>
    </div>
</div>
