jQuery.noConflict();

jQuery(document).ready(function() {
    setSelectedResource(sid);
    jQuery('#data').everyTime(5000, function() {
        setSelectedResource(sid);
    });
});

function init(){
    jQuery('#gemfirePlugin a.member').click(function($){
        var id=jQuery(this).attr('id');
        setSelectedResource(id);
        return false;
    });
    if(sid!=-1)
        jQuery('#gemfirePlugin ul a#'+sid+'.member').append("&larr;"); // XXX change it for a css prop.
    jQuery("#data .loading").hide();
}

var sid=-1;
function setSelectedResource(id) {
    var url="";
    jQuery("#data .loading").show();
    if(id==-1)
        url='/hqu/gemfire/gemfire/membersList.hqu?eid='+eid;
    else
        url='/hqu/gemfire/gemfire/member.hqu?eid='+eid+'&mid='+id;
    jQuery('#data').load(url,reloadTree);
    sid=id;
}

function reloadTree(){
    init();
    jQuery('#tree').load('/hqu/gemfire/gemfire/tree.hqu?eid='+eid,init);
}
