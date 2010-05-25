// This plugin depends on the following jquery plugins:
// jquery.timers-1.2.js
// jquery.ba-bbq.1.2.1.js

(function($) {
	$.fn.treecontrol = function(options) {
		var opts = $.extend({}, $.fn.treecontrol.defaults, options);
		
		return this.each(function() {
			var $this = $(this);
			var savedState = _getState();
			
			if (!$.isEmptyObject(savedState)) {
				_refreshTree(opts, savedState, $this);
			} else if (opts.initialDataset) {
				_render(opts, $this, opts.initialDataset);
				
				var selectedId = opts.initialSelectId;
				
				if (!selectedId) {
					var state = $.deparam.querystring();
					
					if (state) {
						selectedId = state[_CONSTS.selectedNode];
					}
				}
				
				_makeSelection(selectedId);
			} else if (opts.url) {
				_getChildNodes(opts, $this);
			}
						
			if (opts.refreshInterval && opts.url) {
				$this.everyTime(opts.refreshInterval, function(i) {
					_refreshTree(opts, _getState(), $(this));
				});
			}
			
			$this.bind(_CONSTS.selectNodeEvt, {config: opts}, function(event, id) {
				var $target = $("span#" + _CONSTS.contentId(id), $(this));
				
				if ($target.size() > 0) {
					$target.parents("li." + _CLASSES.collapsed).children(" span." + _CLASSES.anchor).click();
					$target.children("a").click();
				} else {
					_pushState(_CONSTS.selectedNode, id);
					_refreshTree(event.data.config, _getState(), $(this));
				}
			});
		});
	};

	$.fn.treecontrol.defaults = {
		// url - user defined property. specifies the service endpoint
		// initialDataset - user defined property. specifies the initial dataset on page load
		// selectedCallback - user defined function. the data object is passed as a parameter
		// refreshInterval - user defined property. specifies the refresh interval.  If not set, no auto refresh occurs.
		// treeId - user defined property. specifies the unique id of the tree, this used to save state across page views.
	};
	
	$.fn.treecontrol.formatContent = function(config, data) {
		return $("<a></a>").text(data.text).attr("href", "#").click(function() {
			var $li = $(this).parents("li");
			
			$("ul." + _CLASSES.treecontrol + " span." + _CLASSES.selected).removeClass(_CLASSES.selected)
			                                                              .children("span." + _CLASSES.nolink)
			                                                              .remove();
			
			var $span = $li.children("span." + _CLASSES.nodecontent + ":first");

			$span.addClass(_CLASSES.selected);
			$span.append($("<span></span>").addClass(_CLASSES.nolink).text($li.data().text));
			
			if (config.selectedCallback && $.isFunction(config.selectedCallback)) {
				config.selectedCallback(data);
			}
			
			_pushState(_CONSTS.selectedNode, $li.data().id);
			
			return false;
		});
	};
	
	$.fn.treecontrol.updateContent = function(config, content, data) {
		var a = content.children("a");
		
		if (data.text != a.text()) {
			a.text(data.text);
		
			if (content.hasClass(_CLASSES.selected)) {
				var nolink = content.children("." + _CLASSES.nolink);
			
				nolink.text(data.text);
			}
		}
		
		if (content.hasClass(_CLASSES.selected) && 
		    config.selectedCallback && 
		    $.isFunction(config.selectedCallback)) {
			config.selectedCallback(data);
		}
	};
	
	function _makeSelection(id) {
		var contentNode = $("span#" + _CONSTS.contentId(id));
		var $li = contentNode.parent("li");
		
		$li.parents("li." + _CLASSES.collapsed).children("span." + _CLASSES.anchor).click();
		contentNode.children("a").click();
	}
	
	function _pushState(label, value) {
		var param = {};
		
		param[label] = value;
		
		$.bbq.pushState(param);	
	};
	
	function _getState() {
		return $.bbq.getState();
	}
	
	function _getOpenNodes() {
		var nodes = [];
			
		$("li." + _CLASSES.expandable + ":not(." + _CLASSES.collapsed + ")").each(function(i) {
			nodes[nodes.length] = $(this).data().id;
		});
		
		return nodes;
	};
	
	function _refreshTree(config, nodeState, $target) {
		_ajaxData(config, nodeState, $target);		
	};
	
	function _getChildNodes(config, $target) {
		var params = {};
		
		if ($target.is("li")) {
			params[_CONSTS.nodeId] = $target.data().id;
		}
		
		_ajaxData(config, params, $target);
	};
	
	function _ajaxData(config, params, $target) {
		$.ajax({
  			url: config.url,
  			data: params,
  			context: $target,
  			dataType: "json",
  			cache: false,
			success: function(data) {
				// Expecting a data object that looks like:
				// { payload: [{id: #, text: "", collapsed: true, extra: {...}, children: [{...}], deleted: true }] }
				if (data.payload && data.payload.length > 0) {
					var target = $(this);
					
					_render(config, target, data.payload);
					
					var savedState = _getState();
					var selectedId = savedState[_CONSTS.selectedNode];
					
					if (data.selectedId && data.selectedId != selectedId) {
						selectedId = data.selectedId;
					}
					
					_makeSelection(selectedId);
			    } 
  			},
  			error: function(xhr, errorText, exception) {
  				_log(errorText);
  				_log(exception);
  			}
		});
	};
	
	function _createNode(config, item) {
		// create li and add data
		var node = $("<li></li>").attr("id", _CONSTS.itemId(item.id))
			                     .data($.extend({}, item, {lastUpdated: new Date()}));
		// create anchor
		var spanAnchor = $("<span></span>").addClass(_CLASSES.anchor)
		                                   .html("&nbsp;")
		                                   .bind("click", { config: config }, _toggleBranch);
		
		node.append(spanAnchor);
		
		// create content section
		var spanContent = $("<span></span>").addClass(_CLASSES.nodecontent)
		                                    .attr("id", _CONSTS.contentId(item.id))
		                                    .append($.fn.treecontrol.formatContent(config, item));
		
		if (item.classes) {
			spanContent.addClass(item.classes);
		}
		
		node.append(spanContent);
				
		if (item.children) {
			node.addClass(_CLASSES.expandable);
				
			_render(config, node, item.children);

			if (item.collapsed) {
				node.addClass(_CLASSES.collapsed);
			}
		}
		
		return node;
	};
	
	function _updateNode(config, node, item) {
		node.data($.extend({}, item, {lastUpdated: new Date()}));
		
		var spanContent = node.children("." + _CLASSES.nodecontent);
		var selected = spanContent.is("." + _CLASSES.selected);
		
		spanContent.removeClass().addClass(_CLASSES.nodecontent);
	
		if (item.classes) {
			spanContent.addClass(item.classes);
		}
		
		if (selected) {
			spanContent.addClass(_CLASSES.selected);
		}
		
		$.fn.treecontrol.updateContent(config, spanContent, item);
		
		if (item.children) {
			node.addClass(_CLASSES.expandable);
			
			if (node.find("ul").children().size() == 0 && item.collapsed) {
				node.addClass(_CLASSES.collapsed);
			}
			
			_render(config, node, item.children);
		}
	};
	
	function _render(config, $container, nodes) {
		// determine if we have a root element, if so, use it, if not, create one
		var tree = ($container.children("ul").size() > 0) ? $container.children("ul:first") : $("<ul></ul>");
		
		// loop through the nodes
		for (var i = 0; i < nodes.length; i++) {
			var item = nodes[i];
			
			// does this node already exists?
			var existingNode = $("#" + _CONSTS.itemId(item.id));
			
			if (existingNode.size() > 0) {
				existingNode.addClass(_CLASSES.touched);
				
				// the node exists in the tree, so update it
				_updateNode(config, existingNode, item);

				// does it exist under the root element?
				if (!tree.has("#" + _CONSTS.itemId(item.id))) {
					if (i > 0 && $("#" + _CONSTS.itemId(data[i-1].id), tree)) {
						// insert it after the previous node
						existingNode.insertAfter($("#" + _CONSTS.itemId(nodes[i-1].id), tree));
					} else {
						// else, it's the first one, so append it
						tree.append(existingNode);
					}
				}
			} else {
				var newNode = _createNode(config, item);
				var touchedNodes = $("." + _CLASSES.touched + ":last", tree);
				
				if (touchedNodes.size() > 0) {
					// we need to insert after last touched node to preserve order
					touchedNodes.after(newNode);
				} else {
					// this is a new node, so create it
					tree.append(newNode);
				}
				
				newNode.addClass(_CLASSES.touched);
			}
		}
		
		// Remove untouched nodes, they've been deleted
		tree.children("li:not(." + _CLASSES.touched + ")").remove();
		tree.children("li." + _CLASSES.touched).removeClass(_CLASSES.touched);
		
		$container.append(tree);
		
		if (!$container.is("li")) {
			var root = $("ul:first", $container).addClass(_CLASSES.treecontrol);
			
			if (config.treeId) {
				root.attr("id", config.treeId);
			}
		}
	};

	function _toggleBranch(event) {
		event.stopPropagation();

		var config = event.data.config;
		var $this = $(this);
		var $li = $this.parent("li");
		var branch = $li.children("ul");
			
		if (branch) {
			var parent = $this.parent();

			if (parent.hasClass(_CLASSES.collapsed)) {
				parent.removeClass(_CLASSES.collapsed);
			} else {
				parent.addClass(_CLASSES.collapsed);
			}
				
			if (!$li.hasClass(_CLASSES.collapsed)) {
				var lastUpdatedMillis = $li.data().lastUpdated.getTime();
				var currentTimeMillis = (new Date()).getTime();
				var diff = currentTimeMillis - lastUpdatedMillis;
					
				if (config.url) {
					var refreshIntervalMilli = diff;
						
					if (config.refreshInterval) {
						refreshIntervalMilli = config.refreshInterval * 60 *1000;
					}
						
					if (branch.children("li").size() == 0 || refreshIntervalMilli < diff) {
						_getChildNodes(config, $li);
					}
				}
			} else if ($("ul>li>span." + _CLASSES.selected, $li).size() > 0) {
				$("span#" + _CONSTS.contentId($li.data().id) + " a").click();
			}
			
			_pushState(_CONSTS.openNodes, _getOpenNodes());
		}
	};

	function _log($message, $obj) {
		if (window.console && window.console.log) {
			window.console.log($message);

			if ($obj) {
				window.console.log($obj);
			}
		}
	};
	
	var _CLASSES = $.fn.treecontrol.CLASSES = {
		selected: "selected",
		collapsed: "collapsed",
		expandable: "expandable",
		anchor: "anchor",
		treecontrol: "treecontrol",
		nolink: "nolink",
		nodecontent: "nodecontent",
		touched: "touched"
	};
	
	var _CONSTS = {
		itemId: function(id) { return "_item" + id; },
		contentId: function(id) { return "_content" + id; },
		nodeId: "nodeId",
		openNodes: "on",
		selectedNode: "sn",
		selectNodeEvt: "selectNode"
	};
})(jQuery);