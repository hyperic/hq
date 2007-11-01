

<style>
.nagContainer{margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;}
.nagTableContainer{border: 1px solid rgb(96, 165, 234); overflow-x: hidden; overflow-y: auto; float: right;display:inline; width: 99%; height: 442px;}
#nagCont table { font-family: Lucida Grande, Verdana;font-size: 11px;width: 100%;border: 1px solid #ccc;border-collapse: collapse;cursor: default;}
#nagCont  table th {font-weight: bold;padding: 3px;padding-left: 5px;border-right: 1px solid #7bafff;border-top: 1px solid #7bafff;}
#nagCont table td {padding: 3px;font-weight: normal;padding-left: 5px; }
#nagCont table thead td, table thead th {background-image: url( "images/ft-head.gif" );background-repeat: no-repeat;background-position: top right;}
#nagCont table thead td.selectedUp, table thead th.selectedUp {background-image: url( "images/ft-headup.gif" ); }
#nagCont  table thead td.selectedDown, table thead th.selectedDown {background-image: url( "images/ft-headdown.gif" ); }
#nagCont table tbody tr td {border-bottom: 1px solid #ddd;}
#nagCont table tbody tr.alt td {background: #eef4fc;}
#nagCont  table tbody tr.selected td {}
#nagCont table tbody tr:hover td {background: #a6c2e7;}
#nagCont table tbody tr.selected:hover td {}
.statusBGCRITICAL {background-color: #FFBBBB; }
.statusCRITICAL{background-color:#F83838;}
.statusBGUNKNOWN {background-color: #FFDA9F; }
.statusUNKNOWN {background-color:#FF9900;}
.statusBGOK {background-color:#DBDBDB;}
.statusOK {background-color:#33FF00;}
.statusBGWARNING {background-color: #FEFFC1; }
.statusWARNING {background-color: #ffff00;}
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