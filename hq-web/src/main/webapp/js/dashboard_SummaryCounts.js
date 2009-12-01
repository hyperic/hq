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


/*-- START dashboard_SummaryCounts.js --*/
function checkParent(e) {
  if (e.checked == false) {
    var parentGroup = e.getAttribute(classStr);
    var endPosition = parentGroup.indexOf("Parent");
    var group = parentGroup.substring(0, endPosition);
    
    var uList = e.form;
    var len = uList.elements.length;
    
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];

      if (e.getAttribute(classStr)==group)
        e.checked=false;
      if (e.getAttribute(classStr)==(group + "CheckAll"))
        e.checked=false;
    }
  }
}

function checkChild(e) {
  var group = e.getAttribute(classStr);
  var uList = e.form;
  var len = uList.elements.length;
  
  if (e.checked == true) {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];
      if (e.getAttribute(classStr)==(group + "Parent"))
        e.checked=true;
    }
  }
    
  else {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];
      if (e.getAttribute(classStr)==(group + "CheckAll"))
        e.checked=false;
    }
  }
}

function ToggleAll(e, group) {
  var uList = e.form;
  var len = uList.elements.length;
  
  if (e.checked == true) {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];

      if (e.getAttribute(classStr)== group)
        e.checked=true;
      if (e.getAttribute(classStr)==(group + "Parent"))
        e.checked=true;
    }
  }
  
  else {
    for (var i = 0; i < len; i++) {
      var e = uList.elements[i];
      if (e.getAttribute(classStr)== group)
        e.checked=false;
    }
  }
}
/*-- END dashboard_SummaryCounts.js --*/

