// NOTE: requires prototype.js

function changeDashboard(formId){
	hqDojo.byId(formId).submit();
}

function selectDefaultDashboard(url){
    var selectDashboardEl = hqDojo.byId('dashSelect');
    var defaultDashboardEl = hqDojo.byId('defaultDashboard');
    
    if (defaultDashboardEl == null || selectDashboardEl == null) return;  // elements don't exist by ids given
	
    var index = selectDashboardEl.selectedIndex;
	var options = selectDashboardEl.options;
	var currDefault = defaultDashboardEl;
	
	if (index < 0) {
		hqDojo.style("dashboardSelectionErrorPanel", "display", "block");
		return;
	}
	
	var newDefault = options[index].value;
	
	if (newDefault != currDefault) {
		defaultDashboardEl.value = newDefault;
		
		hqDojo.style('makeDefaultBtn', "display", "none");
		
	    if (url != null && url != 'undefined') {
	    	hqDojo.xhrPost({
	    		url: url,
	    		content: {
	    			'defaultDashboard' : newDefault
	    		},
	    		load: function(response, args) {
	    			hqDojo.style('makeDefaultUpdatingIcon', "display", "none");
					hqDojo.style('makeDefaultUserMessage', "display", "");
					setTimeout(function() {
						hqDojo.fadeOut({
							node: 'makeDefaultUserMessage'
						}).play();
					}, 3000);
	    		},
	    		error: function(response, args) {
	    			hqDojo.style('makeDefaultUpdatingIcon', "display", "none");
					hqDojo.style('makeDefaultUserError', "display", "");
					setTimeout(function() {
						hqDojo.fadeOut({
							node: 'makeDefaultUserError'
						}).play();
					}, 3000);
	    		}
	    	});
	    } else {
	    	// Old way of doing it, popup dialog is using this
	    	hqDojo.byId('DashboardForm').submit();
	    }
	}
}