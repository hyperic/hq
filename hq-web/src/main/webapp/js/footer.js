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

var conH;
var winH;
var footerH = 28;
var browserH = 88;

function setFoot() {
  var WindowSize = {
    width: window.innerWidth || (window.document.documentElement.clientWidth || window.document.body.clientWidth),
    height: window.innerHeight || (window.document.documentElement.clientHeight || window.document.body.clientHeight)
  };
  
  winH = WindowSize.height;
  conH = dojo11.coords("header", false).h + dojo11.coords("migContainer", false).h;
  
  var myHeight = winH - conH - footerH + browserH;
  
  if (myHeight > 60) {
  	var footerContent = dojo11.byId("footerContent");
  	
  	footerContent.style.marginTop = myHeight + "px";
  }
}