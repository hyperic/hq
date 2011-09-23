jQuery.noConflict();
var sid=-1;

jQuery(document).ready(function() {
    refresh();

    jQuery('#data').everyTime(8000, function() {
        refresh();
    });
});

var refreshing=false;
var stopRefreshing=false;
function init(response, status, xhr){
	if (status == "error") {
	    var msg = "An error occurred: ";
	    jQuery('#error').html("<font color='red'>" + msg + " " + xhr.statusText+ "</font>");
        stopRefreshing=true;
    } else {
	    jQuery('#gemfirePlugin a.member').click(function($) {
			var id = jQuery(this).attr('id');
			setSelectedResource(id);
			return false;
		});
		jQuery("#data .loading").hide();
		refreshing=false;
    }
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

function reloadTree(response, status, xhr){
    init(response, status, xhr);
    jQuery('#tree').load('/hqu/gemfire/gemfire/tree.hqu?eid='+eid,init);
}


function refresh() {
    if(!refreshing && !stopRefreshing){
        refresing=true;
        setSelectedResource(sid);        
    }
}
