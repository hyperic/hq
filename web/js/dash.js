function changeDashboard(formId){
    document.getElementById(formId).submit();
}

function selectDefaultDashboard(selectBox, url){
	var index = selectBox.selectedIndex;
	var options = selectBox.options;
	if(index >= 0){
	    alert(options[index].value);
	}else{
        dojo.byId("dashboardSelectionErrorPanel").style.display="block";
	    //validation error
	    //show message
	    alert("nothingSelected");
	}
}