/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	ADxMenu.js - v4 (4.21) - helper script for Internet Explorer, up to version 6
	http://aplus.co.yu/adxmenu/
	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	(c) Copyright 2003, 2008, Aleksandar Vacic, aplus.co.yu
		This work is licensed under the CC Attribution 3.0 Unported license
		To view a copy of this license, visit http://creativecommons.org/licenses/by/3.0/ or
		send a letter to Creative Commons, 559 Nathan Abbott Way, Stanford, California 94305, USA
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/
//	adds support for WCH. if you need WCH, then load WCH.js BEFORE this file
if (typeof(WCH) == "undefined") WCH = null;

//	hide submenus
document.write('<style type="text/css" media="all">.mainMenu ul {visibility:hidden; position:absolute;}</style>');

var ADxMenu = function() {
	var self = this;

	this.Init = function() {
		self.adxmDL.load(Setup);
	};

	function Setup() {
		var aTmp2, i, j, oLI, aUL, aA;
		var aTmp = xGetElementsByClassName("adxm", document, "ul");
		for (i=0;i<aTmp.length;i++) {
			//	add (or remove) "adxie" class, to be used in cases when two different styles are required: one when Javascript is visible and one when it's not
			//	aTmp[i].className += " adxie";
			aTmp[i].className = aTmp[i].className.replace("adxie", "");
			//	setup drop-down goodness
			aTmp2 = aTmp[i].getElementsByTagName("li");
			for (j=0;j<aTmp2.length;j++) {
				oLI = aTmp2[j];
				aUL = oLI.getElementsByTagName("ul");
				//	if item has submenu, then make the item hoverable
				if (aUL && aUL.length) {
					oLI.UL = aUL[0];	//	direct submenu
					aA = oLI.getElementsByTagName("a");
					if (aA && aA.length)
						oLI.A = aA[0];	//	direct child link
					//	li:hover
					oLI.onmouseenter = function() {
						this.className += " adxmhover";
						this.UL.className += " adxmhoverUL";
						if (this.A) this.A.className += " adxmhoverA";
						if (WCH) WCH.Apply( this.UL, this, true );
					};
					//	li:blur
					oLI.onmouseleave = function() {
						this.className = this.className.replace(/adxmhover/,"");
						this.UL.className = this.UL.className.replace(/adxmhoverUL/,"");
						if (this.A) this.A.className = this.A.className.replace(/adxmhoverA/,"");
						if (WCH) WCH.Discard( this.UL, this );
					};
				}
			}	//for-li.submenu
		}	//for-ul.adxm
	};

	/*	xGetElementsByClassName()
		Returns an array of elements which are
		descendants of parentEle and have tagName and clsName.
		If parentEle is null or not present, document will be used.
		if tagName is null or not present, "*" will be used.
		credits: Mike Foster, cross-browser.com.
	*/
	function xGetElementsByClassName(clsName, parentEle, tagName) {
		var elements = null;
		var found = new Array();
		var re = new RegExp('\\b'+clsName+'\\b');
		if (!parentEle) parentEle = document;
		if (!tagName) tagName = '*';
		if (parentEle.getElementsByTagName) {elements = parentEle.getElementsByTagName(tagName);}
		else if (document.all) {elements = document.all.tags(tagName);}
		if (elements) {
			for (var i = 0; i < elements.length; ++i) {
				if (elements[i].className.search(re) != -1) {
					found[found.length] = elements[i];
				}
			}
		}
		return found;
	}
	
	/*	allows instant "window.onload" (DOM.onload) function execution. shortened version, just IE code
		credits: Dean Edwards/Matthias Miller/John Resig/Rob Chenny
		http://www.cherny.com/webdev/27/domloaded-updated-again
	*/
	this.adxmDL = {
		onload: [],
		loaded: function() {
			if (arguments.callee.done) return;
			arguments.callee.done = true;
			for (i = 0;i < self.adxmDL.onload.length;i++) self.adxmDL.onload[i]();
		},
		load: function(fireThis) {
			this.onload.push(fireThis);
			/*@cc_on @*/
			/*@if (@_win32)
			var proto = "src='javascript:void(0)'";
			if (location.protocol == "https:") proto = "src=//0";
			document.write("<scr"+"ipt id=__ADXMie_onload defer " + proto + "><\/scr"+"ipt>");
			var script = document.getElementById("__ADXMie_onload");
			script.onreadystatechange = function() {
			    if (this.readyState == "complete") {
			        self.adxmDL.loaded();
			    }
			};
			/*@end @*/
		}
	};
	
	return this;
}();
//	load the setup function
ADxMenu.Init();