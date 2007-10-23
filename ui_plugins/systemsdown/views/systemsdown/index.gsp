<script type="text/javascript">
    <%= accordionSidebar( refresh:60 ) %>

    function updateFilterCount(count,obj){
        if(currentCountFilter){
            if(currentCountFilter == obj){
                return;
            }
            currentCountFilter.style.color='#003399';
            currentCountFilter.style.cursor='pointer';
            currentCountFilter.style.fontWeight='bold';
        }
        dojo.io.cookie.setCookie('filtercount', obj.id);
        currentCountFilter = obj;
        currentCountFilter.style.color='#000000';
        currentCountFilter.style.cursor='default';
        currentCountFilter.style.fontWeight='normal';
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
        <%def headerHTML = '<div style="float: right; margin:5px 5px 0px 0px;">Show:&nbsp;<span id="defaultCount" onclick="updateFilterCount(50, this)">50</span>&nbsp;|&nbsp;<span id="onehundred" onclick="updateFilterCount(100,this)" class="countLinksActive">100</span>&nbsp;|&nbsp;<span id="onethousand"  onclick="updateFilterCount(1000,this)" class="countLinksActive">1000</span></div>'%>
        
        <%= dojoTable(id:'SystemsDown', title:'Resource Availability',
            refresh:60, pageControls:false, url:urlFor(action:'data'),
            schema:systemsDownSchema, numRows:50, headerHTML: headerHTML) 
        %>
        </div>
    </div>
</div>
