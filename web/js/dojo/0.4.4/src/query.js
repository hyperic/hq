/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/



dojo.provide("dojo.query");
dojo.require("dojo.experimental");
dojo.experimental("dojo.query");
(function () {
	var d = dojo;
	var h = d.render.html;
	var _getIndexes = function (q) {
		return [q.indexOf("#"), q.indexOf("."), q.indexOf("["), q.indexOf(":")];
	};
	var _lowestFromIndex = function (query, index) {
		var ql = query.length;
		var i = _getIndexes(query);
		var end = ql;
		for (var x = index; x < i.length; x++) {
			if (i[x] >= 0) {
				if (i[x] < end) {
					end = i[x];
				}
			}
		}
		return (end < 0) ? ql : end;
	};
	var getIdEnd = function (query) {
		return _lowestFromIndex(query, 1);
	};
	var getId = function (query) {
		var i = _getIndexes(query);
		if (i[0] != -1) {
			return query.substring(i[0] + 1, getIdEnd(query));
		} else {
			return "";
		}
	};
	var getTagNameEnd = function (query) {
		var i = _getIndexes(query);
		if ((i[0] == 0) || (i[1] == 0)) {
			return 0;
		} else {
			return _lowestFromIndex(query, 0);
		}
	};
	var getTagName = function (query) {
		var tagNameEnd = getTagNameEnd(query);
		return ((tagNameEnd > 0) ? query.substr(0, tagNameEnd).toLowerCase() : "*");
	};
	var smallest = function (arr) {
		var ret = -1;
		for (var x = 0; x < arr.length; x++) {
			var ta = arr[x];
			if (ta >= 0) {
				if ((ta > ret) || (ret == -1)) {
					ret = ta;
				}
			}
		}
		return ret;
	};
	var getClassName = function (query) {
		var i = _getIndexes(query);
		if (-1 == i[1]) {
			return "";
		}
		var di = i[1] + 1;
		var othersStart = smallest(i.slice(2));
		if (di < othersStart) {
			return query.substring(di, othersStart);
		} else {
			if (-1 == othersStart) {
				return query.substr(di);
			} else {
				return "";
			}
		}
	};
	var xPathAttrs = [{key:"|=", match:function (attr, value) {
		return "[contains(concat(' ',@" + attr + ",' '), ' " + value + "-')]";
	}}, {key:"~=", match:function (attr, value) {
		return "[contains(concat(' ',@" + attr + ",' '), ' " + value + " ')]";
	}}, {key:"^=", match:function (attr, value) {
		return "[starts-with(@" + attr + ", '" + value + "')]";
	}}, {key:"*=", match:function (attr, value) {
		return "[contains(@" + attr + ", '" + value + "')]";
	}}, {key:"$=", match:function (attr, value) {
		return "[substring(@" + attr + ", string-length(@" + attr + ")-" + (value.length - 1) + ")='" + value + "']";
	}}, {key:"!=", match:function (attr, value) {
		return "[not(@" + attr + "='" + value + "')]";
	}}, {key:"=", match:function (attr, value) {
		return "[@" + attr + "='" + value + "']";
	}}];
	var strip = function (val) {
		var re = /^\s+|\s+$/g;
		return val.replace(re, "");
	};
	var handleAttrs = function (attrList, query, getDefault, handleMatch) {
		var matcher;
		var i = _getIndexes(query);
		if (i[2] >= 0) {
			var lBktIdx = query.indexOf("]", i[2]);
			var condition = query.substring(i[2] + 1, lBktIdx);
			while (condition && condition.length) {
				if (condition.charAt(0) == "@") {
					condition = condition.slice(1);
				}
				matcher = null;
				for (var x = 0; x < attrList.length; x++) {
					var ta = attrList[x];
					var tci = condition.indexOf(ta.key);
					if (tci >= 0) {
						var attr = condition.substring(0, tci);
						var value = condition.substring(tci + ta.key.length);
						if ((value.charAt(0) == "\"") || (value.charAt(0) == "'")) {
							value = value.substring(1, value.length - 1);
						}
						matcher = ta.match(strip(attr), strip(value));
						break;
					}
				}
				if ((!matcher) && (condition.length)) {
					matcher = getDefault(condition);
				}
				if (matcher) {
					handleMatch(matcher);
				}
				condition = null;
				var nbktIdx = query.indexOf("[", lBktIdx);
				if (0 <= nbktIdx) {
					lBktIdx = query.indexOf("]", nbktIdx);
					if (0 <= lBktIdx) {
						condition = query.substring(nbktIdx + 1, lBktIdx);
					}
				}
			}
		}
	};
	var buildPath = function (query) {
		var xpath = ".";
		var qparts = query.split(" ");
		while (qparts.length) {
			var tqp = qparts.shift();
			var prefix;
			if (tqp == ">") {
				prefix = "/";
				tqp = qparts.shift();
			} else {
				prefix = "//";
			}
			var tagName = getTagName(tqp);
			xpath += prefix + tagName;
			var id = getId(tqp);
			if (id.length) {
				xpath += "[@id='" + id + "'][1]";
			}
			var cn = getClassName(tqp);
			if (cn.length) {
				var padding = " ";
				if (cn.charAt(cn.length - 1) == "*") {
					padding = "";
					cn = cn.substr(0, cn.length - 1);
				}
				xpath += "[contains(concat(' ',@class,' '), ' " + cn + padding + "')]";
			}
			handleAttrs(xPathAttrs, tqp, function (condition) {
				return "[@" + condition + "]";
			}, function (matcher) {
				xpath += matcher;
			});
		}
		return xpath;
	};
	var _xpathFuncCache = {};
	var getXPathFunc = function (path) {
		if (_xpathFuncCache[path]) {
			return _xpathFuncCache[path];
		}
		var doc = d.doc();
		var xpath = buildPath(path);
		var tf = function (parent) {
			var ret = [];
			var xpathResult;
			try {
				xpathResult = doc.evaluate(xpath, parent, null, XPathResult.ANY_TYPE, null);
			}
			catch (e) {
				dojo.debug("failure in exprssion:", xpath, "under:", parent);
				dojo.debug(e);
			}
			var result = xpathResult.iterateNext();
			while (result) {
				ret.push(result);
				result = xpathResult.iterateNext();
			}
			return ret;
		};
		return _xpathFuncCache[path] = tf;
	};
	var _filtersCache = {};
	var _simpleFiltersCache = {};
	var agree = function (first, second) {
		if (!first) {
			return second;
		}
		if (!second) {
			return first;
		}
		return function () {
			return first.apply(window, arguments) && second.apply(window, arguments);
		};
	};
	var _filterDown = function (element, queryParts, matchArr, idx) {
		var nidx = idx + 1;
		var isFinal = (queryParts.length == nidx);
		var tqp = queryParts[idx];
		if (tqp == ">") {
			var ecn = element.childNodes;
			if (!ecn.length) {
				return;
			}
			nidx++;
			var isFinal = (queryParts.length == nidx);
			var tf = getFilterFunc(queryParts[idx + 1]);
			for (var x = ecn.length - 1, te; x >= 0, te = ecn[x]; x--) {
				if (tf(te)) {
					if (isFinal) {
						matchArr.push(te);
					} else {
						_filterDown(te, queryParts, matchArr, nidx);
					}
				}
				if (x == 0) {
					break;
				}
			}
		}
		var candidates = getElementsFunc(tqp)(element);
		if (isFinal) {
			while (candidates.length) {
				matchArr.push(candidates.shift());
			}
		} else {
			while (candidates.length) {
				_filterDown(candidates.shift(), queryParts, matchArr, nidx);
			}
		}
	};
	var filterDown = function (elements, queryParts) {
		ret = [];
		var x = elements.length - 1, te;
		while (te = elements[x--]) {
			_filterDown(te, queryParts, ret, 0);
		}
		return ret;
	};
	var getFilterFunc = function (query) {
		if (_filtersCache[query]) {
			return _filtersCache[query];
		}
		var ff = null;
		var tagName = getTagName(query);
		if (tagName != "*") {
			ff = agree(ff, function (elem) {
				var isTn = ((elem.nodeType == 1) && (tagName == elem.tagName.toLowerCase()));
				return isTn;
			});
		}
		var idComponent = getId(query);
		if (idComponent.length) {
			ff = agree(ff, function (elem) {
				return ((elem.nodeType == 1) && (elem.id == idComponent));
			});
		}
		if (Math.max.apply(this, _getIndexes(query).slice(1)) >= 0) {
			ff = agree(ff, getSimpleFilterFunc(query));
		}
		return _filtersCache[query] = ff;
	};
	var getNodeIndex = function (node) {
		var pn = node.parentNode;
		var pnc = pn.childNodes;
		var nidx = -1;
		var child = pn.firstChild;
		if (!child) {
			return nidx;
		}
		var ci = node["__cachedIndex"];
		var cl = pn["__cachedLength"];
		if (((typeof cl == "number") && (cl != pnc.length)) || (typeof ci != "number")) {
			pn["__cachedLength"] = pnc.length;
			var idx = 1;
			do {
				if (child === node) {
					nidx = idx;
				}
				if (child.nodeType == 1) {
					child["__cachedIndex"] = idx;
					idx++;
				}
				child = child.nextSibling;
			} while (child);
		} else {
			nidx = ci;
		}
		return nidx;
	};
	var firedCount = 0;
	var _getAttr = function (elem, attr) {
		var blank = "";
		if (attr == "class") {
			return elem.className || blank;
		}
		if (attr == "for") {
			return elem.htmlFor || blank;
		}
		return elem.getAttribute(attr, 2) || blank;
	};
	var attrs = [{key:"|=", match:function (attr, value) {
		var valueDash = " " + value + "-";
		return function (elem) {
			var ea = " " + (elem.getAttribute(attr, 2) || "");
			return ((ea == value) || (ea.indexOf(valueDash) == 0));
		};
	}}, {key:"^=", match:function (attr, value) {
		return function (elem) {
			return (_getAttr(elem, attr).indexOf(value) == 0);
		};
	}}, {key:"*=", match:function (attr, value) {
		return function (elem) {
			return (_getAttr(elem, attr).indexOf(value) >= 0);
		};
	}}, {key:"~=", match:function (attr, value) {
		var tval = " " + value + " ";
		return function (elem) {
			var ea = " " + _getAttr(elem, attr) + " ";
			return (ea.indexOf(tval) >= 0);
		};
	}}, {key:"$=", match:function (attr, value) {
		var tval = " " + value;
		return function (elem) {
			var ea = " " + _getAttr(elem, attr);
			return (ea.lastIndexOf(value) == (ea.length - value.length));
		};
	}}, {key:"!=", match:function (attr, value) {
		return function (elem) {
			return (_getAttr(elem, attr) != value);
		};
	}}, {key:"=", match:function (attr, value) {
		return function (elem) {
			return (_getAttr(elem, attr) == value);
		};
	}}];
	var pseudos = [{key:"first-child", match:function (name, condition) {
		return function (elem) {
			if (elem.nodeType != 1) {
				return false;
			}
			var fc = elem.previousSibling;
			while (fc && (fc.nodeType != 1)) {
				fc = fc.previousSibling;
			}
			return (!fc);
		};
	}}, {key:"last-child", match:function (name, condition) {
		return function (elem) {
			if (elem.nodeType != 1) {
				return false;
			}
			var nc = elem.nextSibling;
			while (nc && (nc.nodeType != 1)) {
				nc = nc.nextSibling;
			}
			return (!nc);
		};
	}}, {key:"empty", match:function (name, condition) {
		return function (elem) {
			var cn = elem.childNodes;
			var cnl = elem.childNodes.length;
			for (var x = cnl - 1; x >= 0; x--) {
				var nt = cn[x].nodeType;
				if ((nt == 1) || (nt == 3)) {
					return false;
				}
			}
			return true;
		};
	}}, {key:"contains", match:function (name, condition) {
		return function (elem) {
			return (elem.innerHTML.indexOf(condition) >= 0);
		};
	}}, {key:"not", match:function (name, condition) {
		var ntf = getFilterFunc(condition);
		return function (elem) {
			return (!ntf(elem));
		};
	}}, {key:"nth-child", match:function (name, condition) {
		var pi = parseInt;
		if (condition == "odd") {
			return function (elem) {
				return (((getNodeIndex(elem)) % 2) == 1);
			};
		} else {
			if ((condition == "2n") || (condition == "even")) {
				return function (elem) {
					return ((getNodeIndex(elem) % 2) == 0);
				};
			} else {
				if (condition.indexOf("0n+") == 0) {
					var ncount = pi(condition.substr(3));
					return function (elem) {
						return (elem.parentNode.childNodes[ncount - 1] === elem);
					};
				} else {
					if ((condition.indexOf("n+") > 0) && (condition.length > 3)) {
						var tparts = condition.split("n+", 2);
						var pred = pi(tparts[0]);
						var idx = pi(tparts[1]);
						return function (elem) {
							return ((getNodeIndex(elem) % pred) == idx);
						};
					} else {
						if (condition.indexOf("n") == -1) {
							var ncount = pi(condition);
							return function (elem) {
								return (getNodeIndex(elem) == ncount);
							};
						}
					}
				}
			}
		}
	}}];
	var getSimpleFilterFunc = function (query) {
		var fcHit = (_simpleFiltersCache[query] || _filtersCache[query]);
		if (fcHit) {
			return fcHit;
		}
		var ff = null;
		var i = _getIndexes(query);
		if (i[0] >= 0) {
			var tn = getTagName(query);
			if (tn != "*") {
				ff = agree(ff, function (elem) {
					return (elem.tagName.toLowerCase() == tn);
				});
			}
		}
		var matcher;
		var className = getClassName(query);
		if (className.length) {
			var isWildcard = className.charAt(className.length - 1) == "*";
			if (isWildcard) {
				className = className.substr(0, className.length - 1);
			}
			var re = new RegExp("(?:^|\\s)" + className + (isWildcard ? ".*" : "") + "(?:\\s|$)");
			ff = agree(ff, function (elem) {
				return re.test(elem.className);
			});
		}
		if (i[3] >= 0) {
			var pseudoName = query.substr(i[3] + 1);
			var condition = "";
			var obi = pseudoName.indexOf("(");
			var cbi = pseudoName.lastIndexOf(")");
			if ((0 <= obi) && (0 <= cbi) && (cbi > obi)) {
				condition = pseudoName.substring(obi + 1, cbi);
				pseudoName = pseudoName.substr(0, obi);
			}
			matcher = null;
			for (var x = 0; x < pseudos.length; x++) {
				var ta = pseudos[x];
				if (ta.key == pseudoName) {
					matcher = ta.match(pseudoName, condition);
					break;
				}
			}
			if (matcher) {
				ff = agree(ff, matcher);
			}
		}
		var defaultGetter = (d.isIE) ? function (cond) {
			return function (elem) {
				return elem[cond];
			};
		} : function (cond) {
			return function (elem) {
				return elem.hasAttribute(cond);
			};
		};
		handleAttrs(attrs, query, defaultGetter, function (tmatcher) {
			ff = agree(ff, tmatcher);
		});
		if (!ff) {
			ff = function () {
				return true;
			};
		}
		return _simpleFiltersCache[query] = ff;
	};
	var isTagOnly = function (query) {
		return (Math.max.apply(this, _getIndexes(query)) == -1);
	};
	var _getElementsFuncCache = {};
	var getElementsFunc = function (query, root) {
		var fHit = _getElementsFuncCache[query];
		if (fHit) {
			return fHit;
		}
		var i = _getIndexes(query);
		var id = getId(query);
		if (i[0] == 0) {
			return _getElementsFuncCache[query] = function (root) {
				return [d.byId(id)];
			};
		}
		var filterFunc = getSimpleFilterFunc(query);
		var retFunc;
		if (i[0] >= 0) {
			retFunc = function (root) {
				var te = d.byId(id);
				if (filterFunc(te)) {
					return [te];
				}
			};
		} else {
			var tret;
			var tn = getTagName(query);
			if (isTagOnly(query)) {
				retFunc = function (root) {
					var ret = [];
					var te, x = 0, tret = root.getElementsByTagName(tn);
					while (te = tret[x++]) {
						ret.push(te);
					}
					return ret;
				};
			} else {
				retFunc = function (root) {
					var ret = [];
					var te, x = 0, tret = root.getElementsByTagName(tn);
					while (te = tret[x++]) {
						if (filterFunc(te)) {
							ret.push(te);
						}
					}
					return ret;
				};
			}
		}
		return _getElementsFuncCache[query] = retFunc;
	};
	var _partsCache = {};
	var _queryFuncCache = {};
	var getStepQueryFunc = function (query) {
		if (0 > query.indexOf(" ")) {
			return getElementsFunc(query);
		}
		var sqf = function (root) {
			var qparts = query.split(" ");
			var candidates = getElementsFunc(qparts.shift())(root);
			return filterDown(candidates, qparts);
		};
		return sqf;
	};
	var _getQueryFunc = ((document["evaluate"] && !h.safari) ? function (query) {
		var qparts = query.split(" ");
		if ((document["evaluate"]) && (query.indexOf(":") == -1) && ((true))) {
			var gtIdx = query.indexOf(">");
			if (((qparts.length > 2) && (query.indexOf(">") == -1)) || (qparts.length > 3) || (query.indexOf("[") >= 0) || ((1 == qparts.length) && (0 <= query.indexOf(".")))) {
				return getXPathFunc(query);
			}
		}
		return getStepQueryFunc(query);
	} : getStepQueryFunc);
	var getQueryFunc = function (query) {
		if (_queryFuncCache[query]) {
			return _queryFuncCache[query];
		}
		if (0 > query.indexOf(",")) {
			return _queryFuncCache[query] = _getQueryFunc(query);
		} else {
			var parts = query.split(", ");
			var tf = function (root) {
				var pindex = 0;
				var ret = [];
				var tp;
				while (tp = parts[pindex++]) {
					ret = ret.concat(_getQueryFunc(tp, tp.indexOf(" "))(root));
				}
				return ret;
			};
			return _queryFuncCache[query] = tf;
		}
	};
	var _zipIdx = 0;
	var _zip = function (arr) {
		var ret = [];
		if (!arr) {
			return ret;
		}
		if (arr[0]) {
			ret.push(arr[0]);
		}
		if (arr.length < 2) {
			return ret;
		}
		_zipIdx++;
		arr[0]["_zipIdx"] = _zipIdx;
		for (var x = 1, te; te = arr[x]; x++) {
			if (arr[x]["_zipIdx"] != _zipIdx) {
				ret.push(te);
			}
			te["_zipIdx"] = _zipIdx;
		}
		return ret;
	};
	d.query = function (query, root) {
		if (typeof query != "string") {
			return new Array(query);
		}
		if (typeof root == "string") {
			root = dojo.byId(root);
		}
		return _zip(getQueryFunc(query)(root || document));
	};
})();

