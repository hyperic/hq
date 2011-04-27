// NOTE: This copyright does *not* cover user programs that use HQ
// program services by normal system calls through the application
// program interfaces provided as part of the Hyperic Plug-in Development
// Kit or the Hyperic Client Development Kit - this is merely considered
// normal use of the program, and does *not* fall under the heading of
// "derived work".
// 
// Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
// This file is part of HQ.
// 
// HQ is free software; you can redistribute it and/or modify
// it under the terms version 2 of the GNU General Public License as
// published by the Free Software Foundation. This program is distributed
// in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// even the implied warranty of MERCHANTABILITY or FITNESS FOR A
// PARTICULAR PURPOSE. See the GNU General Public License for more
// details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA.


/*-- START pluginMgr.js --*/
	function resizeContentHeight(target,heightOffSet,minHeight){
		var windowCoords = hqDojo.window.getBox();
		var footerCoords = hqDojo.position(hqDojo.byId("footer"), true);
		var headerCoords = hqDojo.position(hqDojo.byId("header"), true);
		if(heightOffSet===undefined){
		    heightOffSet = 0;
		}
		if(minHeight===undefined){
			minHeight = 0;
		}
		var height = windowCoords.h-footerCoords.h-headerCoords.h-heightOffSet;
		if(height>minHeight){
			var heightString = height+"px";
			hqDojo.style(target,"height",heightString);
		}
	}
	function dateFormatShort(date){
		return hqDojo.date.locale.format(date,{
			selector: "date",
			datePattern: "hh:mm:ss aa"
		});
	}
	function refreshTime(target,animationNode,endColor){
		var now = new Date();
		target.innerHTML=dateFormatShort(now);
		
		if(animationNode!==undefined){
			if(endColor === undefined){
				endColor = "#EEEEEE";
			}
			var anim = [
				hqDojo.animateProperty({
					node:animationNode,
					properties:{
						backgroundColor:"yellow"},
					duration:600
				}),
				hqDojo.animateProperty({
					node:animationNode,
					properties:{
						backgroundColor:endColor},
					duration:600
				})
			];
			hqDojo.fx.chain(anim).play();
		}
	}
	function uncheckCheckboxes(target){
		hqDojo.forEach(target, function(e){
			e.checked=false;
		});	    
	}
	function checkFileType(filePath,validationMessageNode,invalidMessage){
		var ext = filePath.substr(filePath.length - 4);			
		if (ext !== ".jar" && ext !== ".xml") {
			showErrorMessage(validationMessageNode,invalidMessage);
			return false;
		}
		return true;
	}
	function showErrorMessage(validationMessageNode,invalidMessage){
		if(validationMessageNode!==undefined && invalidMessage!==undefined){
			hqDojo.byId(validationMessageNode).innerHTML = invalidMessage;
			var anim = [hqDojo.fadeIn({
							node: validationMessageNode,
							duration: 500
						}),
						hqDojo.fadeOut({
							node: validationMessageNode,
							delay: 5000,
							duration: 500
						})];
			hqDojo.fx.chain(anim).play();
		}
	}
	function getIdList(items){
		var deleteIdsString="";
		hqDojo.forEach(items, function(entry){
				deleteIdsString+=entry.value+",";
		});
		if(deleteIdsString.length>1){
			deleteIdsString = deleteIdsString.substr(0,deleteIdsString.length-1);
		}else{
			deleteIdsString="";
		}
		return deleteIdsString;	
	}
	
/*-- END pluginMgr.js --*/