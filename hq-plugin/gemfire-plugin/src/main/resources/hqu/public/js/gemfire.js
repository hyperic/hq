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
}

var sid=-1;
function setSelectedResource(id) {
    if(id==-1)
        jQuery('#data').load('/hqu/gemfire/gemfire/membersList.hqu?eid='+eid,init);
    else
        jQuery('#data').load('/hqu/gemfire/gemfire/member.hqu?eid='+eid+'&mid='+id,init);
    sid=id;
    jQuery('#tree').load('/hqu/gemfire/gemfire/tree.hqu?eid='+eid,init);
}
