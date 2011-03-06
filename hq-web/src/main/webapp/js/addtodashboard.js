// NOTE: this script depends on dojo libraries being loaded on the page
dojo.require("dijit.Dialog");
			
var AddToDashboard = {
	_config : null, 
	//{
		// title : title of dialog,
		// dialogId : id of the container div,
		// callerId : id of the element that will show the dialog onclick,
		// checkboxAllId : id of parent checkbox,
		// checkboxIdPrefix : id prefix of child checkboxes,
		// url : url endpoint (optional, if form specified),
		// passthroughParams() : logic to create a map of additional parameters that should be sent along with the request (optional),
		// onSuccess() : additional logic to execute before hiding dialog. This is triggered on a successful response, a data parameter will also be available (optional),
		// onFailure() : additional logic to execute before hiding dialog. This is triggered on a successful response, a data parameter will also be available (optional),
	//}
	_selectCount : 0,
	_dialog : null,
	
	initDialog : function (config) {
		this._config = config;
		
		this._dialog = new dijit.Dialog({
			id : this._config.dialogId,
			refocus : true,
			autofocus : false,
			title : this._config.title
		}, dojo.byId(this._config.dialogId)); 
		
		this._injectForm();
		
		var self = this;

     	dojo.byId(this._config.callerId).onclick = function() { 
     		self._dialog.show();        	 
        };
    	
    	dojo.byId(this._config.cancelButtonId).onclick = function() {
    		self._dialog.hide();
    		self._resetDialog();
    	};
    	
    	this._updateAddButtonState(false);
    	
    	dojo.byId(this._config.addButtonId).onclick = function() {
    		var xhrArgs = {
    			form : dojo.byId(self._config.formId),
        		handleAs : "text",
        		load : function(data) {
    				if (self._config.onSuccess) {
    					self._config.onSuccess(data);
    				}
    				dojo.style(self._config.progressId, "display", "none");
					dojo.style(self._config.successMsgId, "display", "");
    				setTimeout(function() {
    					self._dialog.hide();
    					self._resetDialog();
    				}, 1500);
        		},
        		error : function(error){
        			if (self._config.onFailure) {
        				self._config.onFailure(data);
        			}
    				dojo.style(self._config.progressId, "display", "none");
					dojo.style(self._config.failureMsgId, "display", "");
    				setTimeout(function() {
    					self._dialog.hide();
    					self._resetDialog();
    				}, 1500);
        		}
      		}
    		
    		if (self._config.url) {
    			xhrArgs.url = self._config.url;
    		}
    		
    		if (self._config.formId) {
    			xhrArgs.form = dojo.byId(self._config.formId);
    		}
    		
    		if (self._config.passthroughParams) {
    			xhrArgs.content = self._config.passthroughParams();
    		}
			
			dojo.style(self._config.progressId, "display", "");
			
			dojo.xhrPost(xhrArgs);
    	};
    	
    	dojo.byId(this._config.checkboxAllId).onclick = function() {
    		var checked = dojo.byId(self._config.checkboxAllId).checked;
    		var count = 1;
    		var childCheckbox = dojo.byId(self._config.checkboxIdPrefix + count);
    		
    		while (childCheckbox != null) {
    			if (childCheckbox.checked != checked) {
    				childCheckbox.checked = checked;
    				self._updateAddButtonState(childCheckbox.checked);
    			}
    			
    			childCheckbox = dojo.byId(self._config.checkboxIdPrefix + ++count);
    		}
    	};
   
   		var count = 1;
   		var childCheckbox = dojo.byId(this._config.checkboxIdPrefix + count);
    		
   		while (childCheckbox != null) {
   			childCheckbox.onclick = function() {
   				self._updateAddButtonState(this.checked);
   			};
   			
    		childCheckbox = dojo.byId(this._config.checkboxIdPrefix + ++count);
    	}
	},
	
	_updateAddButtonState : function(isChecked) {
   		if (isChecked) {
   			this._selectCount++;
   		} else {
   			if (this._selectCount > 0) this._selectCount--;
   		}
   		
   		var changeClass;
   		
   		if (this._selectCount == 0) {
   			changeClass = dojo.addClass;
   			dojo.byId(this._config.addButtonId).disabled = true;
   		} else {
   			changeClass = dojo.removeClass;
   			dojo.byId(this._config.addButtonId).disabled = false;
   		}
   		
   		changeClass(this._config.addButtonId, "compactbuttoninactive");
   	},
	
	_injectForm : function() {
		var dialogContainer = dojo.byId(this._config.dialogId);
		var form = document.createElement('form');
		
		form.id = form.name = this._config.formId = this._config.dialogId + "_Form";
		form.onSubmit = function() { return false; }
		
		while (dialogContainer.childNodes.length > 0) {
			form.appendChild(dialogContainer.childNodes[0]);
		}
		
		dialogContainer.appendChild(form);
	},
	
	_resetDialog : function() {
		dojo.byId(this._config.checkboxAllId).checked = false;
		
		var count = 1;
		var childCheckbox = dojo.byId(this._config.checkboxIdPrefix + count);
		
		while (childCheckbox != null) {	
			childCheckbox.checked = false; 
			childCheckbox = dojo.byId(this._config.checkboxIdPrefix + ++count);
		}
		
		dojo.style(this._config.successMsgId, "display", "none");
		dojo.style(this._config.failureMsgId, "display", "none");
		dojo.byId(this._config.addButtonId).disabled = true;
		
		this._selectCount = 0;
		
		this._updateAddButtonState(false);
	}
}