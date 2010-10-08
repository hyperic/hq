jQuery.noConflict();
var sid=-1;

jQuery(document).ready(function() {
    refresh();

    jQuery('#data').everyTime(5000, function() {
        refresh();
    });
});

var refresing=false;
function init(){
    jQuery('#gemfirePlugin a.member').click(function($){
        var id=jQuery(this).attr('id');
        setSelectedResource(id);
        return false;
    });
    jQuery("#data .loading").hide();
    refresing=false;
}

function setSelectedResource(id) {
    var url="";
    if(id==-1)
        url='/hqu/gemfire/gemfire/membersList.hqu?eid='+eid;
    else
        url='/hqu/gemfire/gemfire/member.hqu?eid='+eid+'&mid='+id;

    jQuery("#data .loading").show();
    jQuery('#data').load(url,reloadTree);
    sid=id;
}

function reloadTree(){
    init();
    jQuery('#tree').load('/hqu/gemfire/gemfire/tree.hqu?eid='+eid,init);
}


function refresh() {
    if(!refresing){
        refresing=true;
        setSelectedResource(sid);        
    }
}
