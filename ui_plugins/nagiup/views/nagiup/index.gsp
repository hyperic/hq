<div class="nagContainer">
    <div class="nagTableContainer" id="nagCont">
        <div id="nagTable">
        <%def headerHTML = '<div style="float: right; margin-top:5px;padding-right:15px;"></div>'%>
        <%= dojoTable(id:'Nagiup', title:'Nagios Service Detail',
              refresh:60, url:urlFor(action:'data'), pageControls:false,
              schema:nagiupSchema, numRows:15) %>

        </div>
    </div>
</div>