function changeDashboard(formId){
    document.getElementById(formId).submit();
}

function selectDefaultDashboard(selectBoxId, formId){
    selectBox = document.getElementById(selectBoxId);
	var index = selectBox.selectedIndex;
	var options = selectBox.options;
	if(index >= 0){
	    document.getElementById('defaultDashboard').value = options[index].value;
	    document.getElementById(formId).submit();
	}else{
        document.getElementById("dashboardSelectionErrorPanel").style.display="block";
	}
}