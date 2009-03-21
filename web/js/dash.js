// NOTE: requires prototype.js

function changeDashboard(formId){
    $(formId).submit();
}

function selectDefaultDashboard(url){
    var selectDashboardEl = $('dashSelect');
    var defaultDashboardEl = $('defaultDashboard');
    
    if (defaultDashboardEl == null || selectDashboardEl == null) return;  // elements don't exist by ids given
	
    var index = selectDashboardEl.selectedIndex;
	var options = selectDashboardEl.options;
	var currDefault = defaultDashboardEl;
	
	if (index < 0) {
		$("dashboardSelectionErrorPanel").setStyle({ display:"block" });
		return;
	}
	
	var newDefault = options[index].value;
	
	if (newDefault != currDefault) {
		defaultDashboardEl.value = newDefault;
		
		$('makeDefaultBtn').hide();
		
	    if (url != null && url != 'undefined') {
	    	new Ajax.Request(url, {
				method: 'post',
				parameters: { 'defaultDashboard' : newDefault },
				onSuccess: function(transport) {
					$('makeDefaultUpdatingIcon').hide();
					$('makeDefaultUserMessage').show();
					setTimeout("$('makeDefaultUserMessage').fade();", 3000);
				},
				onFailure: function(transport) {
					$('makeDefaultUpdatingIcon').hide();
					$('makeDefaultUserError').show();
					setTimeout("$('makeDefaultUserError').fade();", 3000);
				}
			});
	    } else {
	    	// Old way of doing it, popup dialog is using this
	    	$('DashboardForm').submit();
	    }
	}
}