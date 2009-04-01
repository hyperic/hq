// NOTE: this script depends on dojo libraries being loaded on the page
dojo11.require("dijit.dijit");
dojo11.require("dijit.Dialog");
			
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
		
		this._dialog = new dijit11.Dialog({
			id : this._config.dialogId,
			refocus : true,
			autofocus : false,
			title : this._config.title
		}, dojo11.byId(this._config.dialogId)); 
		
		this._injectForm();
		
		var self = this;

     	dojo11.byId(this._config.callerId).onclick = function() { 
     		self._dialog.show();        	 
        };
    	
    	dojo11.byId(this._config.cancelButtonId).onclick = function() {
    		self._dialog.hide();
    		self._resetDialog();
    	};
    	
    	dojo11.byId(this._config.addButtonId).disabled = true;
    	dojo11.byId(this._config.addButtonId).onclick = function() {
    		var xhrArgs = {
    			form : dojo11.byId(self._config.formId),
        		handleAs : "text",
        		load : function(data) {
    				if (self._config.onSuccess) {
    					self._config.onSuccess(data);
    				}
    				dojo11.style(self._config.progressId, "display", "none");
					dojo11.style(self._config.successMsgId, "display", "");
    				setTimeout(function() {
    					self._dialog.hide();
    					self._resetDialog();
    				}, 1500);
        		},
        		error : function(error){
        			if (self._config.onFailure) {
        				self._config.onFailure(data);
        			}
    				dojo11.style(self._config.progressId, "display", "none");
					dojo11.style(self._config.failureMsgId, "display", "");
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
    			xhrArgs.form = dojo11.byId(self._config.formId);
    		}
    		
    		if (self._config.passthroughParams) {
    			xhrArgs.content = self._config.passthroughParams();
    		}
			
			dojo11.style(self._config.progressId, "display", "");
			
			dojo11.xhrPost(xhrArgs);
    	};
    	
    	dojo11.byId(this._config.checkboxAllId).onclick = function() {
    		var checked = dojo11.byId(self._config.checkboxAllId).checked;
    		var count = 1;
    		var childCheckbox = dojo11.byId(self._config.checkboxIdPrefix + count);
    		
    		while (childCheckbox != null) {
    			childCheckbox.checked = checked;
    			self._updateAddButtonState(childCheckbox);
    			childCheckbox = dojo11.byId(self._config.checkboxIdPrefix + ++count);
    		}
    	};
   
   		var count = 1;
   		var childCheckbox = dojo11.byId(this._config.checkboxIdPrefix + count);
    		
   		while (childCheckbox != null) {
   			childCheckbox.onchange = function() {
   				self._updateAddButtonState(this);
   			};
   			
    		childCheckbox = dojo11.byId(this._config.checkboxIdPrefix + ++count);
    	}
	},
	
	_updateAddButtonState : function(checkbox) {
   		if (checkbox.checked) {
   			this._selectCount++;
   		} else {
   			this._selectCount--;
   		}
   		
   		if (this._selectCount == 0) {
   			dojo11.byId(this._config.addButtonId).disabled = true;
   		} else {
   			dojo11.byId(this._config.addButtonId).disabled = false;   				
   		}	
	},
	
	_injectForm : function() {
		var dialogContainer = dojo11.byId(this._config.dialogId);
		var form = document.createElement('form');
		
		form.id = form.name = this._config.formId = this._config.dialogId + "_Form";
		form.onSubmit = function() { return false; }
		
		while (dialogContainer.childNodes.length > 0) {
			form.appendChild(dialogContainer.childNodes[0]);
		}
		
		dialogContainer.appendChild(form);
	},
	
	_resetDialog : function() {
		dojo11.byId(this._config.checkboxAllId).checked = false;
		
		var count = 1;
		var childCheckbox = dojo11.byId(this._config.checkboxIdPrefix + count);
		
		while (childCheckbox != null) {	
			childCheckbox.checked = false; 
			childCheckbox = dojo11.byId(this._config.checkboxIdPrefix + ++count);
		}
		
		dojo11.style(this._config.successMsgId, "display", "none");
		dojo11.style(this._config.failureMsgId, "display", "none");
		dojo11.byId(this._config.addButtonId).disabled = true;
		
		this._selectCount = 0;
		
	}
}